package com.winter.fund.modules.holding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.common.config.PersistenceProperties;
import com.winter.fund.infrastructure.redis.HoldingCurrentStateCacheService;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.model.UserFundDailyProfitSnapshotEntity;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundDailyProfitSnapshotRepository;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import com.winter.fund.modules.ops.service.OpsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldingServiceTest {

    private UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository;
    private UserFundOperationRecordRepository operationRecordRepository;
    private FundRepository fundRepository;
    private NavHistoryRepository navHistoryRepository;
    private HoldingService holdingService;

    @BeforeEach
    void setUp() {
        dailyProfitSnapshotRepository = mock(UserFundDailyProfitSnapshotRepository.class);
        operationRecordRepository = mock(UserFundOperationRecordRepository.class);
        fundRepository = mock(FundRepository.class);
        navHistoryRepository = mock(NavHistoryRepository.class);
        MarketDataProperties properties = new MarketDataProperties();
        properties.setTimezone("Asia/Shanghai");
        PersistenceProperties persistenceProperties = new PersistenceProperties();
        holdingService = spy(new HoldingService(
            dailyProfitSnapshotRepository,
            operationRecordRepository,
            fundRepository,
            mock(FundEstimateRepository.class),
            navHistoryRepository,
            new HoldingComputationService(),
            properties,
            persistenceProperties,
            mock(OpsService.class),
            mock(HoldingCurrentStateCacheService.class)
        ));
    }

    @Test
    void createManualOperationExecutesHistoricalBuyImmediatelyWhenConfirmedNavExists() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate today = LocalDate.of(2026, 3, 30);
        LocalDate tradeDate = today.minusDays(2);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "BUY", tradeDate.toString(), 2000.0, null, 0.0015, "历史补买");

        doReturn(today).when(holdingService).currentDate();
        mockFund("000001", "华夏成长优选混合");
        mockConfirmedNav("000001", tradeDate, 1.25);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "000001", tradeDate.minusDays(1)
        )).thenReturn(Optional.empty());
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            currentUser.id(), "000001", "已执行", tradeDate, today
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(holdingService).rebuildSnapshotsFrom(currentUser.id(), "000001", tradeDate);
        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("已执行", response.status());
        assertEquals("OPEN_POSITION", response.operation());
        assertEquals(1600.0, response.sharesDelta());
        verify(holdingService).rebuildSnapshotsFrom(currentUser.id(), "000001", tradeDate);
    }

    @Test
    void createManualOperationMarksTodayBuyAsPendingWithoutChangingSharesYet() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "BUY", tradeDate.toString(), 2000.0, null, 0.0015, "今日买入");

        doReturn(tradeDate).when(holdingService).currentDate();
        mockFund("000001", "华夏成长优选混合");
        mockConfirmedNav("000001", tradeDate, 1.25);
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("确认中", response.status());
        assertEquals("BUY", response.operation());
        assertEquals(0.0, response.sharesDelta());
        assertEquals(0.0, response.nav());
    }

    @Test
    void createManualOperationMarksTodaySellAsPendingAndKeepsRequestedShares() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("004997", "SELL", tradeDate.toString(), null, 392.0, 0.0, "今日卖出");

        doReturn(tradeDate).when(holdingService).currentDate();
        mockFund("004997", "汇添富互联网核心资产");
        mockConfirmedNav("004997", tradeDate, 1.2741);
        UserFundDailyProfitSnapshotEntity sameDaySnapshot = snapshot(tradeDate, 392.4339, 1.2486);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "004997", tradeDate
        )).thenReturn(Optional.of(sameDaySnapshot));
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("确认中", response.status());
        assertEquals("SELL", response.operation());
        assertEquals(-392.0, response.sharesDelta());
        assertEquals(0.0, response.amount());
        assertEquals(0.0, response.nav());
    }

    @Test
    void createManualOperationAllowsNextBusinessDateAsPendingTradeDate() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate today = LocalDate.of(2026, 3, 30);
        LocalDate nextBusinessDate = LocalDate.of(2026, 3, 31);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "BUY", nextBusinessDate.toString(), 500.0, null, 0.0, "次日买入");

        doReturn(today).when(holdingService).currentDate();
        mockFund("000001", "华夏成长优选混合");
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("确认中", response.status());
        assertEquals(nextBusinessDate.toString(), response.tradeDate());
    }

    @Test
    void confirmPendingManualBuyPromotesOperationToOpenPosition() {
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        UserFundOperationRecordEntity pendingRecord = pendingBuy("op-pending-buy", "user-demo-001", "000001", tradeDate, 2000.0, 0.0015);

        when(operationRecordRepository.findBySourceAndStatusAndFundCodeAndTradeDateOrderByCreatedAtAsc(
            "MANUAL", "确认中", "000001", tradeDate
        )).thenReturn(List.of(pendingRecord));
        mockConfirmedNav("000001", tradeDate, 1.25);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), tradeDate
        )).thenReturn(Optional.empty());
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), tradeDate.minusDays(1)
        )).thenReturn(Optional.empty());
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<HoldingRebuildTarget> confirmed = holdingService.confirmPendingManualOperations("000001", tradeDate);

        assertEquals(1, confirmed.size());
        assertEquals("已执行", pendingRecord.getStatus());
        assertEquals("OPEN_POSITION", pendingRecord.getOperation());
        assertEquals(1600.0, pendingRecord.getSharesDelta());
        assertEquals(3.0, pendingRecord.getFeeAmount());
        assertEquals(tradeDate, confirmed.get(0).earliestTradeDate());
    }

    @Test
    void confirmPendingManualSellPromotesOperationToClosePosition() {
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        UserFundOperationRecordEntity pendingRecord = pendingSell("op-pending-sell", "user-demo-001", "004997", tradeDate, 300.0, 0.0015);

        when(operationRecordRepository.findBySourceAndStatusAndFundCodeAndTradeDateOrderByCreatedAtAsc(
            "MANUAL", "确认中", "004997", tradeDate
        )).thenReturn(List.of(pendingRecord));
        mockConfirmedNav("004997", tradeDate, 1.2741);
        UserFundDailyProfitSnapshotEntity baseline = snapshot(tradeDate.minusDays(1), 300.0, 1.10);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), tradeDate
        )).thenReturn(Optional.empty());
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), tradeDate.minusDays(1)
        )).thenReturn(Optional.of(baseline));
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            pendingRecord.getUserId(), pendingRecord.getFundCode(), "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<HoldingRebuildTarget> confirmed = holdingService.confirmPendingManualOperations("004997", tradeDate);

        assertEquals(1, confirmed.size());
        assertEquals("已执行", pendingRecord.getStatus());
        assertEquals("CLOSE_POSITION", pendingRecord.getOperation());
        assertEquals(-300.0, pendingRecord.getSharesDelta());
        assertEquals(382.23, pendingRecord.getAmount());
        assertNotNull(pendingRecord.getUpdatedAt());
        assertEquals(tradeDate, confirmed.get(0).earliestTradeDate());
    }

    private void mockFund(String code, String name) {
        when(fundRepository.findByCode(code)).thenReturn(Optional.of(fund(code, name)));
    }

    private void mockConfirmedNav(String fundCode, LocalDate tradeDate, double nav) {
        NavHistoryEntity navHistory = new NavHistoryEntity();
        navHistory.setFundCode(fundCode);
        navHistory.setTradeDate(tradeDate);
        navHistory.setUnitNav(nav);
        when(navHistoryRepository.findByFundCodeAndTradeDate(fundCode, tradeDate)).thenReturn(Optional.of(navHistory));
    }

    private FundEntity fund(String code, String name) {
        FundEntity fund = new FundEntity();
        fund.setCode(code);
        fund.setName(name);
        return fund;
    }

    private UserFundDailyProfitSnapshotEntity snapshot(LocalDate tradeDate, double shares, double averageCost) {
        UserFundDailyProfitSnapshotEntity entity = new UserFundDailyProfitSnapshotEntity();
        entity.setTradeDate(tradeDate);
        entity.setShares(shares);
        entity.setAverageCost(averageCost);
        return entity;
    }

    private UserFundOperationRecordEntity pendingBuy(String id, String userId, String fundCode, LocalDate tradeDate, double amount, double feeRate) {
        UserFundOperationRecordEntity entity = new UserFundOperationRecordEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setOperation("BUY");
        entity.setSource("MANUAL");
        entity.setStatus("确认中");
        entity.setTradeDate(tradeDate);
        entity.setAmount(amount);
        entity.setSharesDelta(0);
        entity.setNav(0);
        entity.setFeeRate(feeRate);
        entity.setFeeAmount(amount * feeRate);
        return entity;
    }

    private UserFundOperationRecordEntity pendingSell(String id, String userId, String fundCode, LocalDate tradeDate, double shares, double feeRate) {
        UserFundOperationRecordEntity entity = new UserFundOperationRecordEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setOperation("SELL");
        entity.setSource("MANUAL");
        entity.setStatus("确认中");
        entity.setTradeDate(tradeDate);
        entity.setAmount(0);
        entity.setSharesDelta(-shares);
        entity.setNav(0);
        entity.setFeeRate(feeRate);
        entity.setFeeAmount(0);
        return entity;
    }
}
