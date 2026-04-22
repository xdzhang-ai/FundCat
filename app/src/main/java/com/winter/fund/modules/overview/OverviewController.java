package com.winter.fund.modules.overview;

/**
 * 首页概览模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.overview.model.OverviewDtos;
import com.winter.fund.modules.overview.service.OverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/overview")
@Tag(name = "Overview", description = "首页分片接口")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /**
     * 返回heroMetrics结果。
     */
    @GetMapping("/hero-metrics")
    @Operation(summary = "首页核心指标", description = "返回首页 hero 区域需要的用户资料和核心指标。")
    public OverviewDtos.HeroMetricsResponse heroMetrics(@AuthenticationPrincipal CurrentUser currentUser) {
        return overviewService.getHeroMetrics(currentUser);
    }

    /**
     * 获取自选列表脉搏。
     */
    @GetMapping("/watchlist-pulse")
    @Operation(summary = "首页自选脉搏", description = "返回首页自选卡片列表。")
    public OverviewDtos.WatchlistPulseResponse watchlistPulse(@AuthenticationPrincipal CurrentUser currentUser) {
        return overviewService.getWatchlistPulse(currentUser);
    }

    /**
     * 返回recentActions结果。
     */
    @GetMapping("/recent-actions")
    @Operation(summary = "首页最近动作", description = "返回首页最近执行过的买卖与定投动作。")
    public OverviewDtos.RecentActionsResponse recentActions(@AuthenticationPrincipal CurrentUser currentUser) {
        return overviewService.getRecentActions(currentUser);
    }

    /**
     * 返回sipDigests结果。
     */
    @GetMapping("/sip-digests")
    @Operation(summary = "首页定投摘要", description = "返回首页展示的定投计划摘要。")
    public OverviewDtos.SipDigestResponse sipDigests(@AuthenticationPrincipal CurrentUser currentUser) {
        return overviewService.getSipDigests(currentUser);
    }
}
