package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 특정 분기까지 확정된 최근 4개 실적으로 계산한 기업가치 스냅샷이다. */
@Entity
@Table(name = "listed_company_valuation_snapshot", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_valuation_period", columnNames = {"listed_company_id", "fiscal_period_index"}))
public class ListedCompanyValuationSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ListedCompany listedCompany;

    private int fiscalPeriodIndex;
    private int calculatedElapsedDay;
    private long trailingRevenue;
    private long trailingOperatingProfit;
    private long trailingNetIncome;
    private long earningsPerShare;
    private long bookValuePerShare;
    private long salesPerShare;
    private long fairValueLower;
    private long fairValueBase;
    private long fairValueUpper;
    private int debtDiscountBasisPoints;
    private Boolean baselineValuation;

    protected ListedCompanyValuationSnapshot() {
    }

    public ListedCompanyValuationSnapshot(
            ListedCompany listedCompany,
            int fiscalPeriodIndex,
            int calculatedElapsedDay,
            long trailingRevenue,
            long trailingOperatingProfit,
            long trailingNetIncome,
            long earningsPerShare,
            long bookValuePerShare,
            long salesPerShare,
            long fairValueLower,
            long fairValueBase,
            long fairValueUpper,
            int debtDiscountBasisPoints,
            boolean baselineValuation
    ) {
        this.listedCompany = listedCompany;
        this.fiscalPeriodIndex = fiscalPeriodIndex;
        this.calculatedElapsedDay = calculatedElapsedDay;
        this.trailingRevenue = trailingRevenue;
        this.trailingOperatingProfit = trailingOperatingProfit;
        this.trailingNetIncome = trailingNetIncome;
        this.earningsPerShare = earningsPerShare;
        this.bookValuePerShare = bookValuePerShare;
        this.salesPerShare = salesPerShare;
        this.fairValueLower = fairValueLower;
        this.fairValueBase = fairValueBase;
        this.fairValueUpper = fairValueUpper;
        this.debtDiscountBasisPoints = debtDiscountBasisPoints;
        this.baselineValuation = baselineValuation;
    }

    public Long getId() { return id; }
    public ListedCompany getListedCompany() { return listedCompany; }
    public int getFiscalPeriodIndex() { return fiscalPeriodIndex; }
    public int getCalculatedElapsedDay() { return calculatedElapsedDay; }
    public long getTrailingRevenue() { return trailingRevenue; }
    public long getTrailingOperatingProfit() { return trailingOperatingProfit; }
    public long getTrailingNetIncome() { return trailingNetIncome; }
    public long getEarningsPerShare() { return earningsPerShare; }
    public long getBookValuePerShare() { return bookValuePerShare; }
    public long getSalesPerShare() { return salesPerShare; }
    public long getFairValueLower() { return fairValueLower; }
    public long getFairValueBase() { return fairValueBase; }
    public long getFairValueUpper() { return fairValueUpper; }
    public int getDebtDiscountBasisPoints() { return debtDiscountBasisPoints; }
    public boolean isBaselineValuation() { return Boolean.TRUE.equals(baselineValuation); }
}
