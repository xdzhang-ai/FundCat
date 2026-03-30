package com.winter.fund.modules.holding.model;

/**
 * 持仓模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_fund_holdings")
public class UserFundHoldingEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(nullable = false)
    private double shares;

    @Column(name = "average_cost", nullable = false)
    private double averageCost;

    @Column(name = "market_value", nullable = false)
    private double marketValue;

    @Column(name = "holding_pnl", nullable = false)
    private double holdingPnl;

    @Column(name = "holding_pnl_rate", nullable = false)
    private double holdingPnlRate;

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

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
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

    public double getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(double marketValue) {
        this.marketValue = marketValue;
    }

    public double getHoldingPnl() {
        return holdingPnl;
    }

    public void setHoldingPnl(double holdingPnl) {
        this.holdingPnl = holdingPnl;
    }

    public double getHoldingPnlRate() {
        return holdingPnlRate;
    }

    public void setHoldingPnlRate(double holdingPnlRate) {
        this.holdingPnlRate = holdingPnlRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
