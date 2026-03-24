package com.fundcat.api.ops;

import java.util.List;

public final class OpsDtos {

    private OpsDtos() {
    }

    public record FeatureFlagResponse(
        String code,
        String name,
        boolean enabled,
        String environment,
        String description,
        String riskLevel
    ) {
    }

    public record ProviderStatusResponse(String providerKey, String status, String notes) {
    }

    public record ToggleFlagRequest(boolean enabled) {
    }

    public record OpsSummaryResponse(
        List<FeatureFlagResponse> featureFlags,
        List<ProviderStatusResponse> providers
    ) {
    }
}
