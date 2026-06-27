package com.jettch.sisgev.roadsegments.service;

import com.jettch.sisgev.roads.repository.RoadRepository;
import com.jettch.sisgev.roadsegments.dto.LengthOverrideRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentCreateRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentResponse;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentUpdateRequest;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gerencia trechos de estradas vicinais — unidade oficial de cálculo (RN-002).
 * Refs: RF-SEG-001/002, RN-001 (multi-tenancy), RN-019 (soft delete), RN-027 (ST_Length).
 */
@Service
@RequiredArgsConstructor
public class RoadSegmentService {

    private final RoadSegmentRepository repository;
    private final RoadRepository roadRepository;
    private final CurrentUserService currentUser;

    @Transactional
    public RoadSegmentResponse create(RoadSegmentCreateRequest req) {
        User user = currentUser.getCurrentUser();
        UUID municipalityId = user.getMunicipalityId();
        if (municipalityId == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município não pode gerenciar trechos");
        }

        // AC-010: valida que a estrada pertence ao município do usuário (RN-001)
        roadRepository.findByIdAndMunicipalityIdAndDeletedAtIsNullAndActiveTrue(req.roadId(), municipalityId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "ROAD_NOT_FOUND", "Estrada não encontrada no município"));

        RoadSegment segment = new RoadSegment();
        segment.setMunicipalityId(municipalityId);
        segment.setRoadId(req.roadId());
        segment.setName(req.name().trim());
        segment.setSegmentOrder(req.segmentOrder());
        segment.setGeometry(req.geometry().toJts());
        segment.setCurrentCondition(RoadCondition.UNKNOWN); // AC-003: sempre UNKNOWN na criação
        segment.setPublished(Boolean.TRUE.equals(req.published()));

        RoadSegment saved = repository.saveAndFlush(segment);
        repository.recalculateLengthMeters(saved.getId()); // AC-009: ST_Length via PostGIS (RN-027)

        return RoadSegmentResponse.from(findActive(saved.getId()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RoadSegmentResponse> list(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<RoadSegment> page;
        if (user.isSuperAdmin()) {
            page = repository.findByDeletedAtIsNull(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = repository.findByMunicipalityIdAndDeletedAtIsNull(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(RoadSegmentResponse::from));
    }

    @Transactional(readOnly = true)
    public RoadSegmentResponse get(UUID id) {
        RoadSegment segment = findActive(id);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId()); // AC-008
        return RoadSegmentResponse.from(segment);
    }

    @Transactional
    public RoadSegmentResponse update(UUID id, RoadSegmentUpdateRequest req) {
        RoadSegment segment = findActive(id);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());

        segment.setName(req.name().trim());
        segment.setSegmentOrder(req.segmentOrder());
        segment.setGeometry(req.geometry().toJts());
        if (req.published() != null) {
            segment.setPublished(req.published());
        }

        repository.saveAndFlush(segment);
        repository.recalculateLengthMeters(id); // AC-006 / AC-009: recalcula ao atualizar geometria

        return RoadSegmentResponse.from(findActive(id));
    }

    @Transactional
    public RoadSegmentResponse overrideLength(UUID id, LengthOverrideRequest req) {
        RoadSegment segment = findActive(id);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId()); // LEN-08

        User user = currentUser.getCurrentUser();
        if (user.getRole() != UserRole.SUPER_ADMIN && user.getRole() != UserRole.ADMIN_OPERACIONAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_LENGTH_OVERRIDE",
                    "Override manual de comprimento restrito a ADMIN_OPERACIONAL ou SUPER_ADMIN");
        }

        if (req.lengthMeters().signum() <= 0) { // LEN-03
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_LENGTH",
                    "length_meters deve ser maior que zero");
        }

        repository.overrideLength(id, req.lengthMeters(), req.justification()); // LEN-01

        return RoadSegmentResponse.from(findActive(id)); // LEN-06
    }

    @Transactional
    public void delete(UUID id) {
        RoadSegment segment = findActive(id);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());

        // AC-007 / RN-019: exclusão lógica
        segment.setActive(false);
        segment.setDeletedAt(LocalDateTime.now());
        repository.save(segment);
    }

    private RoadSegment findActive(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
    }
}
