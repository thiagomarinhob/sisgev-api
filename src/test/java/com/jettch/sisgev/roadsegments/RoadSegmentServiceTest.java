package com.jettch.sisgev.roadsegments;

import com.jettch.sisgev.roads.entity.Road;
import com.jettch.sisgev.roads.repository.RoadRepository;
import com.jettch.sisgev.roadsegments.dto.GeoJsonLineString;
import com.jettch.sisgev.roadsegments.dto.LengthOverrideRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentCreateRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentResponse;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentUpdateRequest;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.roadsegments.service.RoadSegmentService;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadSegmentServiceTest {

    @Mock RoadSegmentRepository segmentRepo;
    @Mock RoadRepository roadRepo;
    @Mock CurrentUserService currentUserService;
    @InjectMocks RoadSegmentService service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private UUID municipalityId;
    private UUID roadId;
    private User fieldUser;
    private Road road;
    private LineString sampleLine;
    private GeoJsonLineString sampleGeoJson;

    @BeforeEach
    void setUp() {
        municipalityId = UUID.randomUUID();
        roadId = UUID.randomUUID();

        fieldUser = new User();
        fieldUser.setId(UUID.randomUUID());
        fieldUser.setMunicipalityId(municipalityId);
        fieldUser.setRole(UserRole.ADMIN_OPERACIONAL);

        road = new Road();
        road.setId(roadId);
        road.setMunicipalityId(municipalityId);
        road.setName("Estrada A");
        road.setActive(true);

        sampleLine = GF.createLineString(new Coordinate[]{
                new Coordinate(-39.1, -5.1),
                new Coordinate(-39.2, -5.2)
        });
        sampleLine.setSRID(4326);

        sampleGeoJson = new GeoJsonLineString("LineString",
                List.of(new double[]{-39.1, -5.1}, new double[]{-39.2, -5.2}));
    }

    // ---- AC-001 / AC-003: criação sempre define currentCondition = UNKNOWN ----

    @Test
    void create_setsCurrentConditionToUnknown() {
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);
        when(roadRepo.findByIdAndMunicipalityIdAndDeletedAtIsNullAndActiveTrue(roadId, municipalityId))
                .thenReturn(Optional.of(road));

        RoadSegment saved = segmentWith(UUID.randomUUID(), BigDecimal.ZERO, RoadCondition.UNKNOWN);
        RoadSegment reloaded = segmentWith(saved.getId(), new BigDecimal("1250.50"), RoadCondition.UNKNOWN);

        when(segmentRepo.saveAndFlush(any())).thenReturn(saved);
        when(segmentRepo.findByIdAndDeletedAtIsNull(saved.getId())).thenReturn(Optional.of(reloaded));

        RoadSegmentResponse response = service.create(
                new RoadSegmentCreateRequest(roadId, "Trecho 01", 1, sampleGeoJson, false));

        // AC-001: resposta tem currentCondition = UNKNOWN
        assertThat(response.currentCondition()).isEqualTo(RoadCondition.UNKNOWN);

        // AC-003: valor salvo no repositório também é UNKNOWN (não do payload)
        ArgumentCaptor<RoadSegment> captor = ArgumentCaptor.forClass(RoadSegment.class);
        verify(segmentRepo).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCurrentCondition()).isEqualTo(RoadCondition.UNKNOWN);
    }

    // ---- AC-009: recalculateLengthMeters chamado após save; response usa valor recarregado ----

    @Test
    void create_callsRecalculateAndReturnsCalculatedLength() {
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);
        when(roadRepo.findByIdAndMunicipalityIdAndDeletedAtIsNullAndActiveTrue(roadId, municipalityId))
                .thenReturn(Optional.of(road));

        UUID savedId = UUID.randomUUID();
        RoadSegment saved = segmentWith(savedId, BigDecimal.ZERO, RoadCondition.UNKNOWN);
        BigDecimal calculatedLength = new BigDecimal("1250.50");
        RoadSegment reloaded = segmentWith(savedId, calculatedLength, RoadCondition.UNKNOWN);

        when(segmentRepo.saveAndFlush(any())).thenReturn(saved);
        when(segmentRepo.findByIdAndDeletedAtIsNull(savedId)).thenReturn(Optional.of(reloaded));

        RoadSegmentResponse response = service.create(
                new RoadSegmentCreateRequest(roadId, "Trecho 01", null, sampleGeoJson, true));

        // AC-009: recalculate chamado com id correto
        verify(segmentRepo).recalculateLengthMeters(savedId);
        // e response contém o length calculado pelo PostGIS (da entidade recarregada)
        assertThat(response.lengthMeters()).isEqualByComparingTo(calculatedLength);
    }

    // ---- AC-005: lista filtrada pelo município do usuário ----

    @Test
    void list_filtersByUserMunicipality() {
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);

        UUID segId = UUID.randomUUID();
        RoadSegment seg = segmentWith(segId, new BigDecimal("500.00"), RoadCondition.UNKNOWN);
        when(segmentRepo.findByMunicipalityIdAndDeletedAtIsNull(eq(municipalityId), any()))
                .thenReturn(new PageImpl<>(List.of(seg)));

        var page = service.list(PageRequest.of(0, 20));

        // AC-005: lista contém apenas itens do município do usuário
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).municipalityId()).isEqualTo(municipalityId);
        verify(segmentRepo).findByMunicipalityIdAndDeletedAtIsNull(eq(municipalityId), any());
        verify(segmentRepo, never()).findByDeletedAtIsNull(any());
    }

    // ---- AC-008: acesso a trecho de outro município → 403 ----

    @Test
    void get_throwsForbiddenForDifferentMunicipality() {
        UUID otherId = UUID.randomUUID();
        UUID segId = UUID.randomUUID();
        RoadSegment seg = segmentWith(segId, new BigDecimal("100.00"), RoadCondition.UNKNOWN);
        seg.setMunicipalityId(otherId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.of(seg));
        doThrow(new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_MUNICIPALITY", "Acesso negado ao município"))
                .when(currentUserService).assertCanAccessMunicipality(otherId);

        // AC-008: 403 ao acessar trecho de outro município
        assertThatThrownBy(() -> service.get(segId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- AC-007: soft delete ----

    @Test
    void delete_setsDeletedAtAndActiveFalse() {
        UUID segId = UUID.randomUUID();
        RoadSegment seg = segmentWith(segId, new BigDecimal("300.00"), RoadCondition.UNKNOWN);
        seg.setMunicipalityId(municipalityId);
        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.of(seg));

        service.delete(segId);

        ArgumentCaptor<RoadSegment> captor = ArgumentCaptor.forClass(RoadSegment.class);
        verify(segmentRepo).save(captor.capture());
        RoadSegment deleted = captor.getValue();

        // AC-007: deleted_at não-nulo e active = false (RN-019)
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.isActive()).isFalse();
    }

    @Test
    void get_afterSoftDelete_throws404() {
        UUID id = UUID.randomUUID();
        when(segmentRepo.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // AC-007: GET após soft delete retorna 404
        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- AC-010: roadId de outro município → 422 ----

    @Test
    void create_throwsUnprocessableWhenRoadNotFoundInMunicipality() {
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);
        when(roadRepo.findByIdAndMunicipalityIdAndDeletedAtIsNullAndActiveTrue(roadId, municipalityId))
                .thenReturn(Optional.empty());

        // AC-010: road inexistente/de outro município → 422
        assertThatThrownBy(() -> service.create(
                new RoadSegmentCreateRequest(roadId, "Trecho X", null, sampleGeoJson, false)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    // ---- AC-006: update recalcula length quando geometria muda ----

    @Test
    void update_recalculatesLengthAndReturnsUpdatedValue() {
        UUID segId = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("500.00"), RoadCondition.UNKNOWN);
        existing.setMunicipalityId(municipalityId);
        existing.setGeometry(sampleLine);

        BigDecimal newLength = new BigDecimal("2500.00");
        RoadSegment reloaded = segmentWith(segId, newLength, RoadCondition.UNKNOWN);
        reloaded.setMunicipalityId(municipalityId);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(reloaded));
        when(segmentRepo.saveAndFlush(any())).thenReturn(existing);

        GeoJsonLineString newGeo = new GeoJsonLineString("LineString",
                List.of(new double[]{-39.0, -5.0}, new double[]{-39.3, -5.3}, new double[]{-39.5, -5.5}));

        RoadSegmentResponse response = service.update(segId,
                new RoadSegmentUpdateRequest("Trecho Atualizado", 2, newGeo, true));

        // AC-006: recalculate chamado após update
        verify(segmentRepo).recalculateLengthMeters(segId);
        // e response usa o valor calculado pelo PostGIS
        assertThat(response.lengthMeters()).isEqualByComparingTo(newLength);
    }

    // ---- LEN-01: override manual por ADMIN_OPERACIONAL ----

    @Test
    void overrideLength_adminOperacional_updatesLengthAndReason() {
        UUID segId = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("1000.00"), RoadCondition.UNKNOWN);
        BigDecimal manualLength = new BigDecimal("950.00");
        String justification = "GPS trace impreciso na curva leste";

        RoadSegment reloaded = segmentWith(segId, manualLength, RoadCondition.UNKNOWN);
        reloaded.setLengthOverrideReason(justification);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(reloaded));
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);

        RoadSegmentResponse response = service.overrideLength(segId,
                new LengthOverrideRequest(manualLength, justification));

        // LEN-01: overrideLength chamado com valores corretos
        verify(segmentRepo).overrideLength(segId, manualLength, justification);
        // LEN-01: response reflete o valor manual
        assertThat(response.lengthMeters()).isEqualByComparingTo(manualLength);
        // LEN-06: response expõe justificativa
        assertThat(response.lengthOverrideReason()).isEqualTo(justification);
    }

    // ---- LEN-02: não-admin → 403 ----

    @Test
    void overrideLength_nonAdminRole_throws403() {
        UUID segId = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("500.00"), RoadCondition.UNKNOWN);

        User gestor = new User();
        gestor.setId(UUID.randomUUID());
        gestor.setMunicipalityId(municipalityId);
        gestor.setRole(UserRole.GESTOR_PREFEITURA);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(gestor);

        // LEN-02: GESTOR_PREFEITURA → 403 FORBIDDEN_LENGTH_OVERRIDE
        assertThatThrownBy(() -> service.overrideLength(segId,
                new LengthOverrideRequest(new BigDecimal("500.00"), "Justificativa valida aqui")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- LEN-03: lengthMeters <= 0 (valor negativo explícito no service) → 422 ----

    @Test
    void overrideLength_negativeLength_throws422() {
        UUID segId = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("500.00"), RoadCondition.UNKNOWN);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.of(existing));
        when(currentUserService.getCurrentUser()).thenReturn(fieldUser);

        // LEN-03: valor negativo → 422 INVALID_LENGTH
        assertThatThrownBy(() -> service.overrideLength(segId,
                new LengthOverrideRequest(new BigDecimal("-1.00"), "Justificativa valida aqui")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException ex = (BusinessException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(ex.getCode()).isEqualTo("INVALID_LENGTH");
                });
    }

    // ---- LEN-07: update geometry → length_override_reason limpo ----

    @Test
    void update_afterManualOverride_clearsLengthOverrideReason() {
        UUID segId = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("950.00"), RoadCondition.UNKNOWN);
        existing.setMunicipalityId(municipalityId);
        existing.setLengthOverrideReason("Ajuste manual anterior");
        existing.setGeometry(sampleLine);

        // Após PUT com nova geometria, o recarregado deve ter reason = null
        RoadSegment reloaded = segmentWith(segId, new BigDecimal("2100.00"), RoadCondition.UNKNOWN);
        reloaded.setMunicipalityId(municipalityId);
        reloaded.setLengthOverrideReason(null);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(reloaded));
        when(segmentRepo.saveAndFlush(any())).thenReturn(existing);

        GeoJsonLineString newGeo = new GeoJsonLineString("LineString",
                List.of(new double[]{-39.0, -5.0}, new double[]{-39.4, -5.4}));

        RoadSegmentResponse response = service.update(segId,
                new RoadSegmentUpdateRequest("Trecho X", 1, newGeo, false));

        // LEN-07: recalculate chamado (o qual limpa length_override_reason via SQL)
        verify(segmentRepo).recalculateLengthMeters(segId);
        // LEN-07: response reflete reason nulo (auto-calculado)
        assertThat(response.lengthOverrideReason()).isNull();
    }

    // ---- LEN-08: override em trecho de outro município → 403 ----

    @Test
    void overrideLength_differentMunicipality_throws403() {
        UUID segId = UUID.randomUUID();
        UUID otherMunicipality = UUID.randomUUID();
        RoadSegment existing = segmentWith(segId, new BigDecimal("500.00"), RoadCondition.UNKNOWN);
        existing.setMunicipalityId(otherMunicipality);

        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.of(existing));
        doThrow(new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_MUNICIPALITY", "Acesso negado ao município"))
                .when(currentUserService).assertCanAccessMunicipality(otherMunicipality);

        // LEN-08: multi-tenancy → 403
        assertThatThrownBy(() -> service.overrideLength(segId,
                new LengthOverrideRequest(new BigDecimal("500.00"), "Justificativa valida aqui")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ---- LEN-09: override em trecho inexistente → 404 ----

    @Test
    void overrideLength_nonExistentSegment_throws404() {
        UUID segId = UUID.randomUUID();
        when(segmentRepo.findByIdAndDeletedAtIsNull(segId)).thenReturn(Optional.empty());

        // LEN-09: trecho inexistente → 404
        assertThatThrownBy(() -> service.overrideLength(segId,
                new LengthOverrideRequest(new BigDecimal("500.00"), "Justificativa valida aqui")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- helpers ----

    private RoadSegment segmentWith(UUID id, BigDecimal length, RoadCondition condition) {
        RoadSegment seg = new RoadSegment();
        seg.setId(id);
        seg.setMunicipalityId(municipalityId);
        seg.setRoadId(roadId);
        seg.setName("Trecho 01");
        seg.setGeometry(sampleLine);
        seg.setLengthMeters(length);
        seg.setCurrentCondition(condition);
        seg.setActive(true);
        seg.setPublished(false);
        seg.setCreatedAt(LocalDateTime.now());
        seg.setUpdatedAt(LocalDateTime.now());
        return seg;
    }
}
