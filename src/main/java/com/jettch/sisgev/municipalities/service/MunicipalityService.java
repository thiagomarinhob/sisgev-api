package com.jettch.sisgev.municipalities.service;

import com.jettch.sisgev.municipalities.dto.MunicipalityRequest;
import com.jettch.sisgev.municipalities.dto.MunicipalityResponse;
import com.jettch.sisgev.municipalities.entity.Municipality;
import com.jettch.sisgev.municipalities.repository.MunicipalityRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BE-06 — CRUD de Municípios, com isolamento multi-tenant (BE-04).
 * Refs: RF-MUN-001/002 · §20.2.
 */
@Service
@RequiredArgsConstructor
public class MunicipalityService {

    private final MunicipalityRepository repository;
    private final CurrentUserService currentUser;

    @Transactional
    public MunicipalityResponse create(MunicipalityRequest request) {
        currentUser.assertSuperAdmin();
        Municipality municipality = new Municipality();
        apply(municipality, request);
        municipality.setActive(true);
        return MunicipalityResponse.from(repository.save(municipality));
    }

    /** SUPER_ADMIN lista todos; demais veem apenas o próprio município. */
    @Transactional(readOnly = true)
    public PagedResponse<MunicipalityResponse> list(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<Municipality> page;
        if (user.isSuperAdmin()) {
            page = repository.findAllByDeletedAtIsNull(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = repository.findByIdAndDeletedAtIsNull(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(MunicipalityResponse::from));
    }

    @Transactional(readOnly = true)
    public MunicipalityResponse get(UUID id) {
        currentUser.assertCanAccessMunicipality(id);
        return MunicipalityResponse.from(findActive(id));
    }

    @Transactional
    public MunicipalityResponse update(UUID id, MunicipalityRequest request) {
        currentUser.assertCanAccessMunicipality(id);
        Municipality municipality = findActive(id);
        apply(municipality, request);
        return MunicipalityResponse.from(repository.save(municipality));
    }

    /** Exclusão lógica (RN-019): marca deleted_at e desativa. Restrito a SUPER_ADMIN. */
    @Transactional
    public void delete(UUID id) {
        currentUser.assertSuperAdmin();
        Municipality municipality = findActive(id);
        municipality.setActive(false);
        municipality.setDeletedAt(LocalDateTime.now());
        repository.save(municipality);
    }

    private void apply(Municipality municipality, MunicipalityRequest request) {
        municipality.setName(request.name().trim());
        municipality.setState(request.state().toUpperCase());
        municipality.setIbgeCode(request.ibgeCode());
    }

    private Municipality findActive(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "MUNICIPALITY_NOT_FOUND", "Município não encontrado"));
    }
}
