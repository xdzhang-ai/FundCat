package com.fundcat.api.dashboard;

import com.fundcat.api.auth.AuthService;
import com.fundcat.api.auth.CurrentUser;
import com.fundcat.api.ops.OpsDtos;
import com.fundcat.api.ops.OpsService;
import com.fundcat.api.portfolio.PortfolioDtos;
import com.fundcat.api.portfolio.PortfolioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final AuthService authService;
    private final OpsService opsService;
    private final PortfolioService portfolioService;

    public DashboardService(AuthService authService, OpsService opsService, PortfolioService portfolioService) {
        this.authService = authService;
        this.opsService = opsService;
        this.portfolioService = portfolioService;
    }

    public DashboardDtos.DashboardResponse getDashboard(CurrentUser currentUser) {
        List<PortfolioDtos.PortfolioSummaryResponse> portfolios = portfolioService.getPortfolios(currentUser);
        double totalValue = portfolios.stream().mapToDouble(PortfolioDtos.PortfolioSummaryResponse::marketValue).sum();
        double totalPnl = portfolios.stream().mapToDouble(PortfolioDtos.PortfolioSummaryResponse::totalPnl).sum();
        List<DashboardDtos.HeroMetricResponse> heroMetrics = List.of(
            new DashboardDtos.HeroMetricResponse("组合市值", formatCurrency(totalValue), formatDelta(totalPnl), totalPnl >= 0 ? "positive" : "negative"),
            new DashboardDtos.HeroMetricResponse("模拟定投", String.valueOf(portfolioService.getSipPlans(currentUser).size()), "下个执行窗口已排期", "neutral"),
            new DashboardDtos.HeroMetricResponse("OCR 队列", String.valueOf(portfolioService.getImportJobs(currentUser).size()), "研究版可替换识别服务", "neutral"),
            new DashboardDtos.HeroMetricResponse("高风险开关", String.valueOf(opsService.getFeatureFlags().stream().filter(OpsDtos.FeatureFlagResponse::enabled).count()), "按环境灰度", "neutral")
        );
        return new DashboardDtos.DashboardResponse(
            authService.me(currentUser),
            heroMetrics,
            opsService.getFeatureFlags(),
            portfolioService.getWatchlist(currentUser),
            portfolios,
            portfolioService.getOrders(currentUser),
            portfolioService.getSipPlans(currentUser),
            portfolioService.getReports(currentUser),
            portfolioService.getAlerts(currentUser),
            portfolioService.getImportJobs(currentUser)
        );
    }

    private String formatCurrency(double value) {
        return "¥" + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatDelta(double value) {
        String prefix = value >= 0 ? "+" : "";
        return prefix + BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
