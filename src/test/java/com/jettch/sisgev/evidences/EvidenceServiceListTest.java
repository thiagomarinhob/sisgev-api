package com.jettch.sisgev.evidences;

import com.jettch.sisgev.evidences.dto.EvidenceResponse;
import com.jettch.sisgev.evidences.entity.InspectionEvidence;
import com.jettch.sisgev.evidences.enums.EvidenceStatus;
import com.jettch.sisgev.evidences.repository.InspectionEvidenceRepository;
import com.jettch.sisgev.evidences.service.EvidenceService;
import com.jettch.sisgev.inspections.repository.InspectionRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.storage.StorageService;
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

import java.math.BigDecimal;
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
class EvidenceServiceListTest {

    @Mock InspectionEvidenceRepository evidenceRepository;
    @Mock InspectionRepository inspectionRepository;
    @Mock RoadSegmentRepository segmentRepository;
    @Mock StorageService storageService;
    @Mock CurrentUserService currentUser;
    @InjectMocks EvidenceService service;

    // SEG-D-08: lista com evidências confirmadas → PagedResponse com totalElements == 2
    @Test
    void listBySegment_returnsPagedResults_whenEvidencesExist() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        InspectionEvidence e1 = evidenceWith(segmentId, municipalityId);
        InspectionEvidence e2 = evidenceWith(segmentId, municipalityId);

        when(segmentRepository.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(evidenceRepository.findByConfirmedRoadSegmentIdOrderByTakenAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

        PagedResponse<EvidenceResponse> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    // SEG-D-09: lista vazia → PagedResponse com totalElements = 0 e content vazio
    @Test
    void listBySegment_returnsEmptyPage_whenNoEvidences() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        when(segmentRepository.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(evidenceRepository.findByConfirmedRoadSegmentIdOrderByTakenAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PagedResponse<EvidenceResponse> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.content()).isEmpty();
    }

    // SEG-D-10: trecho não existe → lança BusinessException(404, SEGMENT_NOT_FOUND)
    @Test
    void listBySegment_throws404_whenSegmentNotFound() {
        UUID segmentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(segmentRepository.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listBySegment(segmentId, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("SEGMENT_NOT_FOUND");
                });
    }

    // SEG-D-11: município incompatível → lança BusinessException(403, FORBIDDEN_MUNICIPALITY)
    @Test
    void listBySegment_throws403_whenWrongMunicipality() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        when(segmentRepository.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
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

    private InspectionEvidence evidenceWith(UUID confirmedSegmentId, UUID municipalityId) {
        InspectionEvidence ev = new InspectionEvidence();
        ev.setConfirmedRoadSegmentId(confirmedSegmentId);
        ev.setMunicipalityId(municipalityId);
        ev.setInspectionId(UUID.randomUUID());
        ev.setFieldAgentId(UUID.randomUUID());
        ev.setClientUuid(UUID.randomUUID());
        ev.setFileUrl("https://storage/evidence.jpg");
        ev.setStorageKey("evidences/key.jpg");
        ev.setLatitude(new BigDecimal("-5.1234567"));
        ev.setLongitude(new BigDecimal("-39.1234567"));
        ev.setTakenAt(LocalDateTime.now());
        ev.setUploadedAt(LocalDateTime.now());
        ev.setStatus(EvidenceStatus.PENDING_REVIEW);
        return ev;
    }
}
