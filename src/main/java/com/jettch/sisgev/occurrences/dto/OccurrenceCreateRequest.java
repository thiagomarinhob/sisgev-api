package com.jettch.sisgev.occurrences.dto;

import com.jettch.sisgev.occurrences.enums.ProblemType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Abertura de ocorrência num trecho (BE-22 · RF-OCC-001). */
public record OccurrenceCreateRequest(

        @NotNull(message = "roadSegmentId é obrigatório")
        UUID roadSegmentId,

        @NotNull(message = "Tipo de problema é obrigatório")
        ProblemType problemType,

        @NotNull(message = "Severidade é obrigatória")
        @Min(value = 0, message = "Severidade mínima é 0")
        @Max(value = 100, message = "Severidade máxima é 100")
        Integer severityScore,

        String description,

        UUID evidenceId
) {
}
