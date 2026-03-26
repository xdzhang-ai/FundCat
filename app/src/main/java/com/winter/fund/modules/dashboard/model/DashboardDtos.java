package com.winter.fund.modules.dashboard.model;

import com.winter.fund.modules.auth.model.AuthDtos;
import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record HeroMetricResponse(String label, String value, String delta, String tone) {
    }

    public record DashboardResponse(
        AuthDtos.UserProfileResponse profile,
        List<HeroMetricResponse> heroMetrics,
        List<OpsDtos.FeatureFlagResponse> featureFlags,
        List<PortfolioDtos.WatchlistItemResponse> watchlist,
        List<PortfolioDtos.PortfolioSummaryResponse> portfolios,
        List<PortfolioDtos.PaperOrderResponse> orders,
        List<PortfolioDtos.SipPlanResponse> sipPlans,
        List<PortfolioDtos.WeeklyReportResponse> reports,
        List<PortfolioDtos.AlertRuleResponse> alerts,
        List<PortfolioDtos.ImportJobResponse> importJobs
    ) {
    }
}
