package com.winter.fund.modules.watchlist.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.watchlist.model.WatchlistDtos;
import com.winter.fund.modules.watchlist.model.WatchlistEntity;
import com.winter.fund.modules.watchlist.model.WatchlistGroupEntity;
import com.winter.fund.modules.watchlist.repository.WatchlistGroupRepository;
import com.winter.fund.modules.watchlist.repository.WatchlistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WatchlistServiceTest {

    private WatchlistRepository watchlistRepository;
    private WatchlistGroupRepository watchlistGroupRepository;
    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistRepository = mock(WatchlistRepository.class);
        watchlistGroupRepository = mock(WatchlistGroupRepository.class);
        MarketDataProperties marketDataProperties = new MarketDataProperties();
        marketDataProperties.setTimezone("Asia/Shanghai");
        watchlistService = spy(new WatchlistService(
            watchlistRepository,
            watchlistGroupRepository,
            mock(FundRepository.class),
            mock(FundEstimateRepository.class),
            mock(NavHistoryRepository.class),
            mock(HoldingService.class),
            mock(OpsService.class),
            marketDataProperties
        ));
    }

    @Test
    void updateWatchlistGroupsSkipsPersistenceWhenGroupIsUnchanged() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        WatchlistEntity watchlist = watchlist("watchlist-001", currentUser.id(), "519674", "group-001");
        WatchlistGroupEntity group = group("group-001", currentUser.id(), "行业主题");
        WatchlistDtos.UpdateWatchlistGroupsRequest request =
            new WatchlistDtos.UpdateWatchlistGroupsRequest(List.of("519674"), "行业主题");
        List<WatchlistDtos.WatchlistItemResponse> expected = List.of();

        when(watchlistRepository.findByUserIdAndFundCode(currentUser.id(), "519674")).thenReturn(Optional.of(watchlist));
        when(watchlistGroupRepository.findByUserIdAndGroupName(currentUser.id(), "行业主题")).thenReturn(Optional.of(group));
        when(watchlistService.getWatchlist(currentUser)).thenReturn(expected);

        List<WatchlistDtos.WatchlistItemResponse> actual = watchlistService.updateWatchlistGroups(currentUser, request);

        assertSame(expected, actual);
        verify(watchlistRepository, never()).saveAll(anyList());
    }

    @Test
    void updateWatchlistGroupsPersistsWhenGroupChanges() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        WatchlistEntity watchlist = watchlist("watchlist-001", currentUser.id(), "519674", "group-old");
        WatchlistGroupEntity group = group("group-new", currentUser.id(), "稳健配置");
        WatchlistDtos.UpdateWatchlistGroupsRequest request =
            new WatchlistDtos.UpdateWatchlistGroupsRequest(List.of("519674"), "稳健配置");
        List<WatchlistDtos.WatchlistItemResponse> expected = List.of();

        when(watchlistRepository.findByUserIdAndFundCode(currentUser.id(), "519674")).thenReturn(Optional.of(watchlist));
        when(watchlistGroupRepository.findByUserIdAndGroupName(currentUser.id(), "稳健配置")).thenReturn(Optional.of(group));
        when(watchlistRepository.saveAll(List.of(watchlist))).thenReturn(List.of(watchlist));
        when(watchlistService.getWatchlist(currentUser)).thenReturn(expected);

        List<WatchlistDtos.WatchlistItemResponse> actual = watchlistService.updateWatchlistGroups(currentUser, request);

        assertSame(expected, actual);
        assertEquals("group-new", watchlist.getGroupId());
        verify(watchlistRepository).saveAll(List.of(watchlist));
    }

    @Test
    void createGroupRejectsDuplicateNameForSameUser() {
        CurrentUser currentUser = new CurrentUser("user-demo-001", "Demo Analyst", "demo_analyst");
        WatchlistDtos.CreateWatchlistGroupRequest request = new WatchlistDtos.CreateWatchlistGroupRequest("科技主线");

        when(watchlistGroupRepository.findByUserIdAndGroupName(currentUser.id(), "科技主线"))
            .thenReturn(Optional.of(group("group-existing", currentUser.id(), "科技主线")));

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> watchlistService.createGroup(currentUser, request)
        );

        assertEquals("当前用户已存在同名分组", error.getMessage());
        verify(watchlistGroupRepository, never()).save(any());
    }

    private WatchlistEntity watchlist(String id, String userId, String fundCode, String groupId) {
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setNote("note");
        entity.setGroupId(groupId);
        return entity;
    }

    private WatchlistGroupEntity group(String id, String userId, String groupName) {
        WatchlistGroupEntity entity = new WatchlistGroupEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setGroupName(groupName);
        return entity;
    }
}
