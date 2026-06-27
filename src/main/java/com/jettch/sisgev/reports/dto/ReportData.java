package com.jettch.sisgev.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Dados consolidados do relatório (RN-022). */
public record ReportData(
        String municipalityName,
        String state,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime generatedAt,
        String generatedBy,
        Map<String, BigDecimal> kmByCondition,
        BigDecimal totalKm,
        BigDecimal repairedKm,
        List<CriticalSegment> criticalSegments
) {
    public record CriticalSegment(String name, String condition, BigDecimal km) {
    }
}
