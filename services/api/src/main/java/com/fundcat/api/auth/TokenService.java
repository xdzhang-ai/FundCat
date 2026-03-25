package com.fundcat.api.auth;

import java.time.Instant;
import java.util.Optional;

public interface TokenService {

    IssuedTokens issue(UserEntity user);

    Optional<CurrentUser> resolveAccessToken(String token);

    Optional<IssuedTokens> refresh(String token);

    void revokeAccessToken(String token);

    record StoredSession(
        String userId,
        String displayName,
        String username,
        String accessToken,
        String refreshToken,
        Instant accessExpiry,
        Instant refreshExpiry
    ) {
    }

    record IssuedTokens(String accessToken, String refreshToken, long expiresIn) {
    }
}
