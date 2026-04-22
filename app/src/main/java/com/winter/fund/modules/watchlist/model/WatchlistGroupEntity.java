package com.winter.fund.modules.watchlist.model;

/**
 * 自选分组实体，负责描述用户可维护的自选分组选项。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_groups")
public class WatchlistGroupEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

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
     * 获取分组名称。
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 设置分组名称。
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    /**
     * 获取创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
