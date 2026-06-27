package com.jettch.sisgev.audit.service;

import com.jettch.sisgev.audit.entity.AuditLog;
import com.jettch.sisgev.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** BE-25 — Grava registros de auditoria. Falhas não podem quebrar a requisição original. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String action, String entityName, UUID entityId,
                       String ipAddress, String userAgent) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUserId(userId);
            entry.setAction(action);
            entry.setEntityName(entityName);
            entry.setEntityId(entityId);
            entry.setIpAddress(ipAddress);
            entry.setUserAgent(userAgent);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Falha ao gravar auditoria de '{}': {}", action, e.getMessage());
        }
    }
}
