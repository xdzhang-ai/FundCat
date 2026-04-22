package com.winter.fund.modules.ops.service;

import com.winter.fund.modules.ops.model.JobRunEntity;
import com.winter.fund.modules.ops.repository.JobRunRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRunService {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private final JobRunRepository jobRunRepository;
    private final Clock clock;

    public JobRunService(JobRunRepository jobRunRepository) {
        this.jobRunRepository = jobRunRepository;
        this.clock = Clock.systemDefaultZone();
    }

    /**
     * 开始一次支持按 runKey 去重的任务执行；若该 runKey 已成功则直接返回空，调用方可选择跳过主逻辑。
     */
    @Transactional
    public Optional<JobRunEntity> start(String jobCode, String jobSource, String jobType, String runKey, String payloadSummary) {
        Optional<JobRunEntity> existing = jobRunRepository.findByJobCodeAndRunKey(jobCode, runKey);
        if (existing.isPresent() && STATUS_SUCCESS.equals(existing.get().getStatus())) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        JobRunEntity entity = existing.orElseGet(JobRunEntity::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
            entity.setCreatedAt(now);
            entity.setAttemptCount(0);
        }
        entity.setJobCode(jobCode);
        entity.setJobSource(jobSource);
        entity.setJobType(jobType);
        entity.setRunKey(runKey);
        entity.setPayloadSummary(payloadSummary);
        entity.setStatus(STATUS_RUNNING);
        entity.setStartedAt(now);
        entity.setFinishedAt(null);
        entity.setDurationMs(null);
        entity.setErrorMessage(null);
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setUpdatedAt(now);
        return Optional.of(jobRunRepository.save(entity));
    }

    /**
     * 标记任务成功。
     */
    @Transactional
    public void completeSuccess(JobRunEntity entity, int total, int success, int failed, int skipped) {
        LocalDateTime now = LocalDateTime.now(clock);
        entity.setStatus(STATUS_SUCCESS);
        entity.setStatsTotal(total);
        entity.setStatsSuccess(success);
        entity.setStatsFailed(failed);
        entity.setStatsSkipped(skipped);
        entity.setFinishedAt(now);
        entity.setDurationMs(durationMillis(entity.getStartedAt(), now));
        entity.setUpdatedAt(now);
        jobRunRepository.save(entity);
    }

    /**
     * 标记任务跳过。
     */
    @Transactional
    public void completeSkipped(String jobCode, String jobSource, String jobType, String runKey, String payloadSummary) {
        LocalDateTime now = LocalDateTime.now(clock);
        JobRunEntity entity = jobRunRepository.findByJobCodeAndRunKey(jobCode, runKey).orElseGet(JobRunEntity::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
            entity.setCreatedAt(now);
            entity.setAttemptCount(1);
        }
        entity.setJobCode(jobCode);
        entity.setJobSource(jobSource);
        entity.setJobType(jobType);
        entity.setRunKey(runKey);
        entity.setPayloadSummary(payloadSummary);
        entity.setStatus(STATUS_SKIPPED);
        entity.setStatsSkipped(entity.getStatsSkipped() + 1);
        entity.setStartedAt(now);
        entity.setFinishedAt(now);
        entity.setDurationMs(0L);
        entity.setUpdatedAt(now);
        jobRunRepository.save(entity);
    }

    /**
     * 标记任务失败。
     */
    @Transactional
    public void completeFailure(JobRunEntity entity, String errorMessage, int total, int success, int failed, int skipped) {
        LocalDateTime now = LocalDateTime.now(clock);
        entity.setStatus(STATUS_FAILED);
        entity.setStatsTotal(total);
        entity.setStatsSuccess(success);
        entity.setStatsFailed(failed);
        entity.setStatsSkipped(skipped);
        entity.setErrorMessage(errorMessage);
        entity.setFinishedAt(now);
        entity.setDurationMs(durationMillis(entity.getStartedAt(), now));
        entity.setUpdatedAt(now);
        jobRunRepository.save(entity);
    }

    private Long durationMillis(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
