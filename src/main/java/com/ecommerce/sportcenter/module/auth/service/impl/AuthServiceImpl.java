package com.ecommerce.sportcenter.module.auth.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.sportcenter.module.auth.entity.RefreshToken;
import com.ecommerce.sportcenter.module.user.dto.mapper.UserMapper;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.auth.repository.RefreshTokenRepository;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import com.ecommerce.sportcenter.module.auth.dto.request.LoginRequest;
import com.ecommerce.sportcenter.module.auth.dto.request.RefreshRequest;
import com.ecommerce.sportcenter.module.auth.dto.response.AuthResponse;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import com.ecommerce.sportcenter.module.base.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import com.ecommerce.sportcenter.module.auth.service.AuthService;
import com.ecommerce.sportcenter.module.auth.service.RevocationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevocationService revocationService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = (User) auth.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, refreshToken);
        return toAuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String username;
        String jti;
        try {
            username = jwtService.extractUsername(request.getRefreshToken());
            jti = jwtService.extractJti(request.getRefreshToken());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }
        RefreshToken stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())
                || !stored.getTokenHash().equals(sha256(request.getRefreshToken()))) {
            // Possible token reuse/theft: kill all sessions for this user.
            refreshTokenRepository.revokeAllByUserId(user.getId());
            throw new BadCredentialsException("Invalid refresh token");
        }
        try {
            if (!jwtService.isRefreshTokenValid(request.getRefreshToken(), user)) {
                throw new BadCredentialsException("Invalid refresh token");
            }
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token expired or invalid");
        }
        // Rotation: revoke the used token, issue a new pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        storeRefreshToken(user, refreshToken);
        return toAuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String username, String accessToken, RefreshRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String jti = jwtService.extractJti(request.getRefreshToken());
            refreshTokenRepository.findByJti(jti).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        }
        if (accessToken != null && !accessToken.isBlank()) {
            String jti = jwtService.extractJti(accessToken);
            revocationService.revokeAccess(jti, jwtService.extractExpiration(accessToken).toInstant());
        }
    }

    @Override
    @Transactional
    public void logoutAll(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        log.info("Get current user - username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("User is disabled");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessValidationException("New password must be different from old password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // Force re-login on all devices after password change.
        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password changed - username={}", username);
    }

    private void storeRefreshToken(User user, String refreshToken) {
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .jti(jwtService.extractJti(refreshToken))
                .tokenHash(sha256(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .revoked(false)
                .build());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private AuthResponse toAuthResponse(String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
