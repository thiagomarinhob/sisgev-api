package com.jettch.sisgev.roadsegments.controller;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
