package com.winter.fund.modules.ops.service;

/**
 * 运维模块服务，负责封装该模块的核心业务逻辑。
 */

import com.winter.fund.modules.ops.model.FeatureFlagEntity;
import com.winter.fund.modules.ops.model.JobRunEntity;
import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.common.config.RocketMqProperties;
import com.winter.fund.modules.ops.repository.FeatureFlagRepository;
import com.winter.fund.modules.ops.repository.JobRunRepository;
import com.winter.fund.common.exception.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpsService {

    private static final Logger log = LoggerFactory.getLogger(OpsService.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final JobRunRepository jobRunRepository;
    private final RocketMqProperties rocketMqProperties;

    public OpsService(
        FeatureFlagRepository featureFlagRepository,
        JobRunRepository jobRunRepository,
        RocketMqProperties rocketMqProperties
    ) {
        this.featureFlagRepository = featureFlagRepository;
        this.jobRunRepository = jobRunRepository;
        this.rocketMqProperties = rocketMqProperties;
    }

    /**
     * 获取summary。
     */
    public OpsDtos.OpsSummaryResponse getSummary() {
        return new OpsDtos.OpsSummaryResponse(getFeatureFlags(), getProviders());
    }

    /**
     * 获取特性开关列表。
     */
    public List<OpsDtos.FeatureFlagResponse> getFeatureFlags() {
        return featureFlagRepository.findAllByOrderByCreatedAtAsc().stream()
            .map(flag -> new OpsDtos.FeatureFlagResponse(
                flag.getCode(),
                flag.getName(),
                flag.isEnabled(),
                flag.getEnvironment(),
                flag.getDescription(),
                flag.getRiskLevel()
            ))
            .toList();
    }

    /**
     * 判断是否enabled。
     */
    public boolean isEnabled(String code) {
        return featureFlagRepository.findByCode(code)
            .map(FeatureFlagEntity::isEnabled)
            .orElse(false);
    }

    /**
     * 转换为ggle。
     */
    public OpsDtos.FeatureFlagResponse toggle(String code, boolean enabled) {
        log.info("Toggling feature flag, code={}, enabled={}", code, enabled);
        FeatureFlagEntity flag = featureFlagRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Feature flag not found"));
        flag.setEnabled(enabled);
        featureFlagRepository.save(flag);
        log.info("Feature flag persisted, code={}, enabled={}", flag.getCode(), flag.isEnabled());
        return new OpsDtos.FeatureFlagResponse(
            flag.getCode(),
            flag.getName(),
            flag.isEnabled(),
            flag.getEnvironment(),
            flag.getDescription(),
            flag.getRiskLevel()
        );
    }

    /**
     * 获取提供方列表。
     */
    public List<OpsDtos.ProviderStatusResponse> getProviders() {
        return List.of(new OpsDtos.ProviderStatusResponse(
            "python-fund-data-service",
            rocketMqProperties.isEnabled() ? "rocketmq-connected" : "waiting-rocketmq",
            rocketMqProperties.isEnabled()
                ? "Fund nav ingestion is produced by the Python data service and consumed in Java through RocketMQ ready events."
                : "Fund nav ingestion is produced by the Python data service. RocketMQ is configured as the target event bus but is not enabled yet."
        ));
    }

    /**
     * 获取最近任务运行记录。
     */
    public List<OpsDtos.JobRunResponse> getRecentJobRuns() {
        return jobRunRepository.findTop50ByOrderByStartedAtDesc().stream().map(this::toJobRunResponse).toList();
    }

    private OpsDtos.JobRunResponse toJobRunResponse(JobRunEntity entity) {
        return new OpsDtos.JobRunResponse(
            entity.getId(),
            entity.getJobCode(),
            entity.getJobSource(),
            entity.getJobType(),
            entity.getRunKey(),
            entity.getStatus(),
            entity.getPayloadSummary(),
            entity.getStatsTotal(),
            entity.getStatsSuccess(),
            entity.getStatsFailed(),
            entity.getStatsSkipped(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getDurationMs(),
            entity.getAttemptCount(),
            entity.getErrorMessage()
        );
    }
}
