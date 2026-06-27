package com.jettch.sisgev.assessments.controller;

import com.jettch.sisgev.assessments.dto.AssessmentCreateRequest;
import com.jettch.sisgev.assessments.dto.AssessmentSummary;
import com.jettch.sisgev.assessments.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService service;

    @PostMapping
    public ResponseEntity<AssessmentSummary> create(@Valid @RequestBody AssessmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public AssessmentSummary get(@PathVariable UUID id) {
        return service.get(id);
    }
}
