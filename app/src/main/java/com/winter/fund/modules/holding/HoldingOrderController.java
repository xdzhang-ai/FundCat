package com.winter.fund.modules.holding;

/**
 * 持仓动作控制器，负责对外暴露最近动作接口。
 */

import com.winter.fund.modules.auth.model.CurrentUser;
import com.winter.fund.modules.holding.model.HoldingDtos;
import com.winter.fund.modules.holding.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Holding Orders", description = "最近动作接口")
public class HoldingOrderController {

    private final HoldingService holdingService;

    public HoldingOrderController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @GetMapping("/orders")
    @Operation(summary = "获取最近动作", description = "返回最近的买入、卖出和定投动作，包含确认中与已执行状态。")
    public List<HoldingDtos.OperationRecordResponse> orders(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Parameter(description = "作用域，目前仅 recent 生效", example = "recent")
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "recent") String scope
    ) {
        return holdingService.getOrders(currentUser, scope);
    }
}
