package com.winter.fund.modules.dashboard;

import com.winter.fund.modules.dashboard.model.DashboardDtos;
import com.winter.fund.modules.dashboard.service.DashboardService;
import com.winter.fund.modules.auth.model.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardDtos.DashboardResponse dashboard(@AuthenticationPrincipal CurrentUser currentUser) {
        return dashboardService.getDashboard(currentUser);
    }
}
