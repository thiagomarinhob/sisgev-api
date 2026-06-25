package com.jettch.sisgev.inspections.controller;

import com.jettch.sisgev.inspections.dto.InspectionResponse;
import com.jettch.sisgev.inspections.dto.InspectionSyncRequest;
import com.jettch.sisgev.inspections.dto.InspectionSyncResult;
import com.jettch.sisgev.inspections.service.InspectionService;
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
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService service;

    @GetMapping
    public PagedResponse<InspectionResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionSyncRequest request) {
        return toResponse(service.create(request));
    }

    @PostMapping("/sync")
    public ResponseEntity<InspectionResponse> sync(@Valid @RequestBody InspectionSyncRequest request) {
        return toResponse(service.sync(request));
    }

    @GetMapping("/{id}")
    public InspectionResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/finish")
    public InspectionResponse finish(@PathVariable UUID id) {
        return service.finish(id);
    }

    /** 201 quando criou; 200 quando já existia (idempotência, RN-025). */
    private ResponseEntity<InspectionResponse> toResponse(InspectionSyncResult result) {
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.inspection());
    }
}
