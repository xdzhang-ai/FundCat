package com.winter.fund.modules.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "holding_lots")
public class HoldingLotEntity {

    @Id
    private String id;

    @Column(name = "portfolio_id", nullable = false)
    private String portfolioId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(nullable = false)
    private double shares;

    @Column(name = "average_cost", nullable = false)
    private double averageCost;

    @Column(name = "current_value", nullable = false)
    private double currentValue;

    @Column(nullable = false)
    private double pnl;

    @Column(nullable = false)
    private double allocation;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private boolean imported;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
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

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getPnl() {
        return pnl;
    }

    public void setPnl(double pnl) {
        this.pnl = pnl;
    }

    public double getAllocation() {
        return allocation;
    }

    public void setAllocation(double allocation) {
        this.allocation = allocation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
