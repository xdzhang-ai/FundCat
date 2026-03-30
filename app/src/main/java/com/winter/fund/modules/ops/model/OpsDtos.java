package com.winter.fund.modules.ops.model;

/**
 * 运维模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "切换功能开关请求")
    public record ToggleFlagRequest(@Schema(description = "是否启用", example = "true") boolean enabled) {
    }

    @Schema(description = "运维摘要响应")
    public record OpsSummaryResponse(
        @Schema(description = "功能开关列表") List<FeatureFlagResponse> featureFlags,
        @Schema(description = "市场数据提供方列表") List<ProviderStatusResponse> providers
    ) {
    }
}
