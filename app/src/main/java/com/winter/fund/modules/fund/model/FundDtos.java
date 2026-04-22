package com.winter.fund.modules.fund.model;

/**
 * 基金模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public final class FundDtos {

    private FundDtos() {
    }

    @Schema(description = "趋势图点位")
    public record TrendPoint(
        @Schema(description = "日期或时间点", example = "2026-03-30") String date,
        @Schema(description = "对应净值", example = "1.6234") double value
    ) {
    }

    public record QuarterlyHoldingResponse(
        String name,
        String symbol,
        double dailyChange,
        double positionRatio,
        Double previousChange,
        String changeLabel
    ) {
    }

    /**
     * 返回IndustryExposureResponse结果。
     */
    public record IndustryExposureResponse(String name, double weight) {
    }

    /**
     * 返回TopHoldingResponse结果。
     */
    public record TopHoldingResponse(String name, String symbol, double dailyChange) {
    }

    @Schema(description = "基金卡片响应")
    public record FundCardResponse(
        @Schema(description = "基金代码", example = "000001") String code,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String name,
        @Schema(description = "标签", example = "[\"AI\",\"高弹性\",\"观察池\"]") List<String> tags,
        @Schema(description = "确认净值", example = "1.6234") double unitNav,
        @Schema(description = "确认涨跌幅", example = "1.28") double dayGrowth,
        @Schema(description = "参考估值净值", example = "1.6351") double estimatedNav,
        @Schema(description = "参考估值涨跌幅", example = "2.01") double estimatedGrowth,
        @Schema(description = "是否仅参考估值", example = "true") boolean referenceOnly,
        @Schema(description = "是否已加入自选", example = "true") boolean watchlisted,
        @Schema(description = "是否已持仓", example = "false") boolean held
    ) {
    }

    @Schema(description = "基金详情响应")
    public record FundDetailResponse(
        @Schema(description = "基金代码", example = "000001") String code,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String name,
        @Schema(description = "标签", example = "[\"AI\",\"高弹性\",\"观察池\"]") List<String> tags,
        @Schema(description = "确认净值", example = "1.6234") double unitNav,
        @Schema(description = "确认涨跌幅", example = "1.28") double dayGrowth,
        @Schema(description = "参考估值净值", example = "1.6351") double estimatedNav,
        @Schema(description = "参考估值涨跌幅", example = "2.01") double estimatedGrowth,
        @Schema(description = "是否仅参考估值", example = "true") boolean referenceOnly,
        @Schema(description = "是否已加入自选", example = "true") boolean watchlisted,
        @Schema(description = "是否已持仓", example = "true") boolean held,
        @Schema(description = "前十大持仓") List<TopHoldingResponse> topHoldings,
        @Schema(description = "净值趋势") List<TrendPoint> navHistory,
        @Schema(description = "估值趋势") List<TrendPoint> estimateHistory,
        @Schema(description = "比较标签") List<String> comparisonLabels,
        @Schema(description = "季度持仓") List<QuarterlyHoldingResponse> quarterlyHoldings,
        @Schema(description = "行业分布") List<IndustryExposureResponse> industryDistribution
    ) {
    }

    @Schema(description = "基金用户状态响应")
    public record FundUserStateResponse(
        @Schema(description = "基金代码", example = "000001") String code,
        @Schema(description = "是否已加入自选", example = "true") boolean watchlisted,
        @Schema(description = "是否已持仓", example = "false") boolean held
    ) {
    }
}
