package com.winter.fund.modules.auth.model;

/**
 * 认证模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_users")
public class UserEntity {

    @Id
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "risk_mode", nullable = false)
    private String riskMode;

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
     * 获取displayname。
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置displayname。
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取username。
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置username。
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取passwordhash。
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置passwordhash。
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 获取riskmode。
     */
    public String getRiskMode() {
        return riskMode;
    }

    /**
     * 设置riskmode。
     */
    public void setRiskMode(String riskMode) {
        this.riskMode = riskMode;
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
