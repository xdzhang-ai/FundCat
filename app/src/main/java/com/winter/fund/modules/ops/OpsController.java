package com.winter.fund.modules.ops;

/**
 * 运维模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.ops.model.OpsDtos;
import com.winter.fund.modules.ops.service.OpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops")
@Tag(name = "Ops", description = "运维开关与数据源状态接口")
public class OpsController {

    private final OpsService opsService;

    public OpsController(OpsService opsService) {
        this.opsService = opsService;
    }

    /**
     * 返回summary结果。
     */
    @GetMapping("/summary")
    @Operation(summary = "获取运维摘要", description = "返回功能开关和市场数据提供方状态。")
    public OpsDtos.OpsSummaryResponse summary() {
        return opsService.getSummary();
    }

    /**
     * 返回featureFlags结果。
     */
    @GetMapping("/feature-flags")
    @Operation(summary = "获取功能开关列表", description = "返回全部功能开关及其当前状态。")
    public java.util.List<OpsDtos.FeatureFlagResponse> featureFlags() {
        return opsService.getFeatureFlags();
    }

    /**
     * 返回最近任务运行记录。
     */
    @GetMapping("/job-runs")
    @Operation(summary = "获取最近任务运行记录", description = "返回 Airflow、Java 定时任务和消息消费的最近执行状态。")
    public java.util.List<OpsDtos.JobRunResponse> jobRuns() {
        return opsService.getRecentJobRuns();
    }

    @PatchMapping("/feature-flags/{code}")
    @Operation(summary = "切换功能开关", description = "按开关编码更新启用状态。")
    public OpsDtos.FeatureFlagResponse toggle(
        @Parameter(description = "功能开关编码", example = "estimate_reference") @PathVariable String code,
        @Valid @RequestBody OpsDtos.ToggleFlagRequest request
    ) {
        return opsService.toggle(code, request.enabled());
    }
}
