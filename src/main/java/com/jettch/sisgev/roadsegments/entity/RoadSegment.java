package com.jettch.sisgev.roadsegments.entity;

import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trecho de estrada vicinal — unidade oficial de cálculo de km (RN-002).
 * Tabela criada na migration V1. {@code geometry} é LineString PostGIS (SRID 4326).
 * {@code lengthMeters} é calculado via ST_Length após cada save (RN-027).
 */
@Entity
@Table(name = "road_segments")
@Getter
@Setter
@NoArgsConstructor
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "road_id", nullable = false)
    private UUID roadId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "segment_order")
    private Integer segmentOrder;

    @Column(columnDefinition = "geometry(LineString,4326)", nullable = false)
    private LineString geometry;

    @Column(name = "length_meters", nullable = false, precision = 12, scale = 2)
    private BigDecimal lengthMeters = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_condition", nullable = false, length = 30)
    private RoadCondition currentCondition = RoadCondition.UNKNOWN;

    @Column(name = "last_assessment_at")
    private LocalDateTime lastAssessmentAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
