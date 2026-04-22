package com.winter.fund.modules.holding.model;

/**
 * 持仓模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class HoldingDtos {

    private HoldingDtos() {
    }

    @Schema(description = "持仓操作记录")
    public record HoldingOperationResponse(
        @Schema(description = "操作记录 ID", example = "op-001") String id,
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "操作类型，可能为 BUY / SELL / OPEN_POSITION / CLOSE_POSITION / SIP_BUY；手工买卖在确认中阶段会先保留 BUY/SELL", example = "OPEN_POSITION") String operation,
        @Schema(description = "操作来源", example = "MANUAL") String source,
        @Schema(description = "执行状态，支持确认中 / 已执行", example = "确认中") String status,
        @Schema(description = "生效交易日期，前端会先根据 15:00 前后换算最终成交日后再传给后端", example = "2026-03-30") String tradeDate,
        @Schema(description = "交易金额，卖出在确认中阶段可能为 0，结算后为卖出毛金额", example = "2000") double amount,
        @Schema(description = "份额变化，卖出时为负数；买入在确认中阶段为 0", example = "1234.5600") double sharesDelta,
        @Schema(description = "交易日期对应的确认净值，确认中阶段为 0", example = "1.6234") double nav,
        @Schema(description = "费率，按 0~1 传值", example = "0.0015") double feeRate,
        @Schema(description = "手续费金额", example = "3.00") double feeAmount
    ) {
    }

    @Schema(description = "最近动作响应")
    public record OperationRecordResponse(
        @Schema(description = "操作记录 ID", example = "op-001") String id,
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String fundName,
        @Schema(description = "操作类型，可能为 BUY / SELL / OPEN_POSITION / CLOSE_POSITION / SIP_BUY；手工买卖确认中阶段会先显示 BUY/SELL", example = "OPEN_POSITION") String operation,
        @Schema(description = "操作来源", example = "MANUAL") String source,
        @Schema(description = "状态，支持确认中 / 已执行", example = "确认中") String status,
        @Schema(description = "交易日期", example = "2026-03-30") String tradeDate,
        @Schema(description = "交易金额，确认中卖出阶段可能暂为 0", example = "2000") double amount,
        @Schema(description = "份额变化，确认中买入阶段可能暂为 0", example = "1234.5600") double shares,
        @Schema(description = "确认净值，确认中阶段为 0", example = "1.6234") double nav,
        @Schema(description = "费率", example = "0.0015") double feeRate,
        @Schema(description = "手续费金额", example = "3.00") double feeAmount
    ) {
    }

    @Schema(description = "持仓列表项")
    public record HoldingListItemResponse(
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String fundName,
        @Schema(description = "当日涨跌幅", example = "1.28") double dayGrowth,
        @Schema(description = "当日盈亏", example = "86.23") double todayPnl,
        @Schema(description = "当前持仓金额", example = "12880.50") double marketValue,
        @Schema(description = "当前持有收益", example = "1180.50") double holdingPnl,
        @Schema(description = "持仓占比，百分比值", example = "32.4500") double allocation
    ) {
    }

    @Schema(description = "持仓总览响应")
    public record HoldingsOverviewResponse(
        @Schema(description = "全部持仓总金额", example = "39680.00") double totalMarketValue,
        @Schema(description = "持仓明细列表") List<HoldingListItemResponse> items
    ) {
    }

    @Schema(description = "基金持仓洞察")
    public record HoldingInsightResponse(
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "当前持有金额", example = "12880.50") double amountHeld,
        @Schema(description = "当前持有收益", example = "1180.50") double holdingPnl,
        @Schema(description = "当前持有收益率", example = "10.09") double holdingReturnRate,
        @Schema(description = "当前持有份额", example = "7800.0000") double shares,
        @Schema(description = "平均成本", example = "1.5000") double averageCost,
        @Schema(description = "持仓占比，百分比值", example = "32.4500") double allocation,
        @Schema(description = "当日涨跌幅", example = "1.28") Double dayChange,
        @Schema(description = "当日盈亏", example = "86.23") Double todayPnl,
        @Schema(description = "上一交易日盈亏", example = "54.12") double yesterdayPnl,
        @Schema(description = "近一年收益率", example = "19.36") double oneYearReturn,
        @Schema(description = "持有天数", example = "122") long holdingDays
    ) {
    }

    @Schema(description = "加持仓或修改持仓请求")
    public record UpsertHoldingRequest(
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "金额口径，T_MINUS_1 表示按 T-1 净值反推，T 表示按 T 日净值口径反推", example = "T_MINUS_1")
        @NotBlank String amountBasis,
        @Schema(description = "持有金额", example = "12880.50")
        @NotNull @Min(1) Double amount,
        @Schema(description = "持有收益", example = "1180.50")
        @NotNull Double holdingPnl
    ) {
    }

    @Schema(description = "手工买入或卖出请求")
    public record CreateHoldingOperationRequest(
        @Schema(description = "基金代码", example = "000001") @NotBlank String fundCode,
        @Schema(description = "操作类型，BUY 为按金额买入，SELL 为按份额卖出；后端会自动归类成建仓或清仓", example = "BUY") @NotBlank String operation,
        @Schema(description = "最终成交日，仅允许近 30 天内或下一交易日；若该日确认净值尚未公布则先记为确认中", example = "2026-03-31") @NotBlank String tradeDate,
        @Schema(description = "买入金额，仅 BUY 需要传", example = "2000") Double amount,
        @Schema(description = "卖出份额，仅 SELL 需要传", example = "300.50") Double shares,
        @Schema(description = "费率，按 0~1 传值", example = "0.0015") @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double feeRate,
        @Schema(description = "备注", example = "补记支付宝买入") String note
    ) {
    }
}
