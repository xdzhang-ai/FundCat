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
@Table(name = "weekly_reports")
public class WeeklyReportEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "week_label", nullable = false)
    private String weekLabel;

    @Column(nullable = false)
    private String summary;

    @Column(name = "return_rate", nullable = false)
    private double returnRate;

    @Column(name = "best_fund_code", nullable = false)
    private String bestFundCode;

    @Column(name = "risk_note", nullable = false)
    private String riskNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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

    public String getWeekLabel() {
        return weekLabel;
    }

    public void setWeekLabel(String weekLabel) {
        this.weekLabel = weekLabel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public double getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(double returnRate) {
        this.returnRate = returnRate;
    }

    public String getBestFundCode() {
        return bestFundCode;
    }

    public void setBestFundCode(String bestFundCode) {
        this.bestFundCode = bestFundCode;
    }

    public String getRiskNote() {
        return riskNote;
    }

    public void setRiskNote(String riskNote) {
        this.riskNote = riskNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
