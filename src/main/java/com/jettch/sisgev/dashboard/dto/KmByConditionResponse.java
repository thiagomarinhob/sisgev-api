package com.jettch.sisgev.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Resumo de km por condição (BE-19). {@code kmByCondition} traz todas as condições
 * (0 quando não há trechos); {@code percentByCondition} é 0 quando o total é 0 (RN-011).
 */
public record KmByConditionResponse(
        UUID municipalityId,
        BigDecimal totalMappedKm,
        Map<String, BigDecimal> kmByCondition,
        Map<String, BigDecimal> percentByCondition
) {
}
