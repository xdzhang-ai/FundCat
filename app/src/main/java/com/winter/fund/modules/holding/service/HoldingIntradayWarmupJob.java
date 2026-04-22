package com.winter.fund.modules.holding.service;

/**
 * 持仓模块日间态预热任务，在开盘前把前一日持仓快照装入 Redis。
 */

import com.winter.fund.modules.ops.model.JobRunEntity;
import com.winter.fund.modules.ops.service.JobRunService;
import com.winter.fund.modules.ops.service.OpsService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldingIntradayWarmupJob {

    private static final Logger log = LoggerFactory.getLogger(HoldingIntradayWarmupJob.class);
    private static final String JOB_CODE = "holding_intraday_warmup";

    private final HoldingService holdingService;
    private final OpsService opsService;
    private final JobRunService jobRunService;
    private final Clock clock;

    public HoldingIntradayWarmupJob(HoldingService holdingService, OpsService opsService, JobRunService jobRunService) {
        this.holdingService = holdingService;
        this.opsService = opsService;
        this.jobRunService = jobRunService;
        this.clock = Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 每天 09:00 预热所有用户持仓的日间动态态。
     * 预热失败不会影响前端回源现算，但会让白天首页/持仓页失去 Redis 命中收益，因此保留任务记录便于排查。
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "${fundcat.market-data.timezone:${APP_TIMEZONE:${user.timezone:UTC}}}")
    @SchedulerLock(name = "holdingIntradayWarmupJob", lockAtMostFor = "PT15M")
    public void warmup() {
        LocalDate tradeDate = LocalDate.now(clock);
        String runKey = tradeDate.toString();
        String payloadSummary = "{\"tradeDate\":\"" + tradeDate + "\"}";
        if (!opsService.isEnabled("holding_intraday_warmup_enabled")) {
            jobRunService.completeSkipped(JOB_CODE, "JAVA_SCHEDULE", "CACHE_WARMUP", runKey, payloadSummary);
            return;
        }
        JobRunEntity jobRun = jobRunService.start(JOB_CODE, "JAVA_SCHEDULE", "CACHE_WARMUP", runKey, payloadSummary).orElse(null);
        if (jobRun == null) {
            log.info("Skipping holding intraday warmup because today's run already succeeded, tradeDate={}", tradeDate);
            return;
        }
        try {
            int warmed = holdingService.warmIntradayStates(tradeDate);
            jobRunService.completeSuccess(jobRun, warmed, warmed, 0, 0);
            log.info("Holding intraday warmup completed, tradeDate={}, warmed={}", tradeDate, warmed);
        } catch (Exception exception) {
            jobRunService.completeFailure(jobRun, exception.getMessage(), 0, 0, 1, 0);
            throw exception;
        }
    }
}
