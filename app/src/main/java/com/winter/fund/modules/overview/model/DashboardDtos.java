package com.winter.fund.modules.overview.model;

/**
 * 仪表盘模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import io.swagger.v3.oas.annotations.media.Schema;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    @Schema(description = "首页核心指标项")
    public record HeroMetricResponse(
        @Schema(description = "指标名称", example = "组合市值") String label,
        @Schema(description = "主值", example = "¥39680.00") String value,
        @Schema(description = "补充变化值", example = "+1180.50") String delta,
        @Schema(description = "语义色调", example = "positive") String tone
    ) {
    }

}
