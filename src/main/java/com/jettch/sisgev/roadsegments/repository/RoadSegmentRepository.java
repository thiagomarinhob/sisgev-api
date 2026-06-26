package com.jettch.sisgev.roadsegments.repository;

import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoadSegmentRepository extends JpaRepository<RoadSegment, UUID> {

    Page<RoadSegment> findByMunicipalityIdAndDeletedAtIsNull(UUID municipalityId, Pageable pageable);

    Page<RoadSegment> findByDeletedAtIsNull(Pageable pageable);

    Optional<RoadSegment> findByIdAndDeletedAtIsNull(UUID id);

    /** Recalcula length_meters via PostGIS ST_Length (RN-027 · §7.1). */
    @Modifying
    @Query(value = "UPDATE road_segments SET length_meters = ST_Length(geometry::geography) WHERE id = :id",
           nativeQuery = true)
    void recalculateLengthMeters(@Param("id") UUID id);
}
