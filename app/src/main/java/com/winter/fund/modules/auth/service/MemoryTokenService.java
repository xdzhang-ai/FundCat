package com.winter.fund.modules.auth.service;

import com.winter.fund.common.config.AuthProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.model.UserEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "fundcat.auth", name = "session-store", havingValue = "memory", matchIfMissing = true)
public class MemoryTokenService implements TokenService {

    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, StoredSession> accessTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StoredSession> refreshTokens = new ConcurrentHashMap<>();

    public MemoryTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public IssuedTokens issue(UserEntity user) {
        Instant accessExpiry = clock.instant().plus(authProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
        Instant refreshExpiry = clock.instant().plus(authProperties.getRefreshTokenTtlDays(), ChronoUnit.DAYS);
        String accessToken = "fc_at_" + UUID.randomUUID();
        String refreshToken = "fc_rt_" + UUID.randomUUID();
        StoredSession session = new StoredSession(
            user.getId(),
            user.getDisplayName(),
            user.getUsername(),
            accessToken,
            refreshToken,
            accessExpiry,
            refreshExpiry
        );
        accessTokens.put(accessToken, session);
        refreshTokens.put(refreshToken, session);
        return new IssuedTokens(accessToken, refreshToken, authProperties.getAccessTokenTtlMinutes() * 60L);
    }

    @Override
    public Optional<CurrentUser> resolveAccessToken(String token) {
        cleanupExpiredTokens();
        StoredSession session = accessTokens.get(token);
        if (session == null || session.accessExpiry().isBefore(clock.instant())) {
            accessTokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(new CurrentUser(session.userId(), session.displayName(), session.username()));
    }

    @Override
    public Optional<IssuedTokens> refresh(String token) {
        cleanupExpiredTokens();
        StoredSession session = refreshTokens.remove(token);
        if (session == null || session.refreshExpiry().isBefore(clock.instant())) {
            refreshTokens.remove(token);
            return Optional.empty();
        }
        accessTokens.remove(session.accessToken());
        UserEntity user = new UserEntity();
        user.setId(session.userId());
        user.setDisplayName(session.displayName());
        user.setUsername(session.username());
        return Optional.of(issue(user));
    }

    @Override
    public void revokeAccessToken(String token) {
        cleanupExpiredTokens();
        StoredSession session = accessTokens.remove(token);
        if (session == null) {
            return;
        }
        refreshTokens.remove(session.refreshToken());
    }

    private void cleanupExpiredTokens() {
        Instant now = clock.instant();
        accessTokens.entrySet().removeIf(entry -> entry.getValue().accessExpiry().isBefore(now));
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().refreshExpiry().isBefore(now));
    }
}
