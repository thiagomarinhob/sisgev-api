package com.jettch.sisgev.municipalities.controller;

import com.jettch.sisgev.municipalities.dto.MunicipalityRequest;
import com.jettch.sisgev.municipalities.dto.MunicipalityResponse;
import com.jettch.sisgev.municipalities.service.MunicipalityService;
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
@RequestMapping("/api/v1/municipalities")
@RequiredArgsConstructor
public class MunicipalityController {

    private final MunicipalityService service;

    @GetMapping
    public PagedResponse<MunicipalityResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    public ResponseEntity<MunicipalityResponse> create(@Valid @RequestBody MunicipalityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public MunicipalityResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public MunicipalityResponse update(@PathVariable UUID id, @Valid @RequestBody MunicipalityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
