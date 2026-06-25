package com.jettch.sisgev.evidences.dto;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dados do upload de evidência (carrier interno montado pelo controller a partir do multipart).
 * {@code fieldAgentId}/{@code municipalityId} NÃO entram aqui — vêm do usuário autenticado.
 */
public record EvidenceUploadCommand(
        MultipartFile file,
        UUID clientUuid,
        UUID inspectionClientUuid,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal gpsAccuracyMeters,
        LocalDateTime takenAt,
        String fieldNote
) {
}
