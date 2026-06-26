package com.jettch.sisgev.roads.dto;

import java.util.List;

/** Representação simplificada de uma geometria MultiLineString no formato GeoJSON, para respostas. */
public record GeoJsonGeometry(String type, List<List<List<Double>>> coordinates) {
}
