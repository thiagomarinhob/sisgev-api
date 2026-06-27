package com.jettch.sisgev.roadsegments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LengthOverrideRequest(
        @NotNull BigDecimal lengthMeters,
        @NotBlank @Size(min = 10, max = 500) String justification
) {}
