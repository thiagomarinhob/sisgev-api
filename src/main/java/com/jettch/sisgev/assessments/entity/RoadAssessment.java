package com.jettch.sisgev.assessments.entity;

import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "road_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RoadAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "road_segment_id", nullable = false)
    private UUID roadSegmentId;

    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoadCondition condition;

    @Column(name = "severity_score", nullable = false)
    private Integer severityScore;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
