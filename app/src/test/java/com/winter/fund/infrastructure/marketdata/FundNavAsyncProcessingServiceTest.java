package com.winter.fund.infrastructure.marketdata;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.winter.fund.infrastructure.redis.FundNavMessageIdempotencyService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class FundNavAsyncProcessingServiceTest {

    @Test
    void retriesLocallyUntilSuccessAndMarksSuccess() {
        FundNavConfirmationService confirmationService = mock(FundNavConfirmationService.class);
        FundNavMessageIdempotencyService idempotencyService = mock(FundNavMessageIdempotencyService.class);
        ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
        FundNavAsyncProcessingService service = new FundNavAsyncProcessingService(
            confirmationService,
            idempotencyService,
            scheduler
        ) {
            @Override
            void sleepBeforeRetry(long backoffMillis, String messageId) {
                // no-op for test
            }
        };
        FundNavReadyBatchMessage message = new FundNavReadyBatchMessage(
            "event-1",
            LocalDate.of(2026, 4, 24),
            List.of("000001", "005827"),
            2,
            LocalDateTime.of(2026, 4, 24, 9, 0, 0)
        );

        doThrow(new IllegalStateException("first"))
            .doThrow(new IllegalStateException("second"))
            .doNothing()
            .when(confirmationService)
            .handleConfirmedNavBatch(message);

        service.processWithLocalRetry("msg-1", message);

        verify(confirmationService, times(3)).handleConfirmedNavBatch(message);
        verify(idempotencyService).markSuccess("msg-1");
        verify(idempotencyService, never()).markFailed("msg-1");
    }

    @Test
    void stopsAfterThreeAttemptsAndMarksFailed() {
        FundNavConfirmationService confirmationService = mock(FundNavConfirmationService.class);
        FundNavMessageIdempotencyService idempotencyService = mock(FundNavMessageIdempotencyService.class);
        ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
        FundNavAsyncProcessingService service = new FundNavAsyncProcessingService(
            confirmationService,
            idempotencyService,
            scheduler
        ) {
            @Override
            void sleepBeforeRetry(long backoffMillis, String messageId) {
                // no-op for test
            }
        };
        FundNavReadyBatchMessage message = new FundNavReadyBatchMessage(
            "event-2",
            LocalDate.of(2026, 4, 24),
            List.of("000001"),
            1,
            LocalDateTime.of(2026, 4, 24, 9, 5, 0)
        );

        doThrow(new IllegalStateException("always fail")).when(confirmationService).handleConfirmedNavBatch(message);

        service.processWithLocalRetry("msg-2", message);

        verify(confirmationService, times(3)).handleConfirmedNavBatch(message);
        verify(idempotencyService).markFailed("msg-2");
        verify(idempotencyService, never()).markSuccess("msg-2");
    }

    @Test
    void submitSkipsDuplicateMessageIds() {
        FundNavConfirmationService confirmationService = mock(FundNavConfirmationService.class);
        FundNavMessageIdempotencyService idempotencyService = mock(FundNavMessageIdempotencyService.class);
        ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);
        FundNavAsyncProcessingService service = new FundNavAsyncProcessingService(
            confirmationService,
            idempotencyService,
            scheduler
        );
        FundNavReadyBatchMessage message = new FundNavReadyBatchMessage(
            "event-3",
            LocalDate.of(2026, 4, 24),
            List.of("000001"),
            1,
            LocalDateTime.of(2026, 4, 24, 9, 10, 0)
        );

        when(idempotencyService.tryAcquire("msg-3")).thenReturn(false);
        when(idempotencyService.getState("msg-3")).thenReturn("SUCCESS");

        service.submit("msg-3", message);

        verify(scheduler, never()).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
        verify(confirmationService, never()).handleConfirmedNavBatch(message);
    }
}
