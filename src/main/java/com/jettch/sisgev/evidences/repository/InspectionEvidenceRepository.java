package com.jettch.sisgev.evidences.repository;

import com.jettch.sisgev.evidences.entity.InspectionEvidence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InspectionEvidenceRepository extends JpaRepository<InspectionEvidence, UUID> {

    /** Idempotência (RN-025): mesmo agente + mesmo client_uuid não duplica a evidência. */
    Optional<InspectionEvidence> findByFieldAgentIdAndClientUuid(UUID fieldAgentId, UUID clientUuid);

    Page<InspectionEvidence> findByConfirmedRoadSegmentIdOrderByTakenAtDesc(UUID confirmedRoadSegmentId, Pageable pageable);
}
