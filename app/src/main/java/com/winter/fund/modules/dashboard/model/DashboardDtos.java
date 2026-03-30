package com.winter.fund.modules.dashboard.model;

/**
 * 仪表盘模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import com.winter.fund.modules.auth.model.AuthDtos;
import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    @Schema(description = "首页核心指标项")
    public record HeroMetricResponse(
        @Schema(description = "指标名称", example = "组合市值") String label,
        @Schema(description = "主值", example = "¥39680.00") String value,
        @Schema(description = "补充变化值", example = "+1180.50") String delta,
        @Schema(description = "语义色调", example = "positive") String tone
    ) {
    }

    @Schema(description = "兼容版仪表盘聚合响应")
    public record DashboardResponse(
        @Schema(description = "当前用户资料") AuthDtos.UserProfileResponse profile,
        @Schema(description = "首页核心指标") List<HeroMetricResponse> heroMetrics,
        @Schema(description = "功能开关") List<OpsDtos.FeatureFlagResponse> featureFlags,
        @Schema(description = "自选列表") List<PortfolioDtos.WatchlistItemResponse> watchlist,
        @Schema(description = "持仓列表") List<PortfolioDtos.PortfolioSummaryResponse> portfolios,
        @Schema(description = "最近动作") List<PortfolioDtos.OperationRecordResponse> orders,
        @Schema(description = "定投计划") List<PortfolioDtos.SipPlanResponse> sipPlans,
        @Schema(description = "周报") List<PortfolioDtos.WeeklyReportResponse> reports,
        @Schema(description = "提醒规则") List<PortfolioDtos.AlertRuleResponse> alerts,
        @Schema(description = "导入任务") List<PortfolioDtos.ImportJobResponse> importJobs
    ) {
    }
}
