package com.jettch.sisgev.roadsegments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.Arrays;
import java.util.List;

/**
 * Representação GeoJSON de uma LineString para entrada e saída de trechos.
 * Coordenadas no formato [longitude, latitude] (WGS84 / SRID 4326).
 */
public record GeoJsonLineString(
        @JsonProperty("type") String type,
        @JsonProperty("coordinates") @NotNull @NotEmpty List<double[]> coordinates
) {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    public LineString toJts() {
        Coordinate[] coords = coordinates.stream()
                .map(pair -> new Coordinate(pair[0], pair[1]))
                .toArray(Coordinate[]::new);
        LineString ls = GEOMETRY_FACTORY.createLineString(coords);
        ls.setSRID(4326);
        return ls;
    }

    public static GeoJsonLineString fromJts(LineString lineString) {
        List<double[]> coords = Arrays.stream(lineString.getCoordinates())
                .map(c -> new double[]{c.x, c.y})
                .toList();
        return new GeoJsonLineString("LineString", coords);
    }
}
