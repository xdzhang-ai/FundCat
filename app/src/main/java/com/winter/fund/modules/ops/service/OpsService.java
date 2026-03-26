package com.winter.fund.modules.ops.service;

import com.winter.fund.modules.ops.model.FeatureFlagEntity;
import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.modules.ops.repository.FeatureFlagRepository;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.infrastructure.marketdata.FundMarketDataProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpsService {

    private static final Logger log = LoggerFactory.getLogger(OpsService.class);

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

    public boolean isEnabled(String code) {
        return featureFlagRepository.findByCode(code)
            .map(FeatureFlagEntity::isEnabled)
            .orElse(false);
    }

    public OpsDtos.FeatureFlagResponse toggle(String code, boolean enabled) {
        log.info("Toggling feature flag, code={}, enabled={}", code, enabled);
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
