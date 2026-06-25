package com.jettch.sisgev.inspections.service;

import com.jettch.sisgev.inspections.dto.InspectionResponse;
import com.jettch.sisgev.inspections.dto.InspectionSyncRequest;
import com.jettch.sisgev.inspections.dto.InspectionSyncResult;
import com.jettch.sisgev.inspections.entity.Inspection;
import com.jettch.sisgev.inspections.enums.InspectionStatus;
import com.jettch.sisgev.inspections.repository.InspectionRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BE-13 — Vistorias (inspections): criação, sincronização idempotente e finalização.
 * Refs: RF-INSP, RN-025 (idempotência), RN-017 (autoria), RN-001 (multi-tenant).
 */
@Service
@RequiredArgsConstructor
public class InspectionService {

    private final InspectionRepository repository;
    private final CurrentUserService currentUser;

    /** POST /inspections — cria a vistoria (idempotente por client_uuid do agente). */
    @Transactional
    public InspectionSyncResult create(InspectionSyncRequest request) {
        return upsert(request, false);
    }

    /** POST /inspections/sync — idempotente e marca a vistoria como sincronizada. */
    @Transactional
    public InspectionSyncResult sync(InspectionSyncRequest request) {
        return upsert(request, true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InspectionResponse> list(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<Inspection> page;
        if (user.isSuperAdmin()) {
            page = repository.findAll(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = repository.findByMunicipalityId(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(InspectionResponse::from));
    }

    @Transactional(readOnly = true)
    public InspectionResponse get(UUID id) {
        Inspection inspection = findById(id);
        currentUser.assertCanAccessMunicipality(inspection.getMunicipalityId());
        return InspectionResponse.from(inspection);
    }

    /** PATCH /inspections/{id}/finish — encerra a vistoria. */
    @Transactional
    public InspectionResponse finish(UUID id) {
        Inspection inspection = findById(id);
        currentUser.assertCanAccessMunicipality(inspection.getMunicipalityId());
        if (inspection.getFinishedAt() == null) {
            inspection.setFinishedAt(LocalDateTime.now());
        }
        inspection.setStatus(InspectionStatus.CLOSED);
        return InspectionResponse.from(repository.save(inspection));
    }

    private InspectionSyncResult upsert(InspectionSyncRequest request, boolean markSynced) {
        User user = currentUser.getCurrentUser();
        if (user.getMunicipalityId() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_MUNICIPALITY", "Usuário sem município não pode registrar vistorias");
        }

        var existing = repository.findByFieldAgentIdAndClientUuid(user.getId(), request.clientUuid());
        if (existing.isPresent()) {
            return new InspectionSyncResult(markSynced(existing.get(), markSynced), false);
        }

        Inspection inspection = new Inspection();
        inspection.setMunicipalityId(user.getMunicipalityId());
        inspection.setFieldAgentId(user.getId());
        inspection.setClientUuid(request.clientUuid());
        inspection.setStartedAt(request.startedAt());
        inspection.setFinishedAt(request.finishedAt());
        inspection.setNotes(request.notes());
        if (markSynced) {
            inspection.setSyncedAt(LocalDateTime.now());
            inspection.setStatus(InspectionStatus.SYNCED);
        } else {
            inspection.setStatus(request.finishedAt() != null
                    ? InspectionStatus.CLOSED : InspectionStatus.IN_PROGRESS);
        }

        try {
            return new InspectionSyncResult(InspectionResponse.from(repository.saveAndFlush(inspection)), true);
        } catch (DataIntegrityViolationException race) {
            // Corrida: outro request inseriu o mesmo (field_agent_id, client_uuid) primeiro.
            Inspection winner = repository.findByFieldAgentIdAndClientUuid(user.getId(), request.clientUuid())
                    .orElseThrow(() -> race);
            return new InspectionSyncResult(markSynced(winner, markSynced), false);
        }
    }

    private InspectionResponse markSynced(Inspection inspection, boolean markSynced) {
        if (markSynced && inspection.getSyncedAt() == null) {
            inspection.setSyncedAt(LocalDateTime.now());
            inspection.setStatus(InspectionStatus.SYNCED);
            inspection = repository.save(inspection);
        }
        return InspectionResponse.from(inspection);
    }

    private Inspection findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "INSPECTION_NOT_FOUND", "Vistoria não encontrada"));
    }
}
