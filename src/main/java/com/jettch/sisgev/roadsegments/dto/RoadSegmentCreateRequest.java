package com.jettch.sisgev.roadsegments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RoadSegmentCreateRequest(
        @NotNull UUID roadId,
        @NotBlank @Size(max = 180) String name,
        Integer segmentOrder,
        @NotNull @Valid GeoJsonLineString geometry,
        Boolean published
) {}
