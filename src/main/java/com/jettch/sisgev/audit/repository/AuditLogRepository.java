package com.jettch.sisgev.audit.repository;

import com.jettch.sisgev.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
