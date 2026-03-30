package com.winter.fund.modules.portfolio.service;

/**
 * 组合与交易模块服务，负责封装该模块的核心业务逻辑。
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
import com.winter.fund.modules.holding.model.UserFundHoldingEntity;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundHoldingRepository;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import com.winter.fund.modules.holding.service.HoldingComputationService;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.portfolio.model.AlertRuleEntity;
import com.winter.fund.modules.portfolio.model.ImportJobEntity;
import com.winter.fund.modules.portfolio.model.PortfolioDtos;
import com.winter.fund.modules.portfolio.model.PortfolioEntity;
import com.winter.fund.modules.portfolio.model.SipPlanEntity;
import com.winter.fund.modules.portfolio.model.WatchlistEntity;
import com.winter.fund.modules.portfolio.model.WatchlistGroupEntity;
import com.winter.fund.modules.portfolio.model.WeeklyReportEntity;
import com.winter.fund.modules.portfolio.repository.AlertRuleRepository;
import com.winter.fund.modules.portfolio.repository.ImportJobRepository;
import com.winter.fund.modules.portfolio.repository.PortfolioRepository;
import com.winter.fund.modules.portfolio.repository.SipPlanRepository;
import com.winter.fund.modules.portfolio.repository.WatchlistGroupRepository;
import com.winter.fund.modules.portfolio.repository.WatchlistRepository;
import com.winter.fund.modules.portfolio.repository.WeeklyReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);
    private static final String SIP_STATUS_ACTIVE = "生效";
    private static final String SIP_STATUS_PAUSED = "暂停";
    private static final String SIP_STATUS_STOPPED = "停止";
    private static final String RECORD_STATUS_PENDING = "确认中";
    private static final String RECORD_STATUS_EXECUTED = "已执行";

    private final WatchlistRepository watchlistRepository;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final PortfolioRepository portfolioRepository;
    private final SipPlanRepository sipPlanRepository;
    private final ImportJobRepository importJobRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final UserFundHoldingRepository userFundHoldingRepository;
    private final UserFundOperationRecordRepository operationRecordRepository;
    private final HoldingComputationService holdingComputationService;
    private final HoldingService holdingService;
    private final OpsService opsService;
    private final Clock clock;

    public PortfolioService(
        WatchlistRepository watchlistRepository,
        WatchlistGroupRepository watchlistGroupRepository,
        PortfolioRepository portfolioRepository,
        SipPlanRepository sipPlanRepository,
        ImportJobRepository importJobRepository,
        WeeklyReportRepository weeklyReportRepository,
        AlertRuleRepository alertRuleRepository,
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        UserFundHoldingRepository userFundHoldingRepository,
        UserFundOperationRecordRepository operationRecordRepository,
        HoldingComputationService holdingComputationService,
        HoldingService holdingService,
        OpsService opsService,
        MarketDataProperties marketDataProperties
    ) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistGroupRepository = watchlistGroupRepository;
        this.portfolioRepository = portfolioRepository;
        this.sipPlanRepository = sipPlanRepository;
        this.importJobRepository = importJobRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.userFundHoldingRepository = userFundHoldingRepository;
        this.operationRecordRepository = operationRecordRepository;
        this.holdingComputationService = holdingComputationService;
        this.holdingService = holdingService;
        this.opsService = opsService;
        this.clock = Clock.system(ZoneId.of(marketDataProperties.getTimezone()));
    }

    public List<PortfolioDtos.WatchlistItemResponse> getWatchlist(CurrentUser currentUser) {
        List<WatchlistEntity> items = watchlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.id());
        Map<String, FundEntity> funds = fundRepository.findByCodeIn(items.stream().map(WatchlistEntity::getFundCode).toList()).stream()
            .collect(Collectors.toMap(FundEntity::getCode, Function.identity()));
        Map<String, FundSnapshotEntity> snapshots = items.stream()
            .map(WatchlistEntity::getFundCode)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), this::latestSnapshot));
        Map<String, FundEstimateEntity> estimates = items.stream()
            .map(WatchlistEntity::getFundCode)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), this::latestEstimate));
        Set<String> heldCodes = userFundHoldingRepository.findByUserIdOrderByMarketValueDesc(currentUser.id()).stream()
            .map(UserFundHoldingEntity::getFundCode)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<String>> groupsByWatchlist = groupsByWatchlistId(items);
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");

        return items.stream()
            .map(item -> {
                FundEntity fund = Optional.ofNullable(funds.get(item.getFundCode()))
                    .orElseThrow(() -> new NotFoundException("Fund not found"));
                FundSnapshotEntity snapshot = snapshots.get(item.getFundCode());
                FundEstimateEntity estimate = estimates.get(item.getFundCode());
                return new PortfolioDtos.WatchlistItemResponse(
                    fund.getCode(),
                    fund.getName(),
                    item.getNote(),
                    round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth(), 4),
                    round(snapshot.getUnitNav(), 4),
                    round(estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav(), 4),
                    groupsByWatchlist.getOrDefault(item.getId(), List.of()),
                    heldCodes.contains(item.getFundCode())
                );
            })
            .toList();
    }

    @Transactional
    public PortfolioDtos.WatchlistItemResponse addWatchlist(CurrentUser currentUser, PortfolioDtos.CreateWatchlistRequest request) {
        log.info("Adding watchlist item, userId={}, fundCode={}, groups={}", currentUser.id(), request.fundCode(), request.groups());
        watchlistRepository.findByUserIdAndFundCode(currentUser.id(), request.fundCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Fund already exists in watchlist");
        });
        FundEntity fund = requiredFund(request.fundCode());
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.id());
        entity.setFundCode(request.fundCode());
        entity.setNote(request.note());
        entity.setCreatedAt(LocalDateTime.now(clock));
        watchlistRepository.save(entity);
        replaceWatchlistGroups(entity.getId(), request.groups());
        return getWatchlist(currentUser).stream()
            .filter(item -> item.code().equals(request.fundCode()))
            .findFirst()
            .orElseGet(() -> toWatchlistItem(currentUser.id(), entity, fund));
    }

    @Transactional
    public List<PortfolioDtos.WatchlistItemResponse> updateWatchlistGroups(CurrentUser currentUser, PortfolioDtos.UpdateWatchlistGroupsRequest request) {
        List<WatchlistEntity> watchlists = request.fundCodes().stream()
            .map(fundCode -> watchlistRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
                .orElseThrow(() -> new NotFoundException("Watchlist item not found")))
            .toList();
        List<String> normalizedGroups = normalizeGroups(request.groups());
        Map<String, List<String>> currentGroupsByWatchlist = groupsByWatchlistId(watchlists);
        List<String> watchlistIdsToUpdate = watchlists.stream()
            .map(WatchlistEntity::getId)
            .filter(watchlistId -> !hasSameGroups(currentGroupsByWatchlist.getOrDefault(watchlistId, List.of()), normalizedGroups))
            .toList();
        log.info("Replacing watchlist groups, userId={}, fundCodes={}, groups={}",
            currentUser.id(), request.fundCodes(), request.groups());
        if (watchlistIdsToUpdate.isEmpty()) {
            log.info("Skipping watchlist group replace because groups are unchanged, userId={}, fundCodes={}, groups={}",
                currentUser.id(), request.fundCodes(), normalizedGroups);
            return getWatchlist(currentUser);
        }
        replaceWatchlistGroups(watchlistIdsToUpdate, normalizedGroups);
        return getWatchlist(currentUser);
    }

    @Transactional
    public void removeWatchlist(CurrentUser currentUser, String fundCode) {
        log.info("Removing watchlist item, userId={}, fundCode={}", currentUser.id(), fundCode);
        WatchlistEntity entity = watchlistRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
            .orElseThrow(() -> new NotFoundException("Watchlist item not found"));
        watchlistGroupRepository.deleteByWatchlistId(entity.getId());
        watchlistRepository.delete(entity);
    }

    public List<PortfolioDtos.PortfolioSummaryResponse> getPortfolios(CurrentUser currentUser) {
        List<UserFundHoldingEntity> holdings = userFundHoldingRepository.findByUserIdOrderByMarketValueDesc(currentUser.id());
        if (holdings.isEmpty()) {
            return List.of();
        }
        double totalMarketValue = holdings.stream().mapToDouble(UserFundHoldingEntity::getMarketValue).sum();
        double totalPnl = holdings.stream().mapToDouble(UserFundHoldingEntity::getHoldingPnl).sum();
        List<PortfolioDtos.HoldingLotResponse> lots = holdings.stream()
            .map(holding -> new PortfolioDtos.HoldingLotResponse(
                holding.getId(),
                holding.getFundCode(),
                holding.getFundName(),
                round(holding.getShares(), 4),
                round(holding.getAverageCost(), 4),
                round(holding.getMarketValue(), 2),
                round(holding.getHoldingPnl(), 2),
                totalMarketValue <= 0 ? 0 : round((holding.getMarketValue() / totalMarketValue) * 100, 4),
                "UNIFIED",
                holding.getUpdatedAt().toString()
            ))
            .toList();
        return List.of(new PortfolioDtos.PortfolioSummaryResponse(
            "portfolio-current",
            "当前持仓",
            "FundCat",
            "CNY",
            round(totalMarketValue, 2),
            round(totalPnl, 2),
            0,
            lots
        ));
    }

    public List<PortfolioDtos.OperationRecordResponse> getOrders(CurrentUser currentUser) {
        return getOrders(currentUser, "recent");
    }

    public List<PortfolioDtos.OperationRecordResponse> getOrders(CurrentUser currentUser, String scope) {
        List<UserFundOperationRecordEntity> records = operationRecordRepository
            .findTop12ByUserIdAndStatusOrderByCreatedAtDesc(currentUser.id(), RECORD_STATUS_EXECUTED);
        log.info("Loading recent operation records, userId={}, scope={}, count={}", currentUser.id(), scope, records.size());
        return records.stream().map(this::toOperationResponse).toList();
    }

    public List<PortfolioDtos.SipPlanResponse> getSipPlans(CurrentUser currentUser) {
        return sipPlanRepository.findByUserIdOrderByNextRunAtAsc(currentUser.id()).stream()
            .map(this::toSipPlanResponse)
            .toList();
    }

    public PortfolioDtos.SipPlanResponse getSipPlan(CurrentUser currentUser, String sipPlanId) {
        return toSipPlanResponse(requiredSipPlan(currentUser.id(), sipPlanId));
    }

    public List<PortfolioDtos.SipExecutionRecordResponse> getSipPlanRecords(CurrentUser currentUser, String sipPlanId) {
        SipPlanEntity plan = requiredSipPlan(currentUser.id(), sipPlanId);
        return operationRecordRepository.findBySipPlanIdOrderByTradeDateDescCreatedAtDesc(plan.getId()).stream()
            .map(this::toSipExecutionRecord)
            .toList();
    }

    public List<PortfolioDtos.SipPlanDigestResponse> getSipDigests(CurrentUser currentUser) {
        return getSipPlans(currentUser).stream()
            .map(plan -> new PortfolioDtos.SipPlanDigestResponse(
                plan.id(),
                plan.fundCode(),
                plan.fundName(),
                plan.amount(),
                cadenceLabel(plan.cadence()),
                plan.nextRunAt().substring(0, 10),
                plan.status()
            ))
            .toList();
    }

    @Transactional
    public PortfolioDtos.SipPlanResponse createSipPlan(CurrentUser currentUser, PortfolioDtos.CreateSipPlanRequest request) {
        log.info("Creating sip plan, userId={}, fundCode={}, cadence={}", currentUser.id(), request.fundCode(), request.cadence());
        requiredFund(request.fundCode());
        sipPlanRepository.findByUserIdAndFundCodeAndStatusNot(currentUser.id(), request.fundCode(), SIP_STATUS_STOPPED).ifPresent(existing -> {
            throw new IllegalArgumentException("Fund already has an active sip plan");
        });
        SipPlanEntity entity = new SipPlanEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(resolveLegacyPortfolioId(currentUser.id(), request.portfolioId()));
        entity.setUserId(currentUser.id());
        entity.setFundCode(request.fundCode());
        entity.setFundName(requiredFund(request.fundCode()).getName());
        entity.setAmount(request.amount());
        entity.setCadence(request.cadence());
        entity.setNextRunAt(LocalDateTime.parse(request.nextRunAt()));
        entity.setActive(true);
        entity.setStatus(SIP_STATUS_ACTIVE);
        entity.setFeeRate(round(request.feeRate(), 6));
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        sipPlanRepository.save(entity);
        log.info("Sip plan persisted, userId={}, sipPlanId={}, nextRunAt={}, feeRate={}",
            currentUser.id(), entity.getId(), entity.getNextRunAt(), entity.getFeeRate());
        return toSipPlanResponse(entity);
    }

    @Transactional
    public PortfolioDtos.SipPlanResponse updateSipPlan(CurrentUser currentUser, String sipPlanId, PortfolioDtos.UpdateSipPlanRequest request) {
        SipPlanEntity entity = requiredSipPlan(currentUser.id(), sipPlanId);
        if (SIP_STATUS_STOPPED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Stopped sip plan cannot be modified");
        }
        entity.setAmount(request.amount());
        entity.setCadence(request.cadence());
        entity.setFeeRate(round(request.feeRate(), 6));
        entity.setNextRunAt(computeNextRunAt(LocalDateTime.now(clock), request.cadence(), request.weekday(), request.monthDay()));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        sipPlanRepository.save(entity);
        log.info("Sip plan updated, userId={}, sipPlanId={}, nextRunAt={}, feeRate={}",
            currentUser.id(), sipPlanId, entity.getNextRunAt(), entity.getFeeRate());
        return toSipPlanResponse(entity);
    }

    @Transactional
    public PortfolioDtos.SipPlanResponse pauseSipPlan(CurrentUser currentUser, String sipPlanId) {
        return updateSipStatus(currentUser.id(), sipPlanId, SIP_STATUS_PAUSED, false);
    }

    @Transactional
    public PortfolioDtos.SipPlanResponse resumeSipPlan(CurrentUser currentUser, String sipPlanId) {
        SipPlanEntity entity = requiredSipPlan(currentUser.id(), sipPlanId);
        if (SIP_STATUS_STOPPED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Stopped sip plan cannot be resumed");
        }
        entity.setStatus(SIP_STATUS_ACTIVE);
        entity.setActive(true);
        entity.setNextRunAt(computeNextRunAt(LocalDateTime.now(clock), entity.getCadence(), null, null));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        sipPlanRepository.save(entity);
        log.info("Sip plan resumed, userId={}, sipPlanId={}, nextRunAt={}", currentUser.id(), sipPlanId, entity.getNextRunAt());
        return toSipPlanResponse(entity);
    }

    @Transactional
    public PortfolioDtos.SipPlanResponse stopSipPlan(CurrentUser currentUser, String sipPlanId) {
        return updateSipStatus(currentUser.id(), sipPlanId, SIP_STATUS_STOPPED, false);
    }

    public List<PortfolioDtos.ImportJobResponse> getImportJobs(CurrentUser currentUser) {
        return importJobRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
            .map(job -> new PortfolioDtos.ImportJobResponse(
                job.getId(),
                job.getSourcePlatform(),
                job.getStatus(),
                job.getFileName(),
                job.getRecognizedHoldings(),
                job.getCreatedAt().toString()
            )).toList();
    }

    @Transactional
    public PortfolioDtos.ImportJobResponse createImportJob(CurrentUser currentUser, PortfolioDtos.CreateImportJobRequest request) {
        log.info("Creating import job, userId={}, sourcePlatform={}, fileName={}",
            currentUser.id(), request.sourcePlatform(), request.fileName());
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.id());
        entity.setSourcePlatform(request.sourcePlatform());
        entity.setStatus("QUEUED");
        entity.setFileName(request.fileName());
        entity.setRecognizedHoldings(0);
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setUpdatedAt(LocalDateTime.now(clock));
        importJobRepository.save(entity);
        log.info("Import job persisted, userId={}, importJobId={}, status={}", currentUser.id(), entity.getId(), entity.getStatus());
        return new PortfolioDtos.ImportJobResponse(
            entity.getId(),
            entity.getSourcePlatform(),
            entity.getStatus(),
            entity.getFileName(),
            entity.getRecognizedHoldings(),
            entity.getCreatedAt().toString()
        );
    }

    public List<PortfolioDtos.WeeklyReportResponse> getReports(CurrentUser currentUser) {
        return weeklyReportRepository.findTop8ByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
            .map(this::toWeeklyReportResponse)
            .toList();
    }

    public List<PortfolioDtos.AlertRuleResponse> getAlerts(CurrentUser currentUser) {
        return alertRuleRepository.findByUserIdOrderByFundCodeAsc(currentUser.id()).stream()
            .map(this::toAlertResponse)
            .toList();
    }

    @Transactional
    public int createSipSnapshot(LocalDateTime snapshotTime) {
        List<SipPlanEntity> duePlans = sipPlanRepository.findAll().stream()
            .filter(plan -> SIP_STATUS_ACTIVE.equals(plan.getStatus()) && plan.getNextRunAt() != null)
            .filter(plan -> !plan.getNextRunAt().isAfter(snapshotTime))
            .toList();

        int created = 0;
        for (SipPlanEntity plan : duePlans) {
            LocalDate tradeDate = snapshotTime.toLocalDate();
            // 15:00 only freezes the execution intent for T day. Shares/nav stay pending until confirmed nav arrives at night.
            if (operationRecordRepository.existsBySipPlanIdAndTradeDate(plan.getId(), tradeDate)) {
                log.info("Skipping duplicate sip snapshot record, sipPlanId={}, tradeDate={}", plan.getId(), tradeDate);
                continue;
            }
            UserFundOperationRecordEntity record = new UserFundOperationRecordEntity();
            record.setId(UUID.randomUUID().toString());
            record.setUserId(plan.getUserId());
            record.setFundCode(plan.getFundCode());
            record.setOperation("SIP_BUY");
            record.setSource("SIP");
            record.setStatus(RECORD_STATUS_PENDING);
            record.setTradeDate(tradeDate);
            record.setAmount(round(plan.getAmount(), 2));
            record.setSharesDelta(0);
            record.setNav(0);
            record.setFeeRate(round(plan.getFeeRate(), 6));
            record.setFeeAmount(round(plan.getAmount() * plan.getFeeRate(), 2));
            record.setSipPlanId(plan.getId());
            record.setNote("15:00 快照生成");
            record.setCreatedAt(LocalDateTime.now(clock));
            record.setUpdatedAt(LocalDateTime.now(clock));
            operationRecordRepository.save(record);
            plan.setNextRunAt(computeNextRunAt(plan.getNextRunAt(), plan.getCadence(), null, null));
            plan.setUpdatedAt(LocalDateTime.now(clock));
            sipPlanRepository.save(plan);
            log.info("Pending sip record persisted, sipPlanId={}, operationId={}, tradeDate={}, amount={}",
                plan.getId(), record.getId(), tradeDate, record.getAmount());
            created++;
        }
        log.info("Sip snapshot finished, snapshotTime={}, created={}", snapshotTime, created);
        return created;
    }

    @Transactional
    public int confirmPendingSipOperations(LocalDate tradeDate) {
        List<UserFundOperationRecordEntity> pendingRecords = operationRecordRepository
            .findBySourceAndStatusAndTradeDateLessThanEqualOrderByTradeDateAscCreatedAtAsc("SIP", RECORD_STATUS_PENDING, tradeDate);
        int confirmed = 0;
        for (UserFundOperationRecordEntity record : pendingRecords) {
            Optional<NavHistoryEntity> navHistory = navHistoryRepository.findByFundCodeAndTradeDate(record.getFundCode(), tradeDate);
            if (navHistory.isEmpty()) {
                log.info("Skipping sip confirmation due to missing nav, operationId={}, fundCode={}, tradeDate={}",
                    record.getId(), record.getFundCode(), tradeDate);
                continue;
            }
            double nav = navHistory.get().getUnitNav();
            record.setTradeDate(tradeDate);
            record.setNav(round(nav, 4));
            record.setSharesDelta(round(record.getAmount() / nav, 4));
            record.setStatus(RECORD_STATUS_EXECUTED);
            record.setUpdatedAt(LocalDateTime.now(clock));
            operationRecordRepository.save(record);
            holdingService.rebuildSnapshotsFrom(record.getUserId(), record.getFundCode(), tradeDate);
            log.info("Pending sip record confirmed, operationId={}, fundCode={}, tradeDate={}, nav={}, sharesDelta={}",
                record.getId(), record.getFundCode(), tradeDate, record.getNav(), record.getSharesDelta());
            confirmed++;
        }
        log.info("Pending sip confirmation finished, tradeDate={}, confirmed={}", tradeDate, confirmed);
        return confirmed;
    }

    private PortfolioDtos.SipPlanResponse updateSipStatus(String userId, String sipPlanId, String status, boolean active) {
        SipPlanEntity entity = requiredSipPlan(userId, sipPlanId);
        entity.setStatus(status);
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now(clock));
        sipPlanRepository.save(entity);
        log.info("Sip plan status updated, userId={}, sipPlanId={}, status={}", userId, sipPlanId, status);
        return toSipPlanResponse(entity);
    }

    private PortfolioDtos.WatchlistItemResponse toWatchlistItem(String userId, WatchlistEntity entity, FundEntity fund) {
        FundSnapshotEntity snapshot = latestSnapshot(entity.getFundCode());
        FundEstimateEntity estimate = latestEstimate(entity.getFundCode());
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        boolean held = userFundHoldingRepository.findByUserIdAndFundCode(userId, entity.getFundCode()).isPresent();
        return new PortfolioDtos.WatchlistItemResponse(
            fund.getCode(),
            fund.getName(),
            entity.getNote(),
            round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth(), 4),
            round(snapshot.getUnitNav(), 4),
            round(estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav(), 4),
            List.of(),
            held
        );
    }

    private PortfolioDtos.SipPlanResponse toSipPlanResponse(SipPlanEntity entity) {
        return new PortfolioDtos.SipPlanResponse(
            entity.getId(),
            entity.getFundCode(),
            entity.getFundName(),
            round(entity.getAmount(), 2),
            entity.getCadence(),
            entity.getNextRunAt().toString(),
            entity.isActive(),
            entity.getStatus(),
            round(entity.getFeeRate(), 6)
        );
    }

    private PortfolioDtos.SipExecutionRecordResponse toSipExecutionRecord(UserFundOperationRecordEntity entity) {
        return new PortfolioDtos.SipExecutionRecordResponse(
            entity.getId(),
            entity.getSipPlanId(),
            entity.getTradeDate().toString(),
            round(entity.getAmount(), 2),
            entity.getStatus(),
            round(entity.getFeeRate(), 6),
            round(entity.getFeeAmount(), 2)
        );
    }

    private PortfolioDtos.OperationRecordResponse toOperationResponse(UserFundOperationRecordEntity entity) {
        String fundName = fundRepository.findByCode(entity.getFundCode()).map(FundEntity::getName).orElse(entity.getFundCode());
        return new PortfolioDtos.OperationRecordResponse(
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

    private PortfolioDtos.WeeklyReportResponse toWeeklyReportResponse(WeeklyReportEntity report) {
        return new PortfolioDtos.WeeklyReportResponse(
            report.getId(),
            report.getWeekLabel(),
            report.getSummary(),
            round(report.getReturnRate(), 4),
            report.getBestFundCode(),
            report.getRiskNote()
        );
    }

    private PortfolioDtos.AlertRuleResponse toAlertResponse(AlertRuleEntity alert) {
        return new PortfolioDtos.AlertRuleResponse(
            alert.getId(),
            alert.getFundCode(),
            alert.getRuleType(),
            round(alert.getThresholdValue(), 4),
            alert.isEnabled(),
            alert.getChannel()
        );
    }

    private void replaceWatchlistGroups(String watchlistId, List<String> groups) {
        replaceWatchlistGroups(List.of(watchlistId), groups);
    }

    private void replaceWatchlistGroups(List<String> watchlistIds, List<String> groups) {
        if (watchlistIds.isEmpty()) {
            return;
        }
        watchlistGroupRepository.deleteByWatchlistIdIn(watchlistIds);
        // Force delete statements out before re-inserting identical groups in the same transaction.
        watchlistGroupRepository.flush();
        List<String> normalizedGroups = normalizeGroups(groups);
        List<WatchlistGroupEntity> entities = new ArrayList<>();
        for (String watchlistId : watchlistIds) {
            for (String group : normalizedGroups) {
                WatchlistGroupEntity entity = new WatchlistGroupEntity();
                entity.setId(UUID.randomUUID().toString());
                entity.setWatchlistId(watchlistId);
                entity.setGroupCode(group);
                entity.setCreatedAt(LocalDateTime.now(clock));
                entities.add(entity);
            }
        }
        if (!entities.isEmpty()) {
            watchlistGroupRepository.saveAll(entities);
        }
    }

    private Map<String, List<String>> groupsByWatchlistId(List<WatchlistEntity> watchlists) {
        if (watchlists.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> groupsByWatchlist = new LinkedHashMap<>();
        watchlistGroupRepository.findByWatchlistIdIn(watchlists.stream().map(WatchlistEntity::getId).toList())
            .forEach(group -> groupsByWatchlist.computeIfAbsent(group.getWatchlistId(), ignored -> new ArrayList<>()).add(group.getGroupCode()));
        return groupsByWatchlist;
    }

    private List<String> normalizeGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        return groups.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(group -> !group.isBlank())
            .filter(group -> !"全部".equals(group))
            .distinct()
            .toList();
    }

    private boolean hasSameGroups(List<String> currentGroups, List<String> requestedGroups) {
        return new LinkedHashSet<>(normalizeGroups(currentGroups)).equals(new LinkedHashSet<>(normalizeGroups(requestedGroups)));
    }

    private String resolveLegacyPortfolioId(String userId, String requestedPortfolioId) {
        if (requestedPortfolioId != null && !requestedPortfolioId.isBlank()) {
            return requestedPortfolioId;
        }
        return portfolioRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
            .findFirst()
            .map(PortfolioEntity::getId)
            .orElse("portfolio-current");
    }

    private SipPlanEntity requiredSipPlan(String userId, String sipPlanId) {
        return sipPlanRepository.findByIdAndUserId(sipPlanId, userId)
            .orElseThrow(() -> new NotFoundException("Sip plan not found"));
    }

    private FundEntity requiredFund(String fundCode) {
        return fundRepository.findByCode(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
    }

    private FundSnapshotEntity latestSnapshot(String fundCode) {
        return fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
    }

    private FundEstimateEntity latestEstimate(String fundCode) {
        return fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
    }

    private String cadenceLabel(String cadence) {
        return switch (cadence) {
            case "DAILY" -> "每日";
            case "WEEKLY" -> "每周";
            case "BIWEEKLY" -> "双周";
            case "MONTHLY" -> "每月";
            default -> cadence;
        };
    }

    private LocalDateTime computeNextRunAt(LocalDateTime baseTime, String cadence, String weekday, String monthDay) {
        return switch (cadence) {
            case "DAILY" -> baseTime.plusDays(1);
            case "WEEKLY" -> nextWeekly(baseTime, weekday, 1);
            case "BIWEEKLY" -> nextWeekly(baseTime, weekday, 2);
            case "MONTHLY" -> nextMonthly(baseTime, monthDay);
            default -> throw new IllegalArgumentException("Unsupported cadence");
        };
    }

    private LocalDateTime nextWeekly(LocalDateTime baseTime, String weekday, int weekStep) {
        int target = weekday == null ? baseTime.getDayOfWeek().getValue() : Integer.parseInt(weekday) + 1;
        LocalDate candidate = baseTime.toLocalDate().plusDays(1);
        while (candidate.getDayOfWeek().getValue() != target) {
            candidate = candidate.plusDays(1);
        }
        if (weekStep == 2) {
            candidate = candidate.plusWeeks(1);
        }
        return LocalDateTime.of(candidate, baseTime.toLocalTime());
    }

    private LocalDateTime nextMonthly(LocalDateTime baseTime, String monthDay) {
        int desiredDay = monthDay == null ? baseTime.getDayOfMonth() : Integer.parseInt(monthDay);
        YearMonth nextMonth = YearMonth.from(baseTime.plusMonths(1));
        int safeDay = Math.min(desiredDay, nextMonth.lengthOfMonth());
        return LocalDateTime.of(nextMonth.atDay(safeDay), baseTime.toLocalTime());
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
