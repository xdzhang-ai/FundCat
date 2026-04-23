package com.winter.fund.infrastructure.marketdata;

import com.winter.fund.infrastructure.redis.FundNavMessageIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class FundNavAsyncProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FundNavAsyncProcessingService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 1000L;

    private final FundNavConfirmationService fundNavConfirmationService;
    private final FundNavMessageIdempotencyService idempotencyService;
    private final ThreadPoolTaskScheduler marketDataTaskScheduler;

    public FundNavAsyncProcessingService(
        FundNavConfirmationService fundNavConfirmationService,
        FundNavMessageIdempotencyService idempotencyService,
        ThreadPoolTaskScheduler marketDataTaskScheduler
    ) {
        this.fundNavConfirmationService = fundNavConfirmationService;
        this.idempotencyService = idempotencyService;
        this.marketDataTaskScheduler = marketDataTaskScheduler;
    }

    /**
     * 提交一条基金净值 ready 消息到本地异步处理链。
     * MQ adapter 在拿到消息后应尽快调用这里并返回 success，后续处理由本地线程池完成。
     */
    public void submit(String messageId, FundNavReadyBatchMessage message) {
        if (!idempotencyService.tryAcquire(messageId)) {
            log.info(
                "Skipped duplicate fund nav ready batch, messageId={}, eventId={}, navDate={}, fundCount={}, state={}",
                messageId,
                message.eventId(),
                message.navDate(),
                message.count(),
                idempotencyService.getState(messageId)
            );
            return;
        }
        marketDataTaskScheduler.execute(() -> processWithLocalRetry(messageId, message));
    }

    void processWithLocalRetry(String messageId, FundNavReadyBatchMessage message) {
        long backoffMillis = INITIAL_BACKOFF_MILLIS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                fundNavConfirmationService.handleConfirmedNavBatch(message);
                idempotencyService.markSuccess(messageId);
                if (attempt > 1) {
                    log.info(
                        "Fund nav ready batch processed after local retry, messageId={}, eventId={}, navDate={}, fundCount={}, attempt={}",
                        messageId,
                        message.eventId(),
                        message.navDate(),
                        message.count(),
                        attempt
                    );
                }
                return;
            } catch (Exception exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    idempotencyService.markFailed(messageId);
                    log.error(
                        "Fund nav ready batch failed after local retries, messageId={}, eventId={}, navDate={}, fundCount={}, attempts={}",
                        messageId,
                        message.eventId(),
                        message.navDate(),
                        message.count(),
                        MAX_ATTEMPTS,
                        exception
                    );
                    return;
                }
                log.warn(
                    "Fund nav ready batch local retry scheduled, messageId={}, eventId={}, navDate={}, fundCount={}, attempt={}, nextBackoffMillis={}",
                    messageId,
                    message.eventId(),
                    message.navDate(),
                    message.count(),
                    attempt,
                    backoffMillis,
                    exception
                );
                sleepBeforeRetry(backoffMillis, messageId);
                backoffMillis *= 2;
            }
        }
    }

    void sleepBeforeRetry(long backoffMillis, String messageId) {
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            idempotencyService.markFailed(messageId);
            throw new IllegalStateException("Interrupted while waiting to retry fund nav ready batch", exception);
        }
    }
}
