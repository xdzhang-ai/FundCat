package com.winter.fund.modules.dashboard;

/**
 * 仪表盘模块控制器，负责对外暴露该模块的 HTTP 接口。
 */

import com.winter.fund.modules.dashboard.model.DashboardDtos;
import com.winter.fund.modules.dashboard.service.DashboardService;
import com.winter.fund.modules.auth.model.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "兼容旧前端的仪表盘聚合接口")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "获取仪表盘聚合数据", description = "兼容旧页面的首页聚合接口，内部复用 overview 和 portfolio 数据。")
    public DashboardDtos.DashboardResponse dashboard(@AuthenticationPrincipal CurrentUser currentUser) {
        return dashboardService.getDashboard(currentUser);
    }
}
