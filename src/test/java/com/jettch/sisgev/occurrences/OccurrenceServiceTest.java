package com.jettch.sisgev.occurrences;

import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.entity.Occurrence;
import com.jettch.sisgev.occurrences.repository.OccurrenceRepository;
import com.jettch.sisgev.occurrences.service.OccurrenceService;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
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
class OccurrenceServiceTest {

    @Mock OccurrenceRepository occurrenceRepo;
    @Mock RoadSegmentRepository segmentRepo;
    @Mock CurrentUserService currentUser;
    @InjectMocks OccurrenceService service;

    // SEG-D-12: lista com ocorrências existentes → PagedResponse com totalElements == 2
    @Test
    void listBySegment_returnsPagedResults_whenOccurrencesExist() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        Occurrence o1 = occurrenceWith(segmentId, municipalityId);
        Occurrence o2 = occurrenceWith(segmentId, municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(occurrenceRepo.findByRoadSegmentIdOrderByOpenedAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(o1, o2), pageable, 2));

        PagedResponse<OccurrenceSummary> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    // SEG-D-13: lista vazia → PagedResponse com totalElements = 0 e content vazio
    @Test
    void listBySegment_returnsEmptyPage_whenNoOccurrences() {
        UUID segmentId = UUID.randomUUID();
        UUID municipalityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        RoadSegment segment = new RoadSegment();
        segment.setId(segmentId);
        segment.setMunicipalityId(municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segmentId)).thenReturn(Optional.of(segment));
        when(occurrenceRepo.findByRoadSegmentIdOrderByOpenedAtDesc(eq(segmentId), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        PagedResponse<OccurrenceSummary> result = service.listBySegment(segmentId, pageable);

        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.content()).isEmpty();
    }

    // SEG-D-14: trecho não existe → lança BusinessException(404, SEGMENT_NOT_FOUND)
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

    // SEG-D-15: município incompatível → lança BusinessException(403, FORBIDDEN_MUNICIPALITY)
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

    private Occurrence occurrenceWith(UUID segmentId, UUID municipalityId) {
        Occurrence o = new Occurrence();
        o.setRoadSegmentId(segmentId);
        o.setMunicipalityId(municipalityId);
        o.setProblemType("POTHOLE");
        o.setStatus("OPEN");
        o.setSeverityScore(3);
        o.setOpenedBy(UUID.randomUUID());
        o.setOpenedAt(LocalDateTime.now());
        return o;
    }
}
