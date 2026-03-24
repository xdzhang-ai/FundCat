package com.fundcat.api.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, StoredSession> accessTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StoredSession> refreshTokens = new ConcurrentHashMap<>();

    public TokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public IssuedTokens issue(UserEntity user) {
        Instant accessExpiry = clock.instant().plus(authProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
        Instant refreshExpiry = clock.instant().plus(authProperties.getRefreshTokenTtlDays(), ChronoUnit.DAYS);
        String accessToken = "fc_at_" + UUID.randomUUID();
        String refreshToken = "fc_rt_" + UUID.randomUUID();
        StoredSession session = new StoredSession(user.getId(), user.getDisplayName(), user.getPhone(), accessExpiry, refreshExpiry);
        accessTokens.put(accessToken, session);
        refreshTokens.put(refreshToken, session);
        return new IssuedTokens(accessToken, refreshToken, authProperties.getAccessTokenTtlMinutes() * 60L);
    }

    public Optional<CurrentUser> resolveAccessToken(String token) {
        cleanupExpiredTokens();
        StoredSession session = accessTokens.get(token);
        if (session == null || session.accessExpiry().isBefore(clock.instant())) {
            accessTokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(new CurrentUser(session.userId(), session.displayName(), session.phone()));
    }

    public Optional<IssuedTokens> refresh(String token) {
        cleanupExpiredTokens();
        StoredSession session = refreshTokens.get(token);
        if (session == null || session.refreshExpiry().isBefore(clock.instant())) {
            refreshTokens.remove(token);
            return Optional.empty();
        }
        UserEntity user = new UserEntity();
        user.setId(session.userId());
        user.setDisplayName(session.displayName());
        user.setPhone(session.phone());
        return Optional.of(issue(user));
    }

    private void cleanupExpiredTokens() {
        Instant now = clock.instant();
        accessTokens.entrySet().removeIf(entry -> entry.getValue().accessExpiry().isBefore(now));
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().refreshExpiry().isBefore(now));
    }

    record StoredSession(String userId, String displayName, String phone, Instant accessExpiry, Instant refreshExpiry) {
    }

    public record IssuedTokens(String accessToken, String refreshToken, long expiresIn) {
    }
}
