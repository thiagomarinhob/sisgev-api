package com.jettch.sisgev.municipalities.dto;

import com.jettch.sisgev.municipalities.entity.Municipality;

import java.time.LocalDateTime;
import java.util.UUID;

public record MunicipalityResponse(
        UUID id,
        String name,
        String state,
        String ibgeCode,
        boolean active,
        LocalDateTime createdAt
) {
    public static MunicipalityResponse from(Municipality m) {
        return new MunicipalityResponse(
                m.getId(),
                m.getName(),
                m.getState(),
                m.getIbgeCode(),
                m.isActive(),
                m.getCreatedAt()
        );
    }
}
