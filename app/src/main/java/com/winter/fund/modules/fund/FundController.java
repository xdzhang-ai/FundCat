package com.winter.fund.modules.fund;

/**
 * 基金模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.fund.model.FundDtos;
import com.winter.fund.modules.fund.model.AkshareBridgeDtos;
import com.winter.fund.modules.fund.service.AkshareBridgeService;
import com.winter.fund.modules.fund.service.FundService;
import com.winter.fund.modules.auth.model.CurrentUser;
import java.util.List;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/funds")
@Tag(name = "Funds", description = "基金检索、详情与持仓洞察接口")
public class FundController {

    private final FundService fundService;
    private final HoldingService holdingService;
    private final AkshareBridgeService akshareBridgeService;

    public FundController(FundService fundService, HoldingService holdingService, AkshareBridgeService akshareBridgeService) {
        this.fundService = fundService;
        this.holdingService = holdingService;
        this.akshareBridgeService = akshareBridgeService;
    }

    @GetMapping
    @Operation(summary = "搜索基金", description = "按名称或代码搜索基金；query 为空时返回优先级排序后的默认列表。")
    public List<FundDtos.FundCardResponse> search(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "搜索关键字，可按基金名称或代码匹配", example = "华夏")
        @RequestParam(required = false) String query
    ) {
        return fundService.search(currentUser.id(), query);
    }

    @GetMapping("/{code}")
    @Operation(summary = "获取基金详情", description = "返回基金详情、趋势、持仓标签以及扩展研究信息。")
    public FundDtos.FundDetailResponse detail(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "基金代码", example = "000001") @PathVariable String code
    ) {
        return fundService.getDetail(currentUser.id(), code);
    }

    @GetMapping("/{code}/user-state")
    @Operation(summary = "获取基金用户状态", description = "返回当前用户对该基金是否已加入自选、是否已持仓。")
    public FundDtos.FundUserStateResponse userState(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "基金代码", example = "000001") @PathVariable String code
    ) {
        return fundService.getUserState(currentUser.id(), code);
    }

    @GetMapping("/{code}/holding-insight")
    @Operation(summary = "获取基金持仓洞察", description = "仅当当前用户持有该基金时返回洞察数据。")
    public HoldingDtos.HoldingInsightResponse holdingInsight(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "基金代码", example = "000001") @PathVariable String code
    ) {
        return holdingService.getInsight(currentUser, code);
    }
}
