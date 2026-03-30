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
@Table(name = "user_fund_operation_records")
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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSharesDelta() {
        return sharesDelta;
    }

    public void setSharesDelta(double sharesDelta) {
        this.sharesDelta = sharesDelta;
    }

    public double getNav() {
        return nav;
    }

    public void setNav(double nav) {
        this.nav = nav;
    }

    public double getFeeRate() {
        return feeRate;
    }

    public void setFeeRate(double feeRate) {
        this.feeRate = feeRate;
    }

    public double getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getSipPlanId() {
        return sipPlanId;
    }

    public void setSipPlanId(String sipPlanId) {
        this.sipPlanId = sipPlanId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
