package com.winter.fund.infrastructure.marketdata;

/**
 * AKShare 桥接客户端文件，负责通过 HTTP 调用本地 Python 基金数据服务。
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.fund.common.config.AkshareBridgeProperties;
import com.winter.fund.common.exception.NotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AkshareBridgeClient {

    private static final Logger log = LoggerFactory.getLogger(AkshareBridgeClient.class);

    private final AkshareBridgeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AkshareBridgeClient(AkshareBridgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .build();
    }

    /**
     * 发起单位净值历史请求。
     */
    public List<Map<String, Object>> getUnitNavHistory(String code) {
        return getList("/funds/" + encode(code) + "/unit-nav-history");
    }

    /**
     * 发起累计净值历史请求。
     */
    public List<Map<String, Object>> getAccNavHistory(String code) {
        return getList("/funds/" + encode(code) + "/acc-nav-history");
    }

    /**
     * 请求并解析列表类型响应。
     */
    private List<Map<String, Object>> getList(String path) {
        String body = send(path);
        try {
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse AKShare bridge list response", exception);
        }
    }

    /**
     * 发送底层 HTTP 请求，并统一处理超时和错误码。
     */
    private String send(String path) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("AKShare bridge is disabled");
        }
        String url = properties.getBaseUrl() + path;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(resolveReadTimeout())
            .GET()
            .build();
        try {
            log.info("Requesting AKShare bridge, url={}", url);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("AKShare bridge responded, url={}, status={}", url, response.statusCode());
            // 404 交给业务层按资源不存在处理，其他错误码统一按桥接调用失败抛出。
            if (response.statusCode() == 404) {
                throw new NotFoundException("AKShare bridge resource was not found");
            }
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("AKShare bridge request failed with status " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call AKShare bridge", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AKShare bridge call was interrupted", exception);
        }
    }

    /**
     * 对路径参数做 URL 编码。
     */
    private String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    /**
     * 解析读取超时时间，未配置时回退到默认值。
     */
    private Duration resolveReadTimeout() {
        Duration timeout = properties.getReadTimeout();
        return timeout == null ? Duration.ofSeconds(15) : timeout;
    }
}
