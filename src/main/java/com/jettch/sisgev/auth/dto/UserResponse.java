package com.jettch.sisgev.auth.dto;

import com.jettch.sisgev.users.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        UUID municipalityId
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getMunicipalityId()
        );
    }
}
