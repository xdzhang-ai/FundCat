package com.winter.fund.modules.auth.service;

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.model.UserEntity;
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
