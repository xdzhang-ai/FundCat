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
@Table(name = "holding_operation_records")
public class UserFundOperationRecordEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String status;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private double amount;

    @Column(name = "shares_delta", nullable = false)
    private double sharesDelta;

    @Column(nullable = false)
    private double nav;

    @Column(name = "fee_rate", nullable = false)
    private double feeRate;

    @Column(name = "fee_amount", nullable = false)
    private double feeAmount;

    @Column(name = "sip_plan_id")
    private String sipPlanId;

    @Column
    private String note;

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
     * 获取操作。
     */
    public String getOperation() {
        return operation;
    }

    /**
     * 设置操作。
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * 获取source。
     */
    public String getSource() {
        return source;
    }

    /**
     * 设置source。
     */
    public void setSource(String source) {
        this.source = source;
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
     * 获取sharesdelta。
     */
    public double getSharesDelta() {
        return sharesDelta;
    }

    /**
     * 设置sharesdelta。
     */
    public void setSharesDelta(double sharesDelta) {
        this.sharesDelta = sharesDelta;
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
     * 获取费率金额。
     */
    public double getFeeAmount() {
        return feeAmount;
    }

    /**
     * 设置费率金额。
     */
    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    /**
     * 获取定投计划id。
     */
    public String getSipPlanId() {
        return sipPlanId;
    }

    /**
     * 设置定投计划id。
     */
    public void setSipPlanId(String sipPlanId) {
        this.sipPlanId = sipPlanId;
    }

    /**
     * 获取note。
     */
    public String getNote() {
        return note;
    }

    /**
     * 设置note。
     */
    public void setNote(String note) {
        this.note = note;
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
