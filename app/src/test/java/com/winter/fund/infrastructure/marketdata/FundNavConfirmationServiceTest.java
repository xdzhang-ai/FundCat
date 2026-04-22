package com.winter.fund.infrastructure.marketdata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.winter.fund.modules.holding.service.HoldingRebuildTarget;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.sip.service.SipService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FundNavConfirmationServiceTest {

    @Test
    void handleConfirmedNavMergesTargetsByUserAndFundBeforeRebuild() {
        HoldingService holdingService = mock(HoldingService.class);
        SipService sipService = mock(SipService.class);
        FundNavConfirmationService service = new FundNavConfirmationService(holdingService, sipService);
        LocalDate navDate = LocalDate.of(2026, 3, 30);

        when(holdingService.confirmPendingManualOperations("000001", navDate)).thenReturn(List.of(
            new HoldingRebuildTarget("user-a", "000001", navDate),
            new HoldingRebuildTarget("user-a", "000001", navDate.minusDays(1))
        ));
        when(sipService.confirmPendingSipOperations("000001", navDate)).thenReturn(List.of(
            new HoldingRebuildTarget("user-a", "000001", navDate.minusDays(2)),
            new HoldingRebuildTarget("user-b", "000001", navDate)
        ));

        service.handleConfirmedNav("000001", navDate);

        verify(holdingService).confirmPendingManualOperations("000001", navDate);
        verify(sipService).confirmPendingSipOperations("000001", navDate);
        verify(holdingService).rebuildSnapshotsFrom("user-a", "000001", navDate.minusDays(2));
        verify(holdingService).rebuildSnapshotsFrom("user-b", "000001", navDate);
        verify(holdingService).evictIntradayStates("000001", navDate);
        verifyNoMoreInteractions(holdingService, sipService);
    }
}
