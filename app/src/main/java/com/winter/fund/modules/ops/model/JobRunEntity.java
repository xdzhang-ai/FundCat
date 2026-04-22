package com.winter.fund.modules.ops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ops_job_runs")
public class JobRunEntity {

    @Id
    private String id;

    @Column(name = "job_code", nullable = false)
    private String jobCode;

    @Column(name = "job_source", nullable = false)
    private String jobSource;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "run_key", nullable = false)
    private String runKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "payload_summary")
    private String payloadSummary;

    @Column(name = "stats_total", nullable = false)
    private int statsTotal;

    @Column(name = "stats_success", nullable = false)
    private int statsSuccess;

    @Column(name = "stats_failed", nullable = false)
    private int statsFailed;

    @Column(name = "stats_skipped", nullable = false)
    private int statsSkipped;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }
    public String getJobSource() { return jobSource; }
    public void setJobSource(String jobSource) { this.jobSource = jobSource; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getRunKey() { return runKey; }
    public void setRunKey(String runKey) { this.runKey = runKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayloadSummary() { return payloadSummary; }
    public void setPayloadSummary(String payloadSummary) { this.payloadSummary = payloadSummary; }
    public int getStatsTotal() { return statsTotal; }
    public void setStatsTotal(int statsTotal) { this.statsTotal = statsTotal; }
    public int getStatsSuccess() { return statsSuccess; }
    public void setStatsSuccess(int statsSuccess) { this.statsSuccess = statsSuccess; }
    public int getStatsFailed() { return statsFailed; }
    public void setStatsFailed(int statsFailed) { this.statsFailed = statsFailed; }
    public int getStatsSkipped() { return statsSkipped; }
    public void setStatsSkipped(int statsSkipped) { this.statsSkipped = statsSkipped; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

