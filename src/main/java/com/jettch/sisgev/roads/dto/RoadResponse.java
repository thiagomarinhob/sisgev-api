package com.jettch.sisgev.roads.dto;

import com.jettch.sisgev.roads.entity.Road;
import com.jettch.sisgev.roads.support.GeoJsonConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RoadResponse(
        UUID id,
        UUID municipalityId,
        String name,
        String description,
        GeoJsonGeometry geometry,
        BigDecimal totalLengthMeters,
        boolean active,
        boolean published,
        LocalDateTime createdAt
) {
    public static RoadResponse from(Road r) {
        return new RoadResponse(
                r.getId(),
                r.getMunicipalityId(),
                r.getName(),
                r.getDescription(),
                GeoJsonConverter.toGeoJson(r.getGeometry()),
                r.getTotalLengthMeters(),
                r.isActive(),
                r.isPublished(),
                r.getCreatedAt()
        );
    }
}
