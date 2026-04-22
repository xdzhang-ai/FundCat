package com.winter.fund.common.async;

/**
 * 公共异步执行配置文件，负责定义后台任务使用的线程池。
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class MarketDataExecutorConfig {

    /**
     * 返回marketDataTaskScheduler结果。
     */
    @Bean
    public ThreadPoolTaskScheduler marketDataTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("fundcat-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        return scheduler;
    }
}
