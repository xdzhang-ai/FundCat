package com.fundcat.api.auth;

import com.fundcat.api.common.NotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
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
            throw new IllegalArgumentException("用户名已存在");
        }
        return toAuthResponse(saved, tokenService.issue(saved));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Username or password is incorrect");
        }
        return toAuthResponse(user, tokenService.issue(user));
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        TokenService.IssuedTokens tokens = tokenService.refresh(request.refreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid or expired"));
        CurrentUser currentUser = tokenService.resolveAccessToken(tokens.accessToken())
            .orElseThrow(() -> new IllegalArgumentException("Unable to rebuild session"));
        UserEntity user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return toAuthResponse(user, tokens);
    }

    public AuthDtos.UserProfileResponse me(CurrentUser currentUser) {
        UserEntity user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return toProfile(user);
    }

    public void logout(String authorizationHeader) {
        String accessToken = extractBearerToken(authorizationHeader);
        tokenService.revokeAccessToken(accessToken);
    }

    private AuthDtos.AuthResponse toAuthResponse(UserEntity user, TokenService.IssuedTokens issuedTokens) {
        return new AuthDtos.AuthResponse(
            issuedTokens.accessToken(),
            issuedTokens.refreshToken(),
            issuedTokens.expiresIn(),
            toProfile(user)
        );
    }

    private AuthDtos.UserProfileResponse toProfile(UserEntity user) {
        return new AuthDtos.UserProfileResponse(user.getId(), user.getDisplayName(), user.getUsername(), user.getRiskMode());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
