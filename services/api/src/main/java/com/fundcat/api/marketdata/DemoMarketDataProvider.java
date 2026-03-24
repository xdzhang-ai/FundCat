package com.fundcat.api.marketdata;

import org.springframework.stereotype.Component;

@Component
public class DemoMarketDataProvider implements FundMarketDataProvider {

    @Override
    public String providerKey() {
        return "demo-aggregator";
    }

    @Override
    public String status() {
        return "healthy";
    }

    @Override
    public String notes() {
        return "Research-mode provider adapter with replaceable aggregation interface.";
    }
}
