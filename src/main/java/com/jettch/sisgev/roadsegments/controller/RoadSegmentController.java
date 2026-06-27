package com.jettch.sisgev.roadsegments.controller;

import com.jettch.sisgev.assessments.dto.AssessmentSummary;
import com.jettch.sisgev.assessments.service.AssessmentService;
import com.jettch.sisgev.evidences.dto.EvidenceResponse;
import com.jettch.sisgev.evidences.service.EvidenceService;
import com.jettch.sisgev.maintenance.dto.MaintenanceEventSummary;
import com.jettch.sisgev.maintenance.service.MaintenanceService;
import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.service.OccurrenceService;
import com.jettch.sisgev.roadsegments.dto.LengthOverrideRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentCreateRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentResponse;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentUpdateRequest;
import com.jettch.sisgev.roadsegments.service.RoadSegmentService;
import com.jettch.sisgev.shared.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/road-segments")
@RequiredArgsConstructor
public class RoadSegmentController {

    private final RoadSegmentService service;
    private final AssessmentService assessmentService;
    private final EvidenceService evidenceService;
    private final OccurrenceService occurrenceService;
    private final MaintenanceService maintenanceService;

    @GetMapping
    public PagedResponse<RoadSegmentResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    public ResponseEntity<RoadSegmentResponse> create(@Valid @RequestBody RoadSegmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public RoadSegmentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public RoadSegmentResponse update(@PathVariable UUID id,
                                      @Valid @RequestBody RoadSegmentUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/length")
    public RoadSegmentResponse overrideLength(@PathVariable UUID id,
                                              @Valid @RequestBody LengthOverrideRequest request) {
        return service.overrideLength(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public PagedResponse<AssessmentSummary> history(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return assessmentService.listBySegment(id, pageable);
    }

    @GetMapping("/{id}/evidences")
    public PagedResponse<EvidenceResponse> evidences(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return evidenceService.listBySegment(id, pageable);
    }

    @GetMapping("/{id}/occurrences")
    public PagedResponse<OccurrenceSummary> occurrences(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return occurrenceService.listBySegment(id, pageable);
    }

    @GetMapping("/{id}/maintenance-events")
    public PagedResponse<MaintenanceEventSummary> maintenanceEvents(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return maintenanceService.listBySegment(id, pageable);
    }
}
