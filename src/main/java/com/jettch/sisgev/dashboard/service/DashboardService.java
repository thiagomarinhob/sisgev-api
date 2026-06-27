package com.jettch.sisgev.dashboard.service;

import com.jettch.sisgev.dashboard.dto.KmByConditionResponse;
import com.jettch.sisgev.dashboard.dto.MapSegmentResponse;
import com.jettch.sisgev.dashboard.repository.DashboardRepository;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BE-19/BE-20 — Indicadores do dashboard. Escopo por município (multi-tenant):
 * usuário comum vê o próprio; SUPER_ADMIN deve informar {@code municipalityId}.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public KmByConditionResponse kmByCondition(UUID municipalityId) {
        UUID target = resolveMunicipality(municipalityId);

        Map<String, BigDecimal> km = new LinkedHashMap<>();
        for (RoadCondition condition : RoadCondition.values()) {
            km.put(condition.name(), BigDecimal.ZERO);
        }
        for (DashboardRepository.KmRow row : dashboardRepository.kmByCondition(target)) {
            km.put(row.getCondition(), scale(row.getKm()));
        }

        BigDecimal total = km.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> percent = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : km.entrySet()) {
            percent.put(entry.getKey(), total.signum() == 0
                    ? BigDecimal.ZERO
                    : entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
        }

        return new KmByConditionResponse(target, scale(total), km, percent);
    }

    @Transactional(readOnly = true)
    public List<MapSegmentResponse> mapSegments(UUID municipalityId) {
        UUID target = resolveMunicipality(municipalityId);
        return dashboardRepository.mapSegments(target).stream()
                .map(row -> new MapSegmentResponse(
                        row.getId(),
                        row.getName(),
                        row.getRoadName(),
                        row.getCondition(),
                        scale(row.getLengthMeters()),
                        row.getGeojson()))
                .toList();
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    /** Resolve o município alvo respeitando o multi-tenant (RN-001/RN-030). */
    private UUID resolveMunicipality(UUID requested) {
        User user = currentUser.getCurrentUser();
        if (user.isSuperAdmin()) {
            if (requested == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "MUNICIPALITY_REQUIRED", "SUPER_ADMIN deve informar municipalityId");
            }
            return requested;
        }
        if (requested != null) {
            currentUser.assertCanAccessMunicipality(requested);
        }
        if (user.getMunicipalityId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município vinculado");
        }
        return user.getMunicipalityId();
    }
}
