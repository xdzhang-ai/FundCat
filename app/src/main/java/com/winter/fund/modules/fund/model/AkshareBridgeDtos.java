package com.winter.fund.modules.fund.model;

/**
 * AKShare 桥接 DTO 文件，负责声明 Java 透传 Python 基金数据服务的返回结构。
 */

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

public final class AkshareBridgeDtos {

    private AkshareBridgeDtos() {
    }

    @Schema(description = "AKShare 单对象桥接响应")
    public record BridgeObjectResponse(
        @Schema(description = "数据来源标识", example = "akshare-bridge") String provider,
        @Schema(description = "原始对象数据") Map<String, Object> payload
    ) {
    }

    @Schema(description = "AKShare 列表桥接响应")
    public record BridgeListResponse(
        @Schema(description = "数据来源标识", example = "akshare-bridge") String provider,
        @Schema(description = "原始列表数据") List<Map<String, Object>> payload
    ) {
    }
}
