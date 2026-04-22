package com.winter.fund.modules.watchlist;

/**
 * 自选模块控制器，负责对外暴露自选列表相关 HTTP 接口。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.watchlist.model.WatchlistDtos;
import com.winter.fund.modules.watchlist.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Watchlist", description = "自选基金接口")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /**
     * 返回watchlist结果。
     */
    @GetMapping("/watchlist")
    @Operation(summary = "获取自选列表", description = "返回当前用户的自选基金、单个分组和是否已持仓状态。")
    public List<WatchlistDtos.WatchlistItemResponse> watchlist(@AuthenticationPrincipal CurrentUser currentUser) {
        return watchlistService.getWatchlist(currentUser);
    }

    @GetMapping("/watchlist/groups")
    @Operation(summary = "获取自选分组", description = "返回当前用户可用的自选分组选项。")
    public List<WatchlistDtos.WatchlistGroupResponse> groups(@AuthenticationPrincipal CurrentUser currentUser) {
        return watchlistService.getGroups(currentUser);
    }

    @PostMapping("/watchlist/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增自选分组", description = "新增当前用户的自选分组，并校验是否存在同名分组。")
    public WatchlistDtos.WatchlistGroupResponse createGroup(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody WatchlistDtos.CreateWatchlistGroupRequest request
    ) {
        return watchlistService.createGroup(currentUser, request);
    }

    @PostMapping("/watchlist")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增自选", description = "将基金加入自选列表，并指定一个用户维度分组。")
    public WatchlistDtos.WatchlistItemResponse addWatchlist(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody WatchlistDtos.CreateWatchlistRequest request
    ) {
        return watchlistService.addWatchlist(currentUser, request);
    }

    @PatchMapping("/watchlist/groups")
    @Operation(summary = "批量替换自选分组", description = "对传入基金列表批量替换单个分组字段。")
    public List<WatchlistDtos.WatchlistItemResponse> updateWatchlistGroups(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody WatchlistDtos.UpdateWatchlistGroupsRequest request
    ) {
        return watchlistService.updateWatchlistGroups(currentUser, request);
    }

    @DeleteMapping("/watchlist/{fundCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除自选", description = "从当前用户自选列表中移除指定基金。")
    public void removeWatchlist(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode
    ) {
        watchlistService.removeWatchlist(currentUser, fundCode);
    }
}
