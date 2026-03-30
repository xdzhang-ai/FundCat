package com.winter.fund.common.config;

/**
 * 公共配置文件，负责承载全局属性或 Spring 基础设施配置。
 */

import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fundcat.market-data")
public class MarketDataProperties {

    private String timezone = ZoneId.systemDefault().getId();
    private int estimatePollIntervalMinutes = 5;
    private int navPollIntervalMinutes = 20;
    private int tradeBackfillMaxDays = 30;
    private LocalTime estimateTradingStart = LocalTime.of(9, 30);
    private LocalTime estimateTradingEnd = LocalTime.of(15, 0);
    private LocalTime navPollStart = LocalTime.of(20, 0);
    private LocalTime navPollEnd = LocalTime.of(23, 59);
    private LocalTime navBackupPollStart = LocalTime.of(8, 0);
    private LocalTime navBackupPollEnd = LocalTime.of(10, 0);

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getEstimatePollIntervalMinutes() {
        return estimatePollIntervalMinutes;
    }

    public void setEstimatePollIntervalMinutes(int estimatePollIntervalMinutes) {
        this.estimatePollIntervalMinutes = estimatePollIntervalMinutes;
    }

    public int getNavPollIntervalMinutes() {
        return navPollIntervalMinutes;
    }

    public void setNavPollIntervalMinutes(int navPollIntervalMinutes) {
        this.navPollIntervalMinutes = navPollIntervalMinutes;
    }

    public int getTradeBackfillMaxDays() {
        return tradeBackfillMaxDays;
    }

    public void setTradeBackfillMaxDays(int tradeBackfillMaxDays) {
        this.tradeBackfillMaxDays = tradeBackfillMaxDays;
    }

    public LocalTime getEstimateTradingStart() {
        return estimateTradingStart;
    }

    public void setEstimateTradingStart(LocalTime estimateTradingStart) {
        this.estimateTradingStart = estimateTradingStart;
    }

    public LocalTime getEstimateTradingEnd() {
        return estimateTradingEnd;
    }

    public void setEstimateTradingEnd(LocalTime estimateTradingEnd) {
        this.estimateTradingEnd = estimateTradingEnd;
    }

    public LocalTime getNavPollStart() {
        return navPollStart;
    }

    public void setNavPollStart(LocalTime navPollStart) {
        this.navPollStart = navPollStart;
    }

    public LocalTime getNavPollEnd() {
        return navPollEnd;
    }

    public void setNavPollEnd(LocalTime navPollEnd) {
        this.navPollEnd = navPollEnd;
    }

    public LocalTime getNavBackupPollStart() {
        return navBackupPollStart;
    }

    public void setNavBackupPollStart(LocalTime navBackupPollStart) {
        this.navBackupPollStart = navBackupPollStart;
    }

    public LocalTime getNavBackupPollEnd() {
        return navBackupPollEnd;
    }

    public void setNavBackupPollEnd(LocalTime navBackupPollEnd) {
        this.navBackupPollEnd = navBackupPollEnd;
    }
}
