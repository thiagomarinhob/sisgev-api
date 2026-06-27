package com.jettch.sisgev.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Trecho colorido para o mapa (BE-20). {@code geometry} é o GeoJSON cru da LineString
 * (vindo de ST_AsGeoJSON); {@code @JsonRawValue} faz o Jackson emiti-lo como objeto JSON,
 * não como string escapada.
 */
public record MapSegmentResponse(
        UUID id,
        String name,
        String roadName,
        String condition,
        BigDecimal lengthMeters,
        @JsonRawValue String geometry
) {
}
