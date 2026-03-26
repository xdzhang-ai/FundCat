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
import java.util.ArrayList;
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
        user.setUsername("demo_analyst");
        user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        user.setRiskMode("research");
        user.setCreatedAt(now.minusDays(21));
        user.setUpdatedAt(now.minusDays(1));
        userRepository.save(user);

        featureFlagRepository.saveAll(List.of(
            featureFlag("estimate_reference", "盘中参考估值", true, "research", "展示盘中参考估值和涨跌幅，默认仅研究环境开启", "high", now.minusDays(14)),
            featureFlag("risk_signal_board", "风险提示榜单", true, "research", "展示加减仓、提醒和实验性榜单能力", "high", now.minusDays(10)),
            featureFlag("ocr_import", "OCR 导入", true, "research", "支持多平台持仓截图识别和校正导入", "medium", now.minusDays(8)),
            featureFlag("weekly_digest", "周报摘要", true, "research", "自动生成每周盈亏与风险提示摘要", "low", now.minusDays(4)),
            featureFlag("quarterly_holdings", "上季度持仓", true, "research", "展示基金上季度披露的主要股票持仓", "medium", now.minusDays(3)),
            featureFlag("industry_distribution", "行业分布", true, "research", "展示基金股票仓位对应的行业分布", "medium", now.minusDays(2))
        ));

        List<FundSeed> fundSeeds = List.of(
            seed("000001", "华夏成长优选混合", "主动权益", "中高风险", "沪深300收益率 * 85% + 中债综合 * 15%", "AI 主线 + 高景气仓位", "AI|高弹性|观察池", 1.20, 0.20, 0.15, 1.6234, 3.2244, 1.28, 4.86, 8.43, 19.36, 126.80, 0.72, 0.18, "寒武纪|兆易创新|中芯国际|北方华创", 1.6351, 2.01, "northbound inflow", now.minusDays(40)),
            seed("005827", "中欧价值回报混合", "均衡混合", "中风险", "偏股混合基金指数", "均衡仓位 + 低波修复", "均衡|低波|核心仓", 1.35, 0.25, 0.12, 2.1831, 4.0988, 0.63, 2.15, 4.92, 12.31, 98.42, 0.52, 0.28, "宁德时代|迈瑞医疗|贵州茅台|美的集团", 2.1880, 0.22, "steady", now.minusDays(30)),
            seed("161725", "招商中证白酒指数", "行业指数", "高风险", "中证白酒指数", "弹性仓位 + 情绪修复", "行业轮动|消费|情绪", 0.90, 0.15, 0.10, 1.0081, 1.2251, -0.44, 1.06, 2.18, 8.34, 86.35, 0.91, 0.02, "贵州茅台|山西汾酒|泸州老窖|五粮液", 1.0023, -0.57, "sector softness", now.minusDays(25)),
            seed("519674", "银河创新成长混合", "成长混合", "高风险", "创业板成长指数", "新质生产力 + 卫星仓", "半导体|成长|卫星仓", 1.30, 0.22, 0.15, 1.4483, 2.9388, 1.93, 6.61, 11.28, 24.93, 77.21, 0.83, 0.06, "中际旭创|新易盛|沪电股份|胜宏科技", 1.4592, 2.43, "momentum", now.minusDays(20)),
            seed("002190", "富国数字经济混合", "成长混合", "高风险", "中证数字经济主题指数", "算力链 + 数字经济", "数字经济|算力|热门", 1.28, 0.22, 0.15, 1.3865, 2.6681, 2.16, 5.42, 12.06, 26.48, 68.23, 0.81, 0.05, "浪潮信息|紫光股份|中科曙光|润泽科技", 1.3947, 2.58, "hot demand", now.minusDays(19)),
            seed("004851", "广发医疗保健股票", "行业主题", "高风险", "中证医药卫生指数", "创新药 + 医疗器械", "医药|器械|修复", 1.45, 0.25, 0.15, 2.0214, 3.1142, 1.14, 3.66, 7.21, 15.88, 59.84, 0.88, 0.01, "迈瑞医疗|爱美客|药明康德|联影医疗", 2.0288, 1.38, "healthcare rebound", now.minusDays(18)),
            seed("003095", "中欧消费主题股票", "行业主题", "中高风险", "中证主要消费指数", "白马消费 + 修复", "消费|品牌|底仓", 1.36, 0.24, 0.12, 1.8126, 3.0208, 0.58, 1.84, 4.06, 10.72, 72.16, 0.74, 0.08, "贵州茅台|美的集团|海尔智家|伊利股份", 1.8162, 0.74, "consumer stability", now.minusDays(17)),
            seed("006113", "南方信息创新混合", "成长混合", "高风险", "中证信息技术应用创新产业指数", "信创 + 自主可控", "信创|软件|自主可控", 1.42, 0.23, 0.15, 1.5677, 2.7054, 1.72, 4.18, 9.44, 21.37, 44.75, 0.79, 0.03, "金山办公|深信服|中科创达|用友网络", 1.5745, 1.95, "software strength", now.minusDays(16)),
            seed("001875", "景顺长城环保优势股票", "行业主题", "高风险", "中证环保产业指数", "新能源设备 + 公用事业", "环保|新能源|趋势", 1.31, 0.20, 0.12, 1.4471, 2.4822, 0.92, 2.56, 5.74, 13.28, 48.32, 0.71, 0.12, "阳光电源|隆基绿能|宁德时代|三峡能源", 1.4522, 1.08, "green utility", now.minusDays(15)),
            seed("010011", "易方达高端制造混合", "成长混合", "高风险", "中证高端装备制造指数", "高端制造 + AI 终端", "制造|机器人|AI终端", 1.18, 0.18, 0.10, 1.7358, 2.9880, 1.46, 3.92, 8.16, 18.74, 88.64, 0.82, 0.04, "汇川技术|埃斯顿|工业富联|立讯精密", 1.7429, 1.84, "manufacturing bid", now.minusDays(14)),
            seed("002621", "华夏新兴消费混合", "均衡混合", "中风险", "中证内地消费主题指数", "内需复苏 + 渠道改善", "内需|消费|均衡", 1.25, 0.20, 0.10, 1.5092, 2.6313, 0.44, 1.58, 3.92, 11.21, 51.43, 0.62, 0.16, "珀莱雅|安井食品|海天味业|青岛啤酒", 1.5121, 0.66, "domestic recovery", now.minusDays(13)),
            seed("008903", "交银科技创新灵活配置", "成长混合", "高风险", "中证科技龙头指数", "科技龙头 + 卫星仓", "科技|龙头|弹性", 1.40, 0.22, 0.15, 1.4266, 2.5178, 2.22, 5.68, 10.93, 23.66, 39.58, 0.78, 0.04, "中际旭创|立讯精密|澜起科技|沪电股份", 1.4368, 2.64, "tech momentum", now.minusDays(12)),
            seed("004997", "汇添富互联网核心资产", "成长混合", "高风险", "中证互联网指数", "互联网平台 + AI 应用", "互联网|平台|AI应用", 1.38, 0.24, 0.15, 1.2741, 2.1044, 1.06, 3.11, 6.28, 14.90, 33.84, 0.68, 0.10, "腾讯控股|美团-W|快手-W|阿里巴巴", 1.2816, 1.34, "platform repair", now.minusDays(11)),
            seed("017528", "工银红利优享混合", "均衡混合", "中低风险", "中证红利指数", "红利资产 + 低波防守", "红利|低波|防守", 1.10, 0.18, 0.08, 1.2847, 2.0876, 0.32, 1.12, 2.84, 8.76, 42.55, 0.51, 0.22, "长江电力|中国神华|中国海油|陕西煤业", 1.2863, 0.40, "dividend hold", now.minusDays(10)),
            seed("015641", "鹏华中证酒ETF联接", "行业指数", "高风险", "中证酒指数", "酒类指数 + 波段观察", "酒ETF|消费|波动", 0.78, 0.12, 0.08, 1.0315, 1.3884, -0.63, 0.88, 1.94, 6.42, 24.36, 0.93, 0.01, "贵州茅台|山西汾酒|泸州老窖|五粮液", 1.0240, -0.72, "liquor softness", now.minusDays(9)),
            seed("007070", "嘉实瑞虹三年定开混合", "偏债混合", "中低风险", "中债综合指数 * 70% + 沪深300 * 30%", "偏债增强 + 稳健底仓", "偏债|稳健|增强", 0.95, 0.18, 0.08, 1.2043, 1.7826, 0.18, 0.74, 2.01, 6.68, 57.72, 0.23, 0.63, "招商银行|宁德时代|中国平安|邮储银行", 1.2051, 0.21, "balanced carry", now.minusDays(8)),
            seed("001632", "天弘创业板ETF联接", "指数增强", "高风险", "创业板指数", "创业板宽基 + 高弹性", "创业板|宽基|弹性", 0.60, 0.10, 0.05, 1.8841, 2.6104, 1.84, 4.92, 9.74, 20.16, 112.37, 0.95, 0.00, "宁德时代|迈瑞医疗|东方财富|汇川技术", 1.8978, 2.06, "growth beta", now.minusDays(7)),
            seed("012348", "博时新能源主题混合", "行业主题", "高风险", "中证新能源指数", "储能链 + 整车弹性", "新能源|储能|高弹性", 1.36, 0.24, 0.15, 1.2649, 2.0032, 1.22, 3.18, 7.66, 16.29, 46.18, 0.84, 0.03, "宁德时代|阳光电源|比亚迪|亿纬锂能", 1.2711, 1.56, "energy rebound", now.minusDays(6)),
            seed("006327", "招商产业债券A", "债券型", "低风险", "中债综合财富指数", "纯债增强 + 票息策略", "债券|低波|票息", 0.35, 0.10, 0.00, 1.1264, 1.4566, 0.05, 0.24, 0.82, 3.24, 63.42, 0.01, 0.91, "国开债|政策性金融债|高等级信用债", 1.1268, 0.05, "bond carry", now.minusDays(5)),
            seed("011609", "华安沪港深外延增长", "灵活配置", "中高风险", "沪深300收益率 * 60% + 恒生指数 * 20% + 中债综合 * 20%", "港股互联网 + A股制造", "沪港深|外延|成长", 1.50, 0.25, 0.15, 1.3186, 2.1151, 0.96, 2.42, 5.11, 12.04, 37.58, 0.66, 0.09, "腾讯控股|美团-W|立讯精密|比亚迪电子", 1.3242, 1.18, "southbound repair", now.minusDays(4))
        );

        fundRepository.saveAll(fundSeeds.stream().map(seed -> fund(
            seed.code(), seed.name(), seed.category(), seed.riskLevel(), seed.benchmark(), seed.tagLine(), seed.tags(),
            seed.managementFee(), seed.custodyFee(), seed.purchaseFee(), seed.createdAt()
        )).toList());

        fundSnapshotRepository.saveAll(fundSeeds.stream().map(seed -> snapshot(
            seed.code(), seed.unitNav(), seed.accumulatedNav(), seed.dayGrowth(), seed.weekGrowth(), seed.monthGrowth(),
            seed.yearGrowth(), seed.assetSize(), seed.stockRatio(), seed.bondRatio(), seed.topHoldings(), now.minusHours(2)
        )).toList());

        fundEstimateRepository.saveAll(fundSeeds.stream().map(seed -> estimate(
            seed.code(), seed.estimatedNav(), seed.estimatedGrowth(), true, seed.sentiment(), now.minusMinutes(8)
        )).toList());

        List<NavHistoryEntity> navHistory = new ArrayList<>();
        fundSeeds.forEach(seed -> navHistory.addAll(buildNavSeries(seed)));
        navHistoryRepository.saveAll(navHistory);

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

    private FundSeed seed(
        String code,
        String name,
        String category,
        String riskLevel,
        String benchmark,
        String tagLine,
        String tags,
        double managementFee,
        double custodyFee,
        double purchaseFee,
        double unitNav,
        double accumulatedNav,
        double dayGrowth,
        double weekGrowth,
        double monthGrowth,
        double yearGrowth,
        double assetSize,
        double stockRatio,
        double bondRatio,
        String topHoldings,
        double estimatedNav,
        double estimatedGrowth,
        String sentiment,
        LocalDateTime createdAt
    ) {
        return new FundSeed(
            code,
            name,
            category,
            riskLevel,
            benchmark,
            tagLine,
            tags,
            managementFee,
            custodyFee,
            purchaseFee,
            unitNav,
            accumulatedNav,
            dayGrowth,
            weekGrowth,
            monthGrowth,
            yearGrowth,
            assetSize,
            stockRatio,
            bondRatio,
            topHoldings,
            estimatedNav,
            estimatedGrowth,
            sentiment,
            createdAt
        );
    }

    private List<NavHistoryEntity> buildNavSeries(FundSeed seed) {
        return List.of(
            nav(seed.code(), roundNav(seed.unitNav() * 0.947), roundNav(seed.accumulatedNav() * 0.934), 28),
            nav(seed.code(), roundNav(seed.unitNav() * 0.962), roundNav(seed.accumulatedNav() * 0.951), 21),
            nav(seed.code(), roundNav(seed.unitNav() * 0.978), roundNav(seed.accumulatedNav() * 0.971), 14),
            nav(seed.code(), roundNav(seed.unitNav() * 0.991), roundNav(seed.accumulatedNav() * 0.987), 7),
            nav(seed.code(), seed.unitNav(), seed.accumulatedNav(), 0)
        );
    }

    private double roundNav(double value) {
        return Math.round(value * 10000.0) / 10000.0;
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

    private record FundSeed(
        String code,
        String name,
        String category,
        String riskLevel,
        String benchmark,
        String tagLine,
        String tags,
        double managementFee,
        double custodyFee,
        double purchaseFee,
        double unitNav,
        double accumulatedNav,
        double dayGrowth,
        double weekGrowth,
        double monthGrowth,
        double yearGrowth,
        double assetSize,
        double stockRatio,
        double bondRatio,
        String topHoldings,
        double estimatedNav,
        double estimatedGrowth,
        String sentiment,
        LocalDateTime createdAt
    ) {}
}
