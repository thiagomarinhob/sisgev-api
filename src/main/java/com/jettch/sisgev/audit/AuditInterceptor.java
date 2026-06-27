package com.jettch.sisgev.audit;

import com.jettch.sisgev.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BE-25 — Audita ações críticas por requisição: escritas (POST/PUT/PATCH/DELETE) e
 * exportações de relatório bem-sucedidas sob /api/v1. Registra usuário, ação, entidade,
 * IP e user agent (RN-018 · §9.5).
 */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final AuditService auditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!shouldAudit(request, response)) {
            return;
        }
        String uri = request.getRequestURI();
        auditService.record(
                currentUserId(),
                request.getMethod() + " " + uri,
                entityName(uri),
                lastUuid(uri),
                clientIp(request),
                request.getHeader("User-Agent"));
    }

    private boolean shouldAudit(HttpServletRequest request, HttpServletResponse response) {
        if (response.getStatus() >= 400) {
            return false;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/")) {
            return false;
        }
        boolean isExport = uri.contains("/reports/");
        return MUTATING.contains(request.getMethod()) || isExport;
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }

    /** Primeiro segmento após /api/v1/ (ex.: "municipalities", "auth", "reports"). */
    private String entityName(String uri) {
        String path = uri.substring("/api/v1/".length());
        int slash = path.indexOf('/');
        String name = slash >= 0 ? path.substring(0, slash) : path;
        return name.isBlank() ? "unknown" : name;
    }

    /** Último UUID presente na URI (geralmente o id da entidade alvo), ou null. */
    private UUID lastUuid(String uri) {
        Matcher m = UUID_PATTERN.matcher(uri);
        String last = null;
        while (m.find()) {
            last = m.group();
        }
        return last != null ? UUID.fromString(last) : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
