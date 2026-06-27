package com.jettch.sisgev.occurrences.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "occurrences")
@Getter
@Setter
@NoArgsConstructor
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "road_segment_id", nullable = false)
    private UUID roadSegmentId;

    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "problem_type", nullable = false, length = 50)
    private String problemType;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "severity_score", nullable = false)
    private Integer severityScore;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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
