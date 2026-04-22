package com.winter.fund.modules.holding.service;

/**
 * 持仓模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.common.config.PersistenceProperties;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.infrastructure.redis.HoldingCurrentStateCacheService;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);
    private static final String STATUS_PENDING = "确认中";
    private static final String STATUS_EXECUTED = "已执行";
    private static final String OPERATION_BUY = "BUY";
    private static final String OPERATION_SELL = "SELL";
    private static final String OPERATION_OPEN_POSITION = "OPEN_POSITION";
    private static final String OPERATION_CLOSE_POSITION = "CLOSE_POSITION";
    private static final String OPERATION_SIP_BUY = "SIP_BUY";

    private final UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository;
    private final UserFundOperationRecordRepository operationRecordRepository;
    private final FundRepository fundRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final HoldingComputationService computationService;
    private final MarketDataProperties marketDataProperties;
    private final PersistenceProperties persistenceProperties;
    private final OpsService opsService;
    private final HoldingCurrentStateCacheService cacheService;
    private final Clock clock;

    public HoldingService(
        UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository,
        UserFundOperationRecordRepository operationRecordRepository,
        FundRepository fundRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        HoldingComputationService computationService,
        MarketDataProperties marketDataProperties,
        PersistenceProperties persistenceProperties,
        OpsService opsService,
        HoldingCurrentStateCacheService cacheService
    ) {
        this.dailyProfitSnapshotRepository = dailyProfitSnapshotRepository;
        this.operationRecordRepository = operationRecordRepository;
        this.fundRepository = fundRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.computationService = computationService;
        this.marketDataProperties = marketDataProperties;
        this.persistenceProperties = persistenceProperties;
        this.opsService = opsService;
        this.cacheService = cacheService;
        this.clock = Clock.system(ZoneId.of(marketDataProperties.getTimezone()));
    }

    /**
     * 返回当前持有基金代码集合，并在 Redis 中做 26 小时缓存。
     * 这里的 held 不是依赖单独的 current 表，而是从用户各基金“最新一条日终快照”里推导。
     * Redis 只承担日中高频查询缓存，缓存 miss 时仍然以日终快照作为事实来源回源重建。
     */
    public Set<String> getHeldFundCodes(String userId) {
        return cacheService.getHeldCodes(userId).orElseGet(() -> {
            Set<String> heldCodes = loadCurrentHoldingStates(userId).stream()
                .map(CurrentHoldingState::fundCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            cacheService.putHeldCodes(userId, heldCodes);
            return heldCodes;
        });
    }

    /**
     * 获取最近的持仓操作记录。
     */
    public List<HoldingDtos.HoldingOperationResponse> getRecentOperations(String userId) {
        return operationRecordRepository.findTop12ByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toOperationResponse)
            .toList();
    }

    /**
     * 获取基金详情页展示的最近订单记录。
     */
    public List<HoldingDtos.OperationRecordResponse> getOrders(CurrentUser currentUser, String scope) {
        List<UserFundOperationRecordEntity> records = operationRecordRepository
            .findTop12ByUserIdOrderByCreatedAtDesc(currentUser.id());
        log.info("Loading recent operation records, userId={}, scope={}, count={}", currentUser.id(), scope, records.size());
        return records.stream().map(this::toOrderResponse).toList();
    }

    /**
     * 获取持仓概览页数据，并优先命中 Redis 缓存。
     * 页面看到的是“当前持仓视图”，但真正持久化的结果只有日终快照；当前态由最新日快照
     * 再结合当前净值口径实时推导，随后缓存到 Redis，避免每次都重新聚合所有基金。
     */
    public HoldingDtos.HoldingsOverviewResponse getOverview(CurrentUser currentUser) {
        return cacheService.getOverview(currentUser.id()).orElseGet(() -> {
            HoldingDtos.HoldingsOverviewResponse response = computeOverview(currentUser.id());
            cacheService.putOverview(currentUser.id(), response);
            cacheService.putHeldCodes(
                currentUser.id(),
                response.items().stream().map(HoldingDtos.HoldingListItemResponse::fundCode).collect(Collectors.toCollection(LinkedHashSet::new))
            );
            return response;
        });
    }

    /**
     * 获取单只基金的持仓洞察，并优先命中 Redis 缓存。
     * 洞察页既要展示当前盈亏，也要展示日涨跌、近一年收益等派生指标，因此先从最新日快照
     * 取出份额和成本，再叠加当前净值、上一交易日净值和历史净值口径做二次计算。
     */
    public HoldingDtos.HoldingInsightResponse getInsight(CurrentUser currentUser, String fundCode) {
        return cacheService.getInsight(currentUser.id(), fundCode).orElseGet(() -> {
            HoldingDtos.HoldingInsightResponse response = computeInsight(currentUser.id(), fundCode);
            cacheService.putInsight(currentUser.id(), fundCode, response);
            return response;
        });
    }

    /**
     * 创建持仓。
     */
    @Transactional
    public UserFundDailyProfitSnapshotEntity createHolding(CurrentUser currentUser, HoldingDtos.UpsertHoldingRequest request) {
        return upsertHolding(currentUser, request.fundCode(), request);
    }

    /**
     * 按金额和盈亏反推份额，并更新当日收盘快照。
     * 这个入口服务于“补录当前持仓”场景：用户给出持有金额和持有收益，系统按选定净值口径
     * 反推出份额和平均成本，再把结果固化到今天的日终快照中。
     */
    @Transactional
    public UserFundDailyProfitSnapshotEntity upsertHolding(CurrentUser currentUser, String fundCode, HoldingDtos.UpsertHoldingRequest request) {
        requiredFund(fundCode);
        LocalDate basisDate = resolveBasisDate(request.amountBasis());
        double basisNav = resolveHoldingBasisNav(fundCode, request.amountBasis(), basisDate);
        log.info("Upserting holding snapshot, userId={}, fundCode={}, amountBasis={}, basisDate={}, basisNav={}",
            currentUser.id(), fundCode, request.amountBasis(), basisDate, basisNav);
        HoldingComputationService.HoldingState state = computationService.rebuildFromAmount(request.amount(), request.holdingPnl(), basisNav);
        UserFundDailyProfitSnapshotEntity snapshot = upsertDailySnapshot(currentUser.id(), fundCode, LocalDate.now(clock), state, resolveCurrentNav(fundCode));
        refreshCaches(currentUser.id(), fundCode);
        log.info("Holding snapshot upserted from amount basis, userId={}, fundCode={}, shares={}, averageCost={}, marketValue={}",
            currentUser.id(), fundCode, snapshot.getShares(), snapshot.getAverageCost(), snapshot.getMarketValue());
        return snapshot;
    }

    /**
     * 创建手动操作。
     * 手工买卖先生成一条操作记录：如果该交易日确认净值已经存在，就直接落成“已执行”并回算；
     * 否则先落成“确认中”，等晚间净值轮询补齐 nav 和份额后再统一更新持仓。
     */
    @Transactional
    public HoldingDtos.HoldingOperationResponse createManualOperation(CurrentUser currentUser, HoldingDtos.CreateHoldingOperationRequest request) {
        FundEntity fund = requiredFund(request.fundCode());
        LocalDate requestedTradeDate = LocalDate.parse(request.tradeDate());
        validateTradeDate(requestedTradeDate);
        Optional<NavHistoryEntity> confirmedNav = navHistoryRepository.findByFundCodeAndTradeDate(fund.getCode(), requestedTradeDate);
        UserFundOperationRecordEntity entity = shouldExecuteImmediately(requestedTradeDate, confirmedNav.isPresent())
            ? buildExecutedOperationRecord(currentUser.id(), fund, request, confirmedNav.orElseThrow().getUnitNav(), requestedTradeDate)
            : buildPendingOperationRecord(currentUser.id(), fund, request, requestedTradeDate);
        operationRecordRepository.save(entity);
        log.info("Manual operation persisted, operationId={}, userId={}, fundCode={}, operation={}, tradeDate={}, amount={}, sharesDelta={}",
            entity.getId(), currentUser.id(), fund.getCode(), entity.getOperation(), entity.getTradeDate(), entity.getAmount(), entity.getSharesDelta());
        if (STATUS_EXECUTED.equals(entity.getStatus())) {
            rebuildSnapshotsFrom(currentUser.id(), fund.getCode(), entity.getTradeDate());
        }
        return toOperationResponse(entity);
    }

    /**
     * 确认所有已拿到净值的待确认手动操作。
     * 这里负责把 MANUAL + 确认中 的记录补齐为已执行记录；真正的持仓结果不会直接写入
     * 某张 current 表，而是通过后续的 rebuildSnapshotsFrom 统一重建日终快照并刷新缓存。
     */
    @Transactional
    public List<HoldingRebuildTarget> confirmPendingManualOperations(String fundCode, LocalDate tradeDate) {
        List<UserFundOperationRecordEntity> pendingRecords = operationRecordRepository
            .findBySourceAndStatusAndFundCodeAndTradeDateOrderByCreatedAtAsc("MANUAL", STATUS_PENDING, fundCode, tradeDate);
        List<HoldingRebuildTarget> rebuildTargets = new ArrayList<>();
        List<UserFundOperationRecordEntity> recordsToUpdate = new ArrayList<>();
        for (UserFundOperationRecordEntity record : pendingRecords) {
            Optional<NavHistoryEntity> navHistory = navHistoryRepository.findByFundCodeAndTradeDate(record.getFundCode(), record.getTradeDate());
            if (navHistory.isEmpty()) {
                log.info("Skipping manual confirmation due to missing nav, operationId={}, fundCode={}, tradeDate={}",
                    record.getId(), record.getFundCode(), record.getTradeDate());
                continue;
            }
            try {
                finalizePendingManualOperation(record, navHistory.get().getUnitNav());
                recordsToUpdate.add(record);
                rebuildTargets.add(new HoldingRebuildTarget(record.getUserId(), record.getFundCode(), record.getTradeDate()));
            } catch (IllegalArgumentException error) {
                log.warn("Unable to confirm pending manual operation, operationId={}, fundCode={}, tradeDate={}, reason={}",
                    record.getId(), record.getFundCode(), record.getTradeDate(), error.getMessage());
            }
        }
        saveInBatches(recordsToUpdate, operationRecordRepository::saveAll);
        log.info("Pending manual confirmation finished, fundCode={}, tradeDate={}, confirmed={}", fundCode, tradeDate, rebuildTargets.size());
        return rebuildTargets;
    }

    /**
     * 从指定交易日起重建持仓日收益快照。
     * 这是持仓落库的统一入口：不论来源是手动买卖还是定投确认，最后都通过“历史快照基线 +
     * 已执行操作流水 + 每日净值”重新推导日终结果。
     */
    @Transactional
    public void rebuildSnapshotsFrom(String userId, String fundCode, LocalDate tradeDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate baselineDate = tradeDate.minusDays(1);
        log.info("Rebuilding holding snapshots, userId={}, fundCode={}, fromTradeDate={}, toTradeDate={}",
            userId, fundCode, tradeDate, today);
        // 基线永远取交易日前最后一条日终快照，因为它保存的是“当晚结算完成后的份额和平均成本”。
        HoldingComputationService.HoldingState state = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, baselineDate)
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(new HoldingComputationService.HoldingState(0, 0));

        // 从受影响的第一天开始，顺序回放之后所有已执行操作；顺序不能丢，因为卖出会消耗当前份额。
        List<UserFundOperationRecordEntity> operations = operationRecordRepository
            .findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(userId, fundCode, STATUS_EXECUTED, tradeDate, today);
        Map<LocalDate, List<UserFundOperationRecordEntity>> operationsByDate = operations.stream()
            .collect(Collectors.groupingBy(UserFundOperationRecordEntity::getTradeDate));

        List<UserFundDailyProfitSnapshotEntity> snapshots = new ArrayList<>();
        for (LocalDate cursor = tradeDate; !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            // 同一天先把所有已执行操作应用到状态上，再用当日净值产出“收盘时”的快照结果。
            for (UserFundOperationRecordEntity operation : operationsByDate.getOrDefault(cursor, List.of())) {
                state = applyOperation(state, operation);
            }
            // 即使当天没有任何操作，只要还持有份额，也要生成一条日终快照来承接净值变化带来的盈亏。
            double nav = navOnOrBefore(fundCode, cursor);
            double previousNav = navOnOrBefore(fundCode, cursor.minusDays(1));
            HoldingComputationService.HoldingMetrics metrics = computationService.metrics(state, nav, previousNav, nav * state.shares());
            snapshots.add(toSnapshot(userId, fundCode, cursor, state, nav, metrics));
        }

        dailyProfitSnapshotRepository.saveAll(snapshots);
        // 日终快照是事实来源，Redis 只是当前态缓存，所以每次回算完成后都要重新刷新缓存视图。
        refreshCaches(userId, fundCode);
        log.info("Holding snapshots rebuilt, userId={}, fundCode={}, snapshotCount={}", userId, fundCode, snapshots.size());
    }

    /**
     * 09:00 预热当天日间动态态缓存。
     * 预热基线取前一日最后一条持仓快照：白天先按昨日收盘口径展示，后续若接入实时估值再只更新 currentNav。
     */
    @Transactional(readOnly = true)
    public int warmIntradayStates(LocalDate tradeDate) {
        LocalDate baselineDate = tradeDate.minusDays(1);
        List<UserFundDailyProfitSnapshotEntity> snapshots = dailyProfitSnapshotRepository.findByTradeDateLessThanEqualOrderByTradeDateDesc(baselineDate);
        Map<String, UserFundDailyProfitSnapshotEntity> latestByUserFund = new LinkedHashMap<>();
        for (UserFundDailyProfitSnapshotEntity snapshot : snapshots) {
            latestByUserFund.putIfAbsent(snapshot.getUserId() + "::" + snapshot.getFundCode(), snapshot);
        }
        int warmed = 0;
        for (UserFundDailyProfitSnapshotEntity snapshot : latestByUserFund.values()) {
            if (snapshot.getShares() <= 0) {
                continue;
            }
            CurrentHoldingState state = loadCurrentHoldingState(snapshot.getUserId(), snapshot.getFundCode())
                .orElse(new CurrentHoldingState(
                    snapshot.getUserId(),
                    snapshot.getFundCode(),
                    requiredFund(snapshot.getFundCode()).getName(),
                    snapshot.getShares(),
                    snapshot.getAverageCost(),
                    snapshot.getTradeDate()
                ));
            cacheService.putIntradayState(
                snapshot.getUserId(),
                snapshot.getFundCode(),
                tradeDate,
                buildIntradayState(snapshot.getUserId(), state, tradeDate, snapshot)
            );
            warmed++;
        }
        return warmed;
    }

    /**
     * 晚间确认净值后失效某基金当日的所有日间缓存，避免继续展示盘中估值口径。
     */
    public void evictIntradayStates(String fundCode, LocalDate tradeDate) {
        cacheService.evictIntradayStatesForFund(fundCode, tradeDate);
    }

    /**
     * 构建已经拿到确认净值的手工操作记录。
     * 买入按金额换算份额，卖出按份额反推金额；同时在落库前就根据交易前持仓状态区分建仓、加仓、
     * 减仓和清仓，避免后面展示最近动作时还要再做一次类型推断。
     */
    private UserFundOperationRecordEntity buildExecutedOperationRecord(
        String userId,
        FundEntity fund,
        HoldingDtos.CreateHoldingOperationRequest request,
        double nav,
        LocalDate tradeDate
    ) {
        UserFundOperationRecordEntity entity = new UserFundOperationRecordEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fund.getCode());
        entity.setSource("MANUAL");
        entity.setTradeDate(tradeDate);
        entity.setNav(round(nav, 4));
        entity.setFeeRate(round(request.feeRate(), 6));
        entity.setNote(request.note());
        entity.setStatus(STATUS_EXECUTED);
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        HoldingComputationService.HoldingState preTradeState = resolveStateBeforeOperation(userId, fund.getCode(), entity.getTradeDate());

        if (OPERATION_BUY.equalsIgnoreCase(request.operation())) {
            if (request.amount() == null || request.amount() <= 0) {
                throw new IllegalArgumentException("买入金额必须大于 0");
            }
            entity.setOperation(classifyBuyOperation(preTradeState));
            entity.setAmount(round(request.amount(), 2));
            entity.setSharesDelta(round(request.amount() / nav, 4));
            entity.setFeeAmount(round(request.amount() * request.feeRate(), 2));
        } else if (OPERATION_SELL.equalsIgnoreCase(request.operation())) {
            if (request.shares() == null || request.shares() <= 0) {
                throw new IllegalArgumentException("卖出份额必须大于 0");
            }
            HoldingComputationService.SellResult sellResult = computationService.applySell(preTradeState, request.shares(), nav, request.feeRate());
            entity.setOperation(classifySellOperation(sellResult.state()));
            entity.setAmount(round(sellResult.grossAmount(), 2));
            entity.setFeeAmount(round(sellResult.feeAmount(), 2));
            if (request.shares() > preTradeState.shares()) {
                throw new IllegalArgumentException("卖出份额不能超过当前持有份额");
            }
            entity.setSharesDelta(round(-request.shares(), 4));
        } else {
            throw new IllegalArgumentException("Unsupported operation");
        }
        return entity;
    }

    /**
     * 构建待确认的手工操作记录。
     * “确认中”阶段只保存后续结算所必需的信息：买入先保留金额，卖出先保留负份额；等确认净值出来后
     * 再补齐 nav、sharesDelta 或卖出金额，避免在未知净值时写入错误结果。
     */
    private UserFundOperationRecordEntity buildPendingOperationRecord(
        String userId,
        FundEntity fund,
        HoldingDtos.CreateHoldingOperationRequest request,
        LocalDate tradeDate
    ) {
        UserFundOperationRecordEntity entity = new UserFundOperationRecordEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fund.getCode());
        entity.setSource("MANUAL");
        entity.setTradeDate(tradeDate);
        entity.setNav(0);
        entity.setFeeRate(round(request.feeRate(), 6));
        entity.setNote(request.note());
        entity.setStatus(STATUS_PENDING);
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setUpdatedAt(LocalDateTime.now(clock));

        if (OPERATION_BUY.equalsIgnoreCase(request.operation())) {
            if (request.amount() == null || request.amount() <= 0) {
                throw new IllegalArgumentException("买入金额必须大于 0");
            }
            entity.setOperation(OPERATION_BUY);
            entity.setAmount(round(request.amount(), 2));
            entity.setSharesDelta(0);
            entity.setFeeAmount(round(request.amount() * request.feeRate(), 2));
            return entity;
        }

        if (OPERATION_SELL.equalsIgnoreCase(request.operation())) {
            if (request.shares() == null || request.shares() <= 0) {
                throw new IllegalArgumentException("卖出份额必须大于 0");
            }
            HoldingComputationService.HoldingState preTradeState = resolveStateBeforeOperation(userId, fund.getCode(), entity.getTradeDate());
            if (request.shares() > preTradeState.shares()) {
                throw new IllegalArgumentException("卖出份额不能超过当前持有份额");
            }
            entity.setOperation(OPERATION_SELL);
            entity.setAmount(0);
            entity.setSharesDelta(round(-request.shares(), 4));
            entity.setFeeAmount(0);
            return entity;
        }

        throw new IllegalArgumentException("Unsupported operation");
    }

    /**
     * 完成待确认手动操作。
     * 这里把“确认中”记录补齐成可参与持仓回放的最终记录：买入补份额，卖出补金额和手续费，
     * 最后再把状态切成已执行，交给统一回算链路处理。
     */
    private void finalizePendingManualOperation(UserFundOperationRecordEntity entity, double nav) {
        HoldingComputationService.HoldingState preTradeState = resolveStateBeforeOperation(entity.getUserId(), entity.getFundCode(), entity.getTradeDate());
        entity.setNav(round(nav, 4));
        if (OPERATION_BUY.equals(entity.getOperation())) {
            if (entity.getAmount() <= 0) {
                throw new IllegalArgumentException("买入金额必须大于 0");
            }
            entity.setSharesDelta(round(entity.getAmount() / nav, 4));
            entity.setFeeAmount(round(entity.getAmount() * entity.getFeeRate(), 2));
            entity.setOperation(classifyBuyOperation(preTradeState));
        } else if (OPERATION_SELL.equals(entity.getOperation())) {
            double shares = Math.abs(entity.getSharesDelta());
            HoldingComputationService.SellResult sellResult = computationService.applySell(preTradeState, shares, nav, entity.getFeeRate());
            entity.setAmount(round(sellResult.grossAmount(), 2));
            entity.setFeeAmount(round(sellResult.feeAmount(), 2));
            entity.setSharesDelta(round(-shares, 4));
            entity.setOperation(classifySellOperation(sellResult.state()));
        } else {
            throw new IllegalArgumentException("Unsupported pending manual operation");
        }
        entity.setStatus(STATUS_EXECUTED);
        entity.setUpdatedAt(LocalDateTime.now(clock));
    }

    /**
     * 把一条已执行操作应用到当前持仓状态上。
     * 买入型操作会增加总成本和份额，卖出型操作会通过 HoldingComputationService 结转成本并减少份额。
     */
    private HoldingComputationService.HoldingState applyOperation(
        HoldingComputationService.HoldingState state,
        UserFundOperationRecordEntity operation
    ) {
        if (isBuyLikeOperation(operation.getOperation())) {
            double totalCost = state.totalCost() + operation.getAmount() + operation.getFeeAmount();
            double totalShares = round(state.shares() + operation.getSharesDelta(), 4);
            return new HoldingComputationService.HoldingState(totalShares, totalShares == 0 ? 0 : round(totalCost / totalShares, 4));
        }
        if (isSellLikeOperation(operation.getOperation())) {
            return computationService.applySell(
                state,
                Math.abs(operation.getSharesDelta()),
                operation.getNav(),
                operation.getFeeRate()
            ).state();
        }
        return state;
    }

    /**
     * 解析指定交易日前的持仓基线。
     * 读取顺序是：
     * 1. 先看同一天是否已经有快照，避免重复补记时丢掉当日已有结果；
     * 2. 没有就取前一交易日最后一条快照作为基线；
     * 3. 再回放同一天已经执行过的记录，得到“这笔新操作发生前”的真实状态。
     */
    private HoldingComputationService.HoldingState resolveStateBeforeOperation(String userId, String fundCode, LocalDate tradeDate) {
        HoldingComputationService.HoldingState sameDayState = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, tradeDate)
            .filter(snapshot -> snapshot.getTradeDate().equals(tradeDate))
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(null);
        if (sameDayState != null) {
            return sameDayState;
        }

        LocalDate baselineDate = tradeDate.minusDays(1);
        HoldingComputationService.HoldingState state = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, baselineDate)
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(new HoldingComputationService.HoldingState(0, 0));
        List<UserFundOperationRecordEntity> sameDayOperations = operationRecordRepository
            .findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(userId, fundCode, STATUS_EXECUTED, tradeDate, tradeDate);
        for (UserFundOperationRecordEntity operation : sameDayOperations) {
            state = applyOperation(state, operation);
        }
        return state;
    }

    /**
     * 仅把当前交易日的持仓状态写入日快照表。
     * 这张表保存的是“某个用户某只基金在某个交易日收盘后的最终结果”，不是临时 current 表；
     * 当前持仓页和 held 判断都从它的最新一条结果派生出来。
     */
    private UserFundDailyProfitSnapshotEntity upsertDailySnapshot(
        String userId,
        String fundCode,
        LocalDate tradeDate,
        HoldingComputationService.HoldingState state,
        double nav
    ) {
        double previousNav = navOnOrBefore(fundCode, tradeDate.minusDays(1));
        HoldingComputationService.HoldingMetrics metrics = computationService.metrics(state, nav, previousNav, nav * state.shares());
        UserFundDailyProfitSnapshotEntity snapshot = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, tradeDate)
            .filter(existing -> existing.getTradeDate().equals(tradeDate))
            .orElseGet(UserFundDailyProfitSnapshotEntity::new);
        if (snapshot.getId() == null) {
            snapshot.setId(UUID.randomUUID().toString());
        }
        snapshot.setUserId(userId);
        snapshot.setFundCode(fundCode);
        snapshot.setTradeDate(tradeDate);
        snapshot.setShares(round(state.shares(), 4));
        snapshot.setAverageCost(round(state.averageCost(), 4));
        snapshot.setNav(round(nav, 4));
        snapshot.setMarketValue(metrics.marketValue());
        snapshot.setDailyPnl(metrics.dailyPnl());
        snapshot.setTotalPnl(metrics.holdingPnl());
        snapshot.setTotalPnlRate(metrics.holdingPnlRate());
        snapshot.setUpdatedAt(LocalDateTime.now(clock));
        return dailyProfitSnapshotRepository.save(snapshot);
    }

    /**
     * 按配置的批量大小分批写库，避免大列表一次性 flush 过重，也避免循环逐条 save。
     */
    private <T> void saveInBatches(List<T> entities, java.util.function.Function<List<T>, List<T>> saver) {
        if (entities.isEmpty()) {
            return;
        }
        int batchSize = Math.max(1, persistenceProperties.getBatchSize());
        for (int start = 0; start < entities.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entities.size());
            saver.apply(entities.subList(start, end));
        }
    }

    private UserFundDailyProfitSnapshotEntity toSnapshot(
        String userId,
        String fundCode,
        LocalDate tradeDate,
        HoldingComputationService.HoldingState state,
        double nav,
        HoldingComputationService.HoldingMetrics metrics
    ) {
        UserFundDailyProfitSnapshotEntity snapshot = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, tradeDate)
            .filter(existing -> existing.getTradeDate().equals(tradeDate))
            .orElseGet(UserFundDailyProfitSnapshotEntity::new);
        if (snapshot.getId() == null) {
            snapshot.setId(UUID.randomUUID().toString());
        }
        snapshot.setUserId(userId);
        snapshot.setFundCode(fundCode);
        snapshot.setTradeDate(tradeDate);
        snapshot.setShares(round(state.shares(), 4));
        snapshot.setAverageCost(round(state.averageCost(), 4));
        snapshot.setNav(round(nav, 4));
        snapshot.setMarketValue(metrics.marketValue());
        snapshot.setDailyPnl(metrics.dailyPnl());
        snapshot.setTotalPnl(metrics.holdingPnl());
        snapshot.setTotalPnlRate(metrics.holdingPnlRate());
        snapshot.setUpdatedAt(LocalDateTime.now(clock));
        return snapshot;
    }

    /**
     * 刷新当前用户的持仓缓存。
     * Redis 只缓存 overview / held-codes / insight 这些日中高频读取结果，任何一条操作或回算完成后
     * 都需要整体重算对应用户的缓存，避免缓存与日终快照脱节。
     */
    private void refreshCaches(String userId, String fundCode) {
        HoldingDtos.HoldingsOverviewResponse overview = computeOverview(userId);
        cacheService.putOverview(userId, overview);
        cacheService.putHeldCodes(
            userId,
            overview.items().stream().map(HoldingDtos.HoldingListItemResponse::fundCode).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        Optional<CurrentHoldingState> state = loadCurrentHoldingState(userId, fundCode);
        if (state.isPresent()) {
            cacheService.putInsight(userId, fundCode, computeInsight(userId, fundCode));
            refreshIntradayState(userId, state.get(), currentDate());
        } else {
            cacheService.evictInsight(userId, fundCode);
            cacheService.evictIntradayState(userId, fundCode, currentDate());
        }
    }

    /**
     * 从最新日快照推导当前持仓总览。
     * 这里故意不用单独的 holding_positions 表，而是把每只基金“最新一条日终快照”视为当前持仓基线，
     * 再叠加当前净值口径得到页面要展示的实时市值和盈亏。
     */
    private HoldingDtos.HoldingsOverviewResponse computeOverview(String userId) {
        List<CurrentHoldingState> holdings = loadCurrentHoldingStates(userId);
        Map<String, HoldingDisplayState> displayStates = holdings.stream()
            .collect(Collectors.toMap(CurrentHoldingState::fundCode, this::resolveDisplayState));
        double totalMarketValue = round(holdings.stream()
            .mapToDouble(state -> displayStates.getOrDefault(state.fundCode(), HoldingDisplayState.empty()).marketValue())
            .sum(), 2);
        List<HoldingDtos.HoldingListItemResponse> items = holdings.stream()
            .sorted(Comparator.comparingDouble((CurrentHoldingState state) ->
                displayStates.getOrDefault(state.fundCode(), HoldingDisplayState.empty()).marketValue()).reversed())
            .map(state -> {
                HoldingDisplayState displayState = displayStates.getOrDefault(state.fundCode(), HoldingDisplayState.empty());
                double marketValue = displayState.marketValue();
                double holdingPnl = displayState.holdingPnl();
                double allocation = totalMarketValue <= 0 ? 0 : round((marketValue / totalMarketValue) * 100, 4);
                return new HoldingDtos.HoldingListItemResponse(
                    state.fundCode(),
                    state.fundName(),
                    round(latestNav(state.fundCode()).getDayGrowth(), 4),
                    displayState.dailyPnl(),
                    marketValue,
                    holdingPnl,
                    allocation
                );
            })
            .toList();
        return new HoldingDtos.HoldingsOverviewResponse(totalMarketValue, items);
    }

    /**
     * 从最新日快照推导单基金洞察。
     * 洞察页的 amountHeld / todayPnl / oneYearReturn 等指标来自不同口径：
     * 份额与成本取最新日快照，日涨跌取最新确认净值，当前收益按当前净值或估值口径实时计算。
     */
    private HoldingDtos.HoldingInsightResponse computeInsight(String userId, String fundCode) {
        CurrentHoldingState holding = loadCurrentHoldingState(userId, fundCode)
            .orElseThrow(() -> new NotFoundException("Holding not found"));
        List<CurrentHoldingState> allHoldings = loadCurrentHoldingStates(userId);
        double totalMarketValue = allHoldings.stream()
            .mapToDouble(state -> resolveDisplayState(state).marketValue())
            .sum();
        NavHistoryEntity latestNav = latestNav(fundCode);
        HoldingDisplayState displayState = resolveDisplayState(holding);
        HoldingComputationService.HoldingMetrics metrics = computationService.metrics(
            new HoldingComputationService.HoldingState(holding.shares(), holding.averageCost()),
            displayState.currentNav(),
            displayState.previousNav(),
            totalMarketValue
        );
        List<UserFundDailyProfitSnapshotEntity> snapshots = dailyProfitSnapshotRepository.findByUserIdAndFundCodeOrderByTradeDateAsc(userId, fundCode);
        LocalDate firstTradeDate = snapshots.isEmpty() ? currentDate() : snapshots.get(0).getTradeDate();
        return new HoldingDtos.HoldingInsightResponse(
            fundCode,
            metrics.marketValue(),
            metrics.holdingPnl(),
            metrics.holdingPnlRate(),
            round(holding.shares(), 4),
            round(holding.averageCost(), 4),
            metrics.allocation(),
            round(latestNav.getDayGrowth(), 4),
            metrics.dailyPnl(),
            round(holding.shares() * (latestNav.getUnitNav() - displayState.previousNav()), 2),
            calculateOneYearReturn(fundCode),
            Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(firstTradeDate, LocalDate.now(clock)))
        );
    }

    /**
     * 按用户聚合最新日快照，得到当前仍持有的基金集合。
     */
    private List<CurrentHoldingState> loadCurrentHoldingStates(String userId) {
        List<UserFundDailyProfitSnapshotEntity> snapshots = dailyProfitSnapshotRepository.findByUserIdOrderByTradeDateDesc(userId);
        Map<String, UserFundDailyProfitSnapshotEntity> latestByFund = new LinkedHashMap<>();
        for (UserFundDailyProfitSnapshotEntity snapshot : snapshots) {
            latestByFund.putIfAbsent(snapshot.getFundCode(), snapshot);
        }
        List<String> fundCodes = latestByFund.values().stream()
            .filter(snapshot -> snapshot.getShares() > 0)
            .map(UserFundDailyProfitSnapshotEntity::getFundCode)
            .toList();
        if (fundCodes.isEmpty()) {
            return List.of();
        }
        Map<String, FundEntity> fundMap = fundRepository.findByCodeIn(fundCodes).stream()
            .collect(Collectors.toMap(FundEntity::getCode, fund -> fund));
        return latestByFund.values().stream()
            .filter(snapshot -> snapshot.getShares() > 0)
            .map(snapshot -> new CurrentHoldingState(
                snapshot.getUserId(),
                snapshot.getFundCode(),
                fundMap.getOrDefault(snapshot.getFundCode(), fallbackFund(snapshot.getFundCode())).getName(),
                snapshot.getShares(),
                snapshot.getAverageCost(),
                snapshot.getTradeDate()
            ))
            .toList();
    }

    /**
     * 读取单只基金的当前持仓状态。
     */
    private Optional<CurrentHoldingState> loadCurrentHoldingState(String userId, String fundCode) {
        return dailyProfitSnapshotRepository.findTopByUserIdAndFundCodeOrderByTradeDateDesc(userId, fundCode)
            .filter(snapshot -> snapshot.getShares() > 0)
            .map(snapshot -> new CurrentHoldingState(
                snapshot.getUserId(),
                snapshot.getFundCode(),
                requiredFund(snapshot.getFundCode()).getName(),
                snapshot.getShares(),
                snapshot.getAverageCost(),
                snapshot.getTradeDate()
            ));
    }

    /**
     * 将当前持仓解析成页面展示口径。
     * 若当天已经预热了 intraday 状态，就优先读取 Redis；否则按最新日快照 + 当前净值口径现算。
     */
    private HoldingDisplayState resolveDisplayState(CurrentHoldingState state) {
        LocalDate today = currentDate();
        return cacheService.getIntradayState(state.userId(), state.fundCode(), today)
            .map(intraday -> new HoldingDisplayState(
                intraday.currentNav(),
                navOnOrBefore(state.fundCode(), today.minusDays(1)),
                round(intraday.marketValue(), 2),
                round(intraday.dailyPnl(), 2),
                round(intraday.holdingPnl(), 2)
            ))
            .orElseGet(() -> {
                double currentNav = resolveCurrentNav(state.fundCode());
                double previousNav = navOnOrBefore(state.fundCode(), today.minusDays(1));
                return new HoldingDisplayState(
                    currentNav,
                    previousNav,
                    round(state.shares() * currentNav, 2),
                    round((currentNav - previousNav) * state.shares(), 2),
                    round((currentNav - state.averageCost()) * state.shares(), 2)
                );
            });
    }

    /**
     * 根据最新快照刷新某只基金当天的日间动态态。
     * 当前还未接入实时估值，所以 currentNav 初始与 baselineNav 相同；后续接入估值后只需要覆盖 currentNav。
     */
    private void refreshIntradayState(String userId, CurrentHoldingState state, LocalDate tradeDate) {
        UserFundDailyProfitSnapshotEntity snapshot = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, state.fundCode(), tradeDate)
            .orElse(null);
        if (snapshot == null) {
            cacheService.evictIntradayState(userId, state.fundCode(), tradeDate);
            return;
        }
        cacheService.putIntradayState(userId, state.fundCode(), tradeDate, buildIntradayState(userId, state, tradeDate, snapshot));
    }

    private HoldingCurrentStateCacheService.IntradayHoldingState buildIntradayState(
        String userId,
        CurrentHoldingState state,
        LocalDate tradeDate,
        UserFundDailyProfitSnapshotEntity baselineSnapshot
    ) {
        double baselineNav = round(baselineSnapshot.getNav(), 4);
        double currentNav = baselineNav;
        return new HoldingCurrentStateCacheService.IntradayHoldingState(
            userId,
            state.fundCode(),
            state.fundName(),
            tradeDate,
            baselineSnapshot.getTradeDate(),
            round(state.shares(), 4),
            round(state.averageCost(), 4),
            baselineNav,
            currentNav,
            round(state.shares() * currentNav, 2),
            0,
            round((currentNav - state.averageCost()) * state.shares(), 2),
            null,
            "SNAPSHOT"
        );
    }

    /**
     * 转换为操作响应。
     */
    private HoldingDtos.HoldingOperationResponse toOperationResponse(UserFundOperationRecordEntity entity) {
        return new HoldingDtos.HoldingOperationResponse(
            entity.getId(),
            entity.getFundCode(),
            entity.getOperation(),
            entity.getSource(),
            entity.getStatus(),
            entity.getTradeDate().toString(),
            round(entity.getAmount(), 2),
            round(entity.getSharesDelta(), 4),
            round(entity.getNav(), 4),
            round(entity.getFeeRate(), 6),
            round(entity.getFeeAmount(), 2)
        );
    }

    /**
     * 转换为订单响应。
     */
    private HoldingDtos.OperationRecordResponse toOrderResponse(UserFundOperationRecordEntity entity) {
        String fundName = fundRepository.findByCode(entity.getFundCode()).map(FundEntity::getName).orElse(entity.getFundCode());
        return new HoldingDtos.OperationRecordResponse(
            entity.getId(),
            entity.getFundCode(),
            fundName,
            entity.getOperation(),
            entity.getSource(),
            entity.getStatus(),
            entity.getTradeDate().toString(),
            round(entity.getAmount(), 2),
            round(Math.abs(entity.getSharesDelta()), 4),
            round(entity.getNav(), 4),
            round(entity.getFeeRate(), 6),
            round(entity.getFeeAmount(), 2)
        );
    }

    /**
     * 解析基准日期。
     */
    private LocalDate resolveBasisDate(String amountBasis) {
        LocalDate today = currentDate();
        if ("T_MINUS_1".equalsIgnoreCase(amountBasis)) {
            return today.minusDays(1);
        }
        if ("T".equalsIgnoreCase(amountBasis)) {
            return today;
        }
        throw new IllegalArgumentException("amountBasis must be T_MINUS_1 or T");
    }

    /**
     * 解析持仓基准净值。
     */
    private double resolveHoldingBasisNav(String fundCode, String amountBasis, LocalDate basisDate) {
        if ("T".equalsIgnoreCase(amountBasis)) {
            return resolveCurrentNav(fundCode);
        }
        return navOnOrBefore(fundCode, basisDate);
    }

    /**
     * 校验补记日期是否可接受。
     */
    private void validateTradeDate(LocalDate tradeDate) {
        LocalDate today = currentDate();
        if (tradeDate.isAfter(nextBusinessDate(today))) {
            throw new IllegalArgumentException("补记日期不能晚于下一交易日");
        }
        if (tradeDate.isBefore(today.minusDays(marketDataProperties.getTradeBackfillMaxDays()))) {
            throw new IllegalArgumentException("补记日期超出允许范围");
        }
    }

    /**
     * 判断待补记操作是否可以立即执行。
     */
    private boolean shouldExecuteImmediately(LocalDate tradeDate, boolean confirmedNavExists) {
        return tradeDate.isBefore(currentDate()) && confirmedNavExists;
    }

    /**
     * 计算下一交易日。
     */
    private LocalDate nextBusinessDate(LocalDate baseDate) {
        LocalDate next = baseDate.plusDays(1);
        while (next.getDayOfWeek().getValue() >= 6) {
            next = next.plusDays(1);
        }
        return next;
    }

    /**
     * 根据前置状态判断买入属于建仓还是加仓。
     */
    private String classifyBuyOperation(HoldingComputationService.HoldingState preTradeState) {
        return preTradeState.shares() <= 0 ? OPERATION_OPEN_POSITION : OPERATION_BUY;
    }

    /**
     * 根据卖出后的状态判断属于卖出还是清仓。
     */
    private String classifySellOperation(HoldingComputationService.HoldingState postTradeState) {
        return postTradeState.shares() <= 0 ? OPERATION_CLOSE_POSITION : OPERATION_SELL;
    }

    /**
     * 判断是否属于买入型操作。
     */
    private boolean isBuyLikeOperation(String operation) {
        return OPERATION_BUY.equals(operation)
            || OPERATION_OPEN_POSITION.equals(operation)
            || OPERATION_SIP_BUY.equals(operation);
    }

    /**
     * 判断是否属于卖出型操作。
     */
    private boolean isSellLikeOperation(String operation) {
        return OPERATION_SELL.equals(operation)
            || OPERATION_CLOSE_POSITION.equals(operation);
    }

    /**
     * 当前净值优先取估值，未开启时回落到最新确认净值。
     */
    private double resolveCurrentNav(String fundCode) {
        if (opsService.isEnabled("estimate_reference")) {
            FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fundCode)
                .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
            return estimate.getEstimatedNav();
        }
        return latestNav(fundCode).getUnitNav();
    }

    /**
     * 获取最新确认净值点。
     */
    private NavHistoryEntity latestNav(String fundCode) {
        return navHistoryRepository.findTopByFundCodeOrderByTradeDateDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund nav history not found"));
    }

    /**
     * 获取必需的基金元数据。
     */
    private FundEntity requiredFund(String fundCode) {
        return fundRepository.findByCode(fundCode).orElseThrow(() -> new NotFoundException("Fund not found"));
    }

    /**
     * 在 fund 元数据缺失时构造兜底名称，避免当前持仓列表直接中断。
     */
    private FundEntity fallbackFund(String fundCode) {
        FundEntity entity = new FundEntity();
        entity.setCode(fundCode);
        entity.setName(fundCode);
        entity.setTags("");
        entity.setTopHoldings("[]");
        entity.setCreatedAt(LocalDateTime.now(clock));
        return entity;
    }

    /**
     * 获取指定日期及之前最近一个确认净值。
     */
    public double navOnOrBefore(String fundCode, LocalDate tradeDate) {
        if (tradeDate == null) {
            return latestNav(fundCode).getUnitNav();
        }
        return navHistoryRepository.findTopByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(fundCode, tradeDate)
            .map(NavHistoryEntity::getUnitNav)
            .orElseGet(() -> latestNav(fundCode).getUnitNav());
    }

    /**
     * 计算近一年收益率。
     */
    private double calculateOneYearReturn(String fundCode) {
        List<NavHistoryEntity> history = navHistoryRepository.findByFundCodeOrderByTradeDateAsc(fundCode);
        if (history.size() < 2) {
            return 0;
        }
        double start = history.get(0).getUnitNav();
        double end = history.get(history.size() - 1).getUnitNav();
        if (start <= 0) {
            return 0;
        }
        return round(((end - start) / start) * 100, 4);
    }

    /**
     * 对数值按指定精度做舍入。
     */
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    /**
     * 当前持仓聚合视图。
     */
    private record CurrentHoldingState(
        String userId,
        String fundCode,
        String fundName,
        double shares,
        double averageCost,
        LocalDate snapshotDate
    ) {
    }

    /**
     * 页面展示时使用的当前口径。
     */
    private record HoldingDisplayState(
        double currentNav,
        double previousNav,
        double marketValue,
        double dailyPnl,
        double holdingPnl
    ) {
        private static HoldingDisplayState empty() {
            return new HoldingDisplayState(0, 0, 0, 0, 0);
        }
    }
}
