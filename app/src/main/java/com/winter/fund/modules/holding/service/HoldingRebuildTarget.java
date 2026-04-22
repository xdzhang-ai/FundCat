package com.winter.fund.modules.holding.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标记一组需要重建持仓快照的目标范围。
 * 确认记录时仍然逐条保留原始操作，但真正回算时会把同一用户同一基金的多条影响合并成一条，
 * 并从最早受影响日期开始统一重建。
 */
public record HoldingRebuildTarget(String userId, String fundCode, LocalDate earliestTradeDate) {

    /**
     * 合并同一用户同一基金的多个重建目标，并保留最早交易日。
     */
    public static List<HoldingRebuildTarget> merge(Collection<HoldingRebuildTarget> targets) {
        Map<String, HoldingRebuildTarget> merged = new LinkedHashMap<>();
        for (HoldingRebuildTarget target : targets) {
            String key = target.userId() + "|" + target.fundCode();
            HoldingRebuildTarget existing = merged.get(key);
            if (existing == null || target.earliestTradeDate().isBefore(existing.earliestTradeDate())) {
                merged.put(key, target);
            }
        }
        return List.copyOf(merged.values());
    }
}
