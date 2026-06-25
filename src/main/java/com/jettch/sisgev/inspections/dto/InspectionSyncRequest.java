package com.jettch.sisgev.inspections.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload de criação/sincronização de vistoria vindo do mobile.
 * {@code fieldAgentId} e {@code municipalityId} NÃO vêm aqui — são derivados do
 * usuário autenticado (RN-017/RN-001). Status é controlado pelo backend.
 */
public record InspectionSyncRequest(

        @NotNull(message = "clientUuid é obrigatório")
        UUID clientUuid,

        @NotNull(message = "startedAt é obrigatório")
        LocalDateTime startedAt,

        LocalDateTime finishedAt,

        String notes
) {
}
