package com.winter.fund.modules.holding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.FundSnapshotRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.model.UserFundDailyProfitSnapshotEntity;
import com.winter.fund.modules.holding.model.UserFundHoldingEntity;
import com.winter.fund.modules.holding.repository.UserFundDailyProfitSnapshotRepository;
import com.winter.fund.modules.holding.repository.UserFundHoldingRepository;
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
    private UserFundHoldingRepository userFundHoldingRepository;
    private FundRepository fundRepository;
    private NavHistoryRepository navHistoryRepository;
    private HoldingService holdingService;

    @BeforeEach
    void setUp() {
        dailyProfitSnapshotRepository = mock(UserFundDailyProfitSnapshotRepository.class);
        operationRecordRepository = mock(UserFundOperationRecordRepository.class);
        userFundHoldingRepository = mock(UserFundHoldingRepository.class);
        fundRepository = mock(FundRepository.class);
        navHistoryRepository = mock(NavHistoryRepository.class);
        MarketDataProperties properties = new MarketDataProperties();
        properties.setTimezone("Asia/Shanghai");
        holdingService = spy(new HoldingService(
            userFundHoldingRepository,
            dailyProfitSnapshotRepository,
            operationRecordRepository,
            fundRepository,
            mock(FundSnapshotRepository.class),
            mock(FundEstimateRepository.class),
            navHistoryRepository,
            new HoldingComputationService(),
            properties,
            mock(OpsService.class)
        ));
    }

    @Test
    void createManualOperationUsesPassedTradeDateWithoutCutoffRouting() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "BUY", tradeDate.toString(), 2000.0, null, 0.0015, "今日买入");

        mockFundAndNav(tradeDate, 1.25);
        doReturn(1.24).when(holdingService).navOnOrBefore("000001", tradeDate.minusDays(1));
        UserFundDailyProfitSnapshotEntity savedSnapshot = new UserFundDailyProfitSnapshotEntity();
        savedSnapshot.setTradeDate(tradeDate);
        savedSnapshot.setShares(1600.0);
        savedSnapshot.setAverageCost(1.2519);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "000001", tradeDate
        )).thenReturn(Optional.empty(), Optional.of(savedSnapshot));
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "000001", tradeDate.minusDays(1)
        )).thenReturn(Optional.empty());
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            currentUser.id(), "000001", "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(holdingService).rebuildSnapshotsFrom(currentUser.id(), "000001", tradeDate);

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("已执行", response.status());
        assertEquals("OPEN_POSITION", response.operation());
        assertEquals(1600.0, response.sharesDelta());
        assertEquals(tradeDate.toString(), response.tradeDate());
    }

    @Test
    void createManualOperationMarksFirstBuyAsOpenPosition() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 28);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "BUY", tradeDate.toString(), 2000.0, null, 0.0015, "首次买入");

        mockFundAndNav(tradeDate, 1.25);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "000001", tradeDate.minusDays(1)
        )).thenReturn(Optional.empty());
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            currentUser.id(), "000001", "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(holdingService).rebuildSnapshotsFrom(currentUser.id(), "000001", tradeDate);

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("OPEN_POSITION", response.operation());
        assertEquals("已执行", response.status());
    }

    @Test
    void createManualOperationMarksSellToZeroAsClosePosition() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 28);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("000001", "SELL", tradeDate.toString(), null, 300.0, 0.0015, "全部卖出");

        mockFundAndNav(tradeDate, 1.25);
        UserFundDailyProfitSnapshotEntity baseline = new UserFundDailyProfitSnapshotEntity();
        baseline.setShares(300.0);
        baseline.setAverageCost(1.10);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "000001", tradeDate.minusDays(1)
        )).thenReturn(Optional.of(baseline));
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            currentUser.id(), "000001", "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(holdingService).rebuildSnapshotsFrom(currentUser.id(), "000001", tradeDate);

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("CLOSE_POSITION", response.operation());
    }

    @Test
    void createManualOperationAllowsSellUsingSameDaySnapshotAsBaseline() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("004997", "SELL", tradeDate.toString(), null, 392.0, 0.0, "按当天快照卖出");

        when(fundRepository.findByCode("004997")).thenReturn(Optional.of(fund("004997", "汇添富互联网核心资产")));
        doReturn(1.2816).when(holdingService).navOnOrBefore("004997", tradeDate);
        doReturn(1.2741).when(holdingService).navOnOrBefore("004997", tradeDate.minusDays(1));
        UserFundDailyProfitSnapshotEntity sameDaySnapshot = new UserFundDailyProfitSnapshotEntity();
        sameDaySnapshot.setTradeDate(tradeDate);
        sameDaySnapshot.setShares(392.4339);
        sameDaySnapshot.setAverageCost(1.2486);
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "004997", tradeDate
        )).thenReturn(Optional.of(sameDaySnapshot));
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(holdingService).rebuildSnapshotsFrom(currentUser.id(), "004997", tradeDate);

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("SELL", response.operation());
        assertEquals("已执行", response.status());
        assertEquals(-392.0, response.sharesDelta());
    }

    @Test
    void createManualOperationFallsBackToCurrentHoldingWhenSnapshotsAreMissing() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        LocalDate tradeDate = LocalDate.of(2026, 3, 30);
        HoldingDtos.CreateHoldingOperationRequest request =
            new HoldingDtos.CreateHoldingOperationRequest("004997", "SELL", tradeDate.toString(), null, 392.0, 0.0, "缺快照兜底");

        when(fundRepository.findByCode("004997")).thenReturn(Optional.of(fund("004997", "汇添富互联网核心资产")));
        doReturn(tradeDate).when(holdingService).currentDate();
        doReturn(1.2741).when(holdingService).navOnOrBefore("004997", tradeDate);
        doReturn(1.2816).when(holdingService).navOnOrBefore("004997", tradeDate.minusDays(1));
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "004997", tradeDate
        )).thenReturn(Optional.empty(), Optional.empty(), Optional.of(snapshot(tradeDate, 0.4339, 1.2486)));
        when(dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            currentUser.id(), "004997", tradeDate.minusDays(1)
        )).thenReturn(Optional.empty());
        when(operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
            currentUser.id(), "004997", "已执行", tradeDate, tradeDate
        )).thenReturn(List.of());
        when(userFundHoldingRepository.findByUserIdAndFundCode(currentUser.id(), "004997"))
            .thenReturn(Optional.of(holding("004997", "汇添富互联网核心资产", 392.4339, 1.2486)));
        when(operationRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dailyProfitSnapshotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userFundHoldingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldingDtos.HoldingOperationResponse response = holdingService.createManualOperation(currentUser, request);

        assertEquals("SELL", response.operation());
        assertEquals("已执行", response.status());
        assertEquals(-392.0, response.sharesDelta());
    }

    private void mockFundAndNav(LocalDate tradeDate, double nav) {
        when(fundRepository.findByCode("000001")).thenReturn(Optional.of(fund()));
        NavHistoryEntity navHistory = new NavHistoryEntity();
        navHistory.setFundCode("000001");
        navHistory.setTradeDate(tradeDate);
        navHistory.setUnitNav(nav);
        doReturn(nav).when(holdingService).navOnOrBefore("000001", tradeDate);
        when(navHistoryRepository.findTopByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc("000001", tradeDate))
            .thenReturn(Optional.of(navHistory));
    }

    private FundEntity fund() {
        return fund("000001", "华夏成长优选混合");
    }

    private FundEntity fund(String code, String name) {
        FundEntity fund = new FundEntity();
        fund.setCode(code);
        fund.setName(name);
        return fund;
    }

    private UserFundHoldingEntity holding(String code, String name, double shares, double averageCost) {
        UserFundHoldingEntity entity = new UserFundHoldingEntity();
        entity.setFundCode(code);
        entity.setFundName(name);
        entity.setShares(shares);
        entity.setAverageCost(averageCost);
        return entity;
    }

    private UserFundDailyProfitSnapshotEntity snapshot(LocalDate tradeDate, double shares, double averageCost) {
        UserFundDailyProfitSnapshotEntity entity = new UserFundDailyProfitSnapshotEntity();
        entity.setTradeDate(tradeDate);
        entity.setShares(shares);
        entity.setAverageCost(averageCost);
        return entity;
    }
}
