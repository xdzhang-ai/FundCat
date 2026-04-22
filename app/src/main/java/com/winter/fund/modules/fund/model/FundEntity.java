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
    private String tags;

    @Column(name = "top_holdings", nullable = false)
    private String topHoldings;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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
     * 获取tags。
     */
    public String getTags() {
        return tags;
    }

    /**
     * 设置tags。
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * 获取topholdings。
     */
    public String getTopHoldings() {
        return topHoldings;
    }

    /**
     * 设置topholdings。
     */
    public void setTopHoldings(String topHoldings) {
        this.topHoldings = topHoldings;
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
