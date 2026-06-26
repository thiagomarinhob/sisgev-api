package com.jettch.sisgev.roads.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.MultiLineString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estrada vicinal (BE-08). A geometria é opcional na criação e é definida/atualizada
 * via importação de GeoJSON (BE-09). Tabela criada na migration V1.
 */
@Entity
@Table(name = "roads")
@Getter
@Setter
@NoArgsConstructor
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "geometry(MultiLineString,4326)")
    private MultiLineString geometry;

    @Column(name = "total_length_meters", precision = 12, scale = 2)
    private BigDecimal totalLengthMeters;

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
