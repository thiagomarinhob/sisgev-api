package com.jettch.sisgev.inspections.repository;

import com.jettch.sisgev.inspections.entity.Inspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    /** Chave de idempotência (RN-025): mesmo agente + mesmo client_uuid não duplica. */
    Optional<Inspection> findByFieldAgentIdAndClientUuid(UUID fieldAgentId, UUID clientUuid);

    Page<Inspection> findByMunicipalityId(UUID municipalityId, Pageable pageable);
}
