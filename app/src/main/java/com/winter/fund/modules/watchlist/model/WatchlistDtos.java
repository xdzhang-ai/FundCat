package com.winter.fund.modules.watchlist.model;

/**
 * 自选模块 DTO 定义文件，负责集中声明接口入参与出参模型。
 */

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public final class WatchlistDtos {

    private WatchlistDtos() {
    }

    @Schema(description = "自选项响应")
    public record WatchlistItemResponse(
        @Schema(description = "基金代码", example = "000001") String code,
        @Schema(description = "基金名称", example = "华夏成长优选混合") String name,
        @Schema(description = "自选备注", example = "AI 主线观察") String note,
        @Schema(description = "估算涨跌幅", example = "2.01") double estimatedGrowth,
        @Schema(description = "确认净值", example = "1.6234") double unitNav,
        @Schema(description = "参考估值净值", example = "1.6351") double estimatedNav,
        @Schema(description = "所属分组", example = "成长进攻") String group,
        @Schema(description = "是否已持仓", example = "true") boolean held
    ) {
    }

    @Schema(description = "自选分组选项")
    public record WatchlistGroupResponse(
        @Schema(description = "分组ID", example = "user-demo-001:成长进攻") String id,
        @Schema(description = "分组名称", example = "成长进攻") String name
    ) {
    }

    @Schema(description = "新增自选请求")
    public record CreateWatchlistRequest(
        @Schema(description = "基金代码", example = "000001") @NotBlank String fundCode,
        @Schema(description = "备注", example = "重点观察仓位") @NotBlank String note,
        @Schema(description = "所属分组", example = "成长进攻") @NotBlank String group
    ) {
    }

    @Schema(description = "批量替换自选分组请求")
    public record UpdateWatchlistGroupsRequest(
        @Schema(description = "基金代码列表", example = "[\"000001\",\"519674\"]") @NotEmpty List<String> fundCodes,
        @Schema(description = "新的分组", example = "稳健配置") @NotBlank String group
    ) {
    }

    @Schema(description = "新增自选分组请求")
    public record CreateWatchlistGroupRequest(
        @Schema(description = "分组名称", example = "科技主线") @NotBlank String name
    ) {
    }
}
