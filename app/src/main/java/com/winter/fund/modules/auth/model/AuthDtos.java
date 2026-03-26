package com.winter.fund.modules.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "username is invalid") String username,
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 30) String displayName,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "username is invalid") String username,
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserProfileResponse(String id, String displayName, String username, String riskMode) {
    }

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileResponse profile
    ) {
    }
}
