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
@Table(name = "fund_intraday_estimates")
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
     * 获取estimatedat。
     */
    public LocalDateTime getEstimatedAt() {
        return estimatedAt;
    }

    /**
     * 设置estimatedat。
     */
    public void setEstimatedAt(LocalDateTime estimatedAt) {
        this.estimatedAt = estimatedAt;
    }

    /**
     * 获取estimated净值。
     */
    public double getEstimatedNav() {
        return estimatedNav;
    }

    /**
     * 设置estimated净值。
     */
    public void setEstimatedNav(double estimatedNav) {
        this.estimatedNav = estimatedNav;
    }

    /**
     * 获取estimatedgrowth。
     */
    public double getEstimatedGrowth() {
        return estimatedGrowth;
    }

    /**
     * 设置estimatedgrowth。
     */
    public void setEstimatedGrowth(double estimatedGrowth) {
        this.estimatedGrowth = estimatedGrowth;
    }

    /**
     * 判断是否referenceonly。
     */
    public boolean isReferenceOnly() {
        return referenceOnly;
    }

    /**
     * 设置referenceonly。
     */
    public void setReferenceOnly(boolean referenceOnly) {
        this.referenceOnly = referenceOnly;
    }

    /**
     * 获取sentiment。
     */
    public String getSentiment() {
        return sentiment;
    }

    /**
     * 设置sentiment。
     */
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }
}
