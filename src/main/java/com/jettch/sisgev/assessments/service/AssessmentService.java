package com.jettch.sisgev.assessments.service;

import com.jettch.sisgev.assessments.dto.AssessmentCreateRequest;
import com.jettch.sisgev.assessments.dto.AssessmentSummary;
import com.jettch.sisgev.assessments.entity.RoadAssessment;
import com.jettch.sisgev.assessments.repository.AssessmentRepository;
import com.jettch.sisgev.evidences.entity.InspectionEvidence;
import com.jettch.sisgev.evidences.repository.InspectionEvidenceRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final RoadSegmentRepository segmentRepository;
    private final InspectionEvidenceRepository evidenceRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public PagedResponse<AssessmentSummary> listBySegment(UUID segmentId, Pageable pageable) {
        RoadSegment segment = loadSegment(segmentId);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());
        return PagedResponse.from(
                assessmentRepository.findByRoadSegmentIdOrderByAssessedAtDesc(segmentId, pageable)
                        .map(AssessmentSummary::from));
    }

    @Transactional(readOnly = true)
    public AssessmentSummary get(UUID id) {
        RoadAssessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "ASSESSMENT_NOT_FOUND", "Avaliação não encontrada"));
        currentUser.assertCanAccessMunicipality(assessment.getMunicipalityId());
        return AssessmentSummary.from(assessment);
    }

    /**
     * BE-17 — Cria a avaliação oficial do trecho (tabela append-only, RN-003) e atualiza a
     * condição atual + last_assessment_at do trecho na mesma transação (RN-004).
     */
    @Transactional
    public AssessmentSummary create(AssessmentCreateRequest request) {
        RoadSegment segment = loadSegment(request.roadSegmentId());
        User user = currentUser.getCurrentUser();
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());

        if (request.evidenceId() != null) {
            InspectionEvidence evidence = evidenceRepository.findById(request.evidenceId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_FOUND", "Evidência base não encontrada"));
            if (!evidence.getMunicipalityId().equals(segment.getMunicipalityId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "EVIDENCE_OTHER_MUNICIPALITY", "Evidência pertence a outro município");
            }
        }

        LocalDateTime assessedAt = request.assessedAt() != null ? request.assessedAt() : LocalDateTime.now();

        RoadAssessment assessment = new RoadAssessment();
        assessment.setMunicipalityId(segment.getMunicipalityId());
        assessment.setRoadSegmentId(segment.getId());
        assessment.setEvidenceId(request.evidenceId());
        assessment.setCondition(request.condition());
        assessment.setSeverityScore(request.severityScore());
        assessment.setSource("MANUAL");
        assessment.setNotes(request.notes());
        assessment.setAssessedBy(user.getId());
        assessment.setAssessedAt(assessedAt);
        RoadAssessment saved = assessmentRepository.save(assessment);

        segment.setCurrentCondition(request.condition());
        segment.setLastAssessmentAt(assessedAt);
        segmentRepository.save(segment);

        return AssessmentSummary.from(saved);
    }

    private RoadSegment loadSegment(UUID segmentId) {
        return segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
    }
}
