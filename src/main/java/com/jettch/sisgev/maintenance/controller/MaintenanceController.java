package com.jettch.sisgev.maintenance.controller;

import com.jettch.sisgev.maintenance.dto.MaintenanceCreateRequest;
import com.jettch.sisgev.maintenance.dto.MaintenanceEventSummary;
import com.jettch.sisgev.maintenance.dto.MaintenanceFinishRequest;
import com.jettch.sisgev.maintenance.dto.MaintenanceUpdateRequest;
import com.jettch.sisgev.maintenance.enums.MaintenanceStatus;
import com.jettch.sisgev.maintenance.service.MaintenanceService;
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
@RequestMapping("/api/v1/maintenance-events")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService service;

    @GetMapping
    public PagedResponse<MaintenanceEventSummary> list(
            @RequestParam(value = "status", required = false) MaintenanceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(status, pageable);
    }

    @PostMapping
    public ResponseEntity<MaintenanceEventSummary> create(@Valid @RequestBody MaintenanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public MaintenanceEventSummary get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public MaintenanceEventSummary update(@PathVariable UUID id, @Valid @RequestBody MaintenanceUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/start")
    public MaintenanceEventSummary start(@PathVariable UUID id) {
        return service.start(id);
    }

    @PatchMapping("/{id}/finish")
    public MaintenanceEventSummary finish(@PathVariable UUID id, @Valid @RequestBody MaintenanceFinishRequest request) {
        return service.finish(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public MaintenanceEventSummary cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
