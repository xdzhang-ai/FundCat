package com.fundcat.api;

import com.fundcat.api.auth.UserEntity;
import com.fundcat.api.auth.UserRepository;
import com.fundcat.api.fund.FundEntity;
import com.fundcat.api.fund.FundEstimateEntity;
import com.fundcat.api.fund.FundEstimateRepository;
import com.fundcat.api.fund.FundRepository;
import com.fundcat.api.fund.FundSnapshotEntity;
import com.fundcat.api.fund.FundSnapshotRepository;
import com.fundcat.api.fund.NavHistoryEntity;
import com.fundcat.api.fund.NavHistoryRepository;
import com.fundcat.api.ops.FeatureFlagEntity;
import com.fundcat.api.ops.FeatureFlagRepository;
import com.fundcat.api.portfolio.AlertRuleEntity;
import com.fundcat.api.portfolio.AlertRuleRepository;
import com.fundcat.api.portfolio.HoldingLotEntity;
import com.fundcat.api.portfolio.HoldingLotRepository;
import com.fundcat.api.portfolio.ImportJobEntity;
import com.fundcat.api.portfolio.ImportJobRepository;
import com.fundcat.api.portfolio.PaperOrderEntity;
import com.fundcat.api.portfolio.PaperOrderRepository;
import com.fundcat.api.portfolio.PortfolioEntity;
import com.fundcat.api.portfolio.PortfolioRepository;
import com.fundcat.api.portfolio.SipPlanEntity;
import com.fundcat.api.portfolio.SipPlanRepository;
import com.fundcat.api.portfolio.WeeklyReportEntity;
import com.fundcat.api.portfolio.WeeklyReportRepository;
import com.fundcat.api.portfolio.WatchlistEntity;
import com.fundcat.api.portfolio.WatchlistRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final PortfolioRepository portfolioRepository;
    private final HoldingLotRepository holdingLotRepository;
    private final PaperOrderRepository paperOrderRepository;
    private final SipPlanRepository sipPlanRepository;
    private final ImportJobRepository importJobRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
        UserRepository userRepository,
        FeatureFlagRepository featureFlagRepository,
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        WatchlistRepository watchlistRepository,
        PortfolioRepository portfolioRepository,
        HoldingLotRepository holdingLotRepository,
        PaperOrderRepository paperOrderRepository,
        SipPlanRepository sipPlanRepository,
        ImportJobRepository importJobRepository,
        WeeklyReportRepository weeklyReportRepository,
        AlertRuleRepository alertRuleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.portfolioRepository = portfolioRepository;
        this.holdingLotRepository = holdingLotRepository;
        this.paperOrderRepository = paperOrderRepository;
        this.sipPlanRepository = sipPlanRepository;
        this.importJobRepository = importJobRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setId("user-demo-001");
        user.setDisplayName("Demo Analyst");
        user.setPhone("10000000000");
        user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        user.setRiskMode("research");
        user.setCreatedAt(now.minusDays(21));
        user.setUpdatedAt(now.minusDays(1));
        userRepository.save(user);

        featureFlagRepository.saveAll(List.of(
            featureFlag("estimate_reference", "盘中参考估值", true, "research", "展示盘中参考估值和涨跌幅，默认仅研究环境开启", "high", now.minusDays(14)),
            featureFlag("risk_signal_board", "风险提示榜单", true, "research", "展示加减仓、提醒和实验性榜单能力", "high", now.minusDays(10)),
            featureFlag("ocr_import", "OCR 导入", true, "research", "支持多平台持仓截图识别和校正导入", "medium", now.minusDays(8)),
            featureFlag("weekly_digest", "周报摘要", true, "research", "自动生成每周盈亏与风险提示摘要", "low", now.minusDays(4))
        ));

        List<FundEntity> funds = List.of(
            fund("000001", "华夏成长优选混合", "主动权益", "中高风险", "沪深300收益率 * 85% + 中债综合 * 15%", "AI 主线 + 高景气仓位", "AI|高弹性|观察池", 1.20, 0.20, 0.15, now.minusDays(40)),
            fund("005827", "中欧价值回报混合", "均衡混合", "中风险", "偏股混合基金指数", "均衡仓位 + 低波修复", "均衡|低波|核心仓", 1.35, 0.25, 0.12, now.minusDays(30)),
            fund("161725", "招商中证白酒指数", "行业指数", "高风险", "中证白酒指数", "弹性仓位 + 情绪修复", "行业轮动|消费|情绪", 0.90, 0.15, 0.10, now.minusDays(25)),
            fund("519674", "银河创新成长混合", "成长混合", "高风险", "创业板成长指数", "新质生产力 + 卫星仓", "半导体|成长|卫星仓", 1.30, 0.22, 0.15, now.minusDays(20))
        );
        fundRepository.saveAll(funds);

        fundSnapshotRepository.saveAll(List.of(
            snapshot("000001", 1.6234, 3.2244, 1.28, 4.86, 8.43, 19.36, 126.80, 0.72, 0.18, "寒武纪|兆易创新|中芯国际|北方华创", now.minusHours(2)),
            snapshot("005827", 2.1831, 4.0988, 0.63, 2.15, 4.92, 12.31, 98.42, 0.52, 0.28, "宁德时代|迈瑞医疗|贵州茅台|美的集团", now.minusHours(2)),
            snapshot("161725", 1.0081, 1.2251, -0.44, 1.06, 2.18, 8.34, 86.35, 0.91, 0.02, "贵州茅台|山西汾酒|泸州老窖|五粮液", now.minusHours(2)),
            snapshot("519674", 1.4483, 2.9388, 1.93, 6.61, 11.28, 24.93, 77.21, 0.83, 0.06, "中际旭创|新易盛|沪电股份|胜宏科技", now.minusHours(2))
        ));

        fundEstimateRepository.saveAll(List.of(
            estimate("000001", 1.6351, 2.01, true, "northbound inflow", now.minusMinutes(8)),
            estimate("005827", 2.1880, 0.22, true, "steady", now.minusMinutes(8)),
            estimate("161725", 1.0023, -0.57, true, "sector softness", now.minusMinutes(8)),
            estimate("519674", 1.4592, 2.43, true, "momentum", now.minusMinutes(8))
        ));

        navHistoryRepository.saveAll(List.of(
            nav("000001", 1.5388, 3.0088, 28),
            nav("000001", 1.5562, 3.0446, 21),
            nav("000001", 1.5791, 3.0930, 14),
            nav("000001", 1.6014, 3.1514, 7),
            nav("000001", 1.6234, 3.2244, 0),
            nav("005827", 2.1098, 3.9928, 28),
            nav("005827", 2.1268, 4.0116, 21),
            nav("005827", 2.1443, 4.0422, 14),
            nav("005827", 2.1652, 4.0738, 7),
            nav("005827", 2.1831, 4.0988, 0),
            nav("161725", 0.9822, 1.1938, 28),
            nav("161725", 0.9941, 1.2061, 21),
            nav("161725", 1.0015, 1.2148, 14),
            nav("161725", 1.0118, 1.2194, 7),
            nav("161725", 1.0081, 1.2251, 0),
            nav("519674", 1.3621, 2.8210, 28),
            nav("519674", 1.3886, 2.8615, 21),
            nav("519674", 1.4093, 2.8938, 14),
            nav("519674", 1.4288, 2.9140, 7),
            nav("519674", 1.4483, 2.9388, 0)
        ));

        watchlistRepository.saveAll(List.of(
            watchlist(user.getId(), "000001", "主账户 AI 进攻仓"),
            watchlist(user.getId(), "005827", "稳健底仓"),
            watchlist(user.getId(), "519674", "趋势增强观察")
        ));

        PortfolioEntity core = portfolio("portfolio-core", user.getId(), "核心组合", "支付宝模拟仓", 18000, now.minusDays(30));
        PortfolioEntity satellite = portfolio("portfolio-sat", user.getId(), "卫星组合", "截图导入样例", 9500, now.minusDays(16));
        portfolioRepository.saveAll(List.of(core, satellite));

        holdingLotRepository.saveAll(List.of(
            holding(core.getId(), "000001", "华夏成长优选混合", 8921.45, 1.5220, 14584.21, 1009.66, 0.47, "manual", false, now.minusHours(8)),
            holding(core.getId(), "005827", "中欧价值回报混合", 5130.12, 2.0321, 11204.70, 780.33, 0.36, "manual", false, now.minusHours(8)),
            holding(satellite.getId(), "519674", "银河创新成长混合", 3860.22, 1.3340, 5586.84, 438.20, 0.17, "ocr", true, now.minusHours(4)),
            holding(satellite.getId(), "161725", "招商中证白酒指数", 2500.55, 0.9682, 2520.41, 99.88, 0.12, "ocr", true, now.minusHours(4))
        ));

        paperOrderRepository.saveAll(List.of(
            order(core.getId(), "000001", "华夏成长优选混合", "BUY", 3000, 1942.34, 3.00, "FILLED", "补仓 AI 主线", now.minusDays(3)),
            order(core.getId(), "005827", "中欧价值回报混合", "BUY", 1800, 840.12, 1.80, "FILLED", "平衡波动", now.minusDays(8)),
            order(satellite.getId(), "519674", "银河创新成长混合", "SELL", 1200, 812.00, 1.20, "FILLED", "兑现部分弹性", now.minusDays(5))
        ));

        sipPlanRepository.saveAll(List.of(
            sip(core.getId(), "005827", "中欧价值回报混合", 800, "WEEKLY", now.plusDays(2)),
            sip(satellite.getId(), "519674", "银河创新成长混合", 500, "BIWEEKLY", now.plusDays(5))
        ));

        importJobRepository.saveAll(List.of(
            importJob(user.getId(), "示例导入", "DONE", "satellite-march.png", 2, now.minusDays(2)),
            importJob(user.getId(), "手工上传", "PROCESSING", "core-account-ocr.png", 0, now.minusHours(6))
        ));

        weeklyReportRepository.saveAll(List.of(
            report(user.getId(), "2026-W12", "本周组合继续由 AI 与成长方向领涨，核心组合跑赢沪深300 3.4pct。", 4.82, "000001", "白酒指数波动较大，建议保持卫星仓。", now.minusDays(2)),
            report(user.getId(), "2026-W11", "均衡仓修复明显，周中加仓后组合波动收敛。", 2.13, "005827", "若估值开关关闭，周报仍按净值口径生成。", now.minusDays(9))
        ));

        alertRuleRepository.saveAll(List.of(
            alert(user.getId(), "000001", "estimate_drawdown", -0.80, true, "INBOX"),
            alert(user.getId(), "519674", "weekly_return", 5.00, true, "INBOX"),
            alert(user.getId(), "161725", "sector_rebound", 2.00, false, "INBOX")
        ));
    }

    private FeatureFlagEntity featureFlag(String code, String name, boolean enabled, String environment, String description, String riskLevel, LocalDateTime createdAt) {
        FeatureFlagEntity entity = new FeatureFlagEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCode(code);
        entity.setName(name);
        entity.setEnabled(enabled);
        entity.setEnvironment(environment);
        entity.setDescription(description);
        entity.setRiskLevel(riskLevel);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private FundEntity fund(String code, String name, String category, String riskLevel, String benchmark, String tagLine, String tags, double managementFee, double custodyFee, double purchaseFee, LocalDateTime createdAt) {
        FundEntity entity = new FundEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setCategory(category);
        entity.setRiskLevel(riskLevel);
        entity.setBenchmark(benchmark);
        entity.setTagLine(tagLine);
        entity.setTags(tags);
        entity.setManagementFee(managementFee);
        entity.setCustodyFee(custodyFee);
        entity.setPurchaseFee(purchaseFee);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private FundSnapshotEntity snapshot(String code, double unitNav, double accumulatedNav, double dayGrowth, double weekGrowth, double monthGrowth, double yearGrowth, double assetSize, double stockRatio, double bondRatio, String holdings, LocalDateTime updatedAt) {
        FundSnapshotEntity entity = new FundSnapshotEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFundCode(code);
        entity.setNavDate(LocalDate.now());
        entity.setUnitNav(unitNav);
        entity.setAccumulatedNav(accumulatedNav);
        entity.setDayGrowth(dayGrowth);
        entity.setWeekGrowth(weekGrowth);
        entity.setMonthGrowth(monthGrowth);
        entity.setYearGrowth(yearGrowth);
        entity.setAssetSize(assetSize);
        entity.setStockRatio(stockRatio);
        entity.setBondRatio(bondRatio);
        entity.setTopHoldings(holdings);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private FundEstimateEntity estimate(String code, double estimatedNav, double estimatedGrowth, boolean referenceOnly, String sentiment, LocalDateTime estimatedAt) {
        FundEstimateEntity entity = new FundEstimateEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFundCode(code);
        entity.setEstimatedNav(estimatedNav);
        entity.setEstimatedGrowth(estimatedGrowth);
        entity.setReferenceOnly(referenceOnly);
        entity.setSentiment(sentiment);
        entity.setEstimatedAt(estimatedAt);
        return entity;
    }

    private NavHistoryEntity nav(String code, double unitNav, double accumulatedNav, int daysAgo) {
        NavHistoryEntity entity = new NavHistoryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFundCode(code);
        entity.setTradeDate(LocalDate.now().minusDays(daysAgo));
        entity.setUnitNav(unitNav);
        entity.setAccumulatedNav(accumulatedNav);
        return entity;
    }

    private WatchlistEntity watchlist(String userId, String fundCode, String note) {
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setNote(note);
        entity.setCreatedAt(LocalDateTime.now().minusDays(6));
        return entity;
    }

    private PortfolioEntity portfolio(String id, String userId, String name, String broker, double initialCash, LocalDateTime createdAt) {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName(name);
        entity.setBroker(broker);
        entity.setCurrency("CNY");
        entity.setInitialCash(initialCash);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private HoldingLotEntity holding(String portfolioId, String fundCode, String fundName, double shares, double averageCost, double currentValue, double pnl, double allocation, String source, boolean imported, LocalDateTime updatedAt) {
        HoldingLotEntity entity = new HoldingLotEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolioId);
        entity.setFundCode(fundCode);
        entity.setFundName(fundName);
        entity.setShares(shares);
        entity.setAverageCost(averageCost);
        entity.setCurrentValue(currentValue);
        entity.setPnl(pnl);
        entity.setAllocation(allocation);
        entity.setSource(source);
        entity.setImported(imported);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private PaperOrderEntity order(String portfolioId, String fundCode, String fundName, String orderType, double amount, double shares, double fee, String status, String note, LocalDateTime executedAt) {
        PaperOrderEntity entity = new PaperOrderEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolioId);
        entity.setFundCode(fundCode);
        entity.setFundName(fundName);
        entity.setOrderType(orderType);
        entity.setAmount(amount);
        entity.setShares(shares);
        entity.setFee(fee);
        entity.setStatus(status);
        entity.setNote(note);
        entity.setExecutedAt(executedAt);
        return entity;
    }

    private SipPlanEntity sip(String portfolioId, String fundCode, String fundName, double amount, String cadence, LocalDateTime nextRunAt) {
        SipPlanEntity entity = new SipPlanEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolioId);
        entity.setFundCode(fundCode);
        entity.setFundName(fundName);
        entity.setAmount(amount);
        entity.setCadence(cadence);
        entity.setNextRunAt(nextRunAt);
        entity.setActive(true);
        return entity;
    }

    private ImportJobEntity importJob(String userId, String platform, String status, String fileName, int recognizedHoldings, LocalDateTime createdAt) {
        ImportJobEntity entity = new ImportJobEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setSourcePlatform(platform);
        entity.setStatus(status);
        entity.setFileName(fileName);
        entity.setRecognizedHoldings(recognizedHoldings);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt.plusHours(1));
        return entity;
    }

    private WeeklyReportEntity report(String userId, String weekLabel, String summary, double returnRate, String bestFundCode, String riskNote, LocalDateTime createdAt) {
        WeeklyReportEntity entity = new WeeklyReportEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setWeekLabel(weekLabel);
        entity.setSummary(summary);
        entity.setReturnRate(returnRate);
        entity.setBestFundCode(bestFundCode);
        entity.setRiskNote(riskNote);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private AlertRuleEntity alert(String userId, String fundCode, String ruleType, double thresholdValue, boolean enabled, String channel) {
        AlertRuleEntity entity = new AlertRuleEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setRuleType(ruleType);
        entity.setThresholdValue(thresholdValue);
        entity.setEnabled(enabled);
        entity.setChannel(channel);
        return entity;
    }
}
