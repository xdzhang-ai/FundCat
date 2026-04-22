package com.winter.fund.modules.watchlist.service;

/**
 * 自选模块服务，负责封装自选基金和自选分组的核心业务逻辑。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);
    private static final String DEFAULT_GROUP_NAME = "全部";

    private final WatchlistRepository watchlistRepository;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final FundRepository fundRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final HoldingService holdingService;
    private final OpsService opsService;
    private final Clock clock;

    public WatchlistService(
        WatchlistRepository watchlistRepository,
        WatchlistGroupRepository watchlistGroupRepository,
        FundRepository fundRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        HoldingService holdingService,
        OpsService opsService,
        MarketDataProperties marketDataProperties
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistGroupRepository = watchlistGroupRepository;
        this.fundRepository = fundRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.holdingService = holdingService;
        this.opsService = opsService;
        this.clock = Clock.system(java.time.ZoneId.of(marketDataProperties.getTimezone()));
    }

    /**
     * 获取当前用户可见的自选列表。
     */
    public List<WatchlistDtos.WatchlistItemResponse> getWatchlist(CurrentUser currentUser) {
        ensureDefaultGroup(currentUser.id());
        List<WatchlistEntity> items = watchlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.id());
        Map<String, FundEntity> funds = fundRepository.findByCodeIn(items.stream().map(WatchlistEntity::getFundCode).toList()).stream()
            .collect(Collectors.toMap(FundEntity::getCode, Function.identity()));
        Map<String, NavHistoryEntity> latestNavs = items.stream()
            .map(WatchlistEntity::getFundCode)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), this::latestNav));
        Map<String, FundEstimateEntity> estimates = items.stream()
            .map(WatchlistEntity::getFundCode)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), this::latestEstimate));
        Map<String, WatchlistGroupEntity> groupsById = groupsById(items.stream().map(WatchlistEntity::getGroupId).toList());
        Set<String> heldCodes = new LinkedHashSet<>(holdingService.getHeldFundCodes(currentUser.id()));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");

        return items.stream()
            .map(item -> {
                FundEntity fund = Optional.ofNullable(funds.get(item.getFundCode()))
                    .orElseThrow(() -> new NotFoundException("Fund not found"));
                NavHistoryEntity latestNav = latestNavs.get(item.getFundCode());
                FundEstimateEntity estimate = estimates.get(item.getFundCode());
                WatchlistGroupEntity group = groupsById.get(item.getGroupId());
                return new WatchlistDtos.WatchlistItemResponse(
                    fund.getCode(),
                    fund.getName(),
                    item.getNote(),
                    round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : latestNav.getDayGrowth(), 4),
                    round(latestNav.getUnitNav(), 4),
                    round(estimateReferenceEnabled ? estimate.getEstimatedNav() : latestNav.getUnitNav(), 4),
                    group == null ? DEFAULT_GROUP_NAME : group.getGroupName(),
                    heldCodes.contains(item.getFundCode())
                );
            })
            .toList();
    }

    /**
     * 获取当前用户的自选分组选项。
     */
    @Transactional
    public List<WatchlistDtos.WatchlistGroupResponse> getGroups(CurrentUser currentUser) {
        ensureDefaultGroup(currentUser.id());
        return watchlistGroupRepository.findByUserIdOrderByCreatedAtAsc(currentUser.id()).stream()
            .map(group -> new WatchlistDtos.WatchlistGroupResponse(group.getId(), group.getGroupName()))
            .toList();
    }

    /**
     * 新增自选分组，并按用户维度校验是否重名。
     */
    @Transactional
    public WatchlistDtos.WatchlistGroupResponse createGroup(CurrentUser currentUser, WatchlistDtos.CreateWatchlistGroupRequest request) {
        String normalizedGroupName = normalizeGroupName(request.name());
        watchlistGroupRepository.findByUserIdAndGroupName(currentUser.id(), normalizedGroupName).ifPresent(existing -> {
            throw new IllegalArgumentException("当前用户已存在同名分组");
        });
        WatchlistGroupEntity entity = new WatchlistGroupEntity();
        entity.setId(buildGroupId(currentUser.id(), normalizedGroupName));
        entity.setUserId(currentUser.id());
        entity.setGroupName(normalizedGroupName);
        entity.setCreatedAt(LocalDateTime.now(clock));
        watchlistGroupRepository.save(entity);
        return new WatchlistDtos.WatchlistGroupResponse(entity.getId(), entity.getGroupName());
    }

    /**
     * 新增自选基金，并关联到指定分组。
     */
    @Transactional
    public WatchlistDtos.WatchlistItemResponse addWatchlist(CurrentUser currentUser, WatchlistDtos.CreateWatchlistRequest request) {
        WatchlistGroupEntity group = findOrCreateGroup(currentUser.id(), request.group());
        log.info("Adding watchlist item, userId={}, fundCode={}, groupId={}", currentUser.id(), request.fundCode(), group.getId());
        watchlistRepository.findByUserIdAndFundCode(currentUser.id(), request.fundCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Fund already exists in watchlist");
        });
        FundEntity fund = requiredFund(request.fundCode());
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.id());
        entity.setFundCode(request.fundCode());
        entity.setNote(request.note());
        entity.setGroupId(group.getId());
        entity.setCreatedAt(LocalDateTime.now(clock));
        watchlistRepository.save(entity);
        return getWatchlist(currentUser).stream()
            .filter(item -> item.code().equals(request.fundCode()))
            .findFirst()
            .orElseGet(() -> toWatchlistItem(currentUser.id(), entity, fund, group));
    }

    /**
     * 批量更新自选基金的分组关联。
     */
    @Transactional
    public List<WatchlistDtos.WatchlistItemResponse> updateWatchlistGroups(
        CurrentUser currentUser,
        WatchlistDtos.UpdateWatchlistGroupsRequest request
    ) {
        WatchlistGroupEntity group = findOrCreateGroup(currentUser.id(), request.group());
        List<WatchlistEntity> watchlists = request.fundCodes().stream()
            .map(fundCode -> watchlistRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
                .orElseThrow(() -> new NotFoundException("Watchlist item not found")))
            .toList();
        List<WatchlistEntity> watchlistsToUpdate = watchlists.stream()
            .filter(watchlist -> !group.getId().equals(watchlist.getGroupId()))
            .toList();
        log.info("Replacing watchlist group, userId={}, fundCodes={}, groupId={}",
            currentUser.id(), request.fundCodes(), group.getId());
        if (watchlistsToUpdate.isEmpty()) {
            return getWatchlist(currentUser);
        }
        watchlistsToUpdate.forEach(watchlist -> watchlist.setGroupId(group.getId()));
        watchlistRepository.saveAll(watchlistsToUpdate);
        return getWatchlist(currentUser);
    }

    /**
     * 删除单只自选基金。
     */
    @Transactional
    public void removeWatchlist(CurrentUser currentUser, String fundCode) {
        log.info("Removing watchlist item, userId={}, fundCode={}", currentUser.id(), fundCode);
        WatchlistEntity entity = watchlistRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
            .orElseThrow(() -> new NotFoundException("Watchlist item not found"));
        watchlistRepository.delete(entity);
    }

    /**
     * 将单条自选实体转换为接口响应。
     */
    private WatchlistDtos.WatchlistItemResponse toWatchlistItem(
        String userId,
        WatchlistEntity entity,
        FundEntity fund,
        WatchlistGroupEntity group
    ) {
        NavHistoryEntity latestNav = latestNav(entity.getFundCode());
        FundEstimateEntity estimate = latestEstimate(entity.getFundCode());
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        boolean held = holdingService.getHeldFundCodes(userId).contains(entity.getFundCode());
        return new WatchlistDtos.WatchlistItemResponse(
            fund.getCode(),
            fund.getName(),
            entity.getNote(),
            round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : latestNav.getDayGrowth(), 4),
            round(latestNav.getUnitNav(), 4),
            round(estimateReferenceEnabled ? estimate.getEstimatedNav() : latestNav.getUnitNav(), 4),
            group.getGroupName(),
            held
        );
    }

    /**
     * 获取或创建默认分组。
     */
    private WatchlistGroupEntity ensureDefaultGroup(String userId) {
        return findOrCreateGroup(userId, DEFAULT_GROUP_NAME);
    }

    /**
     * 根据分组名称查找分组，不存在时自动创建。
     */
    private WatchlistGroupEntity findOrCreateGroup(String userId, String groupName) {
        String normalizedGroupName = normalizeGroupName(groupName);
        return watchlistGroupRepository.findByUserIdAndGroupName(userId, normalizedGroupName)
            .orElseGet(() -> {
                WatchlistGroupEntity entity = new WatchlistGroupEntity();
                entity.setId(buildGroupId(userId, normalizedGroupName));
                entity.setUserId(userId);
                entity.setGroupName(normalizedGroupName);
                entity.setCreatedAt(LocalDateTime.now(clock));
                return watchlistGroupRepository.save(entity);
            });
    }

    /**
     * 构建分组映射，便于按 groupId 回填名称。
     */
    private Map<String, WatchlistGroupEntity> groupsById(List<String> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return watchlistGroupRepository.findByIdIn(groupIds).stream()
            .collect(Collectors.toMap(WatchlistGroupEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 生成稳定的分组 ID，方便迁移脚本和种子数据共用同一规则。
     */
    private String buildGroupId(String userId, String groupName) {
        return userId + ":" + groupName;
    }

    /**
     * 获取必定存在的基金实体。
     */
    private FundEntity requiredFund(String fundCode) {
        return fundRepository.findByCode(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
    }

    /**
     * 获取基金最新净值快照。
     */
    private NavHistoryEntity latestNav(String fundCode) {
        return navHistoryRepository.findTopByFundCodeOrderByTradeDateDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund nav history not found"));
    }

    /**
     * 获取基金最新估值快照。
     */
    private FundEstimateEntity latestEstimate(String fundCode) {
        return fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
    }

    /**
     * 规范化分组名称。
     */
    private String normalizeGroupName(String groupName) {
        if (groupName == null) {
            return DEFAULT_GROUP_NAME;
        }
        String normalized = groupName.trim();
        return normalized.isBlank() ? DEFAULT_GROUP_NAME : normalized;
    }

    /**
     * 按指定精度对数值做舍入。
     */
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
