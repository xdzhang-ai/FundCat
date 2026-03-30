package com.winter.fund.modules.portfolio.service;

/**
 * 组合与交易模块定时任务文件，负责执行后台异步调度逻辑。
 */

import com.winter.fund.common.config.MarketDataProperties;
import java.time.LocalDateTime;
import java.time.ZoneId;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SipPlanSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(SipPlanSchedulerJob.class);

    private final PortfolioService portfolioService;
    private final MarketDataProperties marketDataProperties;

    public SipPlanSchedulerJob(PortfolioService portfolioService, MarketDataProperties marketDataProperties) {
        this.portfolioService = portfolioService;
        this.marketDataProperties = marketDataProperties;
    }

    @Scheduled(cron = "0 0 15 * * *", zone = "${fundcat.market-data.timezone:${APP_TIMEZONE:${user.timezone:UTC}}}")
    @SchedulerLock(name = "sipSnapshotJob", lockAtMostFor = "PT5M")
    public void createDailySnapshot() {
        LocalDateTime snapshotTime = LocalDateTime.now(ZoneId.of(marketDataProperties.getTimezone()));
        log.info("Starting sip snapshot job, snapshotTime={}, timezone={}", snapshotTime, marketDataProperties.getTimezone());
        int created = portfolioService.createSipSnapshot(snapshotTime);
        log.info("Sip snapshot job completed, snapshotTime={}, created={}", snapshotTime, created);
    }
}
