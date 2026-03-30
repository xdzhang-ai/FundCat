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
@Table(name = "funds")
public class FundEntity {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(nullable = false)
    private String benchmark;

    @Column(name = "tag_line", nullable = false)
    private String tagLine;

    @Column(nullable = false)
    private String tags;

    @Column(name = "management_fee", nullable = false)
    private double managementFee;

    @Column(name = "custody_fee", nullable = false)
    private double custodyFee;

    @Column(name = "purchase_fee", nullable = false)
    private double purchaseFee;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(String benchmark) {
        this.benchmark = benchmark;
    }

    public String getTagLine() {
        return tagLine;
    }

    public void setTagLine(String tagLine) {
        this.tagLine = tagLine;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public double getManagementFee() {
        return managementFee;
    }

    public void setManagementFee(double managementFee) {
        this.managementFee = managementFee;
    }

    public double getCustodyFee() {
        return custodyFee;
    }

    public void setCustodyFee(double custodyFee) {
        this.custodyFee = custodyFee;
    }

    public double getPurchaseFee() {
        return purchaseFee;
    }

    public void setPurchaseFee(double purchaseFee) {
        this.purchaseFee = purchaseFee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
