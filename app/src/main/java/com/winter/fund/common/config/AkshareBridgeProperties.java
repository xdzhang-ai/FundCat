package com.winter.fund.common.config;

/**
 * AKShare 桥接配置文件，负责声明 Java 调用 Python 基金数据服务所需的地址与超时。
 */

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fundcat.market-data.akshare-bridge")
public class AkshareBridgeProperties {

    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8000";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(15);

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
     * 获取baseurl。
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置baseurl。
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 获取connecttimeout。
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 设置connecttimeout。
     */
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 获取readtimeout。
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * 设置readtimeout。
     */
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
