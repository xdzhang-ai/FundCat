package com.winter.fund.infrastructure.redis;

/**
 * 持仓当前态缓存服务，负责把日中高频读取的持仓结果缓存在 Redis 中。
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.fund.modules.holding.model.HoldingDtos;
import java.time.LocalDate;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class HoldingCurrentStateCacheService {

    private static final Logger log = LoggerFactory.getLogger(HoldingCurrentStateCacheService.class);
    private static final Duration TTL = Duration.ofHours(26);
    private static final String OVERVIEW_PREFIX = "fundcat:holding:overview:";
    private static final String HELD_CODES_PREFIX = "fundcat:holding:held-codes:";
    private static final String INSIGHT_PREFIX = "fundcat:holding:insight:";
    private static final String INTRADAY_PREFIX = "fundcat:holding:intraday:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public HoldingCurrentStateCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    /**
     * 读取持仓总览缓存。
     */
    public Optional<HoldingDtos.HoldingsOverviewResponse> getOverview(String userId) {
        return read(overviewKey(userId), new TypeReference<>() {
        });
    }

    /**
     * 写入持仓总览缓存。
     */
    public void putOverview(String userId, HoldingDtos.HoldingsOverviewResponse response) {
        write(overviewKey(userId), response);
    }

    /**
     * 读取单基金洞察缓存。
     */
    public Optional<HoldingDtos.HoldingInsightResponse> getInsight(String userId, String fundCode) {
        return read(insightKey(userId, fundCode), new TypeReference<>() {
        });
    }

    /**
     * 写入单基金洞察缓存。
     */
    public void putInsight(String userId, String fundCode, HoldingDtos.HoldingInsightResponse response) {
        write(insightKey(userId, fundCode), response);
    }

    /**
     * 删除单基金洞察缓存。
     */
    public void evictInsight(String userId, String fundCode) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(insightKey(userId, fundCode));
    }

    /**
     * 读取持有基金代码集合缓存。
     */
    public Optional<Set<String>> getHeldCodes(String userId) {
        return read(heldCodesKey(userId), new TypeReference<>() {
        });
    }

    /**
     * 写入持有基金代码集合缓存。
     */
    public void putHeldCodes(String userId, Set<String> heldCodes) {
        write(heldCodesKey(userId), heldCodes);
    }

    /**
     * 读取单基金日间动态态缓存。
     */
    public Optional<IntradayHoldingState> getIntradayState(String userId, String fundCode, LocalDate tradeDate) {
        return read(intradayKey(userId, fundCode, tradeDate), new TypeReference<>() {
        });
    }

    /**
     * 写入单基金日间动态态缓存。
     */
    public void putIntradayState(String userId, String fundCode, LocalDate tradeDate, IntradayHoldingState state) {
        write(intradayKey(userId, fundCode, tradeDate), state);
    }

    /**
     * 删除单基金某日的动态态缓存。
     */
    public void evictIntradayState(String userId, String fundCode, LocalDate tradeDate) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(intradayKey(userId, fundCode, tradeDate));
    }

    /**
     * 按基金和交易日批量删除所有用户的动态态缓存。
     * 当晚确认净值后的正式回算已经完成，此时应该清空白天的估值态缓存，避免继续展示临时口径。
     */
    public void evictIntradayStatesForFund(String fundCode, LocalDate tradeDate) {
        if (redisTemplate == null) {
            return;
        }
        Set<String> keys = redisTemplate.keys(INTRADAY_PREFIX + "*:" + fundCode + ":" + tradeDate);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        redisTemplate.delete(keys);
    }

    /**
     * 统一读取并反序列化 Redis 值。
     */
    private <T> Optional<T> read(String key, TypeReference<T> typeReference) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, typeReference));
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key);
            log.warn("Removed unreadable holding cache, key={}", key);
            return Optional.empty();
        }
    }

    /**
     * 统一写入 Redis 并刷新 TTL。
     */
    private void write(String key, Object value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist holding cache", exception);
        }
    }

    /**
     * 构建总览缓存 key。
     */
    private String overviewKey(String userId) {
        return OVERVIEW_PREFIX + userId;
    }

    /**
     * 构建持有基金集合缓存 key。
     */
    private String heldCodesKey(String userId) {
        return HELD_CODES_PREFIX + userId;
    }

    /**
     * 构建单基金洞察缓存 key。
     */
    private String insightKey(String userId, String fundCode) {
        return INSIGHT_PREFIX + userId + ":" + fundCode;
    }

    /**
     * 构建单基金日间动态态 key。
     */
    private String intradayKey(String userId, String fundCode, LocalDate tradeDate) {
        return INTRADAY_PREFIX + userId + ":" + fundCode + ":" + tradeDate;
    }

    /**
     * 日间持仓动态态。
     * 这份数据只服务白天展示，不是最终事实来源；晚上确认净值后会被主动清除。
     */
    public record IntradayHoldingState(
        String userId,
        String fundCode,
        String fundName,
        LocalDate tradeDate,
        LocalDate baselineDate,
        double shares,
        double averageCost,
        double baselineNav,
        double currentNav,
        double marketValue,
        double dailyPnl,
        double holdingPnl,
        String lastEstimateAt,
        String source
    ) {
    }
}
