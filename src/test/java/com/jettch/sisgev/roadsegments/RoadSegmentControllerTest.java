package com.jettch.sisgev.roadsegments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettch.sisgev.assessments.dto.AssessmentSummary;
import com.jettch.sisgev.assessments.service.AssessmentService;
import com.jettch.sisgev.evidences.dto.EvidenceResponse;
import com.jettch.sisgev.evidences.service.EvidenceService;
import com.jettch.sisgev.maintenance.dto.MaintenanceEventSummary;
import com.jettch.sisgev.maintenance.service.MaintenanceService;
import com.jettch.sisgev.occurrences.dto.OccurrenceSummary;
import com.jettch.sisgev.occurrences.service.OccurrenceService;
import com.jettch.sisgev.roadsegments.controller.RoadSegmentController;
import com.jettch.sisgev.roadsegments.dto.GeoJsonLineString;
import com.jettch.sisgev.roadsegments.dto.LengthOverrideRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentCreateRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentResponse;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentUpdateRequest;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.roadsegments.service.RoadSegmentService;
import com.jettch.sisgev.security.JwtService;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoadSegmentController.class)
class RoadSegmentControllerTest {

    @Autowired MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean RoadSegmentService service;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean AssessmentService assessmentService;
    @MockitoBean EvidenceService evidenceService;
    @MockitoBean OccurrenceService occurrenceService;
    @MockitoBean MaintenanceService maintenanceService;

    private UUID segmentId;
    private UUID roadId;
    private UUID municipalityId;
    private GeoJsonLineString sampleGeo;
    private RoadSegmentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        segmentId = UUID.randomUUID();
        roadId = UUID.randomUUID();
        municipalityId = UUID.randomUUID();
        sampleGeo = new GeoJsonLineString("LineString",
                List.of(new double[]{-39.1, -5.1}, new double[]{-39.2, -5.2}));
        sampleResponse = new RoadSegmentResponse(
                segmentId, municipalityId, roadId,
                "Trecho 01", 1, sampleGeo,
                new BigDecimal("1250.50"), null, RoadCondition.UNKNOWN,
                null, true, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ---- AC-001: POST cria trecho e retorna 201 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void create_returns201WithCreatedSegment() throws Exception {
        when(service.create(any())).thenReturn(sampleResponse);

        RoadSegmentCreateRequest req = new RoadSegmentCreateRequest(
                roadId, "Trecho 01", 1, sampleGeo, false);

        mvc.perform(post("/api/v1/road-segments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // AC-001: HTTP 201
                .andExpect(status().isCreated())
                // AC-001: resposta contém id e currentCondition = UNKNOWN
                .andExpect(jsonPath("$.id").value(segmentId.toString()))
                .andExpect(jsonPath("$.currentCondition").value("UNKNOWN"));
    }

    // ---- AC-002: geometry ausente → 400 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void create_withoutGeometry_returns400() throws Exception {
        String bodyWithoutGeometry = """
                {"roadId":"%s","name":"Trecho X","segmentOrder":1,"published":false}
                """.formatted(roadId);

        mvc.perform(post("/api/v1/road-segments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutGeometry))
                // AC-002: geometry obrigatória → 400
                .andExpect(status().isBadRequest());
    }

    // ---- AC-004: GET /{id} retorna 200 com geometry como GeoJSON ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void get_returns200WithGeoJsonGeometry() throws Exception {
        when(service.get(segmentId)).thenReturn(sampleResponse);

        mvc.perform(get("/api/v1/road-segments/{id}", segmentId))
                // AC-004: HTTP 200
                .andExpect(status().isOk())
                // AC-004: geometry serializada como GeoJSON com type e coordinates
                .andExpect(jsonPath("$.geometry.type").value("LineString"))
                .andExpect(jsonPath("$.geometry.coordinates").isArray())
                .andExpect(jsonPath("$.id").value(segmentId.toString()));
    }

    // ---- AC-005: GET lista retorna 200 paginado ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void list_returns200WithPagedContent() throws Exception {
        PagedResponse<RoadSegmentResponse> pagedResponse =
                new PagedResponse<>(List.of(sampleResponse), 0, 20, 1L, 1);
        when(service.list(any())).thenReturn(pagedResponse);

        mvc.perform(get("/api/v1/road-segments"))
                // AC-005: HTTP 200 com estrutura paginada
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- AC-006: PUT /{id} atualiza e retorna 200 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void update_returns200WithUpdatedSegment() throws Exception {
        RoadSegmentResponse updated = new RoadSegmentResponse(
                segmentId, municipalityId, roadId,
                "Trecho Atualizado", 2, sampleGeo,
                new BigDecimal("2500.00"), null, RoadCondition.UNKNOWN,
                null, true, true,
                LocalDateTime.now(), LocalDateTime.now());
        when(service.update(eq(segmentId), any())).thenReturn(updated);

        RoadSegmentUpdateRequest req = new RoadSegmentUpdateRequest("Trecho Atualizado", 2, sampleGeo, true);

        mvc.perform(put("/api/v1/road-segments/{id}", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // AC-006: HTTP 200 com dados atualizados
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trecho Atualizado"))
                .andExpect(jsonPath("$.lengthMeters").value(2500.00));
    }

    // ---- AC-007: DELETE /{id} retorna 204 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void delete_returns204() throws Exception {
        doNothing().when(service).delete(segmentId);

        mvc.perform(delete("/api/v1/road-segments/{id}", segmentId)
                        .with(csrf()))
                // AC-007: HTTP 204 No Content
                .andExpect(status().isNoContent());

        verify(service).delete(segmentId);
    }

    // ---- Não autenticado → 401 ----

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mvc.perform(get("/api/v1/road-segments"))
                .andExpect(status().isUnauthorized());
    }

    // ---- LEN-01: PATCH /{id}/length retorna 200 com length manual e reason ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_returns200WithManualLengthAndReason() throws Exception {
        String justification = "GPS trace impreciso na curva leste";
        RoadSegmentResponse overridden = new RoadSegmentResponse(
                segmentId, municipalityId, roadId,
                "Trecho 01", 1, sampleGeo,
                new BigDecimal("950.00"), justification, RoadCondition.UNKNOWN,
                null, true, false,
                LocalDateTime.now(), LocalDateTime.now());

        when(service.overrideLength(eq(segmentId), any())).thenReturn(overridden);

        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("950.00"), justification);

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-01: HTTP 200
                .andExpect(status().isOk())
                // LEN-01: lengthMeters = valor manual
                .andExpect(jsonPath("$.lengthMeters").value(950.00))
                // LEN-06: lengthOverrideReason exposto
                .andExpect(jsonPath("$.lengthOverrideReason").value(justification));
    }

    // ---- LEN-04: justification ausente/vazia → 400 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_missingJustification_returns400() throws Exception {
        String bodyWithoutJustification = """
                {"lengthMeters": 950.00}
                """;

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutJustification))
                // LEN-04: justification obrigatória → 400
                .andExpect(status().isBadRequest());
    }

    // ---- LEN-04: justification < 10 chars → 400 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_shortJustification_returns400() throws Exception {
        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("950.00"), "Curto");

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-04: justification < 10 chars → 400
                .andExpect(status().isBadRequest());
    }

    // ---- LEN-05: justification > 500 chars → 400 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_tooLongJustification_returns400() throws Exception {
        String tooLong = "A".repeat(501);
        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("950.00"), tooLong);

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-05: justification > 500 chars → 400
                .andExpect(status().isBadRequest());
    }

    // ---- LEN-02 (controller): não-admin → 403 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_serviceThrowsForbidden_returns403() throws Exception {
        when(service.overrideLength(eq(segmentId), any()))
                .thenThrow(new com.jettch.sisgev.shared.exception.BusinessException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN_LENGTH_OVERRIDE",
                        "Override restrito a admin"));

        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("950.00"), "Justificativa valida aqui");

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-02: 403 para não-admin
                .andExpect(status().isForbidden());
    }

    // ---- LEN-03 (controller): lengthMeters ≤ 0 → 422 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_negativeLength_returns422() throws Exception {
        when(service.overrideLength(eq(segmentId), any()))
                .thenThrow(new com.jettch.sisgev.shared.exception.BusinessException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_LENGTH",
                        "length_meters deve ser maior que zero"));

        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("-1.00"), "Justificativa valida aqui");

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-03: 422 INVALID_LENGTH
                .andExpect(status().isUnprocessableEntity());
    }

    // ---- LEN-09: PATCH em trecho inexistente → 404 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void overrideLength_nonExistentSegment_returns404() throws Exception {
        when(service.overrideLength(eq(segmentId), any()))
                .thenThrow(new com.jettch.sisgev.shared.exception.BusinessException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));

        LengthOverrideRequest req = new LengthOverrideRequest(new BigDecimal("950.00"), "Justificativa valida aqui");

        mvc.perform(patch("/api/v1/road-segments/{id}/length", segmentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                // LEN-09: 404
                .andExpect(status().isNotFound());
    }

    // ---- SEG-D-04: GET /{id}/history — lista com assessments ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void history_returns200WithItems_whenAssessmentsExist() throws Exception {
        AssessmentSummary item = new AssessmentSummary(
                UUID.randomUUID(), RoadCondition.BAD, 75, "MANUAL", null,
                UUID.randomUUID(), LocalDateTime.now(), null);
        PagedResponse<AssessmentSummary> page = new PagedResponse<>(List.of(item), 0, 20, 1L, 1);
        when(assessmentService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/history", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- SEG-D-05: GET /{id}/history — lista vazia ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void history_returns200WithEmptyList_whenNoAssessments() throws Exception {
        PagedResponse<AssessmentSummary> page = new PagedResponse<>(List.of(), 0, 20, 0L, 0);
        when(assessmentService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/history", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- SEG-D-06: GET /{id}/history — trecho inexistente → 404 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void history_returns404_whenSegmentNotFound() throws Exception {
        when(assessmentService.listBySegment(any(), any()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "SEGMENT_NOT_FOUND", "Trecho não encontrado"));

        mvc.perform(get("/api/v1/road-segments/{id}/history", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---- SEG-D-08: GET /{id}/evidences — lista com evidências ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void evidences_returns200WithItems_whenEvidencesExist() throws Exception {
        EvidenceResponse item = new EvidenceResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "UPLOADED", "https://example.com/file.jpg", null,
                null, null,
                new BigDecimal("-5.1"), new BigDecimal("-39.1"), new BigDecimal("3.0"),
                LocalDateTime.now(), LocalDateTime.now(), null,
                null, null, LocalDateTime.now());
        PagedResponse<EvidenceResponse> page = new PagedResponse<>(List.of(item), 0, 20, 1L, 1);
        when(evidenceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/evidences", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- SEG-D-09: GET /{id}/evidences — lista vazia ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void evidences_returns200WithEmptyList_whenNoEvidences() throws Exception {
        PagedResponse<EvidenceResponse> page = new PagedResponse<>(List.of(), 0, 20, 0L, 0);
        when(evidenceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/evidences", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- SEG-D-10: GET /{id}/evidences — trecho inexistente → 404 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void evidences_returns404_whenSegmentNotFound() throws Exception {
        when(evidenceService.listBySegment(any(), any()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "SEGMENT_NOT_FOUND", "Trecho não encontrado"));

        mvc.perform(get("/api/v1/road-segments/{id}/evidences", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---- SEG-D-12: GET /{id}/occurrences — lista com ocorrências ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void occurrences_returns200WithItems_whenOccurrencesExist() throws Exception {
        OccurrenceSummary item = new OccurrenceSummary(
                UUID.randomUUID(), "POTHOLE", "OPEN", 80,
                "Buraco na pista", UUID.randomUUID(), LocalDateTime.now(), null, null);
        PagedResponse<OccurrenceSummary> page = new PagedResponse<>(List.of(item), 0, 20, 1L, 1);
        when(occurrenceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/occurrences", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- SEG-D-13: GET /{id}/occurrences — lista vazia ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void occurrences_returns200WithEmptyList_whenNoOccurrences() throws Exception {
        PagedResponse<OccurrenceSummary> page = new PagedResponse<>(List.of(), 0, 20, 0L, 0);
        when(occurrenceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/occurrences", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- SEG-D-14: GET /{id}/occurrences — trecho inexistente → 404 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void occurrences_returns404_whenSegmentNotFound() throws Exception {
        when(occurrenceService.listBySegment(any(), any()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "SEGMENT_NOT_FOUND", "Trecho não encontrado"));

        mvc.perform(get("/api/v1/road-segments/{id}/occurrences", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---- SEG-D-16: GET /{id}/maintenance-events — lista com eventos ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void maintenanceEvents_returns200WithItems_whenEventsExist() throws Exception {
        MaintenanceEventSummary item = new MaintenanceEventSummary(
                UUID.randomUUID(), "PATCHING", "PLANNED",
                LocalDate.now().plusDays(7), null, null,
                null, null, UUID.randomUUID(), LocalDateTime.now(), null);
        PagedResponse<MaintenanceEventSummary> page = new PagedResponse<>(List.of(item), 0, 20, 1L, 1);
        when(maintenanceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/maintenance-events", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ---- SEG-D-17: GET /{id}/maintenance-events — lista vazia ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void maintenanceEvents_returns200WithEmptyList_whenNoEvents() throws Exception {
        PagedResponse<MaintenanceEventSummary> page = new PagedResponse<>(List.of(), 0, 20, 0L, 0);
        when(maintenanceService.listBySegment(eq(segmentId), any())).thenReturn(page);

        mvc.perform(get("/api/v1/road-segments/{id}/maintenance-events", segmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ---- SEG-D-18: GET /{id}/maintenance-events — trecho inexistente → 404 ----

    @Test
    @WithMockUser(roles = "ADMIN_OPERACIONAL")
    void maintenanceEvents_returns404_whenSegmentNotFound() throws Exception {
        when(maintenanceService.listBySegment(any(), any()))
                .thenThrow(new BusinessException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "SEGMENT_NOT_FOUND", "Trecho não encontrado"));

        mvc.perform(get("/api/v1/road-segments/{id}/maintenance-events", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
