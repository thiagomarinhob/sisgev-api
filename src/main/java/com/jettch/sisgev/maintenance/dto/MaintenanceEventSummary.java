package com.jettch.sisgev.maintenance.dto;

import com.jettch.sisgev.maintenance.entity.MaintenanceEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MaintenanceEventSummary(
        UUID id,
        String type,
        String status,
        LocalDate plannedStartDate,
        LocalDate actualStartDate,
        LocalDate finishedDate,
        BigDecimal repairedLengthMeters,
        String notes,
        UUID createdBy,
        LocalDateTime createdAt,
        UUID occurrenceId
) {
    public static MaintenanceEventSummary from(MaintenanceEvent m) {
        return new MaintenanceEventSummary(
                m.getId(),
                m.getType(),
                m.getStatus(),
                m.getPlannedStartDate(),
                m.getActualStartDate(),
                m.getFinishedDate(),
                m.getRepairedLengthMeters(),
                m.getNotes(),
                m.getCreatedBy(),
                m.getCreatedAt(),
                m.getOccurrenceId()
        );
    }
}
