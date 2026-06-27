package com.jettch.sisgev.occurrences.repository;

import com.jettch.sisgev.occurrences.entity.Occurrence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OccurrenceRepository extends JpaRepository<Occurrence, UUID> {

    Page<Occurrence> findByRoadSegmentIdOrderByOpenedAtDesc(UUID roadSegmentId, Pageable pageable);

    Page<Occurrence> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    Page<Occurrence> findByMunicipalityIdAndStatus(UUID municipalityId, String status, Pageable pageable);

    Page<Occurrence> findByStatus(String status, Pageable pageable);
}
