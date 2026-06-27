package com.jettch.sisgev.occurrences.dto;

import com.jettch.sisgev.occurrences.entity.Occurrence;
import java.time.LocalDateTime;
import java.util.UUID;

public record OccurrenceSummary(
        UUID id,
        String problemType,
        String status,
        int severityScore,
        String description,
        UUID openedBy,
        LocalDateTime openedAt,
        LocalDateTime resolvedAt,
        UUID evidenceId
) {
    public static OccurrenceSummary from(Occurrence o) {
        return new OccurrenceSummary(
                o.getId(),
                o.getProblemType(),
                o.getStatus(),
                o.getSeverityScore(),
                o.getDescription(),
                o.getOpenedBy(),
                o.getOpenedAt(),
                o.getResolvedAt(),
                o.getEvidenceId()
        );
    }
}
