package com.fundcat.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[0-9+\\-]{6,20}$", message = "phone is invalid") String phone,
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 30) String displayName,
        @NotBlank @Pattern(regexp = "^[0-9+\\-]{6,20}$", message = "phone is invalid") String phone,
        @NotBlank @Size(min = 6, max = 100) String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserProfileResponse(String id, String displayName, String phone, String riskMode) {
    }

    public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserProfileResponse profile
    ) {
    }
}
