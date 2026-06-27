package com.jettch.sisgev.occurrences.service;

import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.repository.OccurrenceRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OccurrenceService {

    private final OccurrenceRepository occurrenceRepository;
    private final RoadSegmentRepository segmentRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public PagedResponse<OccurrenceSummary> listBySegment(UUID segmentId, Pageable pageable) {
        RoadSegment segment = segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());
        return PagedResponse.from(
                occurrenceRepository.findByRoadSegmentIdOrderByOpenedAtDesc(segmentId, pageable)
                        .map(OccurrenceSummary::from));
    }
}
