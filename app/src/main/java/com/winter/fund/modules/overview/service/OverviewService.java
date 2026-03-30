package com.winter.fund.modules.overview.service;

/**
 * 首页概览模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.service.AuthService;
import com.winter.fund.modules.dashboard.model.DashboardDtos;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.overview.model.OverviewDtos;
import com.winter.fund.modules.portfolio.service.PortfolioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {

    private final AuthService authService;
    private final OpsService opsService;
    private final HoldingService holdingService;
    private final PortfolioService portfolioService;

    public OverviewService(
        AuthService authService,
        OpsService opsService,
        HoldingService holdingService,
        PortfolioService portfolioService
    ) {
        this.authService = authService;
        this.opsService = opsService;
        this.holdingService = holdingService;
        this.portfolioService = portfolioService;
    }

    public OverviewDtos.HeroMetricsResponse getHeroMetrics(CurrentUser currentUser) {
        List<OpsDtos.FeatureFlagResponse> featureFlags = opsService.getFeatureFlags();
        Map<String, Boolean> featureFlagMap = featureFlags.stream()
            .collect(Collectors.toMap(OpsDtos.FeatureFlagResponse::code, OpsDtos.FeatureFlagResponse::enabled));
        double totalValue = holdingService.getCurrentHoldings(currentUser.id()).stream()
            .mapToDouble(holding -> holding.getMarketValue())
            .sum();
        double totalPnl = holdingService.getCurrentHoldings(currentUser.id()).stream()
            .mapToDouble(holding -> holding.getHoldingPnl())
            .sum();
        List<DashboardDtos.HeroMetricResponse> metrics = List.of(
            new DashboardDtos.HeroMetricResponse("组合市值", formatCurrency(totalValue), formatDelta(totalPnl), totalPnl >= 0 ? "positive" : "negative"),
            new DashboardDtos.HeroMetricResponse("模拟定投", String.valueOf(portfolioService.getSipPlans(currentUser).size()), "下个执行窗口已排期", "neutral"),
            new DashboardDtos.HeroMetricResponse("OCR 队列", featureFlagMap.getOrDefault("ocr_import", false)
                ? String.valueOf(portfolioService.getImportJobs(currentUser).size())
                : "--", featureFlagMap.getOrDefault("ocr_import", false) ? "研究版可替换识别服务" : "OCR 能力已关闭", "neutral"),
            new DashboardDtos.HeroMetricResponse("高风险开关", String.valueOf(featureFlags.stream().filter(OpsDtos.FeatureFlagResponse::enabled).count()), "按环境灰度", "neutral")
        );
        return new OverviewDtos.HeroMetricsResponse(authService.me(currentUser), metrics);
    }

    public OverviewDtos.WatchlistPulseResponse getWatchlistPulse(CurrentUser currentUser) {
        return new OverviewDtos.WatchlistPulseResponse(portfolioService.getWatchlist(currentUser));
    }

    public OverviewDtos.RecentActionsResponse getRecentActions(CurrentUser currentUser) {
        return new OverviewDtos.RecentActionsResponse(portfolioService.getOrders(currentUser, "recent"));
    }

    public OverviewDtos.SipDigestResponse getSipDigests(CurrentUser currentUser) {
        return new OverviewDtos.SipDigestResponse(portfolioService.getSipDigests(currentUser));
    }

    private String formatCurrency(double value) {
        return "¥" + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatDelta(double value) {
        String prefix = value >= 0 ? "+" : "";
        return prefix + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
