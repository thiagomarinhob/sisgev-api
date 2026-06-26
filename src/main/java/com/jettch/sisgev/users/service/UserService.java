package com.jettch.sisgev.users.service;

import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.shared.response.PagedResponse;
import com.jettch.sisgev.shared.security.CurrentUserService;
import com.jettch.sisgev.users.dto.UserCreateRequest;
import com.jettch.sisgev.users.dto.UserResponse;
import com.jettch.sisgev.users.dto.UserUpdateRequest;
import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.enums.UserRole;
import com.jettch.sisgev.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BE-07 — CRUD de usuários com papéis, isolado por município (RN-001).
 * Refs: §12, §20.1.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final CurrentUserService currentUser;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        currentUser.assertCanManageUsers();
        UUID municipalityId = resolveTargetMunicipality(request.role(), request.municipalityId());
        assertEmailAvailable(request.email(), null);

        User user = new User();
        user.setMunicipalityId(municipalityId);
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        return UserResponse.from(repository.save(user));
    }

    /** SUPER_ADMIN lista todos; demais veem apenas usuários do próprio município. */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> list(Pageable pageable) {
        User user = currentUser.getCurrentUser();
        Page<User> page;
        if (user.isSuperAdmin()) {
            page = repository.findAllByDeletedAtIsNull(pageable);
        } else if (user.getMunicipalityId() != null) {
            page = repository.findAllByMunicipalityIdAndDeletedAtIsNull(user.getMunicipalityId(), pageable);
        } else {
            page = Page.empty(pageable);
        }
        return PagedResponse.from(page.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        User target = findActive(id);
        currentUser.assertCanAccessMunicipality(target.getMunicipalityId());
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        currentUser.assertCanManageUsers();
        User target = findActive(id);
        currentUser.assertCanAccessMunicipality(target.getMunicipalityId());

        UUID municipalityId = resolveTargetMunicipality(request.role(), request.municipalityId());
        assertEmailAvailable(request.email(), id);

        target.setName(request.name().trim());
        target.setEmail(request.email().trim().toLowerCase());
        target.setRole(request.role());
        target.setMunicipalityId(municipalityId);
        if (request.password() != null && !request.password().isBlank()) {
            target.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(repository.save(target));
    }

    @Transactional
    public UserResponse activate(UUID id) {
        return setActive(id, true);
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        return setActive(id, false);
    }

    /** Exclusão lógica (RN-019): marca deleted_at e desativa. */
    @Transactional
    public void delete(UUID id) {
        currentUser.assertCanManageUsers();
        User target = findActive(id);
        currentUser.assertCanAccessMunicipality(target.getMunicipalityId());
        target.setActive(false);
        target.setDeletedAt(LocalDateTime.now());
        repository.save(target);
    }

    private UserResponse setActive(UUID id, boolean active) {
        currentUser.assertCanManageUsers();
        User target = findActive(id);
        currentUser.assertCanAccessMunicipality(target.getMunicipalityId());
        target.setActive(active);
        return UserResponse.from(repository.save(target));
    }

    /**
     * Valida a regra "municipalityId obrigatório exceto SUPER_ADMIN" (§12) e impede
     * que um não-SUPER_ADMIN crie/promova um usuário a SUPER_ADMIN (escalonamento de privilégio).
     */
    private UUID resolveTargetMunicipality(UserRole role, UUID requestedMunicipalityId) {
        if (role == UserRole.SUPER_ADMIN) {
            currentUser.assertSuperAdmin();
            return null;
        }
        if (requestedMunicipalityId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "municipalityId é obrigatório para o papel " + role);
        }
        currentUser.assertCanAccessMunicipality(requestedMunicipalityId);
        return requestedMunicipalityId;
    }

    private void assertEmailAvailable(String email, UUID ignoreUserId) {
        String normalized = email.trim().toLowerCase();
        boolean taken = ignoreUserId == null
                ? repository.findByEmailAndDeletedAtIsNull(normalized).isPresent()
                : repository.existsByEmailAndDeletedAtIsNullAndIdNot(normalized, ignoreUserId);
        if (taken) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "E-mail já cadastrado");
        }
    }

    private User findActive(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado"));
    }
}
