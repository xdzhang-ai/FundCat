package com.winter.fund.modules.ops.model;

/**
 * 运维模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public final class OpsDtos {

    private OpsDtos() {
    }

    @Schema(description = "功能开关响应")
    public record FeatureFlagResponse(
        @Schema(description = "开关编码", example = "estimate_reference") String code,
        @Schema(description = "开关名称", example = "盘中参考估值") String name,
        @Schema(description = "是否启用", example = "true") boolean enabled,
        @Schema(description = "环境", example = "research") String environment,
        @Schema(description = "描述", example = "展示盘中参考估值和涨跌幅") String description,
        @Schema(description = "风险等级", example = "high") String riskLevel
    ) {
    }

    @Schema(description = "行情提供方状态")
    public record ProviderStatusResponse(
        @Schema(description = "提供方标识", example = "demo") String providerKey,
        @Schema(description = "状态", example = "UP") String status,
        @Schema(description = "备注", example = "using demo provider") String notes
    ) {
    }

    /**
     * 返回ToggleFlagRequest结果。
     */
    @Schema(description = "切换功能开关请求")
    public record ToggleFlagRequest(@Schema(description = "是否启用", example = "true") boolean enabled) {
    }

    @Schema(description = "运维摘要响应")
    public record OpsSummaryResponse(
        @Schema(description = "功能开关列表") List<FeatureFlagResponse> featureFlags,
        @Schema(description = "市场数据提供方列表") List<ProviderStatusResponse> providers
    ) {
    }

    @Schema(description = "任务运行状态响应")
    public record JobRunResponse(
        @Schema(description = "任务记录 ID", example = "job-run-001") String id,
        @Schema(description = "任务编码", example = "holding_intraday_warmup") String jobCode,
        @Schema(description = "任务来源", example = "JAVA_SCHEDULE") String jobSource,
        @Schema(description = "任务类型", example = "CACHE_WARMUP") String jobType,
        @Schema(description = "运行键", example = "2026-04-03") String runKey,
        @Schema(description = "状态", example = "SUCCESS") String status,
        @Schema(description = "摘要", example = "{\"tradeDate\":\"2026-04-03\"}") String payloadSummary,
        @Schema(description = "总数", example = "120") int statsTotal,
        @Schema(description = "成功数", example = "118") int statsSuccess,
        @Schema(description = "失败数", example = "2") int statsFailed,
        @Schema(description = "跳过数", example = "0") int statsSkipped,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "结束时间") LocalDateTime finishedAt,
        @Schema(description = "耗时毫秒", example = "5234") Long durationMs,
        @Schema(description = "尝试次数", example = "1") int attemptCount,
        @Schema(description = "错误信息", example = "Redis unavailable") String errorMessage
    ) {
    }
}
