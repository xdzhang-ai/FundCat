package com.winter.fund.modules.sip.service;

/**
 * 定投模块服务，负责封装定投计划与执行记录的核心业务逻辑。
 */

import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.common.config.PersistenceProperties;
import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import com.winter.fund.modules.holding.service.HoldingRebuildTarget;
import com.winter.fund.modules.sip.model.SipDtos;
import com.winter.fund.modules.sip.model.SipPlanEntity;
import com.winter.fund.modules.sip.repository.SipPlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SipService {

    private static final Logger log = LoggerFactory.getLogger(SipService.class);
    private static final String SIP_STATUS_ACTIVE = "生效";
    private static final String SIP_STATUS_PAUSED = "暂停";
    private static final String SIP_STATUS_STOPPED = "停止";
    private static final String RECORD_STATUS_PENDING = "确认中";
    private static final String RECORD_STATUS_EXECUTED = "已执行";
    private static final String DEFAULT_PORTFOLIO_ID = "portfolio-current";

    private final SipPlanRepository sipPlanRepository;
    private final FundRepository fundRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final UserFundOperationRecordRepository operationRecordRepository;
    private final PersistenceProperties persistenceProperties;
    private final Clock clock;

    public SipService(
        SipPlanRepository sipPlanRepository,
        FundRepository fundRepository,
        NavHistoryRepository navHistoryRepository,
        UserFundOperationRecordRepository operationRecordRepository,
        PersistenceProperties persistenceProperties,
        MarketDataProperties marketDataProperties
    ) {
        this.sipPlanRepository = sipPlanRepository;
        this.fundRepository = fundRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.operationRecordRepository = operationRecordRepository;
        this.persistenceProperties = persistenceProperties;
        this.clock = Clock.system(java.time.ZoneId.of(marketDataProperties.getTimezone()));
    }

    /**
     * 获取定投计划列表。
     */
    public List<SipDtos.SipPlanResponse> getSipPlans(CurrentUser currentUser) {
        return sipPlanRepository.findByUserIdOrderByNextRunAtAsc(currentUser.id()).stream()
            .map(this::toSipPlanResponse)
            .toList();
    }

    /**
     * 获取单个定投计划详情。
     */
    public SipDtos.SipPlanResponse getSipPlan(CurrentUser currentUser, String sipPlanId) {
        return toSipPlanResponse(requiredSipPlan(currentUser.id(), sipPlanId));
    }

    /**
     * 获取某个定投计划的执行记录。
     */
    public List<SipDtos.SipExecutionRecordResponse> getSipPlanRecords(CurrentUser currentUser, String sipPlanId) {
        SipPlanEntity plan = requiredSipPlan(currentUser.id(), sipPlanId);
        return operationRecordRepository.findBySipPlanIdOrderByTradeDateDescCreatedAtDesc(plan.getId()).stream()
            .map(this::toSipExecutionRecord)
            .toList();
    }

    /**
     * 获取概览页使用的定投摘要列表。
     */
    public List<SipDtos.SipPlanDigestResponse> getSipDigests(CurrentUser currentUser) {
        return getSipPlans(currentUser).stream()
            .map(plan -> new SipDtos.SipPlanDigestResponse(
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

    /**
     * 创建定投计划。
     */
    @Transactional
    public SipDtos.SipPlanResponse createSipPlan(CurrentUser currentUser, SipDtos.CreateSipPlanRequest request) {
        log.info("Creating sip plan, userId={}, fundCode={}, cadence={}", currentUser.id(), request.fundCode(), request.cadence());
        // 同一基金只允许保留一个未停止的计划，避免调度阶段重复生成扣款记录。
        FundEntity fund = requiredFund(request.fundCode());
        sipPlanRepository.findByUserIdAndFundCodeAndStatusNot(currentUser.id(), request.fundCode(), SIP_STATUS_STOPPED).ifPresent(existing -> {
            throw new IllegalArgumentException("Fund already has an active sip plan");
        });
        SipPlanEntity entity = new SipPlanEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(resolvePortfolioId(request.portfolioId()));
        entity.setUserId(currentUser.id());
        entity.setFundCode(request.fundCode());
        entity.setFundName(fund.getName());
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

    /**
     * 更新定投计划的金额、频率和下一次执行时间。
     */
    @Transactional
    public SipDtos.SipPlanResponse updateSipPlan(CurrentUser currentUser, String sipPlanId, SipDtos.UpdateSipPlanRequest request) {
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

    /**
     * 暂停定投计划。
     * 暂停只会阻止后续 15:00 快照继续生成，不会回滚已经生成的待确认记录；恢复后仍可继续执行。
     */
    @Transactional
    public SipDtos.SipPlanResponse pauseSipPlan(CurrentUser currentUser, String sipPlanId) {
        return updateSipStatus(currentUser.id(), sipPlanId, SIP_STATUS_PAUSED, false);
    }

    /**
     * 恢复定投计划。
     * 恢复时会重新置为“生效”，并按当前时间重新推算下一次执行窗口；已经停止的计划不可恢复。
     */
    @Transactional
    public SipDtos.SipPlanResponse resumeSipPlan(CurrentUser currentUser, String sipPlanId) {
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

    /**
     * 停止定投计划。
     * 停止后计划不再参与调度，也不允许再修改或恢复；后续若还要继续同基金定投，需要新建计划。
     */
    @Transactional
    public SipDtos.SipPlanResponse stopSipPlan(CurrentUser currentUser, String sipPlanId) {
        return updateSipStatus(currentUser.id(), sipPlanId, SIP_STATUS_STOPPED, false);
    }

    /**
     * 为到期计划生成待确认的定投执行记录。
     * 15:00 调度只做“生成一条确认中记录”这件事，不在这里直接更新持仓；
     * 等晚间确认净值出来后，再把这条记录补齐成已执行并交给持仓模块统一回算。
     */
    @Transactional
    public int createSipSnapshot(LocalDateTime snapshotTime) {
        List<SipPlanEntity> duePlans = sipPlanRepository.findAll().stream()
            .filter(plan -> SIP_STATUS_ACTIVE.equals(plan.getStatus()) && plan.getNextRunAt() != null)
            .filter(plan -> !plan.getNextRunAt().isAfter(snapshotTime))
            .toList();

        int created = 0;
        List<UserFundOperationRecordEntity> recordsToCreate = new java.util.ArrayList<>();
        List<SipPlanEntity> plansToUpdate = new java.util.ArrayList<>();
        for (SipPlanEntity plan : duePlans) {
            LocalDate tradeDate = snapshotTime.toLocalDate();
            // 同一天只允许存在一条同计划记录，防止定时任务重入造成重复快照。
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
            recordsToCreate.add(record);
            // 快照落库后立刻推进下一次执行时间，避免后续轮询重复命中同一计划。
            plan.setNextRunAt(computeNextRunAt(plan.getNextRunAt(), plan.getCadence(), null, null));
            plan.setUpdatedAt(LocalDateTime.now(clock));
            plansToUpdate.add(plan);
            log.info("Pending sip record persisted, sipPlanId={}, operationId={}, tradeDate={}, amount={}",
                plan.getId(), record.getId(), tradeDate, record.getAmount());
            created++;
        }
        saveInBatches(recordsToCreate, operationRecordRepository::saveAll);
        saveInBatches(plansToUpdate, sipPlanRepository::saveAll);
        log.info("Sip snapshot finished, snapshotTime={}, created={}", snapshotTime, created);
        return created;
    }

    /**
     * 使用确认净值将待确认定投记录转为已执行。
     * 定投确认的职责是把 15:00 生成的 pending 记录补齐 nav 和份额变化；
     * 记录确认完成后，依然走 HoldingService.rebuildSnapshotsFrom 这条与手工买卖一致的持仓回算主链路。
     */
    @Transactional
    public List<HoldingRebuildTarget> confirmPendingSipOperations(String fundCode, LocalDate tradeDate) {
        List<UserFundOperationRecordEntity> pendingRecords = operationRecordRepository
            .findBySourceAndStatusAndFundCodeAndTradeDateOrderByCreatedAtAsc("SIP", RECORD_STATUS_PENDING, fundCode, tradeDate);
        List<HoldingRebuildTarget> rebuildTargets = new java.util.ArrayList<>();
        List<UserFundOperationRecordEntity> recordsToUpdate = new java.util.ArrayList<>();
        for (UserFundOperationRecordEntity record : pendingRecords) {
            Optional<NavHistoryEntity> navHistory = navHistoryRepository.findByFundCodeAndTradeDate(record.getFundCode(), record.getTradeDate());
            if (navHistory.isEmpty()) {
                log.info("Skipping sip confirmation due to missing nav, operationId={}, fundCode={}, tradeDate={}",
                    record.getId(), record.getFundCode(), record.getTradeDate());
                continue;
            }
            double nav = navHistory.get().getUnitNav();
            // 定投在确认阶段才补齐真实净值和份额变化，然后交给持仓模块统一回算。
            record.setNav(round(nav, 4));
            record.setSharesDelta(round(record.getAmount() / nav, 4));
            record.setStatus(RECORD_STATUS_EXECUTED);
            record.setUpdatedAt(LocalDateTime.now(clock));
            recordsToUpdate.add(record);
            rebuildTargets.add(new HoldingRebuildTarget(record.getUserId(), record.getFundCode(), record.getTradeDate()));
            log.info("Pending sip record confirmed, operationId={}, fundCode={}, tradeDate={}, nav={}, sharesDelta={}",
                record.getId(), record.getFundCode(), record.getTradeDate(), record.getNav(), record.getSharesDelta());
        }
        saveInBatches(recordsToUpdate, operationRecordRepository::saveAll);
        log.info("Pending sip confirmation finished, fundCode={}, tradeDate={}, confirmed={}", fundCode, tradeDate, rebuildTargets.size());
        return rebuildTargets;
    }

    /**
     * 更新定投计划状态。
     * 这里统一承接暂停和停止两类状态切换，只改计划自身的调度状态，不触碰已经生成的执行记录。
     */
    private SipDtos.SipPlanResponse updateSipStatus(String userId, String sipPlanId, String status, boolean active) {
        SipPlanEntity entity = requiredSipPlan(userId, sipPlanId);
        entity.setStatus(status);
        entity.setActive(active);
        entity.setUpdatedAt(LocalDateTime.now(clock));
        sipPlanRepository.save(entity);
        log.info("Sip plan status updated, userId={}, sipPlanId={}, status={}", userId, sipPlanId, status);
        return toSipPlanResponse(entity);
    }

    /**
     * 按配置的批量大小分批写入定投记录或计划更新。
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

    /**
     * 转换为定投计划响应。
     */
    private SipDtos.SipPlanResponse toSipPlanResponse(SipPlanEntity entity) {
        return new SipDtos.SipPlanResponse(
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

    /**
     * 转换为定投execution记录。
     */
    private SipDtos.SipExecutionRecordResponse toSipExecutionRecord(UserFundOperationRecordEntity entity) {
        return new SipDtos.SipExecutionRecordResponse(
            entity.getId(),
            entity.getSipPlanId(),
            entity.getTradeDate().toString(),
            round(entity.getAmount(), 2),
            entity.getStatus(),
            round(entity.getFeeRate(), 6),
            round(entity.getFeeAmount(), 2)
        );
    }

    /**
     * 解析组合id。
     */
    private String resolvePortfolioId(String requestedPortfolioId) {
        if (requestedPortfolioId != null && !requestedPortfolioId.isBlank()) {
            return requestedPortfolioId;
        }
        return DEFAULT_PORTFOLIO_ID;
    }

    /**
     * 获取必需的定投计划。
     */
    private SipPlanEntity requiredSipPlan(String userId, String sipPlanId) {
        return sipPlanRepository.findByIdAndUserId(sipPlanId, userId)
            .orElseThrow(() -> new NotFoundException("Sip plan not found"));
    }

    /**
     * 获取必需的基金。
     */
    private FundEntity requiredFund(String fundCode) {
        return fundRepository.findByCode(fundCode)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
    }

    /**
     * 返回cadenceLabel结果。
     */
    private String cadenceLabel(String cadence) {
        return switch (cadence) {
            case "DAILY" -> "每日";
            case "WEEKLY" -> "每周";
            case "BIWEEKLY" -> "双周";
            case "MONTHLY" -> "每月";
            default -> cadence;
        };
    }

    /**
     * 返回computeNextRunAt结果。
     */
    private LocalDateTime computeNextRunAt(LocalDateTime baseTime, String cadence, String weekday, String monthDay) {
        return switch (cadence) {
            case "DAILY" -> baseTime.plusDays(1);
            case "WEEKLY" -> nextWeekly(baseTime, weekday, 1);
            case "BIWEEKLY" -> nextWeekly(baseTime, weekday, 2);
            case "MONTHLY" -> nextMonthly(baseTime, monthDay);
            default -> throw new IllegalArgumentException("Unsupported cadence");
        };
    }

    /**
     * 返回nextWeekly结果。
     */
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

    /**
     * 返回nextMonthly结果。
     */
    private LocalDateTime nextMonthly(LocalDateTime baseTime, String monthDay) {
        int desiredDay = monthDay == null ? baseTime.getDayOfMonth() : Integer.parseInt(monthDay);
        YearMonth nextMonth = YearMonth.from(baseTime.plusMonths(1));
        int safeDay = Math.min(desiredDay, nextMonth.lengthOfMonth());
        return LocalDateTime.of(nextMonth.atDay(safeDay), baseTime.toLocalTime());
    }

    /**
     * 返回round结果。
     */
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
