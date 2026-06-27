package com.jettch.sisgev.maintenance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenance_events")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "road_segment_id", nullable = false)
    private UUID roadSegmentId;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "finished_date")
    private LocalDate finishedDate;

    @Column(name = "repaired_length_meters", precision = 12, scale = 2)
    private BigDecimal repairedLengthMeters;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
