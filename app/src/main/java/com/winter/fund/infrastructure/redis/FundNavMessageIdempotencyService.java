package com.winter.fund.infrastructure.redis;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FundNavMessageIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(FundNavMessageIdempotencyService.class);
    private static final String KEY_PREFIX = "fundcat:mq:fund-nav:message:";
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(30);
    private static final Duration FINAL_STATE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public FundNavMessageIdempotencyService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    /**
     * 尝试抢占消息处理权。
     * 同一个 messageId 只允许一个本地异步任务进入处理链路；无 Redis 时退化为始终允许处理。
     */
    public boolean tryAcquire(String messageId) {
        if (redisTemplate == null) {
            return true;
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key(messageId), "PROCESSING", PROCESSING_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 标记消息已成功处理。
     */
    public void markSuccess(String messageId) {
        updateState(messageId, "SUCCESS");
    }

    /**
     * 标记消息最终处理失败。
     */
    public void markFailed(String messageId) {
        updateState(messageId, "FAILED");
    }

    /**
     * 读取当前消息状态，便于日志或排障。
     */
    public String getState(String messageId) {
        if (redisTemplate == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key(messageId));
    }

    private void updateState(String messageId, String state) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.opsForValue().set(key(messageId), state, FINAL_STATE_TTL);
        log.debug("Updated fund nav message state, messageId={}, state={}", messageId, state);
    }

    private String key(String messageId) {
        return KEY_PREFIX + messageId;
    }
}
