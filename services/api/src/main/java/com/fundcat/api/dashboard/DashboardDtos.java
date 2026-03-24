package com.fundcat.api.dashboard;

import com.fundcat.api.auth.AuthDtos;
import com.fundcat.api.ops.OpsDtos;
import com.fundcat.api.portfolio.PortfolioDtos;
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
