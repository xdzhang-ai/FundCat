package com.fundcat.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fundcat.auth")
public class AuthProperties {

    private long accessTokenTtlMinutes = 240;
    private long refreshTokenTtlDays = 14;
    private String sessionStore = "memory";

    public long getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public long getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public String getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(String sessionStore) {
        this.sessionStore = sessionStore;
    }
}
