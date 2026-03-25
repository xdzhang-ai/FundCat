package com.fundcat.api.fund;

import java.util.List;

public final class FundDtos {

    private FundDtos() {
    }

    public record TrendPoint(String date, double value) {
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

    public record IndustryExposureResponse(String name, double weight) {
    }

    public record TopHoldingResponse(String name, String symbol, double dailyChange) {
    }

    public record FundCardResponse(
        String code,
        String name,
        String category,
        String riskLevel,
        List<String> tags,
        String benchmark,
        double unitNav,
        double dayGrowth,
        double estimatedNav,
        double estimatedGrowth,
        boolean referenceOnly
    ) {
    }

    public record FundDetailResponse(
        String code,
        String name,
        String category,
        String riskLevel,
        List<String> tags,
        String benchmark,
        double unitNav,
        double dayGrowth,
        double estimatedNav,
        double estimatedGrowth,
        boolean referenceOnly,
        double managementFee,
        double custodyFee,
        double purchaseFee,
        double assetSize,
        double stockRatio,
        double bondRatio,
        List<TopHoldingResponse> topHoldings,
        List<TrendPoint> navHistory,
        List<TrendPoint> estimateHistory,
        List<String> comparisonLabels,
        List<QuarterlyHoldingResponse> quarterlyHoldings,
        List<IndustryExposureResponse> industryDistribution
    ) {
    }
}
