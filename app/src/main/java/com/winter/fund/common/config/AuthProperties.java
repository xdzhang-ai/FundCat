package com.winter.fund.common.config;

/**
 * 公共配置文件，负责承载全局属性或 Spring 基础设施配置。
 */

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fundcat.auth")
public class AuthProperties {

    private long accessTokenTtlMinutes = 240;
    private long refreshTokenTtlDays = 14;
    private String sessionStore = "memory";

    /**
     * 获取访问令牌ttlminutes。
     */
    public long getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    /**
     * 设置访问令牌ttlminutes。
     */
    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    /**
     * 获取刷新令牌ttldays。
     */
    public long getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    /**
     * 设置刷新令牌ttldays。
     */
    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    /**
     * 获取sessionstore。
     */
    public String getSessionStore() {
        return sessionStore;
    }

    /**
     * 设置sessionstore。
     */
    public void setSessionStore(String sessionStore) {
        this.sessionStore = sessionStore;
    }
}
