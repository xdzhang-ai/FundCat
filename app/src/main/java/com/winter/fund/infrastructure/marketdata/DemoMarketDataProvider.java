package com.winter.fund.infrastructure.marketdata;

/**
 * 行情基础设施适配文件，负责抽象或实现外部市场数据来源。
 */

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
