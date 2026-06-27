package com.jettch.sisgev.maintenance.repository;

import com.jettch.sisgev.maintenance.entity.MaintenanceEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaintenanceEventRepository extends JpaRepository<MaintenanceEvent, UUID> {

    Page<MaintenanceEvent> findByRoadSegmentIdOrderByCreatedAtDesc(UUID roadSegmentId, Pageable pageable);

    Page<MaintenanceEvent> findByMunicipalityId(UUID municipalityId, Pageable pageable);

    Page<MaintenanceEvent> findByMunicipalityIdAndStatus(UUID municipalityId, String status, Pageable pageable);

    Page<MaintenanceEvent> findByStatus(String status, Pageable pageable);
}
