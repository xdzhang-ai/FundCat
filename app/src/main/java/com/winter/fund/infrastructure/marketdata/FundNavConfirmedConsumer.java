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
     * 这里保留统一的业务消费落点，既可供本地事件触发，也可供 RocketMQ adapter 复用。
     */
    @EventListener
    public void onFundNavReadyBatch(FundNavReadyBatchMessage message) {
        fundNavConfirmationService.handleConfirmedNavBatch(message);
    }
}
