package com.jettch.sisgev.occurrences.dto;

import com.jettch.sisgev.occurrences.enums.OccurrenceStatus;
import jakarta.validation.constraints.NotNull;

/** Mudança de status da ocorrência (BE-22 · RF-OCC-002). */
public record OccurrenceStatusRequest(
        @NotNull(message = "Status é obrigatório")
        OccurrenceStatus status
) {
}
