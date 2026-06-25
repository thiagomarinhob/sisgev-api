package com.jettch.sisgev.evidences.controller;

import com.jettch.sisgev.evidences.dto.EvidenceResponse;
import com.jettch.sisgev.evidences.dto.EvidenceUploadCommand;
import com.jettch.sisgev.evidences.dto.EvidenceUploadResult;
import com.jettch.sisgev.evidences.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidences")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService service;

    /**
     * Upload multipart de evidência. Campos exigidos pelo mobile (§10.4):
     * file, clientUuid, inspectionClientUuid, latitude, longitude, gpsAccuracyMeters, takenAt, fieldNote.
     * 201 quando criado; 200 quando já existia (idempotência por client_uuid).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceResponse> upload(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "clientUuid", required = false) UUID clientUuid,
            @RequestParam(value = "inspectionClientUuid", required = false) UUID inspectionClientUuid,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam(value = "gpsAccuracyMeters", required = false) BigDecimal gpsAccuracyMeters,
            @RequestParam(value = "takenAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime takenAt,
            @RequestParam(value = "fieldNote", required = false) String fieldNote) {

        EvidenceUploadCommand cmd = new EvidenceUploadCommand(
                file, clientUuid, inspectionClientUuid, latitude, longitude, gpsAccuracyMeters,
                takenAt != null ? takenAt.toLocalDateTime() : null, fieldNote);

        EvidenceUploadResult result = service.upload(cmd);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.evidence());
    }
}
