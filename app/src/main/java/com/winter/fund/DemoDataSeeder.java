package com.winter.fund;

/**
 * 启动时注入演示数据，便于本地联调和功能验证。
 */

import com.winter.fund.modules.auth.model.UserEntity;
import com.winter.fund.modules.auth.repository.UserRepository;
import com.winter.fund.modules.fund.model.FundDtos;
import com.winter.fund.modules.fund.model.FundEntity;
import com.winter.fund.modules.fund.model.FundEstimateEntity;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.fund.model.FundNavGrowthEntity;
import com.winter.fund.modules.fund.repository.FundNavGrowthRepository;
import com.winter.fund.modules.fund.repository.FundRepository;
import com.winter.fund.modules.fund.model.NavHistoryEntity;
import com.winter.fund.modules.fund.repository.NavHistoryRepository;
import com.winter.fund.modules.holding.model.UserFundDailyProfitSnapshotEntity;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundDailyProfitSnapshotRepository;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import com.winter.fund.modules.ops.model.FeatureFlagEntity;
import com.winter.fund.modules.ops.repository.FeatureFlagRepository;
import com.winter.fund.modules.sip.model.SipPlanEntity;
import com.winter.fund.modules.sip.repository.SipPlanRepository;
import com.winter.fund.modules.watchlist.model.WatchlistEntity;
import com.winter.fund.modules.watchlist.model.WatchlistGroupEntity;
import com.winter.fund.modules.watchlist.repository.WatchlistGroupRepository;
import com.winter.fund.modules.watchlist.repository.WatchlistRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String SIP_STATUS_ACTIVE = "生效";

    private final UserRepository userRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final FundRepository fundRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final FundNavGrowthRepository fundNavGrowthRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final SipPlanRepository sipPlanRepository;
    private final UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository;
    private final UserFundOperationRecordRepository operationRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock = Clock.systemDefaultZone();

    public DemoDataSeeder(
        UserRepository userRepository,
        FeatureFlagRepository featureFlagRepository,
        FundRepository fundRepository,
        FundEstimateRepository fundEstimateRepository,
        FundNavGrowthRepository fundNavGrowthRepository,
        NavHistoryRepository navHistoryRepository,
        WatchlistRepository watchlistRepository,
        WatchlistGroupRepository watchlistGroupRepository,
        SipPlanRepository sipPlanRepository,
        UserFundDailyProfitSnapshotRepository dailyProfitSnapshotRepository,
        UserFundOperationRecordRepository operationRecordRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.fundRepository = fundRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.fundNavGrowthRepository = fundNavGrowthRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.watchlistRepository = watchlistRepository;
        this.watchlistGroupRepository = watchlistGroupRepository;
        this.sipPlanRepository = sipPlanRepository;
        this.dailyProfitSnapshotRepository = dailyProfitSnapshotRepository;
        this.operationRecordRepository = operationRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 在开发环境首次启动时注入完整的演示数据。
     */
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Skipping demo data seeding because user data already exists");
            return;
        }

        log.info("Seeding demo backend data for FundCat");

        LocalDateTime now = LocalDateTime.now(clock);
        UserEntity user = new UserEntity();
        user.setId("user-demo-001");
        user.setDisplayName("Demo Analyst");
        user.setUsername("demo_analyst");
        user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        user.setRiskMode("research");
        user.setCreatedAt(now.minusDays(21));
        user.setUpdatedAt(now.minusDays(1));
        userRepository.save(user);

        // 先准备 feature flag 和基金主数据，后续自选、定投和持仓都会依赖这些基础记录。
        featureFlagRepository.saveAll(List.of(
            featureFlag("estimate_reference", "盘中参考估值", true, "research", "展示盘中参考估值和涨跌幅，默认仅研究环境开启", "high", now.minusDays(14)),
            featureFlag("risk_signal_board", "风险提示榜单", true, "research", "展示加减仓、提醒和实验性榜单能力", "high", now.minusDays(10)),
            featureFlag("ocr_import", "OCR 导入", true, "research", "支持多平台持仓截图识别和校正导入", "medium", now.minusDays(8)),
            featureFlag("weekly_digest", "周报摘要", true, "research", "自动生成每周盈亏与风险提示摘要", "low", now.minusDays(4)),
            featureFlag("quarterly_holdings", "上季度持仓", true, "research", "展示基金上季度披露的主要股票持仓", "medium", now.minusDays(3)),
            featureFlag("industry_distribution", "行业分布", true, "research", "展示基金股票仓位对应的行业分布", "medium", now.minusDays(2)),
            featureFlag("python_fund_ingest_enabled", "Python 基金采集", true, "research", "控制 Python 侧全量基金净值采集任务是否执行", "high", now.minusDays(2)),
            featureFlag("python_fund_backfill_enabled", "Python 净值补偿", true, "research", "控制 Python 侧前一交易日净值补偿轮询是否执行", "high", now.minusDays(1)),
            featureFlag("python_fund_publish_enabled", "Python 消息发布", true, "research", "控制 Python 侧 outbox 是否向 RocketMQ 发布批量基金净值消息", "high", now.minusDays(1)),
            featureFlag("holding_intraday_warmup_enabled", "持仓日间态预热", true, "research", "控制 09:00 将前一日持仓快照批量加载到 Redis 的任务", "medium", now.minusDays(1))
        ));

        List<FundSeed> fundSeeds = List.of(
            seed("000001", "华夏成长优选混合", "AI|高弹性|观察池", 1.6234, 3.2244, 1.28, 4.86, 8.43, 19.36, 126.80, 0.72, 0.18, "寒武纪|兆易创新|中芯国际|北方华创", 1.6351, 2.01, "northbound inflow", now.minusDays(40)),
            seed("005827", "中欧价值回报混合", "均衡|低波|核心仓", 2.1831, 4.0988, 0.63, 2.15, 4.92, 12.31, 98.42, 0.52, 0.28, "宁德时代|迈瑞医疗|贵州茅台|美的集团", 2.1880, 0.22, "steady", now.minusDays(30)),
            seed("161725", "招商中证白酒指数", "行业轮动|消费|情绪", 1.0081, 1.2251, -0.44, 1.06, 2.18, 8.34, 86.35, 0.91, 0.02, "贵州茅台|山西汾酒|泸州老窖|五粮液", 1.0023, -0.57, "sector softness", now.minusDays(25)),
            seed("519674", "银河创新成长混合", "半导体|成长|卫星仓", 1.4483, 2.9388, 1.93, 6.61, 11.28, 24.93, 77.21, 0.83, 0.06, "中际旭创|新易盛|沪电股份|胜宏科技", 1.4592, 2.43, "momentum", now.minusDays(20)),
            seed("002190", "富国数字经济混合", "数字经济|算力|热门", 1.3865, 2.6681, 2.16, 5.42, 12.06, 26.48, 68.23, 0.81, 0.05, "浪潮信息|紫光股份|中科曙光|润泽科技", 1.3947, 2.58, "hot demand", now.minusDays(19)),
            seed("004851", "广发医疗保健股票", "医药|器械|修复", 2.0214, 3.1142, 1.14, 3.66, 7.21, 15.88, 59.84, 0.88, 0.01, "迈瑞医疗|爱美客|药明康德|联影医疗", 2.0288, 1.38, "healthcare rebound", now.minusDays(18)),
            seed("003095", "中欧消费主题股票", "消费|品牌|底仓", 1.8126, 3.0208, 0.58, 1.84, 4.06, 10.72, 72.16, 0.74, 0.08, "贵州茅台|美的集团|海尔智家|伊利股份", 1.8162, 0.74, "consumer stability", now.minusDays(17)),
            seed("006113", "南方信息创新混合", "信创|软件|自主可控", 1.5677, 2.7054, 1.72, 4.18, 9.44, 21.37, 44.75, 0.79, 0.03, "金山办公|深信服|中科创达|用友网络", 1.5745, 1.95, "software strength", now.minusDays(16)),
            seed("001875", "景顺长城环保优势股票", "环保|新能源|趋势", 1.4471, 2.4822, 0.92, 2.56, 5.74, 13.28, 48.32, 0.71, 0.12, "阳光电源|隆基绿能|宁德时代|三峡能源", 1.4522, 1.08, "green utility", now.minusDays(15)),
            seed("010011", "易方达高端制造混合", "制造|机器人|AI终端", 1.7358, 2.9880, 1.46, 3.92, 8.16, 18.74, 88.64, 0.82, 0.04, "汇川技术|埃斯顿|工业富联|立讯精密", 1.7429, 1.84, "manufacturing bid", now.minusDays(14)),
            seed("002621", "华夏新兴消费混合", "内需|消费|均衡", 1.5092, 2.6313, 0.44, 1.58, 3.92, 11.21, 51.43, 0.62, 0.16, "珀莱雅|安井食品|海天味业|青岛啤酒", 1.5121, 0.66, "domestic recovery", now.minusDays(13)),
            seed("008903", "交银科技创新灵活配置", "科技|龙头|弹性", 1.4266, 2.5178, 2.22, 5.68, 10.93, 23.66, 39.58, 0.78, 0.04, "中际旭创|立讯精密|澜起科技|沪电股份", 1.4368, 2.64, "tech momentum", now.minusDays(12)),
            seed("004997", "汇添富互联网核心资产", "互联网|平台|AI应用", 1.2741, 2.1044, 1.06, 3.11, 6.28, 14.90, 33.84, 0.68, 0.10, "腾讯控股|美团-W|快手-W|阿里巴巴", 1.2816, 1.34, "platform repair", now.minusDays(11)),
            seed("017528", "工银红利优享混合", "红利|低波|防守", 1.2847, 2.0876, 0.32, 1.12, 2.84, 8.76, 42.55, 0.51, 0.22, "长江电力|中国神华|中国海油|陕西煤业", 1.2863, 0.40, "dividend hold", now.minusDays(10)),
            seed("015641", "鹏华中证酒ETF联接", "酒ETF|消费|波动", 1.0315, 1.3884, -0.63, 0.88, 1.94, 6.42, 24.36, 0.93, 0.01, "贵州茅台|山西汾酒|泸州老窖|五粮液", 1.0240, -0.72, "liquor softness", now.minusDays(9)),
            seed("007070", "嘉实瑞虹三年定开混合", "偏债|稳健|增强", 1.2043, 1.7826, 0.18, 0.74, 2.01, 6.68, 57.72, 0.23, 0.63, "招商银行|宁德时代|中国平安|邮储银行", 1.2051, 0.21, "balanced carry", now.minusDays(8)),
            seed("001632", "天弘创业板ETF联接", "创业板|宽基|弹性", 1.8841, 2.6104, 1.84, 4.92, 9.74, 20.16, 112.37, 0.95, 0.00, "宁德时代|迈瑞医疗|东方财富|汇川技术", 1.8978, 2.06, "growth beta", now.minusDays(7)),
            seed("012348", "博时新能源主题混合", "新能源|储能|高弹性", 1.2649, 2.0032, 1.22, 3.18, 7.66, 16.29, 46.18, 0.84, 0.03, "宁德时代|阳光电源|比亚迪|亿纬锂能", 1.2711, 1.56, "energy rebound", now.minusDays(6)),
            seed("006327", "招商产业债券A", "债券|低波|票息", 1.1264, 1.4566, 0.05, 0.24, 0.82, 3.24, 63.42, 0.01, 0.91, "国开债|政策性金融债|高等级信用债", 1.1268, 0.05, "bond carry", now.minusDays(5)),
            seed("011609", "华安沪港深外延增长", "沪港深|外延|成长", 1.3186, 2.1151, 0.96, 2.42, 5.11, 12.04, 37.58, 0.66, 0.09, "腾讯控股|美团-W|立讯精密|比亚迪电子", 1.3242, 1.18, "southbound repair", now.minusDays(4))
        );

        fundRepository.saveAll(fundSeeds.stream().map(seed -> fund(
            seed.code(), seed.name(), seed.tags(), seed.topHoldings(), seed.createdAt()
        )).toList());

        fundNavGrowthRepository.saveAll(fundSeeds.stream().map(seed -> navGrowth(
            seed.code(), seed.weekGrowth(), seed.monthGrowth(), seed.yearGrowth(), now.minusHours(2)
        )).toList());

        fundEstimateRepository.saveAll(fundSeeds.stream().map(seed -> estimate(
            seed.code(), seed.estimatedNav(), seed.estimatedGrowth(), true, seed.sentiment(), now.minusMinutes(8)
        )).toList());

        List<NavHistoryEntity> navHistory = new ArrayList<>();
        fundSeeds.forEach(seed -> navHistory.addAll(buildNavSeries(seed)));
        navHistoryRepository.saveAll(navHistory);

        // 先初始化用户分组，再让自选条目通过 groupId 关联到对应分组。
        List<WatchlistGroupEntity> watchlistGroups = watchlistGroupRepository.saveAll(List.of(
            watchlistGroup(user.getId(), "全部"),
            watchlistGroup(user.getId(), "成长进攻"),
            watchlistGroup(user.getId(), "稳健配置"),
            watchlistGroup(user.getId(), "行业主题")
        ));
        Map<String, WatchlistGroupEntity> watchlistGroupByName = watchlistGroups.stream()
            .collect(Collectors.toMap(WatchlistGroupEntity::getGroupName, Function.identity()));

        watchlistRepository.saveAll(List.of(
            watchlist(user.getId(), "000001", "主账户 AI 进攻仓", watchlistGroupByName.get("成长进攻").getId()),
            watchlist(user.getId(), "005827", "稳健底仓", watchlistGroupByName.get("稳健配置").getId()),
            watchlist(user.getId(), "519674", "趋势增强观察", watchlistGroupByName.get("行业主题").getId())
        ));

        SipPlanEntity coreSip = sip(user.getId(), "portfolio-current", "005827", "中欧价值回报混合", 800, "WEEKLY", now.plusDays(2), 0.0015, SIP_STATUS_ACTIVE);
        SipPlanEntity satelliteSip = sip(user.getId(), "portfolio-current", "519674", "银河创新成长混合", 500, "BIWEEKLY", now.plusDays(5), 0.0010, SIP_STATUS_ACTIVE);
        sipPlanRepository.saveAll(List.of(coreSip, satelliteSip));

        // 最后再写持仓、操作记录和收益快照，确保它们依赖的基金数据已经就绪。
        seedUnifiedHoldings(user.getId(), fundSeeds, coreSip, satelliteSip, now);
    }

    /**
     * 返回指定基金的季度持仓演示数据。
     */
    public List<FundDtos.QuarterlyHoldingResponse> getQuarterlyHoldings(String code) {
        return switch (code) {
            case "000001" -> List.of(
                quarterlyHolding("寒武纪", "688256", 4.62, 9.83, 0.72, null),
                quarterlyHolding("中芯国际", "688981", 3.48, 8.56, 0.38, null),
                quarterlyHolding("兆易创新", "603986", 2.91, 7.42, -0.16, null),
                quarterlyHolding("北方华创", "002371", 1.36, 6.78, 0.53, null),
                quarterlyHolding("海光信息", "688041", 5.04, 5.82, null, "新增")
            );
            case "005827" -> List.of(
                quarterlyHolding("宁德时代", "300750", 1.18, 9.20, 0.24, null),
                quarterlyHolding("迈瑞医疗", "300760", 0.86, 8.35, -0.12, null),
                quarterlyHolding("美的集团", "000333", 0.63, 6.74, 0.41, null),
                quarterlyHolding("贵州茅台", "600519", -0.44, 5.94, -0.20, null),
                quarterlyHolding("中国平安", "601318", 0.57, 5.32, null, "新增")
            );
            case "161725" -> List.of(
                quarterlyHolding("贵州茅台", "600519", 0.88, 15.12, 0.32, null),
                quarterlyHolding("山西汾酒", "600809", 1.43, 13.84, 0.27, null),
                quarterlyHolding("泸州老窖", "000568", 0.76, 12.55, -0.18, null),
                quarterlyHolding("五粮液", "000858", 0.52, 10.11, 0.09, null),
                quarterlyHolding("古井贡酒", "000596", 1.06, 6.28, null, "新增")
            );
            case "519674" -> List.of(
                quarterlyHolding("新易盛", "300502", 1.75, 10.02, -0.22, null),
                quarterlyHolding("中际旭创", "300308", 4.18, 10.02, -0.08, null),
                quarterlyHolding("天孚通信", "300394", 6.49, 8.83, 0.94, null),
                quarterlyHolding("源杰科技", "688498", 7.14, 7.53, 2.34, null),
                quarterlyHolding("生益科技", "600183", 1.62, 7.30, null, "新增")
            );
            default -> List.of();
        };
    }

    /**
     * 返回指定基金的行业分布演示数据。
     */
    public List<FundDtos.IndustryExposureResponse> getIndustryDistribution(String code) {
        return switch (code) {
            case "000001" -> List.of(
                industryExposure("半导体", 38.42),
                industryExposure("算力基础设施", 27.66),
                industryExposure("软件服务", 19.05),
                industryExposure("其他", 14.87)
            );
            case "005827" -> List.of(
                industryExposure("新能源", 24.18),
                industryExposure("医疗器械", 22.07),
                industryExposure("家电", 18.95),
                industryExposure("食品饮料", 17.33),
                industryExposure("其他", 17.47)
            );
            case "161725" -> List.of(
                industryExposure("白酒", 82.15),
                industryExposure("啤酒", 7.32),
                industryExposure("调味品", 5.18),
                industryExposure("其他消费", 5.35)
            );
            case "519674" -> List.of(
                industryExposure("通信", 51.92),
                industryExposure("电子", 29.05),
                industryExposure("其他", 19.03)
            );
            default -> List.of();
        };
    }

    /**
     * 返回指定基金的前十大持仓演示数据。
     */
    public List<FundDtos.TopHoldingResponse> getTopHoldings(String code) {
        return switch (code) {
            case "000001" -> List.of(
                topHolding("寒武纪", "688256", 4.62),
                topHolding("中芯国际", "688981", 3.48),
                topHolding("兆易创新", "603986", 2.91),
                topHolding("北方华创", "002371", 1.36),
                topHolding("海光信息", "688041", 5.04),
                topHolding("沪硅产业", "688126", 1.18),
                topHolding("金山办公", "688111", -0.42),
                topHolding("中际旭创", "300308", 2.24)
            );
            case "005827" -> List.of(
                topHolding("宁德时代", "300750", 1.18),
                topHolding("迈瑞医疗", "300760", 0.86),
                topHolding("美的集团", "000333", 0.63),
                topHolding("贵州茅台", "600519", -0.44),
                topHolding("中国平安", "601318", 0.57),
                topHolding("招商银行", "600036", 0.38),
                topHolding("格力电器", "000651", 0.22),
                topHolding("海尔智家", "600690", 0.54)
            );
            case "161725" -> List.of(
                topHolding("贵州茅台", "600519", 0.88),
                topHolding("山西汾酒", "600809", 1.43),
                topHolding("泸州老窖", "000568", 0.76),
                topHolding("五粮液", "000858", 0.52),
                topHolding("古井贡酒", "000596", 1.06),
                topHolding("迎驾贡酒", "603198", 0.64),
                topHolding("口子窖", "603589", -0.12)
            );
            case "519674" -> List.of(
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
            default -> List.of();
        };
    }

    /**
     * 构建功能开关演示数据。
     */
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

    /**
     * 构建基金主表演示数据。
     */
    private FundEntity fund(String code, String name, String tags, String topHoldings, LocalDateTime createdAt) {
        FundEntity entity = new FundEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setTags(tags);
        entity.setTopHoldings(topHoldings);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    /**
     * 构建基金区间涨幅摘要演示数据。
     */
    private FundNavGrowthEntity navGrowth(String code, double weekGrowth, double monthGrowth, double yearGrowth, LocalDateTime updatedAt) {
        FundNavGrowthEntity entity = new FundNavGrowthEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFundCode(code);
        entity.setNavDate(LocalDate.now());
        entity.setWeekGrowth(weekGrowth);
        entity.setMonthGrowth(monthGrowth);
        entity.setYearGrowth(yearGrowth);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    /**
     * 构建基金估值快照演示数据。
     */
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

    /**
     * 构建基金种子对象，供后续批量派生其他演示数据。
     */
    private FundSeed seed(
        String code,
        String name,
        String tags,
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
            tags,
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

    /**
     * 为单只基金生成近 30 天净值历史。
     */
    private List<NavHistoryEntity> buildNavSeries(FundSeed seed) {
        List<NavHistoryEntity> series = new ArrayList<>();
        for (int daysAgo = 30; daysAgo >= 0; daysAgo--) {
            // 用基础趋势叠加轻微波动，生成更适合图表展示的演示序列。
            double progress = (30 - daysAgo) / 30.0;
            double baseline = seed.unitNav() * (0.94 + (0.06 * progress));
            double seasonalWave = Math.sin(progress * Math.PI * 3) * seed.unitNav() * 0.006;
            double unitNav = daysAgo == 0 ? seed.unitNav() : roundNav(baseline + seasonalWave);
            double accumulatedNav = daysAgo == 0
                ? seed.accumulatedNav()
                : roundNav(seed.accumulatedNav() - ((30 - daysAgo) * 0.008));
            series.add(nav(seed.code(), unitNav, accumulatedNav, daysAgo));
        }
        return series;
    }

    /**
     * 执行数值舍入净值。
     */
    private double roundNav(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    /**
     * 返回nav结果。
     */
    private NavHistoryEntity nav(String code, double unitNav, double accumulatedNav, int daysAgo) {
        NavHistoryEntity entity = new NavHistoryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFundCode(code);
        entity.setTradeDate(LocalDate.now().minusDays(daysAgo));
        entity.setUnitNav(unitNav);
        entity.setAccumulatedNav(accumulatedNav);
        entity.setDayGrowth(daysAgo == 0 ? 0 : roundPercent(((unitNav - accumulatedNav / Math.max(1.0, accumulatedNav)) * 10)));
        return entity;
    }

    /**
     * 返回watchlistGroup结果。
     */
    private WatchlistGroupEntity watchlistGroup(String userId, String groupName) {
        WatchlistGroupEntity entity = new WatchlistGroupEntity();
        entity.setId(userId + ":" + groupName);
        entity.setUserId(userId);
        entity.setGroupName(groupName);
        entity.setCreatedAt(LocalDateTime.now(clock).minusDays(7));
        return entity;
    }

    /**
     * 返回watchlist结果。
     */
    private WatchlistEntity watchlist(String userId, String fundCode, String note, String groupId) {
        WatchlistEntity entity = new WatchlistEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setNote(note);
        entity.setGroupId(groupId);
        entity.setCreatedAt(LocalDateTime.now(clock).minusDays(6));
        return entity;
    }

    /**
     * 返回sip结果。
     */
    private SipPlanEntity sip(String userId, String portfolioId, String fundCode, String fundName, double amount, String cadence, LocalDateTime nextRunAt, double feeRate, String status) {
        SipPlanEntity entity = new SipPlanEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPortfolioId(portfolioId);
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setFundName(fundName);
        entity.setAmount(amount);
        entity.setCadence(cadence);
        entity.setNextRunAt(nextRunAt);
        entity.setActive(SIP_STATUS_ACTIVE.equals(status));
        entity.setStatus(status);
        entity.setFeeRate(feeRate);
        entity.setCreatedAt(LocalDateTime.now(clock).minusDays(12));
        entity.setUpdatedAt(LocalDateTime.now(clock).minusDays(1));
        return entity;
    }

    /**
     * 初始化持仓、操作记录和日收益快照演示数据。
     */
    private void seedUnifiedHoldings(String userId, List<FundSeed> fundSeeds, SipPlanEntity coreSip, SipPlanEntity satelliteSip, LocalDateTime now) {
        Map<String, FundSeed> fundSeedMap = fundSeeds.stream().collect(Collectors.toMap(FundSeed::code, Function.identity()));
        List<UnifiedHoldingSeed> holdings = List.of(
            unifiedHolding(fundSeedMap.get("000001"), 8921.4500, 1.5220, now.minusHours(8)),
            unifiedHolding(fundSeedMap.get("005827"), 5130.1200, 2.0321, now.minusHours(8)),
            unifiedHolding(fundSeedMap.get("519674"), 3860.2200, 1.3340, now.minusHours(4)),
            unifiedHolding(fundSeedMap.get("161725"), 2500.5500, 0.9682, now.minusHours(4))
        );

        List<UserFundOperationRecordEntity> operations = new ArrayList<>();
        operations.add(operation(userId, "000001", "BUY", "MANUAL", "已执行", LocalDate.now().minusDays(3), 3000, 1942.34, 1.5445, 0.0010, null, "补仓 AI 主线", now.minusDays(3)));
        operations.add(operation(userId, "005827", "BUY", "MANUAL", "已执行", LocalDate.now().minusDays(8), 1800, 840.12, 2.1425, 0.0010, null, "平衡波动", now.minusDays(8)));
        operations.add(operation(userId, "519674", "SELL", "MANUAL", "已执行", LocalDate.now().minusDays(5), 1176.11, -812.0000, 1.4484, 0.0010, null, "兑现部分弹性", now.minusDays(5)));
        operations.add(operation(userId, "005827", "SIP_BUY", "SIP", "已执行", LocalDate.now().minusDays(7), 800, 376.2089, 2.1265, coreSip.getFeeRate(), coreSip.getId(), "历史定投已执行", now.minusDays(7)));
        operations.add(operation(userId, "519674", "SIP_BUY", "SIP", "确认中", LocalDate.now(), 500, 0, 0, satelliteSip.getFeeRate(), satelliteSip.getId(), "15:00 快照已生成", now.minusHours(1)));
        operationRecordRepository.saveAll(operations);

        List<UserFundDailyProfitSnapshotEntity> snapshots = new ArrayList<>();
        for (UnifiedHoldingSeed holding : holdings) {
            List<NavHistoryEntity> history = navHistoryRepository.findByFundCodeAndTradeDateBetweenOrderByTradeDateAsc(
                holding.fundCode(),
                LocalDate.now().minusDays(30),
                LocalDate.now()
            );
            Map<LocalDate, NavHistoryEntity> navByDate = history.stream().collect(Collectors.toMap(NavHistoryEntity::getTradeDate, Function.identity()));
            List<LocalDate> orderedDates = history.stream().map(NavHistoryEntity::getTradeDate).sorted().toList();
            for (int i = 0; i < orderedDates.size(); i++) {
                // 每个交易日都基于统一持仓状态推导一条收益快照，方便持仓页直接读取。
                LocalDate tradeDate = orderedDates.get(i);
                double nav = navByDate.get(tradeDate).getUnitNav();
                double previousNav = i == 0 ? nav : navByDate.get(orderedDates.get(i - 1)).getUnitNav();
                snapshots.add(snapshot(
                    userId,
                    holding.fundCode(),
                    tradeDate,
                    holding.shares(),
                    holding.averageCost(),
                    nav,
                    roundMoney(holding.shares() * nav),
                    roundMoney(holding.shares() * (nav - previousNav)),
                    roundMoney((holding.shares() * nav) - (holding.shares() * holding.averageCost())),
                    roundPercent((((holding.shares() * nav) - (holding.shares() * holding.averageCost())) / Math.max(holding.shares() * holding.averageCost(), 0.0001)) * 100),
                    holding.updatedAt()
                ));
            }
        }
        dailyProfitSnapshotRepository.saveAll(snapshots);
    }

    /**
     * 返回unifiedHolding结果。
     */
    private UnifiedHoldingSeed unifiedHolding(FundSeed seed, double shares, double averageCost, LocalDateTime updatedAt) {
        return new UnifiedHoldingSeed(seed.code(), seed.name(), shares, averageCost, updatedAt);
    }

    /**
     * 构建季度持仓条目。
     */
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
            roundPercent(dailyChange),
            roundPercent(positionRatio),
            previousChange == null ? null : roundPercent(previousChange),
            changeLabel
        );
    }

    /**
     * 构建行业分布条目。
     */
    private FundDtos.IndustryExposureResponse industryExposure(String name, double weight) {
        return new FundDtos.IndustryExposureResponse(name, roundPercent(weight));
    }

    /**
     * 构建前十大持仓条目。
     */
    private FundDtos.TopHoldingResponse topHolding(String name, String symbol, double dailyChange) {
        return new FundDtos.TopHoldingResponse(name, symbol, roundPercent(dailyChange));
    }

    private UserFundOperationRecordEntity operation(
        String userId,
        String fundCode,
        String operation,
        String source,
        String status,
        LocalDate tradeDate,
        double amount,
        double sharesDelta,
        double nav,
        double feeRate,
        String sipPlanId,
        String note,
        LocalDateTime createdAt
    ) {
        UserFundOperationRecordEntity entity = new UserFundOperationRecordEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setOperation(operation);
        entity.setSource(source);
        entity.setStatus(status);
        entity.setTradeDate(tradeDate);
        entity.setAmount(amount);
        entity.setSharesDelta(sharesDelta);
        entity.setNav(nav);
        entity.setFeeRate(feeRate);
        entity.setFeeAmount(roundMoney(amount * feeRate));
        entity.setSipPlanId(sipPlanId);
        entity.setNote(note);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt.plusMinutes(5));
        return entity;
    }

    private UserFundDailyProfitSnapshotEntity snapshot(
        String userId,
        String fundCode,
        LocalDate tradeDate,
        double shares,
        double averageCost,
        double nav,
        double marketValue,
        double dailyPnl,
        double totalPnl,
        double totalPnlRate,
        LocalDateTime updatedAt
    ) {
        UserFundDailyProfitSnapshotEntity entity = new UserFundDailyProfitSnapshotEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setFundCode(fundCode);
        entity.setTradeDate(tradeDate);
        entity.setShares(shares);
        entity.setAverageCost(averageCost);
        entity.setNav(nav);
        entity.setMarketValue(marketValue);
        entity.setDailyPnl(dailyPnl);
        entity.setTotalPnl(totalPnl);
        entity.setTotalPnlRate(totalPnlRate);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    /**
     * 执行数值舍入money。
     */
    private double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 执行数值舍入percent。
     */
    private double roundPercent(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private record FundSeed(
        String code,
        String name,
        String tags,
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

    private record UnifiedHoldingSeed(
        String fundCode,
        String fundName,
        double shares,
        double averageCost,
        LocalDateTime updatedAt
    ) {}
}
