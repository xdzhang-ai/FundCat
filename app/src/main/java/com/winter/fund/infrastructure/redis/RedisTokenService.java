package com.winter.fund.infrastructure.redis;

/**
 * Redis 基础设施文件，负责会话或缓存相关的底层存取。
 */

import com.winter.fund.common.config.AuthProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.model.UserEntity;
import com.winter.fund.modules.auth.service.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "fundcat.auth", name = "session-store", havingValue = "redis")
public class RedisTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenService.class);
    private static final String ACCESS_PREFIX = "fundcat:auth:access:";
    private static final String REFRESH_PREFIX = "fundcat:auth:refresh:";

    private final AuthProperties authProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public RedisTokenService(AuthProperties authProperties, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
        save(accessKey(accessToken), session, Duration.ofMinutes(authProperties.getAccessTokenTtlMinutes()));
        save(refreshKey(refreshToken), session, Duration.ofDays(authProperties.getRefreshTokenTtlDays()));
        log.info("Redis token session issued, userId={}, accessTtlMinutes={}, refreshTtlDays={}",
            user.getId(), authProperties.getAccessTokenTtlMinutes(), authProperties.getRefreshTokenTtlDays());
        return new IssuedTokens(accessToken, refreshToken, authProperties.getAccessTokenTtlMinutes() * 60L);
    }

    @Override
    public Optional<CurrentUser> resolveAccessToken(String token) {
        return find(accessKey(token))
            .filter(session -> !session.accessExpiry().isBefore(clock.instant()))
            .map(session -> new CurrentUser(session.userId(), session.displayName(), session.username()));
    }

    @Override
    public Optional<IssuedTokens> refresh(String token) {
        Optional<StoredSession> session = find(refreshKey(token))
            .filter(stored -> !stored.refreshExpiry().isBefore(clock.instant()));
        if (session.isEmpty()) {
            log.warn("Refresh token rejected from redis store");
            return Optional.empty();
        }
        redisTemplate.delete(accessKey(session.get().accessToken()));
        redisTemplate.delete(refreshKey(token));
        UserEntity user = new UserEntity();
        user.setId(session.get().userId());
        user.setDisplayName(session.get().displayName());
        user.setUsername(session.get().username());
        log.info("Redis refresh token consumed, userId={}", user.getId());
        return Optional.of(issue(user));
    }

    @Override
    public void revokeAccessToken(String token) {
        find(accessKey(token)).ifPresent(session -> {
            redisTemplate.delete(accessKey(session.accessToken()));
            redisTemplate.delete(refreshKey(session.refreshToken()));
            log.info("Redis token session revoked, userId={}", session.userId());
        });
    }

    private Optional<StoredSession> find(String key) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, StoredSession.class));
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key);
            log.warn("Corrupted redis token session removed, key={}", key);
            return Optional.empty();
        }
    }

    private void save(String key, StoredSession session, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist token session", exception);
        }
    }

    private String accessKey(String token) {
        return ACCESS_PREFIX + token;
    }

    private String refreshKey(String token) {
        return REFRESH_PREFIX + token;
    }
}
