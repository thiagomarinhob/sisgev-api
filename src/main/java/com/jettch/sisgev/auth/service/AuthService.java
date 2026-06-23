package com.jettch.sisgev.auth.service;

import com.jettch.sisgev.auth.dto.*;
import com.jettch.sisgev.auth.entity.RefreshToken;
import com.jettch.sisgev.auth.repository.RefreshTokenRepository;
import com.jettch.sisgev.config.JwtProperties;
import com.jettch.sisgev.security.JwtService;
import com.jettch.sisgev.shared.exception.BusinessException;
import com.jettch.sisgev.users.entity.User;
import com.jettch.sisgev.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID", "Credenciais inválidas"));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken.getToken(), UserResponse.from(user));
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Refresh token inválido"));

        if (!refreshToken.isValid()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED_OR_REVOKED", "Refresh token expirado ou revogado");
        }

        String newAccessToken = jwtService.generateAccessToken(refreshToken.getUser());
        return new RefreshResponse(newAccessToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken()).ifPresent(rt -> {
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado"));
        return UserResponse.from(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpirationDays()));
        return refreshTokenRepository.save(rt);
    }
}
