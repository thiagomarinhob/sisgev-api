package com.jettch.sisgev.assessments.dto;

import com.jettch.sisgev.assessments.entity.RoadAssessment;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import java.time.LocalDateTime;
import java.util.UUID;

public record AssessmentSummary(
        UUID id,
        RoadCondition condition,
        int severityScore,
        String source,
        String notes,
        UUID assessedBy,
        LocalDateTime assessedAt,
        UUID evidenceId
) {
    public static AssessmentSummary from(RoadAssessment a) {
        return new AssessmentSummary(
                a.getId(),
                a.getCondition(),
                a.getSeverityScore(),
                a.getSource(),
                a.getNotes(),
                a.getAssessedBy(),
                a.getAssessedAt(),
                a.getEvidenceId()
        );
    }
}
