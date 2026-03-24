package com.fundcat.api.fund;

import java.util.List;

public final class FundDtos {

    private FundDtos() {
    }

    public record TrendPoint(String date, double value) {
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
        List<String> topHoldings,
        List<TrendPoint> navHistory,
        List<TrendPoint> estimateHistory,
        List<String> comparisonLabels
    ) {
    }
}
