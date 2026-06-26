package com.jettch.sisgev.roadsegments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoadSegmentUpdateRequest(
        @NotBlank @Size(max = 180) String name,
        Integer segmentOrder,
        @NotNull @Valid GeoJsonLineString geometry,
        Boolean published
) {}
