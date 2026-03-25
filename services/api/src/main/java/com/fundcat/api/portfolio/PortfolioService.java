package com.fundcat.api.portfolio;

import com.fundcat.api.auth.CurrentUser;
import com.fundcat.api.common.NotFoundException;
import com.fundcat.api.fund.FundEntity;
import com.fundcat.api.fund.FundEstimateEntity;
import com.fundcat.api.fund.FundRepository;
import com.fundcat.api.fund.FundEstimateRepository;
import com.fundcat.api.fund.FundSnapshotEntity;
import com.fundcat.api.fund.FundSnapshotRepository;
import com.fundcat.api.ops.OpsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final WatchlistRepository watchlistRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingLotRepository holdingLotRepository;
    private final PaperOrderRepository paperOrderRepository;
    private final SipPlanRepository sipPlanRepository;
    private final ImportJobRepository importJobRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final OpsService opsService;

    public PortfolioService(
        WatchlistRepository watchlistRepository,
        PortfolioRepository portfolioRepository,
        HoldingLotRepository holdingLotRepository,
        PaperOrderRepository paperOrderRepository,
        SipPlanRepository sipPlanRepository,
        ImportJobRepository importJobRepository,
        WeeklyReportRepository weeklyReportRepository,
        AlertRuleRepository alertRuleRepository,
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        OpsService opsService
    ) {
        this.watchlistRepository = watchlistRepository;
        this.portfolioRepository = portfolioRepository;
        this.holdingLotRepository = holdingLotRepository;
        this.paperOrderRepository = paperOrderRepository;
        this.sipPlanRepository = sipPlanRepository;
        this.importJobRepository = importJobRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.opsService = opsService;
    }

    public List<PortfolioDtos.WatchlistItemResponse> getWatchlist(CurrentUser currentUser) {
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
            .map(item -> {
                FundEntity fund = fundRepository.findByCode(item.getFundCode())
                    .orElseThrow(() -> new NotFoundException("Fund not found"));
                FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(item.getFundCode())
                    .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
                FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(item.getFundCode())
                    .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
                return new PortfolioDtos.WatchlistItemResponse(
                    fund.getCode(),
                    fund.getName(),
                    item.getNote(),
                    round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth()),
                    round(snapshot.getUnitNav()),
                    round(estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav())
                );
            }).toList();
    }

    public PortfolioDtos.WatchlistItemResponse addWatchlist(CurrentUser currentUser, PortfolioDtos.CreateWatchlistRequest request) {
        watchlistRepository.findByUserIdAndFundCode(currentUser.id(), request.fundCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Fund already exists in watchlist");
        });
        FundEntity fund = fundRepository.findByCode(request.fundCode())
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.id());
        entity.setFundCode(request.fundCode());
        entity.setNote(request.note());
        entity.setCreatedAt(LocalDateTime.now());
        watchlistRepository.save(entity);
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(request.fundCode())
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(request.fundCode())
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        return new PortfolioDtos.WatchlistItemResponse(
            fund.getCode(),
            fund.getName(),
            request.note(),
            round(opsService.isEnabled("estimate_reference") ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth()),
            round(snapshot.getUnitNav()),
            round(opsService.isEnabled("estimate_reference") ? estimate.getEstimatedNav() : snapshot.getUnitNav())
        );
    }

    public void removeWatchlist(CurrentUser currentUser, String fundCode) {
        WatchlistEntity entity = watchlistRepository.findByUserIdAndFundCode(currentUser.id(), fundCode)
            .orElseThrow(() -> new NotFoundException("Watchlist item not found"));
        watchlistRepository.delete(entity);
    }

    public List<PortfolioDtos.PortfolioSummaryResponse> getPortfolios(CurrentUser currentUser) {
        return portfolioRepository.findByUserIdOrderByCreatedAtAsc(currentUser.id()).stream()
            .map(this::toPortfolioSummary)
            .toList();
    }

    public List<PortfolioDtos.PaperOrderResponse> getOrders(CurrentUser currentUser) {
        List<String> portfolioIds = portfolioRepository.findByUserIdOrderByCreatedAtAsc(currentUser.id()).stream()
            .map(PortfolioEntity::getId)
            .toList();
        if (portfolioIds.isEmpty()) {
            return List.of();
        }
        return paperOrderRepository.findTop12ByPortfolioIdInOrderByExecutedAtDesc(portfolioIds).stream()
            .map(order -> new PortfolioDtos.PaperOrderResponse(
                order.getId(),
                order.getFundCode(),
                order.getFundName(),
                order.getOrderType(),
                round(order.getAmount()),
                round(order.getShares()),
                round(order.getFee()),
                order.getStatus(),
                order.getExecutedAt().toString()
            )).toList();
    }

    public PortfolioDtos.PaperOrderResponse createPaperOrder(CurrentUser currentUser, PortfolioDtos.CreatePaperOrderRequest request) {
        PortfolioEntity portfolio = validatePortfolio(currentUser.id(), request.portfolioId());
        FundEntity fund = fundRepository.findByCode(request.fundCode())
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        PaperOrderEntity entity = new PaperOrderEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolio.getId());
        entity.setFundCode(fund.getCode());
        entity.setFundName(fund.getName());
        entity.setOrderType(request.orderType().toUpperCase());
        entity.setAmount(request.amount());
        entity.setShares(request.shares());
        entity.setFee(request.fee());
        entity.setStatus("FILLED");
        entity.setExecutedAt(LocalDateTime.now());
        entity.setNote(request.note());
        paperOrderRepository.save(entity);
        applyOrderToHolding(portfolio.getId(), fund, snapshot, estimate, request, entity.getExecutedAt());
        return new PortfolioDtos.PaperOrderResponse(
            entity.getId(),
            entity.getFundCode(),
            entity.getFundName(),
            entity.getOrderType(),
            round(entity.getAmount()),
            round(entity.getShares()),
            round(entity.getFee()),
            entity.getStatus(),
            entity.getExecutedAt().toString()
        );
    }

    public List<PortfolioDtos.SipPlanResponse> getSipPlans(CurrentUser currentUser) {
        List<String> portfolioIds = portfolioRepository.findByUserIdOrderByCreatedAtAsc(currentUser.id()).stream()
            .map(PortfolioEntity::getId)
            .toList();
        if (portfolioIds.isEmpty()) {
            return List.of();
        }
        return sipPlanRepository.findByPortfolioIdInOrderByNextRunAtAsc(portfolioIds).stream()
            .map(plan -> new PortfolioDtos.SipPlanResponse(
                plan.getId(),
                plan.getFundCode(),
                plan.getFundName(),
                round(plan.getAmount()),
                plan.getCadence(),
                plan.getNextRunAt().toString(),
                plan.isActive()
            )).toList();
    }

    public PortfolioDtos.SipPlanResponse createSipPlan(CurrentUser currentUser, PortfolioDtos.CreateSipPlanRequest request) {
        PortfolioEntity portfolio = validatePortfolio(currentUser.id(), request.portfolioId());
        FundEntity fund = fundRepository.findByCode(request.fundCode())
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        SipPlanEntity entity = new SipPlanEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolio.getId());
        entity.setFundCode(fund.getCode());
        entity.setFundName(fund.getName());
        entity.setAmount(request.amount());
        entity.setCadence(request.cadence());
        entity.setNextRunAt(LocalDateTime.parse(request.nextRunAt()));
        entity.setActive(true);
        sipPlanRepository.save(entity);
        return new PortfolioDtos.SipPlanResponse(
            entity.getId(),
            entity.getFundCode(),
            entity.getFundName(),
            round(entity.getAmount()),
            entity.getCadence(),
            entity.getNextRunAt().toString(),
            entity.isActive()
        );
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

    public PortfolioDtos.ImportJobResponse createImportJob(CurrentUser currentUser, PortfolioDtos.CreateImportJobRequest request) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(currentUser.id());
        entity.setSourcePlatform(request.sourcePlatform());
        entity.setStatus("QUEUED");
        entity.setFileName(request.fileName());
        entity.setRecognizedHoldings(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(entity);
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
            .map(report -> new PortfolioDtos.WeeklyReportResponse(
                report.getId(),
                report.getWeekLabel(),
                report.getSummary(),
                round(report.getReturnRate()),
                report.getBestFundCode(),
                report.getRiskNote()
            )).toList();
    }

    public List<PortfolioDtos.AlertRuleResponse> getAlerts(CurrentUser currentUser) {
        return alertRuleRepository.findByUserIdOrderByFundCodeAsc(currentUser.id()).stream()
            .map(alert -> new PortfolioDtos.AlertRuleResponse(
                alert.getId(),
                alert.getFundCode(),
                alert.getRuleType(),
                round(alert.getThresholdValue()),
                alert.isEnabled(),
                alert.getChannel()
            )).toList();
    }

    private PortfolioEntity validatePortfolio(String userId, String portfolioId) {
        return portfolioRepository.findById(portfolioId)
            .filter(portfolio -> portfolio.getUserId().equals(userId))
            .orElseThrow(() -> new NotFoundException("Portfolio not found"));
    }

    private PortfolioDtos.PortfolioSummaryResponse toPortfolioSummary(PortfolioEntity portfolio) {
        List<HoldingLotEntity> holdings = holdingLotRepository.findByPortfolioIdOrderByAllocationDesc(portfolio.getId());
        double marketValue = holdings.stream().mapToDouble(HoldingLotEntity::getCurrentValue).sum();
        double totalPnl = holdings.stream().mapToDouble(HoldingLotEntity::getPnl).sum();
        return new PortfolioDtos.PortfolioSummaryResponse(
            portfolio.getId(),
            portfolio.getName(),
            portfolio.getBroker(),
            portfolio.getCurrency(),
            round(marketValue),
            round(totalPnl),
            round(portfolio.getInitialCash()),
            holdings.stream().map(holding -> new PortfolioDtos.HoldingLotResponse(
                holding.getId(),
                holding.getFundCode(),
                holding.getFundName(),
                round(holding.getShares()),
                round(holding.getAverageCost()),
                round(holding.getCurrentValue()),
                round(holding.getPnl()),
                round(holding.getAllocation()),
                holding.getSource(),
                holding.getUpdatedAt().toString()
            )).toList()
        );
    }

    public Map<String, PortfolioDtos.PortfolioSummaryResponse> portfolioMap(CurrentUser currentUser) {
        return getPortfolios(currentUser).stream().collect(Collectors.toMap(PortfolioDtos.PortfolioSummaryResponse::id, Function.identity()));
    }

    private void applyOrderToHolding(
        String portfolioId,
        FundEntity fund,
        FundSnapshotEntity snapshot,
        FundEstimateEntity estimate,
        PortfolioDtos.CreatePaperOrderRequest request,
        LocalDateTime executedAt
    ) {
        HoldingLotEntity holding = holdingLotRepository.findByPortfolioIdAndFundCode(portfolioId, fund.getCode())
            .orElseGet(() -> {
                HoldingLotEntity entity = new HoldingLotEntity();
                entity.setId(UUID.randomUUID().toString());
                entity.setPortfolioId(portfolioId);
                entity.setFundCode(fund.getCode());
                entity.setFundName(fund.getName());
                entity.setShares(0);
                entity.setAverageCost(0);
                entity.setCurrentValue(0);
                entity.setPnl(0);
                entity.setAllocation(0);
                entity.setSource("manual");
                entity.setImported(false);
                entity.setUpdatedAt(executedAt);
                return entity;
            });

        double currentPrice = opsService.isEnabled("estimate_reference") ? estimate.getEstimatedNav() : snapshot.getUnitNav();
        double existingShares = holding.getShares();
        double existingAverageCost = holding.getAverageCost();
        String orderType = request.orderType().toUpperCase();
        double nextShares;
        double nextAverageCost;

        if ("BUY".equals(orderType)) {
            nextShares = existingShares + request.shares();
            double totalCostBasis = (existingShares * existingAverageCost) + request.amount() + request.fee();
            nextAverageCost = nextShares == 0 ? 0 : totalCostBasis / nextShares;
        } else {
            if (existingShares <= 0 || request.shares() > existingShares) {
                throw new IllegalArgumentException("持仓份额不足，无法模拟卖出");
            }
            nextShares = existingShares - request.shares();
            nextAverageCost = nextShares == 0 ? 0 : existingAverageCost;
        }

        if (nextShares == 0) {
            holdingLotRepository.delete(holding);
        } else {
            holding.setShares(nextShares);
            holding.setAverageCost(nextAverageCost);
            holding.setCurrentValue(nextShares * currentPrice);
            holding.setPnl((nextShares * currentPrice) - (nextShares * nextAverageCost));
            holding.setUpdatedAt(executedAt);
            holdingLotRepository.save(holding);
        }

        rebalanceAllocations(portfolioId);
    }

    private void rebalanceAllocations(String portfolioId) {
        List<HoldingLotEntity> holdings = holdingLotRepository.findByPortfolioIdOrderByAllocationDesc(portfolioId);
        double marketValue = holdings.stream().mapToDouble(HoldingLotEntity::getCurrentValue).sum();
        holdings.forEach(holding -> holding.setAllocation(marketValue == 0 ? 0 : holding.getCurrentValue() / marketValue));
        holdingLotRepository.saveAll(holdings);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
