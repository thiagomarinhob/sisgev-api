package com.jettch.sisgev.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** Registro de intervenção/manutenção num trecho (BE-23 · RF-MNT-001). */
public record MaintenanceCreateRequest(

        @NotNull(message = "roadSegmentId é obrigatório")
        UUID roadSegmentId,

        @NotBlank(message = "Tipo de intervenção é obrigatório")
        String type,

        UUID occurrenceId,

        LocalDate plannedStartDate,

        String notes
) {
}
