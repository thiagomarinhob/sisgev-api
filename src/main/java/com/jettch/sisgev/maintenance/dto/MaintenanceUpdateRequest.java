package com.jettch.sisgev.maintenance.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Edição de intervenção (BE-23). */
public record MaintenanceUpdateRequest(

        @NotBlank(message = "Tipo de intervenção é obrigatório")
        String type,

        LocalDate plannedStartDate,

        String notes
) {
}
