package com.jettch.sisgev.shared.security;

import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.enums.UserRole;
import com.jettch.sisgev.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * BE-04 — Isolamento multi-tenant por município.
 *
 * Resolve o usuário autenticado (o {@code JwtAuthFilter} coloca o {@code userId}
 * como principal) e oferece os guards de tenancy reutilizáveis por todos os módulos.
 * Refs: RN-001, RN-030 · backend §11.2.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /** Usuário autenticado a partir do contexto de segurança. 401 se ausente/inválido. */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Usuário não autenticado");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Usuário não encontrado"));
    }

    public UUID getCurrentMunicipalityId() {
        return getCurrentUser().getMunicipalityId();
    }

    /**
     * Garante que o usuário pode acessar dados do município informado.
     * SUPER_ADMIN acessa qualquer um; os demais só o próprio. Caso contrário, 403.
     * Nunca confiar em municipalityId vindo do payload/URL — sempre validar contra o do usuário.
     */
    public void assertCanAccessMunicipality(UUID municipalityId) {
        User user = getCurrentUser();
        if (user.isSuperAdmin()) {
            return;
        }
        if (!Objects.equals(user.getMunicipalityId(), municipalityId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_MUNICIPALITY", "Acesso negado ao município");
        }
    }

    /** Exige papel SUPER_ADMIN. Caso contrário, 403. */
    public void assertSuperAdmin() {
        if (!getCurrentUser().isSuperAdmin()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Ação restrita a SUPER_ADMIN");
        }
    }

    /**
     * Exige SUPER_ADMIN ou ADMIN_OPERACIONAL (gestão de usuários/papéis é ação sensível, RN-018).
     * Caso contrário, 403.
     */
    public void assertCanManageUsers() {
        User user = getCurrentUser();
        if (user.isSuperAdmin() || user.getRole() == UserRole.ADMIN_OPERACIONAL) {
            return;
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Ação restrita a administradores");
    }
}
