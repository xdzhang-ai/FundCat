package com.winter.fund.modules.fund.model;

/**
 * 基金模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_estimates")
public class FundEstimateEntity {

    @Id
    private String id;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "estimated_at", nullable = false)
    private LocalDateTime estimatedAt;

    @Column(name = "estimated_nav", nullable = false)
    private double estimatedNav;

    @Column(name = "estimated_growth", nullable = false)
    private double estimatedGrowth;

    @Column(name = "reference_only", nullable = false)
    private boolean referenceOnly;

    @Column(nullable = false)
    private String sentiment;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public LocalDateTime getEstimatedAt() {
        return estimatedAt;
    }

    public void setEstimatedAt(LocalDateTime estimatedAt) {
        this.estimatedAt = estimatedAt;
    }

    public double getEstimatedNav() {
        return estimatedNav;
    }

    public void setEstimatedNav(double estimatedNav) {
        this.estimatedNav = estimatedNav;
    }

    public double getEstimatedGrowth() {
        return estimatedGrowth;
    }

    public void setEstimatedGrowth(double estimatedGrowth) {
        this.estimatedGrowth = estimatedGrowth;
    }

    public boolean isReferenceOnly() {
        return referenceOnly;
    }

    public void setReferenceOnly(boolean referenceOnly) {
        this.referenceOnly = referenceOnly;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }
}
