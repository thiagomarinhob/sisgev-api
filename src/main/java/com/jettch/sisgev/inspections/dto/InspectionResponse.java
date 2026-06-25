package com.jettch.sisgev.inspections.dto;

import com.jettch.sisgev.inspections.entity.Inspection;

import java.time.LocalDateTime;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        UUID clientUuid,
        UUID municipalityId,
        UUID fieldAgentId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime syncedAt,
        String notes,
        LocalDateTime createdAt
) {
    public static InspectionResponse from(Inspection i) {
        return new InspectionResponse(
                i.getId(),
                i.getClientUuid(),
                i.getMunicipalityId(),
                i.getFieldAgentId(),
                i.getStatus().name(),
                i.getStartedAt(),
                i.getFinishedAt(),
                i.getSyncedAt(),
                i.getNotes(),
                i.getCreatedAt()
        );
    }
}
