package com.winter.fund.modules.sip.model;

/**
 * 定投模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class SipDtos {

    private SipDtos() {
    }

    @Schema(description = "定投计划响应")
    public record SipPlanResponse(
        @Schema(description = "定投计划 ID", example = "sip-001") String id,
        @Schema(description = "基金代码", example = "000001") String fundCode,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String fundName,
        @Schema(description = "每期定投金额", example = "1000") double amount,
        @Schema(description = "周期", example = "WEEKLY") String cadence,
        @Schema(description = "下一次执行时间", example = "2026-04-06T15:00:00") String nextRunAt,
        @Schema(description = "兼容字段，是否激活", example = "true") boolean active,
        @Schema(description = "计划状态", example = "生效") String status,
        @Schema(description = "费率", example = "0.0015") double feeRate
    ) {
    }

    @Schema(description = "定投执行记录响应")
    public record SipExecutionRecordResponse(
        @Schema(description = "操作记录 ID", example = "op-sip-001") String id,
        @Schema(description = "定投计划 ID", example = "sip-001") String sipPlanId,
        @Schema(description = "执行日期", example = "2026-03-30") String executedOn,
        @Schema(description = "定投金额", example = "1000") double amount,
        @Schema(description = "执行状态", example = "确认中") String status,
        @Schema(description = "费率", example = "0.0015") double feeRate,
        @Schema(description = "手续费金额", example = "1.50") double feeAmount
    ) {
    }

    public record SipPlanDetailResponse(
        SipPlanResponse plan,
        List<SipExecutionRecordResponse> records
    ) {
    }

    public record SipPlanDigestResponse(
        String id,
        String fundCode,
        String fundName,
        double amount,
        String cadenceLabel,
        String nextRunOn,
        String status
    ) {
    }

    @Schema(description = "创建定投计划请求")
    public record CreateSipPlanRequest(
        @Schema(description = "兼容字段，当前可不传，未传时将使用默认组合标识", example = "portfolio-current") String portfolioId,
        @Schema(description = "基金代码", example = "000001") @NotBlank String fundCode,
        @Schema(description = "每期定投金额", example = "1000") @NotNull @Min(1) Double amount,
        @Schema(description = "周期", example = "WEEKLY") @NotBlank String cadence,
        @Schema(description = "前端选择的下次执行时间", example = "2026-04-06T15:00:00") @NotBlank String nextRunAt,
        @Schema(description = "费率", example = "0.0015") @Min(0) double feeRate
    ) {
    }

    @Schema(description = "更新定投计划请求")
    public record UpdateSipPlanRequest(
        @Schema(description = "每期定投金额", example = "1200") @NotNull @Min(1) Double amount,
        @Schema(description = "周期", example = "MONTHLY") @NotBlank String cadence,
        @Schema(description = "周几执行，按英文星期缩写传值", example = "MONDAY") String weekday,
        @Schema(description = "每月几号执行", example = "15") String monthDay,
        @Schema(description = "费率", example = "0.0015") @Min(0) double feeRate
    ) {
    }
}
