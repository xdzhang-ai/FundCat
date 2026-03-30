package com.winter.fund.modules.holding.service;

/**
 * 持仓模块定时任务文件，负责执行后台异步调度逻辑。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import com.winter.fund.modules.holding.repository.UserFundOperationRecordRepository;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyProfitRebuildJob {

    private static final Logger log = LoggerFactory.getLogger(DailyProfitRebuildJob.class);

    private final UserFundOperationRecordRepository operationRecordRepository;
    private final HoldingService holdingService;
    private final MarketDataProperties marketDataProperties;

    public DailyProfitRebuildJob(
        UserFundOperationRecordRepository operationRecordRepository,
        HoldingService holdingService,
        MarketDataProperties marketDataProperties
    ) {
        this.operationRecordRepository = operationRecordRepository;
        this.holdingService = holdingService;
        this.marketDataProperties = marketDataProperties;
    }

    @Scheduled(cron = "0 50 23 * * *", zone = "${fundcat.market-data.timezone:${APP_TIMEZONE:${user.timezone:UTC}}}")
    @SchedulerLock(name = "dailyProfitRebuildJob", lockAtMostFor = "PT10M")
    public void rebuildTodaySnapshots() {
        LocalDate tradeDate = LocalDate.now(java.time.ZoneId.of(marketDataProperties.getTimezone()));
        Set<String> targets = new LinkedHashSet<>();
        log.info("Starting daily profit rebuild job, tradeDate={}, timezone={}", tradeDate, marketDataProperties.getTimezone());
        for (UserFundOperationRecordEntity record : operationRecordRepository.findByStatusAndTradeDateOrderByCreatedAtAsc("已执行", tradeDate)) {
            targets.add(record.getUserId() + "::" + record.getFundCode());
        }
        for (String target : targets) {
            String[] parts = target.split("::", 2);
            holdingService.rebuildSnapshotsFrom(parts[0], parts[1], tradeDate);
        }
        log.info("Daily profit rebuild completed, tradeDate={}, targets={}", tradeDate, targets.size());
    }
}
