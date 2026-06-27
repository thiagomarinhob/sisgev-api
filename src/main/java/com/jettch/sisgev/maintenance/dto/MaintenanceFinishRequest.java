package com.jettch.sisgev.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Conclusão da intervenção (BE-23 · RF-MNT-002): exige data; km recuperados não pode ser negativo. */
public record MaintenanceFinishRequest(

        @NotNull(message = "Data de conclusão é obrigatória")
        LocalDate finishedDate,

        @PositiveOrZero(message = "km recuperados não pode ser negativo")
        BigDecimal repairedLengthMeters,

        String notes
) {
}
