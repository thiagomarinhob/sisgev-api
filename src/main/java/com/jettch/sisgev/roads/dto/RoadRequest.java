package com.jettch.sisgev.roads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação/edição de estrada (§12 — validações de Estrada).
 * A geometria não é informada aqui — é definida via POST /roads/import-geojson (BE-09).
 */
public record RoadRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 180, message = "Nome deve ter entre 2 e 180 caracteres")
        String name,

        String description,

        boolean published
) {
}
