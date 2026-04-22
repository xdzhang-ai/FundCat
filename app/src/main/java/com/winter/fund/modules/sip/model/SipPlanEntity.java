package com.winter.fund.modules.sip.model;

/**
 * 定投模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sip_plans")
public class SipPlanEntity {

    @Id
    private String id;

    @Column(name = "portfolio_id", nullable = false)
    private String portfolioId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String cadence;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String status;

    @Column(name = "fee_rate", nullable = false)
    private double feeRate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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
     * 获取组合id。
     */
    public String getPortfolioId() {
        return portfolioId;
    }

    /**
     * 设置组合id。
     */
    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
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
     * 获取基金name。
     */
    public String getFundName() {
        return fundName;
    }

    /**
     * 设置基金name。
     */
    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    /**
     * 获取金额。
     */
    public double getAmount() {
        return amount;
    }

    /**
     * 设置金额。
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * 获取频率。
     */
    public String getCadence() {
        return cadence;
    }

    /**
     * 设置频率。
     */
    public void setCadence(String cadence) {
        this.cadence = cadence;
    }

    /**
     * 获取nextrunat。
     */
    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    /**
     * 设置nextrunat。
     */
    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    /**
     * 判断是否active。
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置active。
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 获取状态。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取费率rate。
     */
    public double getFeeRate() {
        return feeRate;
    }

    /**
     * 设置费率rate。
     */
    public void setFeeRate(double feeRate) {
        this.feeRate = feeRate;
    }

    /**
     * 获取createdat。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置createdat。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
