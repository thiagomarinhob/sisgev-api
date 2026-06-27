package com.jettch.sisgev.occurrences.controller;

import com.jettch.sisgev.occurrences.dto.OccurrenceCreateRequest;
import com.jettch.sisgev.occurrences.dto.OccurrenceStatusRequest;
import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.dto.OccurrenceUpdateRequest;
import com.jettch.sisgev.occurrences.enums.OccurrenceStatus;
import com.jettch.sisgev.occurrences.service.OccurrenceService;
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
@RequestMapping("/api/v1/occurrences")
@RequiredArgsConstructor
public class OccurrenceController {

    private final OccurrenceService service;

    @GetMapping
    public PagedResponse<OccurrenceSummary> list(
            @RequestParam(value = "status", required = false) OccurrenceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(status, pageable);
    }

    @PostMapping
    public ResponseEntity<OccurrenceSummary> create(@Valid @RequestBody OccurrenceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public OccurrenceSummary get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public OccurrenceSummary update(@PathVariable UUID id, @Valid @RequestBody OccurrenceUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public OccurrenceSummary changeStatus(@PathVariable UUID id, @Valid @RequestBody OccurrenceStatusRequest request) {
        return service.changeStatus(id, request.status());
    }
}
