package com.winter.fund.modules.fund.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "nav_history")
public class NavHistoryEntity {

    @Id
    private String id;

    @Column(name = "fund_code", nullable = false)
    private String fundCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "unit_nav", nullable = false)
    private double unitNav;

    @Column(name = "accumulated_nav", nullable = false)
    private double accumulatedNav;

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

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
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
}
