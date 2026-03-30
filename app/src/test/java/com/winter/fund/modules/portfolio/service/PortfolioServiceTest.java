package com.winter.fund.modules.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.FundSnapshotRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.repository.UserFundHoldingRepository;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import com.winter.fund.modules.holding.service.HoldingComputationService;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import com.winter.fund.modules.portfolio.model.WatchlistEntity;
import com.winter.fund.modules.portfolio.model.WatchlistGroupEntity;
import com.winter.fund.modules.portfolio.repository.AlertRuleRepository;
import com.winter.fund.modules.portfolio.repository.ImportJobRepository;
import com.winter.fund.modules.portfolio.repository.PortfolioRepository;
import com.winter.fund.modules.portfolio.repository.SipPlanRepository;
import com.winter.fund.modules.portfolio.repository.WatchlistGroupRepository;
import com.winter.fund.modules.portfolio.repository.WatchlistRepository;
import com.winter.fund.modules.portfolio.repository.WeeklyReportRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PortfolioServiceTest {

    private WatchlistRepository watchlistRepository;
    private WatchlistGroupRepository watchlistGroupRepository;
    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        watchlistRepository = mock(WatchlistRepository.class);
        watchlistGroupRepository = mock(WatchlistGroupRepository.class);
        MarketDataProperties marketDataProperties = new MarketDataProperties();
        marketDataProperties.setTimezone("Asia/Shanghai");
        portfolioService = spy(new PortfolioService(
            watchlistRepository,
            watchlistGroupRepository,
            mock(PortfolioRepository.class),
            mock(SipPlanRepository.class),
            mock(ImportJobRepository.class),
            mock(WeeklyReportRepository.class),
            mock(AlertRuleRepository.class),
            mock(FundRepository.class),
            mock(FundSnapshotRepository.class),
            mock(FundEstimateRepository.class),
            mock(NavHistoryRepository.class),
            mock(UserFundHoldingRepository.class),
            mock(UserFundOperationRecordRepository.class),
            mock(HoldingComputationService.class),
            mock(HoldingService.class),
            mock(OpsService.class),
            marketDataProperties
        ));
    }

    @Test
    void updateWatchlistGroupsSkipsPersistenceWhenGroupsAreUnchanged() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        WatchlistEntity watchlist = watchlist("watchlist-001", currentUser.id(), "519674");
        WatchlistGroupEntity currentGroup = watchlistGroup(watchlist.getId(), "行业主题");
        PortfolioDtos.UpdateWatchlistGroupsRequest request =
            new PortfolioDtos.UpdateWatchlistGroupsRequest(List.of("519674"), List.of("行业主题"));
        List<PortfolioDtos.WatchlistItemResponse> expected = List.of();

        when(watchlistRepository.findByUserIdAndFundCode(currentUser.id(), "519674")).thenReturn(Optional.of(watchlist));
        when(watchlistGroupRepository.findByWatchlistIdIn(List.of(watchlist.getId()))).thenReturn(List.of(currentGroup));
        doReturn(expected).when(portfolioService).getWatchlist(currentUser);

        List<PortfolioDtos.WatchlistItemResponse> actual = portfolioService.updateWatchlistGroups(currentUser, request);

        assertSame(expected, actual);
        verify(watchlistGroupRepository, never()).deleteByWatchlistIdIn(anyList());
        verify(watchlistGroupRepository, never()).flush();
        verify(watchlistGroupRepository, never()).saveAll(anyList());
    }

    @Test
    void updateWatchlistGroupsFlushesDeleteBeforeInsertWhenGroupsChange() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        WatchlistEntity watchlist = watchlist("watchlist-001", currentUser.id(), "519674");
        WatchlistGroupEntity currentGroup = watchlistGroup(watchlist.getId(), "行业主题");
        PortfolioDtos.UpdateWatchlistGroupsRequest request =
            new PortfolioDtos.UpdateWatchlistGroupsRequest(List.of("519674"), List.of("核心配置"));
        List<PortfolioDtos.WatchlistItemResponse> expected = List.of();

        when(watchlistRepository.findByUserIdAndFundCode(currentUser.id(), "519674")).thenReturn(Optional.of(watchlist));
        when(watchlistGroupRepository.findByWatchlistIdIn(List.of(watchlist.getId()))).thenReturn(List.of(currentGroup));
        when(watchlistGroupRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(expected).when(portfolioService).getWatchlist(currentUser);

        List<PortfolioDtos.WatchlistItemResponse> actual = portfolioService.updateWatchlistGroups(currentUser, request);

        assertSame(expected, actual);
        InOrder inOrder = inOrder(watchlistGroupRepository);
        inOrder.verify(watchlistGroupRepository).deleteByWatchlistIdIn(eq(List.of(watchlist.getId())));
        inOrder.verify(watchlistGroupRepository).flush();
        inOrder.verify(watchlistGroupRepository).saveAll(anyList());
    }

    private WatchlistEntity watchlist(String id, String userId, String fundCode) {
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setNote("note");
        return entity;
    }

    private WatchlistGroupEntity watchlistGroup(String watchlistId, String groupCode) {
        WatchlistGroupEntity entity = new WatchlistGroupEntity();
        entity.setId("group-" + groupCode);
        entity.setWatchlistId(watchlistId);
        entity.setGroupCode(groupCode);
        return entity;
    }
}
