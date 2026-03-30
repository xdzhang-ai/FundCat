package com.winter.fund.modules.portfolio.model;

/**
 * 组合与交易模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class PortfolioDtos {

    private PortfolioDtos() {
    }

    @Schema(description = "自选项响应")
    public record WatchlistItemResponse(
        @Schema(description = "基金代码", example = "000001") String code,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String name,
        @Schema(description = "自选备注", example = "AI 主线观察") String note,
        @Schema(description = "估算涨跌幅", example = "2.01") double estimatedGrowth,
        @Schema(description = "确认净值", example = "1.6234") double unitNav,
        @Schema(description = "参考估值净值", example = "1.6351") double estimatedNav,
        @Schema(description = "所属分组", example = "[\"重点观察\",\"AI\"]") List<String> groups,
        @Schema(description = "是否已持仓", example = "true") boolean held
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

    @Schema(description = "操作记录响应")
    public record OperationRecordResponse(
        @Schema(description = "操作记录 ID", example = "op-001") String id,
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String fundName,
        @Schema(description = "操作类型，可能为 BUY / SELL / OPEN_POSITION / CLOSE_POSITION / SIP_BUY", example = "OPEN_POSITION") String operation,
        @Schema(description = "操作来源", example = "MANUAL") String source,
        @Schema(description = "状态", example = "已执行") String status,
        @Schema(description = "交易日期", example = "2026-03-30") String tradeDate,
        @Schema(description = "交易金额", example = "2000") double amount,
        @Schema(description = "份额变化", example = "1234.5600") double shares,
        @Schema(description = "确认净值", example = "1.6234") double nav,
        @Schema(description = "费率", example = "0.0015") double feeRate,
        @Schema(description = "手续费金额", example = "3.00") double feeAmount
    ) {
    }

    @Schema(description = "定投计划响应")
    public record SipPlanResponse(
        @Schema(description = "定投计划 ID", example = "sip-001") String id,
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String fundName,
        @Schema(description = "每期定投金额", example = "1000") double amount,
        @Schema(description = "周期", example = "WEEKLY") String cadence,
        @Schema(description = "下一次执行时间", example = "2026-04-06T15:00:00") String nextRunAt,
        @Schema(description = "兼容字段，是否激活", example = "true") boolean active,
        @Schema(description = "计划状态", example = "生效") String status,
        @Schema(description = "费率", example = "0.0015") double feeRate
    ) {
    }

    @Schema(description = "定投执行记录响应")
    public record SipExecutionRecordResponse(
        @Schema(description = "操作记录 ID", example = "op-sip-001") String id,
        @Schema(description = "定投计划 ID", example = "sip-001") String sipPlanId,
        @Schema(description = "执行日期", example = "2026-03-30") String executedOn,
        @Schema(description = "定投金额", example = "1000") double amount,
        @Schema(description = "执行状态", example = "确认中") String status,
        @Schema(description = "费率", example = "0.0015") double feeRate,
        @Schema(description = "手续费金额", example = "1.50") double feeAmount
    ) {
    }

    public record SipPlanDetailResponse(
        SipPlanResponse plan,
        List<SipExecutionRecordResponse> records
    ) {
    }

    public record SipPlanDigestResponse(
        String id,
        String fundCode,
        String fundName,
        double amount,
        String cadenceLabel,
        String nextRunOn,
        String status
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

    @Schema(description = "新增自选请求")
    public record CreateWatchlistRequest(
        @Schema(description = "基金代码", example = "000001") @NotBlank String fundCode,
        @Schema(description = "备注", example = "重点观察仓位") @NotBlank String note,
        @Schema(description = "分组列表", example = "[\"重点观察\",\"AI\"]") List<String> groups
    ) {
    }

    @Schema(description = "批量替换自选分组请求")
    public record UpdateWatchlistGroupsRequest(
        @Schema(description = "基金代码列表", example = "[\"000001\",\"519674\"]") @NotEmpty List<String> fundCodes,
        @Schema(description = "新的分组列表", example = "[\"核心仓\"]") @NotNull List<String> groups
    ) {
    }

    @Schema(description = "创建定投计划请求")
    public record CreateSipPlanRequest(
        @Schema(description = "兼容字段，当前可不传", example = "portfolio-current") String portfolioId,
        @Schema(description = "基金代码", example = "000001") @NotBlank String fundCode,
        @Schema(description = "每期定投金额", example = "1000") @NotNull @Min(1) Double amount,
        @Schema(description = "周期", example = "WEEKLY") @NotBlank String cadence,
        @Schema(description = "前端选择的下次执行时间", example = "2026-04-06T15:00:00") @NotBlank String nextRunAt,
        @Schema(description = "费率", example = "0.0015") @Min(0) double feeRate
    ) {
    }

    @Schema(description = "更新定投计划请求")
    public record UpdateSipPlanRequest(
        @Schema(description = "每期定投金额", example = "1200") @NotNull @Min(1) Double amount,
        @Schema(description = "周期", example = "MONTHLY") @NotBlank String cadence,
        @Schema(description = "周几执行，按英文星期缩写传值", example = "MONDAY") String weekday,
        @Schema(description = "每月几号执行", example = "15") String monthDay,
        @Schema(description = "费率", example = "0.0015") @Min(0) double feeRate
    ) {
    }

    @Schema(description = "创建 OCR 导入任务请求")
    public record CreateImportJobRequest(
        @Schema(description = "来源平台", example = "支付宝") @NotBlank String sourcePlatform,
        @Schema(description = "文件名", example = "holdings-2026-03-30.png") @NotBlank String fileName
    ) {
    }
}
