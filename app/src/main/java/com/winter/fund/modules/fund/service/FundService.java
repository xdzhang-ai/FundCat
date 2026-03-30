package com.winter.fund.modules.fund.service;

/**
 * 基金模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.fund.model.FundDtos;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
import com.winter.fund.modules.fund.model.FundSnapshotEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.FundSnapshotRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.repository.UserFundHoldingRepository;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.portfolio.repository.WatchlistRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FundService {

    private static final Logger log = LoggerFactory.getLogger(FundService.class);

    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final UserFundHoldingRepository userFundHoldingRepository;
    private final OpsService opsService;

    public FundService(
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        WatchlistRepository watchlistRepository,
        UserFundHoldingRepository userFundHoldingRepository,
        OpsService opsService
    ) {
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.userFundHoldingRepository = userFundHoldingRepository;
        this.opsService = opsService;
    }

    public List<FundDtos.FundCardResponse> search(String userId, String query) {
        log.info("Searching funds for userId={}, query={}", userId, query == null ? "" : query.trim());
        Set<String> watchlistCodes = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(watchlist -> watchlist.getFundCode())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> holdingCodes = userFundHoldingRepository.findByUserIdOrderByMarketValueDesc(userId).stream()
            .map(holding -> holding.getFundCode())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<FundEntity> funds;
        if (query == null || query.isBlank()) {
            List<FundEntity> recentFunds = fundRepository.findTop12ByOrderByCreatedAtDesc();
            LinkedHashSet<String> prioritizedCodes = new LinkedHashSet<>();
            prioritizedCodes.addAll(holdingCodes);
            prioritizedCodes.addAll(watchlistCodes);
            prioritizedCodes.addAll(recentFunds.stream().map(FundEntity::getCode).toList());

            Map<String, FundEntity> fundMap = fundRepository.findByCodeIn(List.copyOf(prioritizedCodes)).stream()
                .collect(Collectors.toMap(FundEntity::getCode, Function.identity()));
            funds = prioritizedCodes.stream()
                .map(fundMap::get)
                .filter(java.util.Objects::nonNull)
                .limit(12)
                .toList();
        } else {
            funds = fundRepository.findTop8ByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query.trim(), query.trim());
        }

        return funds.stream().map(fund -> toCard(fund, watchlistCodes, holdingCodes)).toList();
    }

    public FundDtos.FundDetailResponse getDetail(String userId, String code) {
        log.info("Loading fund detail, userId={}, code={}", userId, code);
        FundEntity fund = fundRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        List<NavHistoryEntity> recentHistory = navHistoryRepository.findByFundCodeOrderByTradeDateAsc(code);
        List<FundDtos.TrendPoint> navHistory = buildLongRangeHistory(snapshot, recentHistory);
        double displayedNav = estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav();
        double displayedGrowth = estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth();
        List<FundDtos.TrendPoint> estimateHistory = estimateReferenceEnabled
            ? buildEstimateHistory(navHistory, snapshot.getNavDate(), snapshot.getUnitNav(), estimate.getEstimatedNav())
            : navHistory;
        List<String> comparisonLabels = List.of(fund.getCategory(), fund.getRiskLevel(), fund.getTagLine());
        List<FundDtos.QuarterlyHoldingResponse> quarterlyHoldings = opsService.isEnabled("quarterly_holdings")
            ? buildQuarterlyHoldings(code)
            : List.of();
        List<FundDtos.IndustryExposureResponse> industryDistribution = opsService.isEnabled("industry_distribution")
            ? buildIndustryDistribution(code)
            : List.of();
        boolean watchlisted = watchlistRepository.findByUserIdAndFundCode(userId, code).isPresent();
        boolean held = userFundHoldingRepository.findByUserIdAndFundCode(userId, code).isPresent();
        return new FundDtos.FundDetailResponse(
            fund.getCode(),
            fund.getName(),
            fund.getCategory(),
            fund.getRiskLevel(),
            splitPipe(fund.getTags()),
            fund.getBenchmark(),
            round(snapshot.getUnitNav()),
            round(snapshot.getDayGrowth()),
            round(displayedNav),
            round(displayedGrowth),
            estimateReferenceEnabled && estimate.isReferenceOnly(),
            watchlisted,
            held,
            round(fund.getManagementFee()),
            round(fund.getCustodyFee()),
            round(fund.getPurchaseFee()),
            round(snapshot.getAssetSize()),
            round(snapshot.getStockRatio()),
            round(snapshot.getBondRatio()),
            buildTopHoldings(code),
            navHistory,
            estimateHistory,
            comparisonLabels,
            quarterlyHoldings,
            industryDistribution
        );
    }

    public FundDtos.FundUserStateResponse getUserState(String userId, String code) {
        log.info("Loading fund user state, userId={}, code={}", userId, code);
        fundRepository.findByCode(code).orElseThrow(() -> new NotFoundException("Fund not found"));
        boolean watchlisted = watchlistRepository.findByUserIdAndFundCode(userId, code).isPresent();
        boolean held = userFundHoldingRepository.findByUserIdAndFundCode(userId, code).isPresent();
        return new FundDtos.FundUserStateResponse(code, watchlisted, held);
    }

    private FundDtos.FundCardResponse toCard(FundEntity fund, Set<String> watchlistCodes, Set<String> holdingCodes) {
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        return new FundDtos.FundCardResponse(
            fund.getCode(),
            fund.getName(),
            fund.getCategory(),
            fund.getRiskLevel(),
            splitPipe(fund.getTags()),
            fund.getBenchmark(),
            round(snapshot.getUnitNav()),
            round(snapshot.getDayGrowth()),
            round(estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav()),
            round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth()),
            estimateReferenceEnabled && estimate.isReferenceOnly(),
            watchlistCodes.contains(fund.getCode()),
            holdingCodes.contains(fund.getCode())
        );
    }

    private List<FundDtos.TrendPoint> buildEstimateHistory(
        List<FundDtos.TrendPoint> navHistory,
        LocalDate navDate,
        double unitNav,
        double estimateNav
    ) {
        List<FundDtos.TrendPoint> points = new ArrayList<>(navHistory);
        for (int i = 0; i < 6; i++) {
            double ratio = (i + 1) / 6.0;
            double value = unitNav + (estimateNav - unitNav) * ratio;
            LocalDateTime estimatePointTime = navDate.atTime(10 + i, 0);
            points.add(new FundDtos.TrendPoint(estimatePointTime.toString(), round(value)));
        }
        return points;
    }

    private List<FundDtos.TrendPoint> buildLongRangeHistory(FundSnapshotEntity snapshot, List<NavHistoryEntity> recentHistory) {
        // When demo history is sparse, interpolate anchor points so the trend chart stays continuous.
        LocalDate endDate = snapshot.getNavDate();
        double endValue = snapshot.getUnitNav();
        double monthValue = derivedAnchorValue(endValue, snapshot.getMonthGrowth());
        double quarterValue = derivedAnchorValue(endValue, Math.max(snapshot.getMonthGrowth() * 1.8, snapshot.getMonthGrowth() + 3.2));
        double halfYearValue = derivedAnchorValue(endValue, Math.max(snapshot.getMonthGrowth() * 2.6, snapshot.getYearGrowth() * 0.55));
        double yearValue = derivedAnchorValue(endValue, snapshot.getYearGrowth());
        double threeYearValue = derivedAnchorValue(endValue, Math.max(snapshot.getYearGrowth() * 2.45, snapshot.getYearGrowth() + 18.0));

        List<Anchor> anchors = List.of(
            new Anchor(endDate.minusYears(3), round(threeYearValue)),
            new Anchor(endDate.minusYears(1), round(yearValue)),
            new Anchor(endDate.minusMonths(6), round(halfYearValue)),
            new Anchor(endDate.minusMonths(3), round(quarterValue)),
            new Anchor(endDate.minusMonths(1), round(monthValue)),
            new Anchor(endDate, round(endValue))
        );

        List<FundDtos.TrendPoint> generated = new ArrayList<>();
        for (int i = 0; i < anchors.size() - 1; i++) {
            Anchor start = anchors.get(i);
            Anchor finish = anchors.get(i + 1);
            generated.addAll(interpolateSegment(start, finish, i > 0));
        }

        if (recentHistory.isEmpty()) {
            return generated;
        }

        LocalDate firstActualDate = recentHistory.get(0).getTradeDate();
        List<FundDtos.TrendPoint> merged = generated.stream()
            .filter(point -> LocalDate.parse(point.date().substring(0, 10)).isBefore(firstActualDate))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        recentHistory.forEach(point -> merged.add(new FundDtos.TrendPoint(point.getTradeDate().toString(), round(point.getUnitNav()))));
        return merged;
    }

    private List<FundDtos.TrendPoint> interpolateSegment(Anchor start, Anchor finish, boolean skipFirst) {
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start.date(), finish.date());
        List<FundDtos.TrendPoint> points = new ArrayList<>();
        for (long day = skipFirst ? 1 : 0; day <= totalDays; day++) {
            double progress = totalDays == 0 ? 1.0 : day / (double) totalDays;
            double baseline = start.value() + (finish.value() - start.value()) * progress;
            double wave = Math.sin(progress * Math.PI * 3) * baseline * 0.012 * progress * (1 - progress);
            points.add(new FundDtos.TrendPoint(start.date().plusDays(day).toString(), round(baseline + wave)));
        }
        return points;
    }

    private double derivedAnchorValue(double endValue, double growthPercent) {
        return endValue / (1 + (growthPercent / 100.0));
    }

    private List<FundDtos.QuarterlyHoldingResponse> buildQuarterlyHoldings(String code) {
        switch (code) {
            case "000001":
                return List.of(
                quarterlyHolding("寒武纪", "688256", 4.62, 9.83, 0.72, null),
                quarterlyHolding("中芯国际", "688981", 3.48, 8.56, 0.38, null),
                quarterlyHolding("兆易创新", "603986", 2.91, 7.42, -0.16, null),
                quarterlyHolding("北方华创", "002371", 1.36, 6.78, 0.53, null),
                quarterlyHolding("海光信息", "688041", 5.04, 5.82, null, "新增")
            );
            case "005827":
                return List.of(
                quarterlyHolding("宁德时代", "300750", 1.18, 9.20, 0.24, null),
                quarterlyHolding("迈瑞医疗", "300760", 0.86, 8.35, -0.12, null),
                quarterlyHolding("美的集团", "000333", 0.63, 6.74, 0.41, null),
                quarterlyHolding("贵州茅台", "600519", -0.44, 5.94, -0.20, null),
                quarterlyHolding("中国平安", "601318", 0.57, 5.32, null, "新增")
            );
            case "161725":
                return List.of(
                quarterlyHolding("贵州茅台", "600519", 0.88, 15.12, 0.32, null),
                quarterlyHolding("山西汾酒", "600809", 1.43, 13.84, 0.27, null),
                quarterlyHolding("泸州老窖", "000568", 0.76, 12.55, -0.18, null),
                quarterlyHolding("五粮液", "000858", 0.52, 10.11, 0.09, null),
                quarterlyHolding("古井贡酒", "000596", 1.06, 6.28, null, "新增")
            );
            case "519674":
                return List.of(
                quarterlyHolding("新易盛", "300502", 1.75, 10.02, -0.22, null),
                quarterlyHolding("中际旭创", "300308", 4.18, 10.02, -0.08, null),
                quarterlyHolding("天孚通信", "300394", 6.49, 8.83, 0.94, null),
                quarterlyHolding("源杰科技", "688498", 7.14, 7.53, 2.34, null),
                quarterlyHolding("生益科技", "600183", 1.62, 7.30, null, "新增")
            );
            default:
                return List.of();
        }
    }

    private List<FundDtos.IndustryExposureResponse> buildIndustryDistribution(String code) {
        switch (code) {
            case "000001":
                return List.of(
                industryExposure("半导体", 38.42),
                industryExposure("算力基础设施", 27.66),
                industryExposure("软件服务", 19.05),
                industryExposure("其他", 14.87)
            );
            case "005827":
                return List.of(
                industryExposure("新能源", 24.18),
                industryExposure("医疗器械", 22.07),
                industryExposure("家电", 18.95),
                industryExposure("食品饮料", 17.33),
                industryExposure("其他", 17.47)
            );
            case "161725":
                return List.of(
                industryExposure("白酒", 82.15),
                industryExposure("啤酒", 7.32),
                industryExposure("调味品", 5.18),
                industryExposure("其他消费", 5.35)
            );
            case "519674":
                return List.of(
                industryExposure("通信", 51.92),
                industryExposure("电子", 29.05),
                industryExposure("其他", 19.03)
            );
            default:
                return List.of();
        }
    }

    private List<FundDtos.TopHoldingResponse> buildTopHoldings(String code) {
        switch (code) {
            case "000001":
                return List.of(
                topHolding("寒武纪", "688256", 4.62),
                topHolding("中芯国际", "688981", 3.48),
                topHolding("兆易创新", "603986", 2.91),
                topHolding("北方华创", "002371", 1.36),
                topHolding("海光信息", "688041", 5.04),
                topHolding("沪硅产业", "688126", 1.18),
                topHolding("金山办公", "688111", -0.42),
                topHolding("中际旭创", "300308", 2.24)
            );
            case "005827":
                return List.of(
                topHolding("宁德时代", "300750", 1.18),
                topHolding("迈瑞医疗", "300760", 0.86),
                topHolding("美的集团", "000333", 0.63),
                topHolding("贵州茅台", "600519", -0.44),
                topHolding("中国平安", "601318", 0.57),
                topHolding("招商银行", "600036", 0.38),
                topHolding("格力电器", "000651", 0.22),
                topHolding("海尔智家", "600690", 0.54)
            );
            case "161725":
                return List.of(
                topHolding("贵州茅台", "600519", 0.88),
                topHolding("山西汾酒", "600809", 1.43),
                topHolding("泸州老窖", "000568", 0.76),
                topHolding("五粮液", "000858", 0.52),
                topHolding("古井贡酒", "000596", 1.06),
                topHolding("迎驾贡酒", "603198", 0.64),
                topHolding("口子窖", "603589", -0.12)
            );
            case "519674":
                return List.of(
                topHolding("新易盛", "300502", 1.75),
                topHolding("中际旭创", "300308", 4.18),
                topHolding("天孚通信", "300394", 6.49),
                topHolding("源杰科技", "688498", 7.14),
                topHolding("生益科技", "600183", 1.62),
                topHolding("长飞光纤光缆", "601869", 12.12),
                topHolding("深南电路", "002916", 1.26),
                topHolding("仕佳光子", "688313", 6.96),
                topHolding("东山精密", "002384", 0.64),
                topHolding("德科立", "688205", 7.87)
            );
            default:
                return List.of();
        }
    }

    private FundDtos.QuarterlyHoldingResponse quarterlyHolding(
        String name,
        String symbol,
        double dailyChange,
        double positionRatio,
        Double previousChange,
        String changeLabel
    ) {
        return new FundDtos.QuarterlyHoldingResponse(
            name,
            symbol,
            round(dailyChange),
            round(positionRatio),
            previousChange == null ? null : round(previousChange),
            changeLabel
        );
    }

    private FundDtos.IndustryExposureResponse industryExposure(String name, double weight) {
        return new FundDtos.IndustryExposureResponse(name, round(weight));
    }

    private FundDtos.TopHoldingResponse topHolding(String name, String symbol, double dailyChange) {
        return new FundDtos.TopHoldingResponse(name, symbol, round(dailyChange));
    }

    private List<String> splitPipe(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(raw.split("\\|"));
    }

    private record Anchor(LocalDate date, double value) {
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
