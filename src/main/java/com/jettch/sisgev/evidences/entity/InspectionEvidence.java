package com.jettch.sisgev.evidences.entity;

import com.jettch.sisgev.evidences.enums.EvidenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evidência (foto georreferenciada) coletada em campo. Tabela criada na V1.
 * O binário fica no storage; aqui guardamos apenas metadados e URLs (RN-023, §11.3).
 * {@code location} é a geometria PostGIS (SRID 4326) usada para buscas espaciais (BE-15).
 */
@Entity
@Table(name = "inspection_evidences")
@Getter
@Setter
@NoArgsConstructor
public class InspectionEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "municipality_id", nullable = false)
    private UUID municipalityId;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "field_agent_id", nullable = false)
    private UUID fieldAgentId;

    @Column(name = "suggested_road_segment_id")
    private UUID suggestedRoadSegmentId;

    @Column(name = "confirmed_road_segment_id")
    private UUID confirmedRoadSegmentId;

    @Column(name = "client_uuid", nullable = false)
    private UUID clientUuid;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "thumbnail_url", columnDefinition = "text")
    private String thumbnailUrl;

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    private String storageKey;

    @Column(name = "mime_type", length = 80)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash", length = 128)
    private String fileHash;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "gps_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal gpsAccuracyMeters;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvidenceStatus status;

    @Column(name = "field_note", columnDefinition = "text")
    private String fieldNote;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

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
