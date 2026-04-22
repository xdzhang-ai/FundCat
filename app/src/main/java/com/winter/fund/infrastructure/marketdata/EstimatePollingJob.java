package com.winter.fund.infrastructure.marketdata;

/**
 * 估值轮询调度文件，当前只保留盘中估值轮询时钟入口。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EstimatePollingJob {

    private static final Logger log = LoggerFactory.getLogger(EstimatePollingJob.class);

    private final MarketDataProperties marketDataProperties;
    private final FundEstimateRepository fundEstimateRepository;

    public EstimatePollingJob(
        MarketDataProperties marketDataProperties,
        FundEstimateRepository fundEstimateRepository
    ) {
        this.marketDataProperties = marketDataProperties;
        this.fundEstimateRepository = fundEstimateRepository;
    }

    /**
     * 轮询estimates。
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @SchedulerLock(name = "estimatePollingJob", lockAtMostFor = "PT55S")
    public void pollEstimates() {
        ZonedDateTime now = now();
        if (!isWithinWindow(now.toLocalTime(), marketDataProperties.getEstimateTradingStart(), marketDataProperties.getEstimateTradingEnd())) {
            return;
        }
        if (!isDue(now.getMinute(), marketDataProperties.getEstimatePollIntervalMinutes())) {
            return;
        }
        log.info(
            "Estimate polling tick, at={}, trackedFunds={}",
            now,
            fundEstimateRepository.count()
        );
    }

    /**
     * 返回now结果。
     */
    private ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.of(marketDataProperties.getTimezone()));
    }

    /**
     * 判断是否withinwindow。
     */
    private boolean isWithinWindow(LocalTime current, LocalTime start, LocalTime end) {
        return !current.isBefore(start) && !current.isAfter(end);
    }

    /**
     * 判断是否due。
     */
    private boolean isDue(int minute, int intervalMinutes) {
        return intervalMinutes > 0 && minute % intervalMinutes == 0;
    }
}
