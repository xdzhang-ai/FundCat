package com.winter.fund.modules.portfolio.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class PortfolioDtos {

    private PortfolioDtos() {
    }

    public record WatchlistItemResponse(
        String code,
        String name,
        String note,
        double estimatedGrowth,
        double unitNav,
        double estimatedNav
    ) {
    }

    public record HoldingLotResponse(
        String id,
        String fundCode,
        String fundName,
        double shares,
        double averageCost,
        double currentValue,
        double pnl,
        double allocation,
        String source,
        String updatedAt
    ) {
    }

    public record PortfolioSummaryResponse(
        String id,
        String name,
        String broker,
        String currency,
        double marketValue,
        double totalPnl,
        double cash,
        List<HoldingLotResponse> holdings
    ) {
    }

    public record PaperOrderResponse(
        String id,
        String fundCode,
        String fundName,
        String orderType,
        double amount,
        double shares,
        double fee,
        String status,
        String executedAt
    ) {
    }

    public record SipPlanResponse(
        String id,
        String fundCode,
        String fundName,
        double amount,
        String cadence,
        String nextRunAt,
        boolean active
    ) {
    }

    public record WeeklyReportResponse(
        String id,
        String weekLabel,
        String summary,
        double returnRate,
        String bestFundCode,
        String riskNote
    ) {
    }

    public record AlertRuleResponse(
        String id,
        String fundCode,
        String ruleType,
        double thresholdValue,
        boolean enabled,
        String channel
    ) {
    }

    public record ImportJobResponse(
        String id,
        String sourcePlatform,
        String status,
        String fileName,
        int recognizedHoldings,
        String createdAt
    ) {
    }

    public record CreateWatchlistRequest(@NotBlank String fundCode, @NotBlank String note) {
    }

    public record CreatePaperOrderRequest(
        @NotBlank String portfolioId,
        @NotBlank String fundCode,
        @NotBlank String orderType,
        @Min(1) double amount,
        @Min(0) double shares,
        @Min(0) double fee,
        @NotBlank String note
    ) {
    }

    public record CreateSipPlanRequest(
        @NotBlank String portfolioId,
        @NotBlank String fundCode,
        @NotNull @Min(1) Double amount,
        @NotBlank String cadence,
        @NotBlank String nextRunAt
    ) {
    }

    public record CreateImportJobRequest(@NotBlank String sourcePlatform, @NotBlank String fileName) {
    }
}
