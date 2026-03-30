package com.winter.fund.infrastructure.marketdata;

/**
 * 行情基础设施定时任务文件，负责轮询估值、净值等外部市场数据。
 */

import com.winter.fund.common.config.MarketDataProperties;
import com.winter.fund.modules.fund.repository.FundEstimateRepository;
import com.winter.fund.modules.portfolio.service.PortfolioService;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataPollingJob {

    private static final Logger log = LoggerFactory.getLogger(MarketDataPollingJob.class);

    private final MarketDataProperties marketDataProperties;
    private final List<FundMarketDataProvider> providers;
    private final FundEstimateRepository fundEstimateRepository;
    private final PortfolioService portfolioService;

    public MarketDataPollingJob(
        MarketDataProperties marketDataProperties,
        List<FundMarketDataProvider> providers,
        FundEstimateRepository fundEstimateRepository,
        PortfolioService portfolioService
    ) {
        this.marketDataProperties = marketDataProperties;
        this.providers = providers;
        this.fundEstimateRepository = fundEstimateRepository;
        this.portfolioService = portfolioService;
    }

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
            "Estimate polling tick, at={}, trackedFunds={}, providers={}",
            now,
            fundEstimateRepository.count(),
            providers.stream().map(FundMarketDataProvider::providerKey).toList()
        );
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 45000)
    @SchedulerLock(name = "confirmedNavPollingJob", lockAtMostFor = "PT55S")
    public void pollConfirmedNav() {
        ZonedDateTime now = now();
        if (!isWithinWindow(now.toLocalTime(), marketDataProperties.getNavPollStart(), marketDataProperties.getNavPollEnd())) {
            return;
        }
        if (!isDue(now.getMinute(), marketDataProperties.getNavPollIntervalMinutes())) {
            return;
        }
        int confirmedSip = portfolioService.confirmPendingSipOperations(now.toLocalDate());
        log.info("Confirmed nav polling tick, at={}, confirmedSipRecords={}", now, confirmedSip);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @SchedulerLock(name = "navRetryJob", lockAtMostFor = "PT55S")
    public void retryConfirmedNav() {
        ZonedDateTime now = now();
        if (!isWithinWindow(now.toLocalTime(), marketDataProperties.getNavBackupPollStart(), marketDataProperties.getNavBackupPollEnd())) {
            return;
        }
        if (!isDue(now.getMinute(), marketDataProperties.getNavPollIntervalMinutes())) {
            return;
        }
        int confirmedSip = portfolioService.confirmPendingSipOperations(now.toLocalDate().minusDays(1));
        log.info("Confirmed nav retry tick, at={}, confirmedSipRecords={}", now, confirmedSip);
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(ZoneId.of(marketDataProperties.getTimezone()));
    }

    private boolean isWithinWindow(LocalTime current, LocalTime start, LocalTime end) {
        return !current.isBefore(start) && !current.isAfter(end);
    }

    private boolean isDue(int minute, int intervalMinutes) {
        return intervalMinutes > 0 && minute % intervalMinutes == 0;
    }
}
