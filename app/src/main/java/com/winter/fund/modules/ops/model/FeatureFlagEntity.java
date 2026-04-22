package com.winter.fund.modules.ops.model;

/**
 * 运维模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ops_feature_flags")
public class FeatureFlagEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private String description;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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
     * 获取code。
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置code。
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取name。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置name。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 判断是否enabled。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置enabled。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取environment。
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * 设置environment。
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * 获取description。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置description。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取risklevel。
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * 设置risklevel。
     */
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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
}
