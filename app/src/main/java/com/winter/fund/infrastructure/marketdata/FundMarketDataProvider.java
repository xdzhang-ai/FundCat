package com.winter.fund.infrastructure.marketdata;

/**
 * 行情基础设施适配文件，负责抽象或实现外部市场数据来源。
 */

public interface FundMarketDataProvider {

    String providerKey();

    String status();

    String notes();
}
