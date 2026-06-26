package com.jettch.sisgev.roadsegments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jettch.sisgev.roadsegments.controller.RoadSegmentController;
import com.jettch.sisgev.roadsegments.dto.GeoJsonLineString;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentCreateRequest;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentResponse;
import com.jettch.sisgev.roadsegments.dto.RoadSegmentUpdateRequest;
import com.jettch.sisgev.roadsegments.enums.RoadCondition;
import com.jettch.sisgev.roadsegments.service.RoadSegmentService;
import com.jettch.sisgev.security.JwtService;
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
                new BigDecimal("1250.50"), RoadCondition.UNKNOWN,
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
                new BigDecimal("2500.00"), RoadCondition.UNKNOWN,
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
}
