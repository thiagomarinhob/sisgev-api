package com.jettch.sisgev.users.dto;

import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID municipalityId,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getMunicipalityId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt()
        );
    }
}
