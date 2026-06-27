package com.jettch.sisgev.maintenance.service;

import com.jettch.sisgev.maintenance.dto.MaintenanceCreateRequest;
import com.jettch.sisgev.maintenance.dto.MaintenanceEventSummary;
import com.jettch.sisgev.maintenance.dto.MaintenanceFinishRequest;
import com.jettch.sisgev.maintenance.dto.MaintenanceUpdateRequest;
import com.jettch.sisgev.maintenance.entity.MaintenanceEvent;
import com.jettch.sisgev.maintenance.enums.MaintenanceStatus;
import com.jettch.sisgev.maintenance.repository.MaintenanceEventRepository;
import com.jettch.sisgev.roadsegments.entity.RoadSegment;
import com.jettch.sisgev.roadsegments.repository.RoadSegmentRepository;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BE-23 — Intervenções: CRUD + transições (start/finish/cancel).
 * Conclusão registra finished_date e km recuperados (RF-MNT-002, §20.7).
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceEventRepository maintenanceEventRepository;
    private final RoadSegmentRepository segmentRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceEventSummary> listBySegment(UUID segmentId, Pageable pageable) {
        RoadSegment segment = loadSegment(segmentId);
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());
        return PagedResponse.from(
                maintenanceEventRepository.findByRoadSegmentIdOrderByCreatedAtDesc(segmentId, pageable)
                        .map(MaintenanceEventSummary::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<MaintenanceEventSummary> list(MaintenanceStatus status, Pageable pageable) {
        User user = currentUser.getCurrentUser();
        String st = status != null ? status.name() : null;
        Page<MaintenanceEvent> page;
        if (user.isSuperAdmin()) {
            page = st != null ? maintenanceEventRepository.findByStatus(st, pageable)
                    : maintenanceEventRepository.findAll(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = st != null ? maintenanceEventRepository.findByMunicipalityIdAndStatus(user.getMunicipalityId(), st, pageable)
                    : maintenanceEventRepository.findByMunicipalityId(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(MaintenanceEventSummary::from));
    }

    @Transactional(readOnly = true)
    public MaintenanceEventSummary get(UUID id) {
        MaintenanceEvent event = findById(id);
        currentUser.assertCanAccessMunicipality(event.getMunicipalityId());
        return MaintenanceEventSummary.from(event);
    }

    @Transactional
    public MaintenanceEventSummary create(MaintenanceCreateRequest request) {
        RoadSegment segment = loadSegment(request.roadSegmentId());
        User user = currentUser.getCurrentUser();
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(segment.getMunicipalityId());

        MaintenanceEvent event = new MaintenanceEvent();
        event.setMunicipalityId(segment.getMunicipalityId());
        event.setRoadSegmentId(segment.getId());
        event.setOccurrenceId(request.occurrenceId());
        event.setType(request.type().trim());
        event.setStatus(MaintenanceStatus.PLANNED.name());
        event.setPlannedStartDate(request.plannedStartDate());
        event.setNotes(request.notes());
        event.setCreatedBy(user.getId());
        return MaintenanceEventSummary.from(maintenanceEventRepository.save(event));
    }

    @Transactional
    public MaintenanceEventSummary update(UUID id, MaintenanceUpdateRequest request) {
        MaintenanceEvent event = reviewable(id);
        event.setType(request.type().trim());
        event.setPlannedStartDate(request.plannedStartDate());
        event.setNotes(request.notes());
        return MaintenanceEventSummary.from(maintenanceEventRepository.save(event));
    }

    @Transactional
    public MaintenanceEventSummary start(UUID id) {
        MaintenanceEvent event = reviewable(id);
        event.setStatus(MaintenanceStatus.IN_PROGRESS.name());
        if (event.getActualStartDate() == null) {
            event.setActualStartDate(LocalDate.now());
        }
        return MaintenanceEventSummary.from(maintenanceEventRepository.save(event));
    }

    @Transactional
    public MaintenanceEventSummary finish(UUID id, MaintenanceFinishRequest request) {
        MaintenanceEvent event = reviewable(id);
        event.setStatus(MaintenanceStatus.FINISHED.name());
        event.setFinishedDate(request.finishedDate());
        if (request.repairedLengthMeters() != null) {
            event.setRepairedLengthMeters(request.repairedLengthMeters());
        }
        if (request.notes() != null) {
            event.setNotes(request.notes());
        }
        return MaintenanceEventSummary.from(maintenanceEventRepository.save(event));
    }

    @Transactional
    public MaintenanceEventSummary cancel(UUID id) {
        MaintenanceEvent event = reviewable(id);
        event.setStatus(MaintenanceStatus.CANCELLED.name());
        return MaintenanceEventSummary.from(maintenanceEventRepository.save(event));
    }

    /** Carrega a intervenção e valida papel de revisor + tenancy. */
    private MaintenanceEvent reviewable(UUID id) {
        MaintenanceEvent event = findById(id);
        currentUser.assertReviewer();
        currentUser.assertCanAccessMunicipality(event.getMunicipalityId());
        return event;
    }

    private MaintenanceEvent findById(UUID id) {
        return maintenanceEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "MAINTENANCE_NOT_FOUND", "Intervenção não encontrada"));
    }

    private RoadSegment loadSegment(UUID segmentId) {
        return segmentRepository.findByIdAndDeletedAtIsNull(segmentId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "SEGMENT_NOT_FOUND", "Trecho não encontrado"));
    }
}
