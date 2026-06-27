package com.jettch.sisgev.dashboard.repository;

import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de leitura do dashboard. Só trechos ativos + publicados (RN-028).
 * Aliases camelCase são citados ("...") para preservar o case no ResultSet (Postgres).
 */
public interface DashboardRepository extends Repository<RoadSegment, UUID> {

    /** BE-19 — km por condição atual: SUM(length_meters)/1000 agrupado por current_condition (§8.2). */
    @Query(value = """
            SELECT current_condition AS condition,
                   COALESCE(SUM(length_meters), 0) / 1000.0 AS km
            FROM road_segments
            WHERE municipality_id = :municipalityId
              AND active = TRUE AND published = TRUE AND deleted_at IS NULL
            GROUP BY current_condition
            """, nativeQuery = true)
    List<KmRow> kmByCondition(@Param("municipalityId") UUID municipalityId);

    /** BE-20 — trechos para o mapa, com geometria em GeoJSON (§7.4). */
    @Query(value = """
            SELECT rs.id AS id,
                   rs.name AS name,
                   r.name AS "roadName",
                   rs.current_condition AS condition,
                   rs.length_meters AS "lengthMeters",
                   ST_AsGeoJSON(rs.geometry) AS geojson
            FROM road_segments rs
            JOIN roads r ON r.id = rs.road_id
            WHERE rs.municipality_id = :municipalityId
              AND rs.active = TRUE AND rs.published = TRUE AND rs.deleted_at IS NULL
            ORDER BY r.name, rs.segment_order NULLS LAST
            """, nativeQuery = true)
    List<MapSegmentRow> mapSegments(@Param("municipalityId") UUID municipalityId);

    /** BE-21 — km por condição reconstruída numa data: última avaliação até :cutoff (§8.3, §7.3). */
    @Query(value = """
            SELECT cond AS condition, COALESCE(SUM(length_meters), 0) / 1000.0 AS km
            FROM (
              SELECT rs.length_meters,
                     COALESCE((SELECT ra.condition FROM road_assessments ra
                               WHERE ra.road_segment_id = rs.id AND ra.assessed_at <= :cutoff
                               ORDER BY ra.assessed_at DESC LIMIT 1), 'UNKNOWN') AS cond
              FROM road_segments rs
              WHERE rs.municipality_id = :municipalityId
                AND rs.active = TRUE AND rs.published = TRUE AND rs.deleted_at IS NULL
            ) t
            GROUP BY cond
            """, nativeQuery = true)
    List<KmRow> kmByConditionAt(@Param("municipalityId") UUID municipalityId,
                               @Param("cutoff") LocalDateTime cutoff);

    /** BE-21 — trechos para o mapa com a condição válida numa data. */
    @Query(value = """
            SELECT rs.id AS id,
                   rs.name AS name,
                   r.name AS "roadName",
                   COALESCE((SELECT ra.condition FROM road_assessments ra
                             WHERE ra.road_segment_id = rs.id AND ra.assessed_at <= :cutoff
                             ORDER BY ra.assessed_at DESC LIMIT 1), 'UNKNOWN') AS condition,
                   rs.length_meters AS "lengthMeters",
                   ST_AsGeoJSON(rs.geometry) AS geojson
            FROM road_segments rs
            JOIN roads r ON r.id = rs.road_id
            WHERE rs.municipality_id = :municipalityId
              AND rs.active = TRUE AND rs.published = TRUE AND rs.deleted_at IS NULL
            ORDER BY r.name, rs.segment_order NULLS LAST
            """, nativeQuery = true)
    List<MapSegmentRow> mapSegmentsAt(@Param("municipalityId") UUID municipalityId,
                                      @Param("cutoff") LocalDateTime cutoff);

    interface KmRow {
        String getCondition();
        BigDecimal getKm();
    }

    interface MapSegmentRow {
        UUID getId();
        String getName();
        String getRoadName();
        String getCondition();
        BigDecimal getLengthMeters();
        String getGeojson();
    }
}
