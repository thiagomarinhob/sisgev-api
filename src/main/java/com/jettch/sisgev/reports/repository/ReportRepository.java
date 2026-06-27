package com.jettch.sisgev.reports.repository;

import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Consultas de leitura para o relatório gerencial (BE-24). */
public interface ReportRepository extends Repository<RoadSegment, UUID> {

    /** Trechos críticos/intransitáveis (maior km primeiro). */
    @Query(value = """
            SELECT name AS name,
                   current_condition AS condition,
                   length_meters / 1000.0 AS km
            FROM road_segments
            WHERE municipality_id = :municipalityId
              AND active = TRUE AND published = TRUE AND deleted_at IS NULL
              AND current_condition IN ('CRITICAL', 'IMPASSABLE')
            ORDER BY length_meters DESC
            """, nativeQuery = true)
    List<CriticalRow> criticalSegments(@Param("municipalityId") UUID municipalityId);

    /** km recuperados (intervenções FINISHED com finished_date no período). */
    @Query(value = """
            SELECT COALESCE(SUM(repaired_length_meters), 0) / 1000.0
            FROM maintenance_events
            WHERE municipality_id = :municipalityId
              AND status = 'FINISHED'
              AND finished_date BETWEEN :start AND :end
            """, nativeQuery = true)
    BigDecimal repairedKm(@Param("municipalityId") UUID municipalityId,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end);

    interface CriticalRow {
        String getName();
        String getCondition();
        BigDecimal getKm();
    }
}
