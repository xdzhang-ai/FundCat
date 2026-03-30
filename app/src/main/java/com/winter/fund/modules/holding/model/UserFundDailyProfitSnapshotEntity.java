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
@Table(name = "user_fund_daily_profit_snapshots")
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public double getShares() {
        return shares;
    }

    public void setShares(double shares) {
        this.shares = shares;
    }

    public double getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(double averageCost) {
        this.averageCost = averageCost;
    }

    public double getNav() {
        return nav;
    }

    public void setNav(double nav) {
        this.nav = nav;
    }

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getDailyPnl() {
        return dailyPnl;
    }

    public void setDailyPnl(double dailyPnl) {
        this.dailyPnl = dailyPnl;
    }

    public double getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(double totalPnl) {
        this.totalPnl = totalPnl;
    }

    public double getTotalPnlRate() {
        return totalPnlRate;
    }

    public void setTotalPnlRate(double totalPnlRate) {
        this.totalPnlRate = totalPnlRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
