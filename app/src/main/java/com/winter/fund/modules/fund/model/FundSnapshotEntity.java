package com.winter.fund.modules.fund.model;

/**
 * 基金模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_snapshots")
public class FundSnapshotEntity {

    @Id
    private String id;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "unit_nav", nullable = false)
    private double unitNav;

    @Column(name = "accumulated_nav", nullable = false)
    private double accumulatedNav;

    @Column(name = "day_growth", nullable = false)
    private double dayGrowth;

    @Column(name = "week_growth", nullable = false)
    private double weekGrowth;

    @Column(name = "month_growth", nullable = false)
    private double monthGrowth;

    @Column(name = "year_growth", nullable = false)
    private double yearGrowth;

    @Column(name = "asset_size", nullable = false)
    private double assetSize;

    @Column(name = "stock_ratio", nullable = false)
    private double stockRatio;

    @Column(name = "bond_ratio", nullable = false)
    private double bondRatio;

    @Column(name = "top_holdings", nullable = false)
    private String topHoldings;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public LocalDate getNavDate() {
        return navDate;
    }

    public void setNavDate(LocalDate navDate) {
        this.navDate = navDate;
    }

    public double getUnitNav() {
        return unitNav;
    }

    public void setUnitNav(double unitNav) {
        this.unitNav = unitNav;
    }

    public double getAccumulatedNav() {
        return accumulatedNav;
    }

    public void setAccumulatedNav(double accumulatedNav) {
        this.accumulatedNav = accumulatedNav;
    }

    public double getDayGrowth() {
        return dayGrowth;
    }

    public void setDayGrowth(double dayGrowth) {
        this.dayGrowth = dayGrowth;
    }

    public double getWeekGrowth() {
        return weekGrowth;
    }

    public void setWeekGrowth(double weekGrowth) {
        this.weekGrowth = weekGrowth;
    }

    public double getMonthGrowth() {
        return monthGrowth;
    }

    public void setMonthGrowth(double monthGrowth) {
        this.monthGrowth = monthGrowth;
    }

    public double getYearGrowth() {
        return yearGrowth;
    }

    public void setYearGrowth(double yearGrowth) {
        this.yearGrowth = yearGrowth;
    }

    public double getAssetSize() {
        return assetSize;
    }

    public void setAssetSize(double assetSize) {
        this.assetSize = assetSize;
    }

    public double getStockRatio() {
        return stockRatio;
    }

    public void setStockRatio(double stockRatio) {
        this.stockRatio = stockRatio;
    }

    public double getBondRatio() {
        return bondRatio;
    }

    public void setBondRatio(double bondRatio) {
        this.bondRatio = bondRatio;
    }

    public String getTopHoldings() {
        return topHoldings;
    }

    public void setTopHoldings(String topHoldings) {
        this.topHoldings = topHoldings;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
