package com.winter.fund.modules.holding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HoldingComputationServiceTest {

    private HoldingComputationService computationService;

    @BeforeEach
    void setUp() {
        computationService = new HoldingComputationService();
    }

    @Test
    void rebuildFromAmountUsesConfirmedNavBasis() {
        HoldingComputationService.HoldingState state = computationService.rebuildFromAmount(5000, 300, 1.25);

        assertEquals(4000.0000, state.shares(), 0.0001);
        assertEquals(1.1750, state.averageCost(), 0.0001);
    }

    @Test
    void applyBuyAddsFeeIntoTotalCost() {
        HoldingComputationService.HoldingState base = new HoldingComputationService.HoldingState(1000, 1.2000);

        HoldingComputationService.HoldingState next = computationService.applyBuy(base, 1000, 1.25, 0.0015);

        assertEquals(1800.0000, next.shares(), 0.0001);
        assertEquals(1.2231, next.averageCost(), 0.0001);
    }

    @Test
    void applySellUsesAverageCostAndReturnsNetAmount() {
        HoldingComputationService.HoldingState base = new HoldingComputationService.HoldingState(1000, 1.2000);

        HoldingComputationService.SellResult result = computationService.applySell(base, 200, 1.35, 0.001);

        assertEquals(800.0000, result.state().shares(), 0.0001);
        assertEquals(1.2000, result.state().averageCost(), 0.0001);
        assertEquals(270.00, result.grossAmount(), 0.0001);
        assertEquals(0.27, result.feeAmount(), 0.0001);
        assertEquals(269.73, result.netAmount(), 0.0001);
    }

    @Test
    void metricsUsesUnifiedHoldingFormulas() {
        HoldingComputationService.HoldingState state = new HoldingComputationService.HoldingState(4000, 1.1750);

        HoldingComputationService.HoldingMetrics metrics = computationService.metrics(state, 1.30, 1.28, 10000);

        assertEquals(5200.00, metrics.marketValue(), 0.0001);
        assertEquals(500.00, metrics.holdingPnl(), 0.0001);
        assertEquals(10.6383, metrics.holdingPnlRate(), 0.0001);
        assertEquals(80.00, metrics.dailyPnl(), 0.0001);
        assertEquals(52.0000, metrics.allocation(), 0.0001);
    }
}
