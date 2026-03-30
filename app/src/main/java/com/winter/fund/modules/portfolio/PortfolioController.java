package com.winter.fund.modules.portfolio;

/**
 * 组合与交易模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import com.winter.fund.modules.portfolio.service.PortfolioService;
import com.winter.fund.modules.auth.model.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Portfolio", description = "自选、定投、导入与历史动作接口")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/watchlist")
    @Operation(summary = "获取自选列表", description = "返回当前用户的自选基金、分组和是否已持仓状态。")
    public List<PortfolioDtos.WatchlistItemResponse> watchlist(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getWatchlist(currentUser);
    }

    @PostMapping("/watchlist")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增自选", description = "将基金加入自选列表，并可指定一个或多个分组。")
    public PortfolioDtos.WatchlistItemResponse addWatchlist(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateWatchlistRequest request
    ) {
        return portfolioService.addWatchlist(currentUser, request);
    }

    @PatchMapping("/watchlist/groups")
    @Operation(summary = "批量替换自选分组", description = "对传入基金列表批量替换分组集合。")
    public List<PortfolioDtos.WatchlistItemResponse> updateWatchlistGroups(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.UpdateWatchlistGroupsRequest request
    ) {
        return portfolioService.updateWatchlistGroups(currentUser, request);
    }

    @DeleteMapping("/watchlist/{fundCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除自选", description = "从当前用户自选列表中移除指定基金。")
    public void removeWatchlist(@AuthenticationPrincipal CurrentUser currentUser, @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode) {
        portfolioService.removeWatchlist(currentUser, fundCode);
    }

    @GetMapping("/portfolios")
    @Operation(summary = "获取当前持仓视图", description = "兼容旧页面的持仓聚合接口。")
    public List<PortfolioDtos.PortfolioSummaryResponse> portfolios(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getPortfolios(currentUser);
    }

    @GetMapping("/orders")
    @Operation(summary = "获取最近动作", description = "返回最近已执行的买入、卖出和定投动作。")
    public List<PortfolioDtos.OperationRecordResponse> orders(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "作用域，目前仅 recent 生效", example = "recent")
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "recent") String scope
    ) {
        return portfolioService.getOrders(currentUser, scope);
    }

    @GetMapping("/sips")
    @Operation(summary = "获取定投计划列表", description = "返回当前用户全部定投计划。")
    public List<PortfolioDtos.SipPlanResponse> sips(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getSipPlans(currentUser);
    }

    @PostMapping("/sips")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建定投计划", description = "创建新的定投计划，并写入首个执行窗口前的计划状态。")
    public PortfolioDtos.SipPlanResponse createSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateSipPlanRequest request
    ) {
        return portfolioService.createSipPlan(currentUser, request);
    }

    @GetMapping("/sips/{sipPlanId}")
    @Operation(summary = "获取定投计划详情", description = "返回指定定投计划的详情。")
    public PortfolioDtos.SipPlanResponse sip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return portfolioService.getSipPlan(currentUser, sipPlanId);
    }

    @GetMapping("/sips/{sipPlanId}/records")
    @Operation(summary = "获取定投执行记录", description = "返回指定定投计划的执行记录列表。")
    public List<PortfolioDtos.SipExecutionRecordResponse> sipRecords(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return portfolioService.getSipPlanRecords(currentUser, sipPlanId);
    }

    @PatchMapping("/sips/{sipPlanId}")
    @Operation(summary = "修改定投计划", description = "更新金额、周期和费率，15:00 后修改将归属到下一交易日。")
    public PortfolioDtos.SipPlanResponse updateSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId,
        @Valid @RequestBody PortfolioDtos.UpdateSipPlanRequest request
    ) {
        return portfolioService.updateSipPlan(currentUser, sipPlanId, request);
    }

    @PostMapping("/sips/{sipPlanId}/pause")
    @Operation(summary = "暂停定投", description = "将指定定投计划置为暂停状态。")
    public PortfolioDtos.SipPlanResponse pauseSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return portfolioService.pauseSipPlan(currentUser, sipPlanId);
    }

    @PostMapping("/sips/{sipPlanId}/resume")
    @Operation(summary = "恢复定投", description = "恢复已暂停的定投计划，并重算下一次执行时间。")
    public PortfolioDtos.SipPlanResponse resumeSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return portfolioService.resumeSipPlan(currentUser, sipPlanId);
    }

    @PostMapping("/sips/{sipPlanId}/stop")
    @Operation(summary = "停止定投", description = "将指定定投计划置为停止状态，停止后不可恢复。")
    public PortfolioDtos.SipPlanResponse stopSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "定投计划 ID", example = "sip-001") @PathVariable String sipPlanId
    ) {
        return portfolioService.stopSipPlan(currentUser, sipPlanId);
    }

    @GetMapping("/import-jobs")
    @Operation(summary = "获取导入任务", description = "返回当前用户最近的 OCR 导入任务。")
    public List<PortfolioDtos.ImportJobResponse> importJobs(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getImportJobs(currentUser);
    }

    @PostMapping("/import-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建导入任务", description = "创建 OCR 导入任务占位记录。")
    public PortfolioDtos.ImportJobResponse createImportJob(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateImportJobRequest request
    ) {
        return portfolioService.createImportJob(currentUser, request);
    }

    @GetMapping("/reports/weekly")
    @Operation(summary = "获取周报", description = "返回当前用户的周报列表。")
    public List<PortfolioDtos.WeeklyReportResponse> reports(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getReports(currentUser);
    }

    @GetMapping("/alerts")
    @Operation(summary = "获取提醒规则", description = "返回当前用户的基金提醒规则列表。")
    public List<PortfolioDtos.AlertRuleResponse> alerts(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getAlerts(currentUser);
    }
}
