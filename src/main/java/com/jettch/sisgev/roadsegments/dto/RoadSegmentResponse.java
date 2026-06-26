package com.jettch.sisgev.roadsegments.dto;

import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RoadSegmentResponse(
        UUID id,
        UUID municipalityId,
        UUID roadId,
        String name,
        Integer segmentOrder,
        GeoJsonLineString geometry,
        BigDecimal lengthMeters,
        RoadCondition currentCondition,
        LocalDateTime lastAssessmentAt,
        boolean active,
        boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoadSegmentResponse from(RoadSegment s) {
        return new RoadSegmentResponse(
                s.getId(),
                s.getMunicipalityId(),
                s.getRoadId(),
                s.getName(),
                s.getSegmentOrder(),
                GeoJsonLineString.fromJts(s.getGeometry()),
                s.getLengthMeters(),
                s.getCurrentCondition(),
                s.getLastAssessmentAt(),
                s.isActive(),
                s.isPublished(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
