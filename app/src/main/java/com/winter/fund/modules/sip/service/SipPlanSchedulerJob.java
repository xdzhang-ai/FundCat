package com.winter.fund.modules.sip.service;

/**
 * 定投模块定时任务文件，负责执行后台异步调度逻辑。
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

    private final SipService sipService;
    private final MarketDataProperties marketDataProperties;

    public SipPlanSchedulerJob(SipService sipService, MarketDataProperties marketDataProperties) {
        this.sipService = sipService;
        this.marketDataProperties = marketDataProperties;
    }

    /**
     * 创建每日快照。
     * 这个调度点固定在 15:00，只负责把到期定投计划转换成“确认中”的执行记录，
     * 便于晚间拿到确认净值后再统一结算，不会在这里直接修改用户持仓。
     */
    @Scheduled(cron = "0 0 15 * * *", zone = "${fundcat.market-data.timezone:${APP_TIMEZONE:${user.timezone:UTC}}}")
    @SchedulerLock(name = "sipSnapshotJob", lockAtMostFor = "PT5M")
    public void createDailySnapshot() {
        LocalDateTime snapshotTime = LocalDateTime.now(ZoneId.of(marketDataProperties.getTimezone()));
        log.info("Starting sip snapshot job, snapshotTime={}, timezone={}", snapshotTime, marketDataProperties.getTimezone());
        int created = sipService.createSipSnapshot(snapshotTime);
        log.info("Sip snapshot job completed, snapshotTime={}, created={}", snapshotTime, created);
    }
}
