package com.jettch.sisgev.evidences.dto;

import com.jettch.sisgev.evidences.entity.InspectionEvidence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        UUID clientUuid,
        UUID inspectionId,
        String status,
        String fileUrl,
        String thumbnailUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal gpsAccuracyMeters,
        LocalDateTime takenAt,
        LocalDateTime uploadedAt,
        String fieldNote,
        LocalDateTime createdAt
) {
    public static EvidenceResponse from(InspectionEvidence e) {
        return new EvidenceResponse(
                e.getId(),
                e.getClientUuid(),
                e.getInspectionId(),
                e.getStatus().name(),
                e.getFileUrl(),
                e.getThumbnailUrl(),
                e.getLatitude(),
                e.getLongitude(),
                e.getGpsAccuracyMeters(),
                e.getTakenAt(),
                e.getUploadedAt(),
                e.getFieldNote(),
                e.getCreatedAt()
        );
    }
}
