package com.jettch.sisgev.roads.support;

import tools.jackson.databind.JsonNode;
import com.jettch.sisgev.roads.dto.GeoJsonGeometry;
import com.jettch.sisgev.shared.exception.BusinessException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * BE-09 — Converte GeoJSON (RFC 7946) em {@link MultiLineString} (e vice-versa para resposta).
 * Aceita geometrias {@code LineString}/{@code MultiLineString} soltas ou envolvidas em
 * {@code Feature}/{@code FeatureCollection}, combinando todas as linhas encontradas.
 * GeoJSON usa [longitude, latitude] (spec §10.2).
 */
public final class GeoJsonConverter {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoJsonConverter() {
    }

    public static MultiLineString parseMultiLineString(JsonNode root) {
        List<LineString> lines = new ArrayList<>();
        collectLines(root, lines);
        if (lines.isEmpty()) {
            throw invalid("GeoJSON não contém nenhuma geometria LineString/MultiLineString válida");
        }
        MultiLineString multiLineString = FACTORY.createMultiLineString(lines.toArray(new LineString[0]));
        multiLineString.setSRID(4326);
        return multiLineString;
    }

    public static GeoJsonGeometry toGeoJson(MultiLineString geometry) {
        if (geometry == null) {
            return null;
        }
        List<List<List<Double>>> coordinates = new ArrayList<>();
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            LineString line = (LineString) geometry.getGeometryN(i);
            List<List<Double>> points = new ArrayList<>();
            for (Coordinate c : line.getCoordinates()) {
                points.add(List.of(c.getX(), c.getY()));
            }
            coordinates.add(points);
        }
        return new GeoJsonGeometry("MultiLineString", coordinates);
    }

    private static void collectLines(JsonNode node, List<LineString> lines) {
        if (node == null || node.isNull()) {
            throw invalid("GeoJSON ausente");
        }
        String type = textOf(node, "type");
        switch (type) {
            case "LineString" -> lines.add(toLineString(requiredArray(node, "coordinates")));
            case "MultiLineString" -> {
                for (JsonNode lineNode : requiredArray(node, "coordinates")) {
                    lines.add(toLineString(lineNode));
                }
            }
            case "Feature" -> collectLines(node.get("geometry"), lines);
            case "FeatureCollection" -> {
                for (JsonNode feature : requiredArray(node, "features")) {
                    collectLines(feature, lines);
                }
            }
            default -> throw invalid("Tipo de geometria não suportado: " + type);
        }
    }

    private static LineString toLineString(JsonNode coordinatesNode) {
        if (!coordinatesNode.isArray() || coordinatesNode.size() < 2) {
            throw invalid("LineString precisa de ao menos 2 pontos");
        }
        Coordinate[] coordinates = new Coordinate[coordinatesNode.size()];
        for (int i = 0; i < coordinatesNode.size(); i++) {
            coordinates[i] = toCoordinate(coordinatesNode.get(i));
        }
        return FACTORY.createLineString(coordinates);
    }

    private static Coordinate toCoordinate(JsonNode pointNode) {
        if (!pointNode.isArray() || pointNode.size() < 2) {
            throw invalid("Coordenada inválida — esperado [longitude, latitude]");
        }
        return new Coordinate(pointNode.get(0).asDouble(), pointNode.get(1).asDouble());
    }

    private static JsonNode requiredArray(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw invalid("Campo \"" + field + "\" ausente ou inválido");
        }
        return value;
    }

    private static String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            throw invalid("Campo \"" + field + "\" ausente ou inválido");
        }
        return value.asString();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_GEOJSON", message);
    }
}
