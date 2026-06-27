package com.jettch.sisgev.evidences.service;

import com.jettch.sisgev.evidences.dto.EvidenceResponse;
import com.jettch.sisgev.evidences.dto.EvidenceUploadCommand;
import com.jettch.sisgev.evidences.dto.EvidenceUploadResult;
import com.jettch.sisgev.evidences.entity.InspectionEvidence;
import com.jettch.sisgev.evidences.enums.EvidenceStatus;
import com.jettch.sisgev.evidences.repository.InspectionEvidenceRepository;
import com.jettch.sisgev.evidences.support.ImageThumbnailer;
import com.jettch.sisgev.inspections.entity.Inspection;
import com.jettch.sisgev.inspections.repository.InspectionRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.storage.StorageService;
import com.jettch.sisgev.storage.StoredFile;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * BE-14 — Upload de evidência: salva o binário no storage, gera miniatura e
 * persiste apenas metadados/URLs. Idempotente por (field_agent_id, client_uuid).
 * Refs: RF-EVD-001, RN-023/024/025, §11.3.
 */
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private static final int THUMB_MAX_DIMENSION = 320;

    private final InspectionEvidenceRepository evidenceRepository;
    private final InspectionRepository inspectionRepository;
    private final RoadSegmentRepository segmentRepository;
    private final StorageService storageService;
    private final CurrentUserService currentUser;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /** Raio (m) para sugerir o trecho mais próximo da foto (BE-15, configurável). */
    @Value("${geo.near-segment-radius-meters:100}")
    private double nearSegmentRadiusMeters;

    @Transactional
    public EvidenceUploadResult upload(EvidenceUploadCommand cmd) {
        validate(cmd);

        User user = currentUser.getCurrentUser();
        if (user.getMunicipalityId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município não pode enviar evidências");
        }

        // Idempotência: mesma foto (client_uuid) não é reenviada.
        var existing = evidenceRepository.findByFieldAgentIdAndClientUuid(user.getId(), cmd.clientUuid());
        if (existing.isPresent()) {
            return new EvidenceUploadResult(EvidenceResponse.from(existing.get()), false);
        }

        // A vistoria precisa ter sido sincronizada antes das fotos.
        Inspection inspection = inspectionRepository
                .findByFieldAgentIdAndClientUuid(user.getId(), cmd.inspectionClientUuid())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "INSPECTION_NOT_FOUND", "Vistoria não encontrada — sincronize a vistoria antes das fotos"));

        byte[] bytes = readBytes(cmd.file());
        String contentType = cmd.file().getContentType();
        String baseKey = "evidences/" + user.getMunicipalityId() + "/" + cmd.clientUuid();

        StoredFile original = storageService.upload(baseKey + extensionFor(contentType), bytes, contentType);
        String thumbnailUrl = uploadThumbnail(baseKey, bytes);

        InspectionEvidence evidence = new InspectionEvidence();
        evidence.setMunicipalityId(user.getMunicipalityId());
        evidence.setInspectionId(inspection.getId());
        evidence.setFieldAgentId(user.getId());
        evidence.setClientUuid(cmd.clientUuid());
        evidence.setFileUrl(original.url());
        evidence.setThumbnailUrl(thumbnailUrl);
        evidence.setStorageKey(original.key());
        evidence.setMimeType(contentType);
        evidence.setFileSizeBytes((long) bytes.length);
        evidence.setFileHash(sha256Hex(bytes));
        evidence.setLatitude(cmd.latitude());
        evidence.setLongitude(cmd.longitude());
        evidence.setLocation(toPoint(cmd.longitude(), cmd.latitude()));
        // BE-15: sugere o trecho mais próximo (admin confirma depois, RN-015).
        evidence.setSuggestedRoadSegmentId(segmentRepository.findNearestSegmentId(
                user.getMunicipalityId(),
                cmd.longitude().doubleValue(),
                cmd.latitude().doubleValue(),
                nearSegmentRadiusMeters));
        evidence.setGpsAccuracyMeters(cmd.gpsAccuracyMeters());
        evidence.setTakenAt(cmd.takenAt());
        evidence.setUploadedAt(LocalDateTime.now());
        evidence.setStatus(EvidenceStatus.PENDING_REVIEW);
        evidence.setFieldNote(cmd.fieldNote());

        try {
            return new EvidenceUploadResult(
                    EvidenceResponse.from(evidenceRepository.saveAndFlush(evidence)), true);
        } catch (DataIntegrityViolationException race) {
            InspectionEvidence winner = evidenceRepository
                    .findByFieldAgentIdAndClientUuid(user.getId(), cmd.clientUuid())
                    .orElseThrow(() -> race);
            return new EvidenceUploadResult(EvidenceResponse.from(winner), false);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<EvidenceResponse> listBySegment(UUID segmentId, Pageable pageable) {
        RoadSegment segment = segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());
        return PagedResponse.from(
                evidenceRepository.findByConfirmedRoadSegmentIdOrderByTakenAtDesc(segmentId, pageable)
                        .map(EvidenceResponse::from));
    }

    // ---------------------------------------------------------------------
    // BE-16 — Revisão de evidências (approve / reject / mark-duplicated / associate)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<EvidenceResponse> list(EvidenceStatus status, Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<InspectionEvidence> page;
        if (user.isSuperAdmin()) {
            page = (status != null)
                    ? evidenceRepository.findByStatus(status, pageable)
                    : evidenceRepository.findAll(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = (status != null)
                    ? evidenceRepository.findByMunicipalityIdAndStatus(user.getMunicipalityId(), status, pageable)
                    : evidenceRepository.findByMunicipalityId(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(EvidenceResponse::from));
    }

    @Transactional(readOnly = true)
    public EvidenceResponse get(UUID id) {
        InspectionEvidence evidence = findById(id);
        currentUser.assertCanAccessMunicipality(evidence.getMunicipalityId());
        return EvidenceResponse.from(evidence);
    }

    @Transactional
    public EvidenceResponse approve(UUID id) {
        return review(id, e -> e.setStatus(EvidenceStatus.APPROVED));
    }

    @Transactional
    public EvidenceResponse reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Motivo da rejeição é obrigatório");
        }
        return review(id, e -> {
            e.setStatus(EvidenceStatus.REJECTED);
            e.setAdminNote(reason);
        });
    }

    @Transactional
    public EvidenceResponse markDuplicated(UUID id) {
        return review(id, e -> e.setStatus(EvidenceStatus.DUPLICATED));
    }

    @Transactional
    public EvidenceResponse associateSegment(UUID id, UUID segmentId) {
        RoadSegment segment = segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
        return review(id, e -> {
            if (!e.getMunicipalityId().equals(segment.getMunicipalityId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "SEGMENT_OTHER_MUNICIPALITY", "Trecho pertence a outro município");
            }
            e.setConfirmedRoadSegmentId(segment.getId());
        });
    }

    private EvidenceResponse review(UUID id, Consumer<InspectionEvidence> mutation) {
        InspectionEvidence evidence = findById(id);
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(evidence.getMunicipalityId());
        mutation.accept(evidence);
        evidence.setReviewedAt(LocalDateTime.now());
        evidence.setReviewedBy(currentUser.getCurrentUser().getId());
        return EvidenceResponse.from(evidenceRepository.save(evidence));
    }

    private InspectionEvidence findById(UUID id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "EVIDENCE_NOT_FOUND", "Evidência não encontrada"));
    }

    private String uploadThumbnail(String baseKey, byte[] bytes) {
        try {
            byte[] thumb = ImageThumbnailer.toJpegThumbnail(bytes, THUMB_MAX_DIMENSION);
            if (thumb == null) {
                return null; // não é imagem decodificável
            }
            return storageService.upload(baseKey + "_thumb.jpg", thumb, "image/jpeg").url();
        } catch (IOException e) {
            return null;
        }
    }

    private void validate(EvidenceUploadCommand cmd) {
        if (cmd.file() == null || cmd.file().isEmpty()) {
            throw badRequest("Arquivo (file) é obrigatório");
        }
        if (cmd.clientUuid() == null) {
            throw badRequest("clientUuid é obrigatório");
        }
        if (cmd.inspectionClientUuid() == null) {
            throw badRequest("inspectionClientUuid é obrigatório");
        }
        if (cmd.latitude() == null || cmd.longitude() == null) {
            throw badRequest("latitude e longitude são obrigatórias");
        }
        if (cmd.takenAt() == null) {
            throw badRequest("takenAt é obrigatório");
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private byte[] readBytes(org.springframework.web.multipart.MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FILE_READ_ERROR", "Não foi possível ler o arquivo");
        }
    }

    private Point toPoint(BigDecimal longitude, BigDecimal latitude) {
        Point point = geometryFactory.createPoint(
                new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
        point.setSRID(4326);
        return point;
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            default -> "";
        };
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception e) {
            return null;
        }
    }
}
