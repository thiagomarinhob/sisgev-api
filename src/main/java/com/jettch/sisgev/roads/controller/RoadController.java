package com.jettch.sisgev.roads.controller;

import com.jettch.sisgev.roads.dto.RoadGeoJsonImportRequest;
import com.jettch.sisgev.roads.dto.RoadRequest;
import com.jettch.sisgev.roads.dto.RoadResponse;
import com.jettch.sisgev.roads.service.RoadService;
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
@RequestMapping("/api/v1/roads")
@RequiredArgsConstructor
public class RoadController {

    private final RoadService service;

    @GetMapping
    public PagedResponse<RoadResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    public ResponseEntity<RoadResponse> create(@Valid @RequestBody RoadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public RoadResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public RoadResponse update(@PathVariable UUID id, @Valid @RequestBody RoadRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-geojson")
    public RoadResponse importGeoJson(@Valid @RequestBody RoadGeoJsonImportRequest request) {
        return service.importGeoJson(request);
    }
}
