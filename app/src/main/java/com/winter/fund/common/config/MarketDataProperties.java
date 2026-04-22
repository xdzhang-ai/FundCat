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

    /**
     * 获取timezone。
     */
    public String getTimezone() {
        return timezone;
    }

    /**
     * 设置timezone。
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * 获取估值pollintervalminutes。
     */
    public int getEstimatePollIntervalMinutes() {
        return estimatePollIntervalMinutes;
    }

    /**
     * 设置估值pollintervalminutes。
     */
    public void setEstimatePollIntervalMinutes(int estimatePollIntervalMinutes) {
        this.estimatePollIntervalMinutes = estimatePollIntervalMinutes;
    }

    /**
     * 获取净值pollintervalminutes。
     */
    public int getNavPollIntervalMinutes() {
        return navPollIntervalMinutes;
    }

    /**
     * 设置净值pollintervalminutes。
     */
    public void setNavPollIntervalMinutes(int navPollIntervalMinutes) {
        this.navPollIntervalMinutes = navPollIntervalMinutes;
    }

    /**
     * 获取tradebackfillmaxdays。
     */
    public int getTradeBackfillMaxDays() {
        return tradeBackfillMaxDays;
    }

    /**
     * 设置tradebackfillmaxdays。
     */
    public void setTradeBackfillMaxDays(int tradeBackfillMaxDays) {
        this.tradeBackfillMaxDays = tradeBackfillMaxDays;
    }

    /**
     * 获取估值tradingstart。
     */
    public LocalTime getEstimateTradingStart() {
        return estimateTradingStart;
    }

    /**
     * 设置估值tradingstart。
     */
    public void setEstimateTradingStart(LocalTime estimateTradingStart) {
        this.estimateTradingStart = estimateTradingStart;
    }

    /**
     * 获取估值tradingend。
     */
    public LocalTime getEstimateTradingEnd() {
        return estimateTradingEnd;
    }

    /**
     * 设置估值tradingend。
     */
    public void setEstimateTradingEnd(LocalTime estimateTradingEnd) {
        this.estimateTradingEnd = estimateTradingEnd;
    }

    /**
     * 获取净值pollstart。
     */
    public LocalTime getNavPollStart() {
        return navPollStart;
    }

    /**
     * 设置净值pollstart。
     */
    public void setNavPollStart(LocalTime navPollStart) {
        this.navPollStart = navPollStart;
    }

    /**
     * 获取净值pollend。
     */
    public LocalTime getNavPollEnd() {
        return navPollEnd;
    }

    /**
     * 设置净值pollend。
     */
    public void setNavPollEnd(LocalTime navPollEnd) {
        this.navPollEnd = navPollEnd;
    }

    /**
     * 获取净值backuppollstart。
     */
    public LocalTime getNavBackupPollStart() {
        return navBackupPollStart;
    }

    /**
     * 设置净值backuppollstart。
     */
    public void setNavBackupPollStart(LocalTime navBackupPollStart) {
        this.navBackupPollStart = navBackupPollStart;
    }

    /**
     * 获取净值backuppollend。
     */
    public LocalTime getNavBackupPollEnd() {
        return navBackupPollEnd;
    }

    /**
     * 设置净值backuppollend。
     */
    public void setNavBackupPollEnd(LocalTime navBackupPollEnd) {
        this.navBackupPollEnd = navBackupPollEnd;
    }
}
