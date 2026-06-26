package com.jettch.sisgev.roads.service;

import com.jettch.sisgev.roads.dto.RoadGeoJsonImportRequest;
import com.jettch.sisgev.roads.dto.RoadRequest;
import com.jettch.sisgev.roads.dto.RoadResponse;
import com.jettch.sisgev.roads.entity.Road;
import com.jettch.sisgev.roads.repository.RoadRepository;
import com.jettch.sisgev.roads.support.GeoJsonConverter;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiLineString;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BE-08 — CRUD de estradas. BE-09 — importação de geometria via GeoJSON.
 * Refs: RF-ROAD-001/002, RN-001, RN-019 · §12, §20.3.
 */
@Service
@RequiredArgsConstructor
public class RoadService {

    private final RoadRepository repository;
    private final CurrentUserService currentUser;

    @Transactional
    public RoadResponse create(RoadRequest request) {
        UUID municipalityId = requireMunicipalityOfCurrentUser();
        assertNameAvailable(municipalityId, request.name(), null);

        Road road = new Road();
        road.setMunicipalityId(municipalityId);
        apply(road, request);
        road.setActive(true);
        return RoadResponse.from(repository.save(road));
    }

    /** SUPER_ADMIN lista todas; demais veem apenas estradas do próprio município. */
    @Transactional(readOnly = true)
    public PagedResponse<RoadResponse> list(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<Road> page;
        if (user.isSuperAdmin()) {
            page = repository.findAllByDeletedAtIsNull(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = repository.findAllByMunicipalityIdAndDeletedAtIsNull(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(RoadResponse::from));
    }

    @Transactional(readOnly = true)
    public RoadResponse get(UUID id) {
        Road road = findActive(id);
        currentUser.assertCanAccessMunicipality(road.getMunicipalityId());
        return RoadResponse.from(road);
    }

    @Transactional
    public RoadResponse update(UUID id, RoadRequest request) {
        Road road = findActive(id);
        currentUser.assertCanAccessMunicipality(road.getMunicipalityId());
        assertNameAvailable(road.getMunicipalityId(), request.name(), id);
        apply(road, request);
        return RoadResponse.from(repository.save(road));
    }

    /** Exclusão lógica (RN-019): marca deleted_at e desativa. */
    @Transactional
    public void delete(UUID id) {
        Road road = findActive(id);
        currentUser.assertCanAccessMunicipality(road.getMunicipalityId());
        road.setActive(false);
        road.setDeletedAt(LocalDateTime.now());
        repository.save(road);
    }

    /**
     * BE-09 — Substitui a geometria de uma estrada existente a partir de um GeoJSON
     * e recalcula {@code total_length_meters} (RN-027 aplicado a roads, §7.1).
     */
    @Transactional
    public RoadResponse importGeoJson(RoadGeoJsonImportRequest request) {
        Road road = findActive(request.roadId());
        currentUser.assertCanAccessMunicipality(road.getMunicipalityId());

        MultiLineString geometry = GeoJsonConverter.parseMultiLineString(request.geojson());
        road.setGeometry(geometry);
        repository.saveAndFlush(road);

        BigDecimal length = repository.calculateLengthMeters(road.getId()).setScale(2, RoundingMode.HALF_UP);
        road.setTotalLengthMeters(length);
        return RoadResponse.from(repository.save(road));
    }

    private void apply(Road road, RoadRequest request) {
        road.setName(request.name().trim());
        road.setDescription(request.description());
        road.setPublished(request.published());
    }

    /** Nunca confiar em municipalityId vindo do payload — sempre usar o do usuário autenticado (RN-001). */
    private UUID requireMunicipalityOfCurrentUser() {
        User user = currentUser.getCurrentUser();
        if (user.getMunicipalityId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município não pode cadastrar estradas");
        }
        return user.getMunicipalityId();
    }

    private void assertNameAvailable(UUID municipalityId, String name, UUID ignoreRoadId) {
        String trimmed = name.trim();
        boolean taken = ignoreRoadId == null
                ? repository.existsByMunicipalityIdAndNameIgnoreCaseAndDeletedAtIsNull(municipalityId, trimmed)
                : repository.existsByMunicipalityIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(municipalityId, trimmed, ignoreRoadId);
        if (taken) {
            throw new BusinessException(HttpStatus.CONFLICT, "ROAD_NAME_ALREADY_EXISTS",
                    "Já existe uma estrada com esse nome neste município");
        }
    }

    private Road findActive(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ROAD_NOT_FOUND", "Estrada não encontrada"));
    }
}
