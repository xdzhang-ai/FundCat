package com.winter.fund.infrastructure.marketdata;

import org.springframework.stereotype.Component;

@Component
public class FundNavConfirmedConsumer {

    private final FundNavAsyncProcessingService asyncProcessingService;

    public FundNavConfirmedConsumer(FundNavAsyncProcessingService asyncProcessingService) {
        this.asyncProcessingService = asyncProcessingService;
    }

    /**
     * RocketMQ adapter 的统一入口。
     * 对 RocketMQ 消息优先使用 broker 分配的 messageId 做 Redis 粒度幂等。
     */
    public void onRocketMqFundNavReadyBatch(String messageId, FundNavReadyBatchMessage message) {
        asyncProcessingService.submit(messageId, message);
    }
}
