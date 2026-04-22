package com.winter.fund.modules.fund.model;

/**
 * 基金模块实体文件，负责描述数据库持久化对象结构。
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "fund_nav_history")
public class NavHistoryEntity {

    @Id
    private String id;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "nav_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "unit_nav", nullable = false)
    private double unitNav;

    @Column(name = "accumulated_nav", nullable = false)
    private double accumulatedNav;

    @Column(name = "day_growth", nullable = false)
    private double dayGrowth;

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
     * 获取trade日期。
     */
    public LocalDate getTradeDate() {
        return tradeDate;
    }

    /**
     * 设置trade日期。
     */
    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    /**
     * 获取unit净值。
     */
    public double getUnitNav() {
        return unitNav;
    }

    /**
     * 设置unit净值。
     */
    public void setUnitNav(double unitNav) {
        this.unitNav = unitNav;
    }

    /**
     * 获取accumulated净值。
     */
    public double getAccumulatedNav() {
        return accumulatedNav;
    }

    /**
     * 设置accumulated净值。
     */
    public void setAccumulatedNav(double accumulatedNav) {
        this.accumulatedNav = accumulatedNav;
    }

    /**
     * 获取day涨跌幅。
     */
    public double getDayGrowth() {
        return dayGrowth;
    }

    /**
     * 设置day涨跌幅。
     */
    public void setDayGrowth(double dayGrowth) {
        this.dayGrowth = dayGrowth;
    }
}
