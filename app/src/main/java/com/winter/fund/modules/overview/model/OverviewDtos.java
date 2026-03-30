package com.winter.fund.modules.overview.model;

/**
 * 首页概览模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import com.winter.fund.modules.auth.model.AuthDtos;
import com.winter.fund.modules.dashboard.model.DashboardDtos;
import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class OverviewDtos {

    private OverviewDtos() {
    }

    @Schema(description = "首页核心指标响应")
    public record HeroMetricsResponse(
        @Schema(description = "当前登录用户资料") AuthDtos.UserProfileResponse profile,
        @Schema(description = "首页核心指标列表") List<DashboardDtos.HeroMetricResponse> metrics
    ) {
    }

    @Schema(description = "首页自选脉搏响应")
    public record WatchlistPulseResponse(@Schema(description = "自选基金列表") List<PortfolioDtos.WatchlistItemResponse> items) {
    }

    @Schema(description = "首页最近动作响应")
    public record RecentActionsResponse(@Schema(description = "最近已执行操作列表") List<PortfolioDtos.OperationRecordResponse> items) {
    }

    @Schema(description = "首页定投摘要响应")
    public record SipDigestResponse(@Schema(description = "定投摘要列表") List<PortfolioDtos.SipPlanDigestResponse> items) {
    }
}
