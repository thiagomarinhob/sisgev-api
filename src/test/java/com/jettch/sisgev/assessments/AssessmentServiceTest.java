package com.jettch.sisgev.assessments;

import com.jettch.sisgev.assessments.dto.AssessmentSummary;
import com.jettch.sisgev.assessments.entity.RoadAssessment;
import com.jettch.sisgev.assessments.repository.AssessmentRepository;
import com.jettch.sisgev.assessments.service.AssessmentService;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock AssessmentRepository assessmentRepo;
    @Mock RoadSegmentRepository segmentRepo;
    @Mock CurrentUserService currentUser;
    @InjectMocks AssessmentService service;

    // SEG-D-04: lista com assessments existentes → PagedResponse com elementos corretos
    @Test
    void listBySegment_returnsPagedResults_whenAssessmentsExist() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        RoadAssessment a1 = assessmentWith(segmentId, municipalityId);
        RoadAssessment a2 = assessmentWith(segmentId, municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(assessmentRepo.findByRoadSegmentIdOrderByAssessedAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2), pageable, 2));

        PagedResponse<AssessmentSummary> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    // SEG-D-05: lista vazia → PagedResponse com totalElements = 0 e content vazio
    @Test
    void listBySegment_returnsEmptyPage_whenNoAssessments() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(assessmentRepo.findByRoadSegmentIdOrderByAssessedAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PagedResponse<AssessmentSummary> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.content()).isEmpty();
    }

    // SEG-D-06: trecho não existe → lança BusinessException(404, SEGMENT_NOT_FOUND)
    @Test
    void listBySegment_throws404_whenSegmentNotFound() {
        UUID segmentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBySegment(segmentId, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("SEGMENT_NOT_FOUND");
                });
    }

    // SEG-D-07: município incompatível → lança BusinessException(403, FORBIDDEN_MUNICIPALITY)
    @Test
    void listBySegment_throws403_whenWrongMunicipality() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        doThrow(new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_MUNICIPALITY", "Acesso negado ao município"))
                .when(currentUser).assertCanAccessMunicipality(municipalityId);

        assertThatThrownBy(() -> service.listBySegment(segmentId, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FORBIDDEN_MUNICIPALITY");
                });
    }

    // ---- helper ----

    private RoadAssessment assessmentWith(UUID segmentId, UUID municipalityId) {
        RoadAssessment a = new RoadAssessment();
        a.setRoadSegmentId(segmentId);
        a.setMunicipalityId(municipalityId);
        a.setCondition(RoadCondition.GOOD);
        a.setSeverityScore(1);
        a.setSource("FIELD");
        a.setAssessedBy(UUID.randomUUID());
        a.setAssessedAt(LocalDateTime.now());
        return a;
    }
}
