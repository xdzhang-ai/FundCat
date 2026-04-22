package com.winter.fund.infrastructure.marketdata;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FundNavConfirmedConsumer {

    private final FundNavConfirmationService fundNavConfirmationService;

    public FundNavConfirmedConsumer(FundNavConfirmationService fundNavConfirmationService) {
        this.fundNavConfirmationService = fundNavConfirmationService;
    }

    /**
     * 批量基金 ready 消息的本地消费入口。
     * 当前主要给未来 RocketMQ adapter 预留统一的业务落点。
     */
    @EventListener
    public void onFundNavReadyBatch(FundNavReadyBatchMessage message) {
        fundNavConfirmationService.handleConfirmedNavBatch(message);
    }
}
