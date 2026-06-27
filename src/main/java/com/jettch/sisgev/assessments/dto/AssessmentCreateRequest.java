package com.jettch.sisgev.assessments.dto;

import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Criação de avaliação oficial de um trecho (BE-17).
 * {@code evidenceId} e {@code assessedAt} são opcionais (default: agora). Severity 0–100 (§20.6).
 */
public record AssessmentCreateRequest(

        @NotNull(message = "roadSegmentId é obrigatório")
        UUID roadSegmentId,

        UUID evidenceId,

        @NotNull(message = "Condição é obrigatória")
        RoadCondition condition,

        @NotNull(message = "Severidade é obrigatória")
        @Min(value = 0, message = "Severidade mínima é 0")
        @Max(value = 100, message = "Severidade máxima é 100")
        Integer severityScore,

        String notes,

        LocalDateTime assessedAt
) {
}
