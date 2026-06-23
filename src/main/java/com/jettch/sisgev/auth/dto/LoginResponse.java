package com.jettch.sisgev.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
