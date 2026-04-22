package com.winter.fund.modules.auth.service;

/**
 * 认证模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.common.config.AuthProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.model.UserEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "fundcat.auth", name = "session-store", havingValue = "memory", matchIfMissing = true)
public class MemoryTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(MemoryTokenService.class);
    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, StoredSession> accessTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StoredSession> refreshTokens = new ConcurrentHashMap<>();

    public MemoryTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * 判断是否sue。
     */
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
        log.info("In-memory token session issued, userId={}, accessCacheSize={}, refreshCacheSize={}",
            user.getId(), accessTokens.size(), refreshTokens.size());
        return new IssuedTokens(accessToken, refreshToken, authProperties.getAccessTokenTtlMinutes() * 60L);
    }

    /**
     * 解析访问令牌。
     */
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

    /**
     * 返回refresh结果。
     */
    @Override
    public Optional<IssuedTokens> refresh(String token) {
        cleanupExpiredTokens();
        StoredSession session = refreshTokens.remove(token);
        if (session == null || session.refreshExpiry().isBefore(clock.instant())) {
            refreshTokens.remove(token);
            log.warn("In-memory refresh token rejected");
            return Optional.empty();
        }
        accessTokens.remove(session.accessToken());
        UserEntity user = new UserEntity();
        user.setId(session.userId());
        user.setDisplayName(session.displayName());
        user.setUsername(session.username());
        log.info("In-memory refresh token consumed, userId={}", user.getId());
        return Optional.of(issue(user));
    }

    /**
     * 撤销访问令牌。
     */
    @Override
    public void revokeAccessToken(String token) {
        cleanupExpiredTokens();
        StoredSession session = accessTokens.remove(token);
        if (session == null) {
            return;
        }
        refreshTokens.remove(session.refreshToken());
        log.info("In-memory token session revoked, userId={}", session.userId());
    }

    /**
     * 清理expiredtokens。
     */
    private void cleanupExpiredTokens() {
        Instant now = clock.instant();
        accessTokens.entrySet().removeIf(entry -> entry.getValue().accessExpiry().isBefore(now));
        refreshTokens.entrySet().removeIf(entry -> entry.getValue().refreshExpiry().isBefore(now));
    }
}
