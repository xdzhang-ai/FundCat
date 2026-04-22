package com.winter.fund.modules.fund.service;

/**
 * 基金模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.fund.model.FundDtos;
import com.winter.fund.DemoDataSeeder;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.repository.FundNavGrowthRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.fund.service.AkshareBridgeService;
import com.winter.fund.common.exception.NotFoundException;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.ops.service.OpsService;
import com.winter.fund.modules.watchlist.repository.WatchlistRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final FundEstimateRepository fundEstimateRepository;
    private final FundNavGrowthRepository fundNavGrowthRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final HoldingService holdingService;
    private final OpsService opsService;
    private final DemoDataSeeder demoDataSeeder;
    private final ObjectMapper objectMapper;
    private final AkshareBridgeService akshareBridgeService;

    public FundService(
        FundRepository fundRepository,
        FundEstimateRepository fundEstimateRepository,
        FundNavGrowthRepository fundNavGrowthRepository,
        NavHistoryRepository navHistoryRepository,
        WatchlistRepository watchlistRepository,
        HoldingService holdingService,
        OpsService opsService,
        DemoDataSeeder demoDataSeeder,
        ObjectMapper objectMapper,
        AkshareBridgeService akshareBridgeService
    ) {
        this.fundRepository = fundRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.fundNavGrowthRepository = fundNavGrowthRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.holdingService = holdingService;
        this.opsService = opsService;
        this.demoDataSeeder = demoDataSeeder;
        this.objectMapper = objectMapper;
        this.akshareBridgeService = akshareBridgeService;
    }

    /**
     * 搜索基金列表，并优先返回用户更关注的基金。
     */
    public List<FundDtos.FundCardResponse> search(String userId, String query) {
        log.info("Searching funds for userId={}, query={}", userId, query == null ? "" : query.trim());
        Set<String> watchlistCodes = watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(watchlist -> watchlist.getFundCode())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> holdingCodes = holdingService.getHeldFundCodes(userId);

        List<FundEntity> funds;
        if (query == null || query.isBlank()) {
            // 空查询时优先拼出“持仓 + 自选 + 最近基金”的候选池，让默认列表更贴近用户当前关注面。
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

    /**
     * 加载单只基金的完整详情视图。
     */
    public FundDtos.FundDetailResponse getDetail(String userId, String code) {
        log.info("Loading fund detail, userId={}, code={}", userId, code);
        FundEntity fund = fundRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        NavHistoryEntity latestNav = navHistoryRepository.findTopByFundCodeOrderByTradeDateDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund nav history not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        List<NavHistoryEntity> recentHistory = navHistoryRepository.findByFundCodeOrderByTradeDateAsc(code);
        if (shouldFetchRemoteHistory(recentHistory)) {
            recentHistory = fetchRemoteHistory(code, recentHistory);
        }
        List<FundDtos.TrendPoint> navHistory = buildLongRangeHistory(recentHistory);
        double displayedNav = estimateReferenceEnabled ? estimate.getEstimatedNav() : latestNav.getUnitNav();
        double displayedGrowth = estimateReferenceEnabled ? estimate.getEstimatedGrowth() : latestNav.getDayGrowth();
        // 估值模式下只在真实净值曲线尾部追加盘中估值点，避免修改前端图表的数据协议。
        List<FundDtos.TrendPoint> estimateHistory = estimateReferenceEnabled
            ? buildEstimateHistory(navHistory, latestNav.getTradeDate(), latestNav.getUnitNav(), estimate.getEstimatedNav())
            : navHistory;
        List<String> comparisonLabels = new ArrayList<>(splitPipe(fund.getTags()));
        if (comparisonLabels.isEmpty()) {
            comparisonLabels = List.of("基金研究", "净值跟踪");
        }
        List<FundDtos.QuarterlyHoldingResponse> quarterlyHoldings = opsService.isEnabled("quarterly_holdings")
            ? demoDataSeeder.getQuarterlyHoldings(code)
            : List.of();
        List<FundDtos.IndustryExposureResponse> industryDistribution = opsService.isEnabled("industry_distribution")
            ? demoDataSeeder.getIndustryDistribution(code)
            : List.of();
        boolean watchlisted = watchlistRepository.findByUserIdAndFundCode(userId, code).isPresent();
        boolean held = holdingService.getHeldFundCodes(userId).contains(code);
        fundNavGrowthRepository.findTopByFundCodeOrderByNavDateDesc(code).ifPresentOrElse(
            growth -> log.debug("Loaded nav growth summary, code={}, navDate={}", code, growth.getNavDate()),
            () -> log.debug("Nav growth summary missing, code={}", code)
        );
        return new FundDtos.FundDetailResponse(
            fund.getCode(),
            fund.getName(),
            splitPipe(fund.getTags()),
            round(latestNav.getUnitNav()),
            round(latestNav.getDayGrowth()),
            round(displayedNav),
            round(displayedGrowth),
            estimateReferenceEnabled && estimate.isReferenceOnly(),
            watchlisted,
            held,
            parseTopHoldings(fund.getTopHoldings(), code),
            navHistory,
            estimateHistory,
            comparisonLabels,
            quarterlyHoldings,
            industryDistribution
        );
    }

    /**
     * 获取当前用户对该基金的自选和持仓状态。
     */
    public FundDtos.FundUserStateResponse getUserState(String userId, String code) {
        log.info("Loading fund user state, userId={}, code={}", userId, code);
        fundRepository.findByCode(code).orElseThrow(() -> new NotFoundException("Fund not found"));
        boolean watchlisted = watchlistRepository.findByUserIdAndFundCode(userId, code).isPresent();
        boolean held = holdingService.getHeldFundCodes(userId).contains(code);
        return new FundDtos.FundUserStateResponse(code, watchlisted, held);
    }

    /**
     * 将基金实体转换为列表卡片响应。
     */
    private FundDtos.FundCardResponse toCard(FundEntity fund, Set<String> watchlistCodes, Set<String> holdingCodes) {
        NavHistoryEntity latestNav = navHistoryRepository.findTopByFundCodeOrderByTradeDateDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund nav history not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        return new FundDtos.FundCardResponse(
            fund.getCode(),
            fund.getName(),
            splitPipe(fund.getTags()),
            round(latestNav.getUnitNav()),
            round(latestNav.getDayGrowth()),
            round(estimateReferenceEnabled ? estimate.getEstimatedNav() : latestNav.getUnitNav()),
            round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : latestNav.getDayGrowth()),
            estimateReferenceEnabled && estimate.isReferenceOnly(),
            watchlistCodes.contains(fund.getCode()),
            holdingCodes.contains(fund.getCode())
        );
    }

    /**
     * 基于最新净值和盘中估值补出一段估值走势。
     */
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

    /**
     * 构建长周期净值历史。
     */
    private List<FundDtos.TrendPoint> buildLongRangeHistory(List<NavHistoryEntity> recentHistory) {
        return recentHistory.stream()
            .map(point -> new FundDtos.TrendPoint(point.getTradeDate().toString(), round(point.getUnitNav())))
            .toList();
    }

    /**
     * 本地库只保留最近 30 天净值时，详情页仍然需要 1y/3y 区间。
     * 这里在本地历史明显不足时回源 Python 数据服务，避免前端直接请求外部源。
     */
    private boolean shouldFetchRemoteHistory(List<NavHistoryEntity> history) {
        if (history.isEmpty()) {
            return true;
        }
        LocalDate oldest = history.get(0).getTradeDate();
        return oldest.isAfter(LocalDate.now().minusDays(30));
    }

    private List<NavHistoryEntity> fetchRemoteHistory(String code, List<NavHistoryEntity> fallbackHistory) {
        try {
            List<Map<String, Object>> unitHistory = akshareBridgeService.getUnitNavHistory(code).payload();
            List<Map<String, Object>> accHistory = akshareBridgeService.getAccNavHistory(code).payload();
            Map<String, Double> accumulatedByDate = accHistory.stream()
                .filter(item -> item.get("净值日期") != null && item.get("累计净值") != null)
                .collect(Collectors.toMap(
                    item -> String.valueOf(item.get("净值日期")),
                    item -> Double.parseDouble(String.valueOf(item.get("累计净值"))),
                    (left, right) -> right
                ));
            List<NavHistoryEntity> history = new ArrayList<>();
            for (Map<String, Object> item : unitHistory) {
                Object dateValue = item.get("净值日期");
                Object unitNavValue = item.get("单位净值");
                if (dateValue == null || unitNavValue == null) {
                    continue;
                }
                NavHistoryEntity entity = new NavHistoryEntity();
                entity.setId("remote-" + code + "-" + dateValue);
                entity.setFundCode(code);
                entity.setTradeDate(LocalDate.parse(String.valueOf(dateValue)));
                entity.setUnitNav(Double.parseDouble(String.valueOf(unitNavValue)));
                entity.setAccumulatedNav(accumulatedByDate.getOrDefault(String.valueOf(dateValue), entity.getUnitNav()));
                entity.setDayGrowth(parseDouble(item.get("日增长率")));
                history.add(entity);
            }
            history.sort(java.util.Comparator.comparing(NavHistoryEntity::getTradeDate));
            return history.isEmpty() ? fallbackHistory : history;
        } catch (Exception exception) {
            log.warn("Unable to fetch remote fund history, code={}, reason={}", code, exception.getMessage());
            return fallbackHistory;
        }
    }

    /**
     * 解析基金前十大持仓，优先读取持久化 JSON，缺失时回退到演示数据。
     */
    private List<FundDtos.TopHoldingResponse> parseTopHoldings(String raw, String fundCode) {
        if (raw == null || raw.isBlank()) {
            return demoDataSeeder.getTopHoldings(fundCode);
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<FundDtos.TopHoldingResponse>>() {
            });
        } catch (Exception exception) {
            log.warn("Unable to parse top holdings from fund record, code={}", fundCode);
            return demoDataSeeder.getTopHoldings(fundCode);
        }
    }

    /**
     * 拆分pipe。
     */
    private List<String> splitPipe(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(raw.split("\\|"));
    }

    /**
     * 返回round结果。
     */
    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0;
        }
        String raw = String.valueOf(value).replace("%", "").trim();
        if (raw.isBlank() || "--".equals(raw)) {
            return 0;
        }
        return Double.parseDouble(raw);
    }
}
