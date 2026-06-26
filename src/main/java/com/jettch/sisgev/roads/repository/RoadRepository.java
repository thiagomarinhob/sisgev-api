package com.jettch.sisgev.roads.repository;

import com.jettch.sisgev.roads.entity.Road;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import java.util.Optional;
import java.util.UUID;

public interface RoadRepository extends JpaRepository<Road, UUID> {

    Optional<Road> findByIdAndDeletedAtIsNull(UUID id);

    Page<Road> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Road> findAllByMunicipalityIdAndDeletedAtIsNull(UUID municipalityId, Pageable pageable);

    boolean existsByMunicipalityIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID municipalityId, String name);

    boolean existsByMunicipalityIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID municipalityId, String name, UUID id);

    /** RN-027 (aplicado a roads): comprimento geodésico calculado pela geometria persistida. */
    @Query(value = "SELECT ST_Length(CAST(geometry AS geography)) FROM roads WHERE id = :id", nativeQuery = true)
    BigDecimal calculateLengthMeters(@Param("id") UUID id);
    Optional<Road> findByIdAndMunicipalityIdAndDeletedAtIsNullAndActiveTrue(UUID id, UUID municipalityId);
}
