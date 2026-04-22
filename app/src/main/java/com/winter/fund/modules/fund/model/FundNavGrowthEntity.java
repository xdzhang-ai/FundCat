package com.winter.fund.modules.fund.model;

/**
 * 基金区间涨幅摘要实体，负责持久化基金在某个净值日的区间涨幅结果。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_nav_growth")
public class FundNavGrowthEntity {

    @Id
    private String id;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "nav_date", nullable = false)
    private LocalDate navDate;

    @Column(name = "week_growth", nullable = false)
    private double weekGrowth;

    @Column(name = "month_growth", nullable = false)
    private double monthGrowth;

    @Column(name = "year_growth", nullable = false)
    private double yearGrowth;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 获取id。
     */
    public String getId() {
        return id;
    }

    /**
     * 设置id。
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取基金code。
     */
    public String getFundCode() {
        return fundCode;
    }

    /**
     * 设置基金code。
     */
    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    /**
     * 获取净值日期。
     */
    public LocalDate getNavDate() {
        return navDate;
    }

    /**
     * 设置净值日期。
     */
    public void setNavDate(LocalDate navDate) {
        this.navDate = navDate;
    }

    /**
     * 获取近一周涨幅。
     */
    public double getWeekGrowth() {
        return weekGrowth;
    }

    /**
     * 设置近一周涨幅。
     */
    public void setWeekGrowth(double weekGrowth) {
        this.weekGrowth = weekGrowth;
    }

    /**
     * 获取近一月涨幅。
     */
    public double getMonthGrowth() {
        return monthGrowth;
    }

    /**
     * 设置近一月涨幅。
     */
    public void setMonthGrowth(double monthGrowth) {
        this.monthGrowth = monthGrowth;
    }

    /**
     * 获取近一年涨幅。
     */
    public double getYearGrowth() {
        return yearGrowth;
    }

    /**
     * 设置近一年涨幅。
     */
    public void setYearGrowth(double yearGrowth) {
        this.yearGrowth = yearGrowth;
    }

    /**
     * 获取更新时间。
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
