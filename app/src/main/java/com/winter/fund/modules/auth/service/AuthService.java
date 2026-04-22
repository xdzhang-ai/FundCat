package com.winter.fund.modules.auth.service;

/**
 * 认证模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.auth.model.AuthDtos;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.model.UserEntity;
import com.winter.fund.modules.auth.repository.UserRepository;
import com.winter.fund.common.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * 返回register结果。
     */
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        log.info("Registering new user, username={}", request.username());
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setDisplayName(request.displayName());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRiskMode("research");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        UserEntity saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Registration rejected due to duplicate username, username={}", request.username());
            throw new IllegalArgumentException("用户名已存在");
        }
        log.info("User persisted successfully, userId={}, username={}", saved.getId(), saved.getUsername());
        return toAuthResponse(saved, tokenService.issue(saved));
    }

    /**
     * 返回login结果。
     */
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        log.info("Login attempt, username={}", request.username());
        UserEntity user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed due to password mismatch, username={}", request.username());
            throw new IllegalArgumentException("Username or password is incorrect");
        }
        log.info("Login succeeded, userId={}", user.getId());
        return toAuthResponse(user, tokenService.issue(user));
    }

    /**
     * 返回refresh结果。
     */
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        log.info("Refreshing access token");
        TokenService.IssuedTokens tokens = tokenService.refresh(request.refreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or expired"));
        CurrentUser currentUser = tokenService.resolveAccessToken(tokens.accessToken())
            .orElseThrow(() -> new IllegalArgumentException("Unable to rebuild session"));
        UserEntity user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new NotFoundException("User not found"));
        log.info("Access token refreshed, userId={}", user.getId());
        return toAuthResponse(user, tokens);
    }

    /**
     * 返回me结果。
     */
    public AuthDtos.UserProfileResponse me(CurrentUser currentUser) {
        UserEntity user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return toProfile(user);
    }

    /**
     * 执行logout流程。
     */
    public void logout(String authorizationHeader) {
        String accessToken = extractBearerToken(authorizationHeader);
        log.info("Revoking access token");
        tokenService.revokeAccessToken(accessToken);
        log.info("Access token revoked");
    }

    /**
     * 转换为认证响应。
     */
    private AuthDtos.AuthResponse toAuthResponse(UserEntity user, TokenService.IssuedTokens issuedTokens) {
        return new AuthDtos.AuthResponse(
            issuedTokens.accessToken(),
            issuedTokens.refreshToken(),
            issuedTokens.expiresIn(),
            toProfile(user)
        );
    }

    /**
     * 转换为资料。
     */
    private AuthDtos.UserProfileResponse toProfile(UserEntity user) {
        return new AuthDtos.UserProfileResponse(user.getId(), user.getDisplayName(), user.getUsername(), user.getRiskMode());
    }

    /**
     * 返回extractBearerToken结果。
     */
    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
