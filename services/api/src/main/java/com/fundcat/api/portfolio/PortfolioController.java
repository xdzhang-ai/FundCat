package com.fundcat.api.portfolio;

import com.fundcat.api.auth.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/watchlist")
    public List<PortfolioDtos.WatchlistItemResponse> watchlist(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getWatchlist(currentUser);
    }

    @PostMapping("/watchlist")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioDtos.WatchlistItemResponse addWatchlist(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateWatchlistRequest request
    ) {
        return portfolioService.addWatchlist(currentUser, request);
    }

    @DeleteMapping("/watchlist/{fundCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWatchlist(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable String fundCode) {
        portfolioService.removeWatchlist(currentUser, fundCode);
    }

    @GetMapping("/portfolios")
    public List<PortfolioDtos.PortfolioSummaryResponse> portfolios(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getPortfolios(currentUser);
    }

    @GetMapping("/orders")
    public List<PortfolioDtos.PaperOrderResponse> orders(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getOrders(currentUser);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioDtos.PaperOrderResponse createOrder(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreatePaperOrderRequest request
    ) {
        return portfolioService.createPaperOrder(currentUser, request);
    }

    @GetMapping("/sips")
    public List<PortfolioDtos.SipPlanResponse> sips(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getSipPlans(currentUser);
    }

    @PostMapping("/sips")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioDtos.SipPlanResponse createSip(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateSipPlanRequest request
    ) {
        return portfolioService.createSipPlan(currentUser, request);
    }

    @GetMapping("/import-jobs")
    public List<PortfolioDtos.ImportJobResponse> importJobs(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getImportJobs(currentUser);
    }

    @PostMapping("/import-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioDtos.ImportJobResponse createImportJob(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody PortfolioDtos.CreateImportJobRequest request
    ) {
        return portfolioService.createImportJob(currentUser, request);
    }

    @GetMapping("/reports/weekly")
    public List<PortfolioDtos.WeeklyReportResponse> reports(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getReports(currentUser);
    }

    @GetMapping("/alerts")
    public List<PortfolioDtos.AlertRuleResponse> alerts(@AuthenticationPrincipal CurrentUser currentUser) {
        return portfolioService.getAlerts(currentUser);
    }
}
