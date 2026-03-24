package com.fundcat.api.ops;

import com.fundcat.api.common.NotFoundException;
import com.fundcat.api.marketdata.FundMarketDataProvider;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpsService {

    private final FeatureFlagRepository featureFlagRepository;
    private final List<FundMarketDataProvider> providers;

    public OpsService(FeatureFlagRepository featureFlagRepository, List<FundMarketDataProvider> providers) {
        this.featureFlagRepository = featureFlagRepository;
        this.providers = providers;
    }

    public OpsDtos.OpsSummaryResponse getSummary() {
        return new OpsDtos.OpsSummaryResponse(getFeatureFlags(), getProviders());
    }

    public List<OpsDtos.FeatureFlagResponse> getFeatureFlags() {
        return featureFlagRepository.findAllByOrderByCreatedAtAsc().stream()
            .map(flag -> new OpsDtos.FeatureFlagResponse(
                flag.getCode(),
                flag.getName(),
                flag.isEnabled(),
                flag.getEnvironment(),
                flag.getDescription(),
                flag.getRiskLevel()
            ))
            .toList();
    }

    public OpsDtos.FeatureFlagResponse toggle(String code, boolean enabled) {
        FeatureFlagEntity flag = featureFlagRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Feature flag not found"));
        flag.setEnabled(enabled);
        featureFlagRepository.save(flag);
        return new OpsDtos.FeatureFlagResponse(
            flag.getCode(),
            flag.getName(),
            flag.isEnabled(),
            flag.getEnvironment(),
            flag.getDescription(),
            flag.getRiskLevel()
        );
    }

    public List<OpsDtos.ProviderStatusResponse> getProviders() {
        return providers.stream()
            .map(provider -> new OpsDtos.ProviderStatusResponse(provider.providerKey(), provider.status(), provider.notes()))
            .toList();
    }
}
