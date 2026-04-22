package com.winter.fund.modules.holding.service;

/**
 * 持仓模块服务，负责封装该模块的核心业务逻辑。
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class HoldingComputationService {

    /**
     * 重建from金额。
     */
    public HoldingState rebuildFromAmount(double amount, double holdingPnl, double nav) {
        if (nav <= 0) {
            throw new IllegalArgumentException("净值必须大于 0");
        }
        double totalCost = amount - holdingPnl;
        if (totalCost <= 0) {
            throw new IllegalArgumentException("持有收益不能大于或等于持有金额");
        }
        double shares = round(amount / nav, 4);
        double averageCost = round(totalCost / shares, 4);
        return new HoldingState(shares, averageCost);
    }

    /**
     * 应用买入。
     */
    public HoldingState applyBuy(HoldingState base, double amount, double nav, double feeRate) {
        validateNav(nav);
        validateFeeRate(feeRate);
        if (amount <= 0) {
            throw new IllegalArgumentException("买入金额必须大于 0");
        }
        double feeAmount = round(amount * feeRate, 2);
        double sharesDelta = round(amount / nav, 4);
        double totalCost = base.totalCost() + amount + feeAmount;
        double totalShares = round(base.shares() + sharesDelta, 4);
        return new HoldingState(totalShares, round(totalCost / totalShares, 4));
    }

    /**
     * 应用卖出。
     */
    public SellResult applySell(HoldingState base, double shares, double nav, double feeRate) {
        validateNav(nav);
        validateFeeRate(feeRate);
        if (shares <= 0) {
            throw new IllegalArgumentException("卖出份额必须大于 0");
        }
        if (shares > base.shares()) {
            throw new IllegalArgumentException("卖出份额不能超过当前持有份额");
        }
        double grossAmount = round(shares * nav, 2);
        double feeAmount = round(grossAmount * feeRate, 2);
        double totalShares = round(base.shares() - shares, 4);
        double totalCost = round(base.totalCost() - (shares * base.averageCost()), 4);
        HoldingState next = totalShares <= 0
            ? new HoldingState(0, 0)
            : new HoldingState(totalShares, round(totalCost / totalShares, 4));
        return new SellResult(next, grossAmount, round(grossAmount - feeAmount, 2), feeAmount);
    }

    /**
     * 获取当前用户信息metrics。
     */
    public HoldingMetrics metrics(HoldingState state, double nav, double previousNav, double totalMarketValue) {
        validateNav(nav);
        double marketValue = round(state.shares() * nav, 2);
        double totalCost = state.totalCost();
        double holdingPnl = round(marketValue - totalCost, 2);
        double holdingPnlRate = totalCost <= 0 ? 0 : round((holdingPnl / totalCost) * 100, 4);
        double dailyPnl = round(state.shares() * (nav - previousNav), 2);
        double allocation = totalMarketValue <= 0 ? 0 : round((marketValue / totalMarketValue) * 100, 4);
        return new HoldingMetrics(marketValue, holdingPnl, holdingPnlRate, dailyPnl, allocation);
    }

    /**
     * 执行validateNav流程。
     */
    private void validateNav(double nav) {
        if (nav <= 0) {
            throw new IllegalArgumentException("净值必须大于 0");
        }
    }

    /**
     * 执行validateFeeRate流程。
     */
    private void validateFeeRate(double feeRate) {
        if (feeRate < 0 || feeRate > 1) {
            throw new IllegalArgumentException("费率必须在 0 到 1 之间");
        }
    }

    /**
     * 返回round结果。
     */
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 返回HoldingState结果。
     */
    public record HoldingState(double shares, double averageCost) {

        /**
         * 转换为tal成本。
         */
        public double totalCost() {
            return BigDecimal.valueOf(shares)
                .multiply(BigDecimal.valueOf(averageCost))
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
        }
    }

    /**
     * 返回SellResult结果。
     */
    public record SellResult(HoldingState state, double grossAmount, double netAmount, double feeAmount) {
    }

    /**
     * 返回HoldingMetrics结果。
     */
    public record HoldingMetrics(double marketValue, double holdingPnl, double holdingPnlRate, double dailyPnl, double allocation) {
    }
}
