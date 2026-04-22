package com.winter.fund.modules.auth.model;

/**
 * 认证模块模型文件，负责定义实体、值对象或上下文数据结构。
 */

public final class CurrentUser {

    private final String id;
    private final String displayName;
    private final String username;

    public CurrentUser(String id, String displayName, String username) {
        this.id = id;
        this.displayName = displayName;
        this.username = username;
    }

    /**
     * 返回id结果。
     */
    public String id() {
        return id;
    }

    /**
     * 返回displayName结果。
     */
    public String displayName() {
        return displayName;
    }

    /**
     * 返回username结果。
     */
    public String username() {
        return username;
    }
}
