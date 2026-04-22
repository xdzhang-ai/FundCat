package com.winter.fund.modules.holding.model;

/**
 * 持仓模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holding_daily_snapshots")
public class UserFundDailyProfitSnapshotEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private double shares;

    @Column(name = "average_cost", nullable = false)
    private double averageCost;

    @Column(nullable = false)
    private double nav;

    @Column(name = "market_value", nullable = false)
    private double marketValue;

    @Column(name = "daily_pnl", nullable = false)
    private double dailyPnl;

    @Column(name = "total_pnl", nullable = false)
    private double totalPnl;

    @Column(name = "total_pnl_rate", nullable = false)
    private double totalPnlRate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 获取id。
     */
    public String getId() {
        return id;
    }

    /**
     * 设置id。
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取用户id。
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户id。
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 获取基金code。
     */
    public String getFundCode() {
        return fundCode;
    }

    /**
     * 设置基金code。
     */
    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    /**
     * 获取trade日期。
     */
    public LocalDate getTradeDate() {
        return tradeDate;
    }

    /**
     * 设置trade日期。
     */
    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    /**
     * 获取shares。
     */
    public double getShares() {
        return shares;
    }

    /**
     * 设置shares。
     */
    public void setShares(double shares) {
        this.shares = shares;
    }

    /**
     * 获取average成本。
     */
    public double getAverageCost() {
        return averageCost;
    }

    /**
     * 设置average成本。
     */
    public void setAverageCost(double averageCost) {
        this.averageCost = averageCost;
    }

    /**
     * 获取净值。
     */
    public double getNav() {
        return nav;
    }

    /**
     * 设置净值。
     */
    public void setNav(double nav) {
        this.nav = nav;
    }

    /**
     * 获取市场value。
     */
    public double getMarketValue() {
        return marketValue;
    }

    /**
     * 设置市场value。
     */
    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    /**
     * 获取每日pnl。
     */
    public double getDailyPnl() {
        return dailyPnl;
    }

    /**
     * 设置每日pnl。
     */
    public void setDailyPnl(double dailyPnl) {
        this.dailyPnl = dailyPnl;
    }

    /**
     * 获取totalpnl。
     */
    public double getTotalPnl() {
        return totalPnl;
    }

    /**
     * 设置totalpnl。
     */
    public void setTotalPnl(double totalPnl) {
        this.totalPnl = totalPnl;
    }

    /**
     * 获取totalpnlrate。
     */
    public double getTotalPnlRate() {
        return totalPnlRate;
    }

    /**
     * 设置totalpnlrate。
     */
    public void setTotalPnlRate(double totalPnlRate) {
        this.totalPnlRate = totalPnlRate;
    }

    /**
     * 获取updatedat。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置updatedat。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
