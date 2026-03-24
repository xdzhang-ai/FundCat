package com.fundcat.api.auth;

import com.fundcat.api.common.NotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
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
        if (userRepository.findByPhone(request.phone()).isPresent()) {
            throw new IllegalArgumentException("Phone already registered");
        }
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setDisplayName(request.displayName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRiskMode("research");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(user);
        return toAuthResponse(saved, tokenService.issue(saved));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByPhone(request.phone())
            .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Phone or password is incorrect");
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

    private AuthDtos.AuthResponse toAuthResponse(UserEntity user, TokenService.IssuedTokens issuedTokens) {
        return new AuthDtos.AuthResponse(
            issuedTokens.accessToken(),
            issuedTokens.refreshToken(),
            issuedTokens.expiresIn(),
            toProfile(user)
        );
    }

    private AuthDtos.UserProfileResponse toProfile(UserEntity user) {
        return new AuthDtos.UserProfileResponse(user.getId(), user.getDisplayName(), user.getPhone(), user.getRiskMode());
    }
}
