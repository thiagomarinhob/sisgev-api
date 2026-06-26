package com.jettch.sisgev.roads.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** BE-09 — Payload de importação de geometria de uma estrada existente via GeoJSON. */
public record RoadGeoJsonImportRequest(

        @NotNull(message = "roadId é obrigatório")
        UUID roadId,

        @NotNull(message = "geojson é obrigatório")
        JsonNode geojson
) {
}
