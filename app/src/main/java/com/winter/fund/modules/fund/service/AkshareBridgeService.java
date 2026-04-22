package com.winter.fund.modules.fund.service;

/**
 * AKShare 桥接服务文件，负责组织 Java 端对 Python 基金数据服务的读取结果。
 */

import com.winter.fund.infrastructure.marketdata.AkshareBridgeClient;
import com.winter.fund.modules.fund.model.AkshareBridgeDtos;
import org.springframework.stereotype.Service;

@Service
public class AkshareBridgeService {

    private static final String PROVIDER = "akshare-bridge";

    private final AkshareBridgeClient client;

    public AkshareBridgeService(AkshareBridgeClient client) {
        this.client = client;
    }

    /**
     * 获取unit净值历史。
     */
    public AkshareBridgeDtos.BridgeListResponse getUnitNavHistory(String code) {
        return new AkshareBridgeDtos.BridgeListResponse(PROVIDER, client.getUnitNavHistory(code));
    }

    /**
     * 获取acc净值历史。
     */
    public AkshareBridgeDtos.BridgeListResponse getAccNavHistory(String code) {
        return new AkshareBridgeDtos.BridgeListResponse(PROVIDER, client.getAccNavHistory(code));
    }
}
