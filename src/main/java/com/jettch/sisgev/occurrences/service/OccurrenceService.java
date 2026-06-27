package com.jettch.sisgev.occurrences.service;

import com.jettch.sisgev.occurrences.dto.OccurrenceCreateRequest;
import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.dto.OccurrenceUpdateRequest;
import com.jettch.sisgev.occurrences.entity.Occurrence;
import com.jettch.sisgev.occurrences.enums.OccurrenceStatus;
import com.jettch.sisgev.occurrences.repository.OccurrenceRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BE-22 — Ocorrências: CRUD + mudança de status. Resolver não apaga o registro (RN-029).
 */
@Service
@RequiredArgsConstructor
public class OccurrenceService {

    private final OccurrenceRepository occurrenceRepository;
    private final RoadSegmentRepository segmentRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public PagedResponse<OccurrenceSummary> listBySegment(UUID segmentId, Pageable pageable) {
        RoadSegment segment = loadSegment(segmentId);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());
        return PagedResponse.from(
                occurrenceRepository.findByRoadSegmentIdOrderByOpenedAtDesc(segmentId, pageable)
                        .map(OccurrenceSummary::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OccurrenceSummary> list(OccurrenceStatus status, Pageable pageable) {
        User user = currentUser.getCurrentUser();
        String st = status != null ? status.name() : null;
        Page<Occurrence> page;
        if (user.isSuperAdmin()) {
            page = st != null ? occurrenceRepository.findByStatus(st, pageable)
                    : occurrenceRepository.findAll(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = st != null ? occurrenceRepository.findByMunicipalityIdAndStatus(user.getMunicipalityId(), st, pageable)
                    : occurrenceRepository.findByMunicipalityId(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(OccurrenceSummary::from));
    }

    @Transactional(readOnly = true)
    public OccurrenceSummary get(UUID id) {
        Occurrence occurrence = findById(id);
        currentUser.assertCanAccessMunicipality(occurrence.getMunicipalityId());
        return OccurrenceSummary.from(occurrence);
    }

    @Transactional
    public OccurrenceSummary create(OccurrenceCreateRequest request) {
        RoadSegment segment = loadSegment(request.roadSegmentId());
        User user = currentUser.getCurrentUser();
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());

        Occurrence occurrence = new Occurrence();
        occurrence.setMunicipalityId(segment.getMunicipalityId());
        occurrence.setRoadSegmentId(segment.getId());
        occurrence.setEvidenceId(request.evidenceId());
        occurrence.setProblemType(request.problemType().name());
        occurrence.setStatus(OccurrenceStatus.OPEN.name());
        occurrence.setSeverityScore(request.severityScore());
        occurrence.setDescription(request.description());
        occurrence.setOpenedBy(user.getId());
        occurrence.setOpenedAt(LocalDateTime.now());
        return OccurrenceSummary.from(occurrenceRepository.save(occurrence));
    }

    @Transactional
    public OccurrenceSummary update(UUID id, OccurrenceUpdateRequest request) {
        Occurrence occurrence = findById(id);
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(occurrence.getMunicipalityId());
        occurrence.setProblemType(request.problemType().name());
        occurrence.setSeverityScore(request.severityScore());
        occurrence.setDescription(request.description());
        return OccurrenceSummary.from(occurrenceRepository.save(occurrence));
    }

    @Transactional
    public OccurrenceSummary changeStatus(UUID id, OccurrenceStatus status) {
        Occurrence occurrence = findById(id);
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(occurrence.getMunicipalityId());
        occurrence.setStatus(status.name());
        occurrence.setResolvedAt(status == OccurrenceStatus.RESOLVED ? LocalDateTime.now() : null);
        return OccurrenceSummary.from(occurrenceRepository.save(occurrence));
    }

    private Occurrence findById(UUID id) {
        return occurrenceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "OCCURRENCE_NOT_FOUND", "Ocorrência não encontrada"));
    }

    private RoadSegment loadSegment(UUID segmentId) {
        return segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
    }
}
