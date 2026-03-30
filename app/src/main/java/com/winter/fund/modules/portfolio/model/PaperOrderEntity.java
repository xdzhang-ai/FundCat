package com.winter.fund.modules.portfolio.model;

/**
 * 组合与交易模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_orders")
public class PaperOrderEntity {

    @Id
    private String id;

    @Column(name = "portfolio_id", nullable = false)
    private String portfolioId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double shares;

    @Column(nullable = false)
    private double fee;

    @Column(nullable = false)
    private String status;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(nullable = false)
    private String note;

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

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getShares() {
        return shares;
    }

    public void setShares(double shares) {
        this.shares = shares;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
