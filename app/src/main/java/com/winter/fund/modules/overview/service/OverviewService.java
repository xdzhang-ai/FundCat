package com.winter.fund.modules.overview.service;

/**
 * 首页概览模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.auth.service.AuthService;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.overview.model.DashboardDtos;
import com.winter.fund.modules.overview.model.OverviewDtos;
import com.winter.fund.modules.sip.service.SipService;
import com.winter.fund.modules.watchlist.service.WatchlistService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {

    private final AuthService authService;
    private final HoldingService holdingService;
    private final WatchlistService watchlistService;
    private final SipService sipService;

    public OverviewService(
        AuthService authService,
        HoldingService holdingService,
        WatchlistService watchlistService,
        SipService sipService
    ) {
        this.authService = authService;
        this.holdingService = holdingService;
        this.watchlistService = watchlistService;
        this.sipService = sipService;
    }

    /**
     * 获取核心指标。
     */
    public OverviewDtos.HeroMetricsResponse getHeroMetrics(CurrentUser currentUser) {
        List<DashboardDtos.HeroMetricResponse> metrics = List.of(
        );
        return new OverviewDtos.HeroMetricsResponse(authService.me(currentUser), metrics);
    }

    /**
     * 获取自选脉搏。
     */
    public OverviewDtos.WatchlistPulseResponse getWatchlistPulse(CurrentUser currentUser) {
        return new OverviewDtos.WatchlistPulseResponse(watchlistService.getWatchlist(currentUser));
    }

    /**
     * 获取recent动作列表。
     */
    public OverviewDtos.RecentActionsResponse getRecentActions(CurrentUser currentUser) {
        return new OverviewDtos.RecentActionsResponse(holdingService.getOrders(currentUser, "recent"));
    }

    /**
     * 获取定投摘要列表。
     */
    public OverviewDtos.SipDigestResponse getSipDigests(CurrentUser currentUser) {
        return new OverviewDtos.SipDigestResponse(sipService.getSipDigests(currentUser));
    }
}
