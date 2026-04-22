package com.winter.fund.modules.watchlist.model;

/**
 * 自选模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_items")
public class WatchlistEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(nullable = false)
    private String note;

    @Column(name = "group_id", nullable = false)
    private String groupId;

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
     * 获取groupid。
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * 设置groupid。
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
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
