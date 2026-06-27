package com.jettch.sisgev.evidences.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Associação manual de uma evidência a um trecho confirmado pelo admin. */
public record AssociateSegmentRequest(
        @NotNull(message = "roadSegmentId é obrigatório")
        UUID roadSegmentId
) {
}
