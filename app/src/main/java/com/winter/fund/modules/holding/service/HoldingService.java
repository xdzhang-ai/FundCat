package com.winter.fund.modules.holding.service;

/**
 * 持仓模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
import com.winter.fund.modules.fund.model.FundSnapshotEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.FundSnapshotRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.model.UserFundDailyProfitSnapshotEntity;
import com.winter.fund.modules.holding.model.UserFundHoldingEntity;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundDailyProfitSnapshotRepository;
import com.winter.fund.modules.holding.repository.UserFundHoldingRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);
    private static final String STATUS_EXECUTED = "已执行";
    private static final String OPERATION_BUY = "BUY";
    private static final String OPERATION_SELL = "SELL";
    private static final String OPERATION_OPEN_POSITION = "OPEN_POSITION";
    private static final String OPERATION_CLOSE_POSITION = "CLOSE_POSITION";
    private static final String OPERATION_SIP_BUY = "SIP_BUY";
    private final UserFundHoldingRepository userFundHoldingRepository;
    private final UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository;
    private final UserFundOperationRecordRepository operationRecordRepository;
    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final HoldingComputationService computationService;
    private final MarketDataProperties marketDataProperties;
    private final OpsService opsService;
    private final Clock clock;

    public HoldingService(
        UserFundHoldingRepository userFundHoldingRepository,
        UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository,
        UserFundOperationRecordRepository operationRecordRepository,
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        HoldingComputationService computationService,
        MarketDataProperties marketDataProperties,
        OpsService opsService
    ) {
        this.userFundHoldingRepository = userFundHoldingRepository;
        this.dailyProfitSnapshotRepository = dailyProfitSnapshotRepository;
        this.operationRecordRepository = operationRecordRepository;
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.computationService = computationService;
        this.marketDataProperties = marketDataProperties;
        this.opsService = opsService;
        this.clock = Clock.system(ZoneId.of(marketDataProperties.getTimezone()));
    }

    public List<UserFundHoldingEntity> getCurrentHoldings(String userId) {
        return userFundHoldingRepository.findByUserIdOrderByMarketValueDesc(userId);
    }

    public List<HoldingDtos.HoldingOperationResponse> getRecentOperations(String userId) {
        return operationRecordRepository.findTop12ByUserIdAndStatusOrderByCreatedAtDesc(userId, STATUS_EXECUTED).stream()
            .map(this::toOperationResponse)
            .toList();
    }

    public HoldingDtos.HoldingsOverviewResponse getOverview(CurrentUser currentUser) {
        List<UserFundHoldingEntity> holdings = getCurrentHoldings(currentUser.id());
        double totalMarketValue = round(holdings.stream().mapToDouble(UserFundHoldingEntity::getMarketValue).sum(), 2);
        List<HoldingDtos.HoldingListItemResponse> items = holdings.stream()
            .map(holding -> {
                FundSnapshotEntity snapshot = latestSnapshot(holding.getFundCode());
                double allocation = totalMarketValue <= 0 ? 0 : round((holding.getMarketValue() / totalMarketValue) * 100, 4);
                double todayPnl = round(resolveCurrentNav(holding.getFundCode()) - snapshot.getUnitNav(), 4) * holding.getShares();
                return new HoldingDtos.HoldingListItemResponse(
                    holding.getFundCode(),
                    holding.getFundName(),
                    round(snapshot.getDayGrowth(), 4),
                    round(todayPnl, 2),
                    round(holding.getMarketValue(), 2),
                    round(holding.getHoldingPnl(), 2),
                    allocation
                );
            })
            .toList();
        return new HoldingDtos.HoldingsOverviewResponse(totalMarketValue, items);
    }

    public HoldingDtos.HoldingInsightResponse getInsight(CurrentUser currentUser, String fundCode) {
        UserFundHoldingEntity holding = userFundHoldingRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
            .orElseThrow(() -> new NotFoundException("Holding not found"));
        List<UserFundHoldingEntity> allHoldings = getCurrentHoldings(currentUser.id());
        double totalMarketValue = allHoldings.stream().mapToDouble(UserFundHoldingEntity::getMarketValue).sum();
        FundSnapshotEntity snapshot = latestSnapshot(fundCode);
        double currentNav = resolveCurrentNav(fundCode);
        NavHistoryEntity previousNav = navHistoryRepository.findTopByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
            fundCode,
            LocalDate.now(clock).minusDays(1)
        ).orElseThrow(() -> new NotFoundException("Previous nav not found"));
        HoldingComputationService.HoldingMetrics metrics = computationService.metrics(
            new HoldingComputationService.HoldingState(holding.getShares(), holding.getAverageCost()),
            currentNav,
            previousNav.getUnitNav(),
            totalMarketValue
        );
        UserFundDailyProfitSnapshotEntity firstSnapshot = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(currentUser.id(), fundCode, LocalDate.now(clock))
            .orElseThrow(() -> new NotFoundException("Holding snapshot not found"));
        return new HoldingDtos.HoldingInsightResponse(
            fundCode,
            metrics.marketValue(),
            metrics.holdingPnl(),
            metrics.holdingPnlRate(),
            round(holding.getShares(), 4),
            round(holding.getAverageCost(), 4),
            metrics.allocation(),
            round(snapshot.getDayGrowth(), 4),
            metrics.dailyPnl(),
            round(holding.getShares() * (snapshot.getUnitNav() - previousNav.getUnitNav()), 2),
            calculateOneYearReturn(fundCode),
            Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(firstSnapshot.getTradeDate(), LocalDate.now(clock)))
        );
    }

    @Transactional
    public UserFundHoldingEntity createHolding(CurrentUser currentUser, HoldingDtos.UpsertHoldingRequest request) {
        return upsertHolding(currentUser, request.fundCode(), request);
    }

    @Transactional
    public UserFundHoldingEntity upsertHolding(CurrentUser currentUser, String fundCode, HoldingDtos.UpsertHoldingRequest request) {
        FundEntity fund = requiredFund(fundCode);
        LocalDate basisDate = resolveBasisDate(request.amountBasis());
        double basisNav = navOnOrBefore(fundCode, basisDate);
        log.info("Upserting holding snapshot, userId={}, fundCode={}, amountBasis={}, basisDate={}, basisNav={}",
            currentUser.id(), fundCode, request.amountBasis(), basisDate, basisNav);
        HoldingComputationService.HoldingState state = computationService.rebuildFromAmount(request.amount(), request.holdingPnl(), basisNav);
        UserFundHoldingEntity entity = userFundHoldingRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
            .orElseGet(() -> newHolding(currentUser.id(), fund));
        applyMetrics(entity, state, resolveCurrentNav(fundCode), navOnOrBefore(fundCode, LocalDate.now(clock).minusDays(1)), totalMarketValueExcluding(currentUser.id(), fundCode));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        userFundHoldingRepository.save(entity);
        upsertDailySnapshot(currentUser.id(), fundCode, LocalDate.now(clock), state, resolveCurrentNav(fundCode));
        log.info("Holding upserted, userId={}, fundCode={}, shares={}, averageCost={}, marketValue={}",
            currentUser.id(), fundCode, entity.getShares(), entity.getAverageCost(), entity.getMarketValue());
        return entity;
    }

    @Transactional
    public HoldingDtos.HoldingOperationResponse createManualOperation(CurrentUser currentUser, HoldingDtos.CreateHoldingOperationRequest request) {
        FundEntity fund = requiredFund(request.fundCode());
        LocalDate requestedTradeDate = LocalDate.parse(request.tradeDate());
        validateTradeDate(requestedTradeDate);
        double tradeNav = navOnOrBefore(fund.getCode(), requestedTradeDate);
        UserFundOperationRecordEntity entity = buildOperationRecord(currentUser.id(), fund, request, tradeNav, requestedTradeDate);
        operationRecordRepository.save(entity);
        log.info("Manual operation persisted, operationId={}, userId={}, fundCode={}, operation={}, tradeDate={}, amount={}, sharesDelta={}",
            entity.getId(), currentUser.id(), fund.getCode(), entity.getOperation(), entity.getTradeDate(), entity.getAmount(), entity.getSharesDelta());
        if (entity.getTradeDate().equals(currentDate())) {
            refreshCurrentDaySnapshot(currentUser.id(), fund.getCode(), entity);
        } else {
            rebuildSnapshotsFrom(currentUser.id(), fund.getCode(), entity.getTradeDate());
        }
        log.info("Holding operation recorded, userId={}, fundCode={}, requestOperation={}, effectiveOperation={}, tradeDate={}, status={}",
            currentUser.id(), fund.getCode(), request.operation(), entity.getOperation(), entity.getTradeDate(), entity.getStatus());
        return toOperationResponse(entity);
    }

    @Transactional
    public void rebuildSnapshotsFrom(String userId, String fundCode, LocalDate tradeDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate baselineDate = tradeDate.minusDays(1);
        log.info("Rebuilding holding snapshots, userId={}, fundCode={}, fromTradeDate={}, toTradeDate={}",
            userId, fundCode, tradeDate, today);
        // Historical backfill always starts from the latest persisted pre-trade snapshot.
        // We intentionally avoid reversing from pnl snapshots because shares/cost are the true baseline.
        HoldingComputationService.HoldingState state = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, baselineDate)
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(new HoldingComputationService.HoldingState(0, 0));

        List<UserFundOperationRecordEntity> operations = operationRecordRepository
            .findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(userId, fundCode, "已执行", tradeDate, today);
        log.info("Loaded executed operations for rebuild, userId={}, fundCode={}, operationCount={}",
            userId, fundCode, operations.size());
        Map<LocalDate, List<UserFundOperationRecordEntity>> operationsByDate = operations.stream()
            .collect(Collectors.groupingBy(UserFundOperationRecordEntity::getTradeDate));

        List<UserFundDailyProfitSnapshotEntity> snapshots = new ArrayList<>();
        for (LocalDate cursor = tradeDate; !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            for (UserFundOperationRecordEntity operation : operationsByDate.getOrDefault(cursor, List.of())) {
                state = applyOperation(state, operation);
            }
            double nav = navOnOrBefore(fundCode, cursor);
            double previousNav = navOnOrBefore(fundCode, cursor.minusDays(1));
            HoldingComputationService.HoldingMetrics metrics = computationService.metrics(state, nav, previousNav, nav * state.shares());
            snapshots.add(toSnapshot(userId, fundCode, cursor, state, nav, metrics));
        }

        dailyProfitSnapshotRepository.saveAll(snapshots);
        snapshots.stream().max(Comparator.comparing(UserFundDailyProfitSnapshotEntity::getTradeDate)).ifPresent(latest -> {
            FundEntity fund = requiredFund(fundCode);
            UserFundHoldingEntity holding = userFundHoldingRepository.findByUserIdAndFundCode(userId, fundCode)
                .orElseGet(() -> newHolding(userId, fund));
            holding.setShares(latest.getShares());
            holding.setAverageCost(latest.getAverageCost());
            holding.setMarketValue(latest.getMarketValue());
            holding.setHoldingPnl(latest.getTotalPnl());
            holding.setHoldingPnlRate(latest.getTotalPnlRate());
            holding.setUpdatedAt(LocalDateTime.now(clock));
            userFundHoldingRepository.save(holding);
            log.info("Current holding refreshed from snapshots, userId={}, fundCode={}, latestTradeDate={}, shares={}, marketValue={}",
                userId, fundCode, latest.getTradeDate(), latest.getShares(), latest.getMarketValue());
        });
        log.info("Holding snapshots rebuilt, userId={}, fundCode={}, snapshotCount={}", userId, fundCode, snapshots.size());
    }

    private UserFundOperationRecordEntity buildOperationRecord(
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
            HoldingComputationService.SellResult result = computationService.applySell(
                state,
                Math.abs(operation.getSharesDelta()),
                operation.getNav(),
                operation.getFeeRate()
            );
            return result.state();
        }
        return state;
    }

    private HoldingComputationService.HoldingState resolveStateBeforeOperation(String userId, String fundCode, LocalDate tradeDate) {
        HoldingComputationService.HoldingState sameDayState = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, tradeDate)
            .filter(snapshot -> snapshot.getTradeDate().equals(tradeDate))
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(null);
        if (sameDayState != null) {
            log.info("Resolved pre-trade state from same-day snapshot, userId={}, fundCode={}, tradeDate={}, shares={}, averageCost={}",
                userId, fundCode, tradeDate, sameDayState.shares(), sameDayState.averageCost());
            return sameDayState;
        }

        LocalDate baselineDate = tradeDate.minusDays(1);
        HoldingComputationService.HoldingState state = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, baselineDate)
            .map(snapshot -> new HoldingComputationService.HoldingState(snapshot.getShares(), snapshot.getAverageCost()))
            .orElse(new HoldingComputationService.HoldingState(0, 0));
        List<UserFundOperationRecordEntity> sameDayOperations = operationRecordRepository
            .findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(userId, fundCode, STATUS_EXECUTED, tradeDate, tradeDate);
        if (state.shares() <= 0) {
            LocalDate today = currentDate();
            List<UserFundOperationRecordEntity> futureOperations = tradeDate.isBefore(today)
                ? operationRecordRepository.findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
                    userId,
                    fundCode,
                    STATUS_EXECUTED,
                    tradeDate.plusDays(1),
                    today
                )
                : List.of();
            if (futureOperations.isEmpty()) {
                UserFundHoldingEntity currentHolding = userFundHoldingRepository.findByUserIdAndFundCode(userId, fundCode).orElse(null);
                if (currentHolding != null && currentHolding.getShares() > 0) {
                    HoldingComputationService.HoldingState fallbackState = new HoldingComputationService.HoldingState(
                        currentHolding.getShares(),
                        currentHolding.getAverageCost()
                    );
                    log.info("Resolved pre-trade state from current holding fallback, userId={}, fundCode={}, tradeDate={}, shares={}, averageCost={}",
                        userId, fundCode, tradeDate, fallbackState.shares(), fallbackState.averageCost());
                    return fallbackState;
                }
            } else {
                log.warn("Missing baseline snapshot before trade date and future operations exist, userId={}, fundCode={}, tradeDate={}, futureOperationCount={}",
                    userId, fundCode, tradeDate, futureOperations.size());
            }
        }
        for (UserFundOperationRecordEntity operation : sameDayOperations) {
            state = applyOperation(state, operation);
        }
        log.info("Resolved pre-trade state from baseline and same-day operations, userId={}, fundCode={}, tradeDate={}, baselineDate={}, sameDayOperationCount={}, shares={}, averageCost={}",
            userId, fundCode, tradeDate, baselineDate, sameDayOperations.size(), state.shares(), state.averageCost());
        return state;
    }

    private String classifyBuyOperation(HoldingComputationService.HoldingState preTradeState) {
        return preTradeState.shares() <= 0 ? OPERATION_OPEN_POSITION : OPERATION_BUY;
    }

    private String classifySellOperation(HoldingComputationService.HoldingState postTradeState) {
        return postTradeState.shares() <= 0 ? OPERATION_CLOSE_POSITION : OPERATION_SELL;
    }

    private boolean isBuyLikeOperation(String operation) {
        return OPERATION_BUY.equals(operation)
            || OPERATION_OPEN_POSITION.equals(operation)
            || OPERATION_SIP_BUY.equals(operation);
    }

    private boolean isSellLikeOperation(String operation) {
        return OPERATION_SELL.equals(operation)
            || OPERATION_CLOSE_POSITION.equals(operation);
    }

    private void upsertDailySnapshot(String userId, String fundCode, LocalDate tradeDate, HoldingComputationService.HoldingState state, double nav) {
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
        snapshot.setShares(state.shares());
        snapshot.setAverageCost(state.averageCost());
        snapshot.setNav(round(nav, 4));
        snapshot.setMarketValue(metrics.marketValue());
        snapshot.setDailyPnl(metrics.dailyPnl());
        snapshot.setTotalPnl(metrics.holdingPnl());
        snapshot.setTotalPnlRate(metrics.holdingPnlRate());
        snapshot.setUpdatedAt(LocalDateTime.now(clock));
        dailyProfitSnapshotRepository.save(snapshot);
        log.info("Daily profit snapshot upserted, userId={}, fundCode={}, tradeDate={}, marketValue={}, totalPnl={}",
            userId, fundCode, tradeDate, snapshot.getMarketValue(), snapshot.getTotalPnl());
    }

    private void refreshCurrentDaySnapshot(String userId, String fundCode, UserFundOperationRecordEntity operation) {
        HoldingComputationService.HoldingState preTradeState = resolveStateBeforeOperation(userId, fundCode, operation.getTradeDate());
        HoldingComputationService.HoldingState postTradeState = applyOperation(preTradeState, operation);
        double nav = navOnOrBefore(fundCode, operation.getTradeDate());
        upsertDailySnapshot(userId, fundCode, operation.getTradeDate(), postTradeState, nav);
        UserFundDailyProfitSnapshotEntity latest = dailyProfitSnapshotRepository
            .findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(userId, fundCode, operation.getTradeDate())
            .filter(snapshot -> snapshot.getTradeDate().equals(operation.getTradeDate()))
            .orElseThrow(() -> new NotFoundException("Holding snapshot not found"));
        FundEntity fund = requiredFund(fundCode);
        UserFundHoldingEntity holding = userFundHoldingRepository.findByUserIdAndFundCode(userId, fundCode)
            .orElseGet(() -> newHolding(userId, fund));
        holding.setShares(latest.getShares());
        holding.setAverageCost(latest.getAverageCost());
        holding.setMarketValue(latest.getMarketValue());
        holding.setHoldingPnl(latest.getTotalPnl());
        holding.setHoldingPnlRate(latest.getTotalPnlRate());
        holding.setUpdatedAt(LocalDateTime.now(clock));
        userFundHoldingRepository.save(holding);
        log.info("Current day holding refreshed directly from operation, userId={}, fundCode={}, tradeDate={}, shares={}, marketValue={}",
            userId, fundCode, operation.getTradeDate(), holding.getShares(), holding.getMarketValue());
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

    private void validateTradeDate(LocalDate tradeDate) {
        LocalDate today = currentDate();
        if (tradeDate.isAfter(today)) {
            throw new IllegalArgumentException("补记日期不能晚于今天");
        }
        if (tradeDate.isBefore(today.minusDays(marketDataProperties.getTradeBackfillMaxDays()))) {
            throw new IllegalArgumentException("补记日期超出允许范围");
        }
    }

    private UserFundHoldingEntity newHolding(String userId, FundEntity fund) {
        UserFundHoldingEntity entity = new UserFundHoldingEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fund.getCode());
        entity.setFundName(fund.getName());
        return entity;
    }

    private void applyMetrics(
        UserFundHoldingEntity entity,
        HoldingComputationService.HoldingState state,
        double nav,
        double previousNav,
        double otherMarketValue
    ) {
        HoldingComputationService.HoldingMetrics metrics = computationService.metrics(
            state,
            nav,
            previousNav,
            otherMarketValue + (state.shares() * nav)
        );
        entity.setShares(round(state.shares(), 4));
        entity.setAverageCost(round(state.averageCost(), 4));
        entity.setMarketValue(metrics.marketValue());
        entity.setHoldingPnl(metrics.holdingPnl());
        entity.setHoldingPnlRate(metrics.holdingPnlRate());
    }

    private double totalMarketValueExcluding(String userId, String fundCode) {
        return getCurrentHoldings(userId).stream()
            .filter(holding -> !holding.getFundCode().equals(fundCode))
            .mapToDouble(UserFundHoldingEntity::getMarketValue)
            .sum();
    }

    private double resolveCurrentNav(String fundCode) {
        if (opsService.isEnabled("estimate_reference")) {
            FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fundCode)
                .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
            return estimate.getEstimatedNav();
        }
        return latestSnapshot(fundCode).getUnitNav();
    }

    private FundSnapshotEntity latestSnapshot(String fundCode) {
        return fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
    }

    private FundEntity requiredFund(String fundCode) {
        return fundRepository.findByCode(fundCode).orElseThrow(() -> new NotFoundException("Fund not found"));
    }

    public double navOnOrBefore(String fundCode, LocalDate tradeDate) {
        if (tradeDate == null) {
            return latestSnapshot(fundCode).getUnitNav();
        }
        return navHistoryRepository.findTopByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(fundCode, tradeDate)
            .map(NavHistoryEntity::getUnitNav)
            .orElseGet(() -> latestSnapshot(fundCode).getUnitNav());
    }

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

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    LocalDate currentDate() {
        return LocalDate.now(clock);
    }
}
