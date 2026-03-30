package com.winter.fund.modules.holding;

/**
 * 持仓模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.model.UserFundHoldingEntity;
import com.winter.fund.modules.holding.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holdings")
@Tag(name = "Holdings", description = "持仓与手工买卖接口")
public class HoldingController {

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping("/overview")
    @Operation(summary = "获取持仓总览", description = "返回当前用户的全部持仓汇总与列表。")
    public HoldingDtos.HoldingsOverviewResponse overview(@AuthenticationPrincipal CurrentUser currentUser) {
        return holdingService.getOverview(currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增持仓快照", description = "按前端传入的金额口径和收益反推份额、成本并写入当前持仓。")
    public UserFundHoldingEntity createHolding(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody HoldingDtos.UpsertHoldingRequest request
    ) {
        return holdingService.createHolding(currentUser, request);
    }

    @PatchMapping("/{fundCode}")
    @Operation(summary = "修改持仓快照", description = "覆盖指定基金的当前持仓结果。")
    public UserFundHoldingEntity updateHolding(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "基金代码", example = "000001") @PathVariable String fundCode,
        @Valid @RequestBody HoldingDtos.UpsertHoldingRequest request
    ) {
        return holdingService.upsertHolding(currentUser, fundCode, request);
    }

    @PostMapping("/operations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "补记买卖", description = "按前端传入的交易日期补记手工买卖，并基于该日期可用的确认净值立即执行。")
    public HoldingDtos.HoldingOperationResponse createOperation(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody HoldingDtos.CreateHoldingOperationRequest request
    ) {
        return holdingService.createManualOperation(currentUser, request);
    }
}
