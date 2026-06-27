package com.jettch.sisgev.assessments.repository;

import com.jettch.sisgev.assessments.entity.RoadAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<RoadAssessment, UUID> {
    Page<RoadAssessment> findByRoadSegmentIdOrderByAssessedAtDesc(UUID roadSegmentId, Pageable pageable);
}
