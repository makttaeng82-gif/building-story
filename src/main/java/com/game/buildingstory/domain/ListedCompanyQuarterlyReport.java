package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 한 번 확정된 상장기업 분기 실적을 보존하는 불변 이력이다. */
@Entity
@Table(name = "listed_company_quarterly_report", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_quarterly_period", columnNames = {"listed_company_id", "fiscal_period_index"}))
public class ListedCompanyQuarterlyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ListedCompany listedCompany;

    private int fiscalPeriodIndex;
    private int fiscalYear;
    private int fiscalQuarter;
    private int publishedElapsedDay;
    private long expectedRevenue;
    private long expectedNetIncome;
    private long revenue;
    private long costOfRevenue;
    private long operatingExpenses;
    private long operatingProfit;
    private long interestExpense;
    private long taxExpense;
    private long netIncome;
    private long capitalExpenditure;
    private long depreciation;
    private long endingCash;
    private long endingNonCashAssets;
    private long endingDebt;
    private long endingOtherLiabilities;
    private long endingNetAssets;
    private long earningsPerShare;
    private int revenueGrowthBasisPoints;
    private int earningsSurpriseBasisPoints;
    private Long totalDividend;
    private Long dividendPerShare;
    private Boolean baselineHistory;

    protected ListedCompanyQuarterlyReport() {
    }

    public ListedCompanyQuarterlyReport(
            ListedCompany listedCompany,
            int fiscalPeriodIndex,
            int fiscalYear,
            int fiscalQuarter,
            int publishedElapsedDay,
            long expectedRevenue,
            long expectedNetIncome,
            long revenue,
            long costOfRevenue,
            long operatingExpenses,
            long operatingProfit,
            long interestExpense,
            long taxExpense,
            long netIncome,
            long capitalExpenditure,
            long depreciation,
            long endingCash,
            long endingNonCashAssets,
            long endingDebt,
            long endingOtherLiabilities,
            long endingNetAssets,
            long earningsPerShare,
            int revenueGrowthBasisPoints,
            int earningsSurpriseBasisPoints,
            long totalDividend,
            long dividendPerShare,
            boolean baselineHistory
    ) {
        this.listedCompany = listedCompany;
        this.fiscalPeriodIndex = fiscalPeriodIndex;
        this.fiscalYear = fiscalYear;
        this.fiscalQuarter = fiscalQuarter;
        this.publishedElapsedDay = publishedElapsedDay;
        this.expectedRevenue = expectedRevenue;
        this.expectedNetIncome = expectedNetIncome;
        this.revenue = revenue;
        this.costOfRevenue = costOfRevenue;
        this.operatingExpenses = operatingExpenses;
        this.operatingProfit = operatingProfit;
        this.interestExpense = interestExpense;
        this.taxExpense = taxExpense;
        this.netIncome = netIncome;
        this.capitalExpenditure = capitalExpenditure;
        this.depreciation = depreciation;
        this.endingCash = endingCash;
        this.endingNonCashAssets = endingNonCashAssets;
        this.endingDebt = endingDebt;
        this.endingOtherLiabilities = endingOtherLiabilities;
        this.endingNetAssets = endingNetAssets;
        this.earningsPerShare = earningsPerShare;
        this.revenueGrowthBasisPoints = revenueGrowthBasisPoints;
        this.earningsSurpriseBasisPoints = earningsSurpriseBasisPoints;
        this.totalDividend = totalDividend;
        this.dividendPerShare = dividendPerShare;
        this.baselineHistory = baselineHistory;
    }

    public Long getId() { return id; }
    public ListedCompany getListedCompany() { return listedCompany; }
    public int getFiscalPeriodIndex() { return fiscalPeriodIndex; }
    public int getFiscalYear() { return fiscalYear; }
    public int getFiscalQuarter() { return fiscalQuarter; }
    public int getPublishedElapsedDay() { return publishedElapsedDay; }
    public long getExpectedRevenue() { return expectedRevenue; }
    public long getExpectedNetIncome() { return expectedNetIncome; }
    public long getRevenue() { return revenue; }
    public long getCostOfRevenue() { return costOfRevenue; }
    public long getOperatingExpenses() { return operatingExpenses; }
    public long getOperatingProfit() { return operatingProfit; }
    public long getInterestExpense() { return interestExpense; }
    public long getTaxExpense() { return taxExpense; }
    public long getNetIncome() { return netIncome; }
    public long getCapitalExpenditure() { return capitalExpenditure; }
    public long getDepreciation() { return depreciation; }
    public long getEndingCash() { return endingCash; }
    public long getEndingNonCashAssets() { return endingNonCashAssets; }
    public long getEndingDebt() { return endingDebt; }
    public long getEndingOtherLiabilities() { return endingOtherLiabilities; }
    public long getEndingNetAssets() { return endingNetAssets; }
    public long getEarningsPerShare() { return earningsPerShare; }
    public int getRevenueGrowthBasisPoints() { return revenueGrowthBasisPoints; }
    public int getEarningsSurpriseBasisPoints() { return earningsSurpriseBasisPoints; }
    public long getTotalDividend() { return totalDividend == null ? 0 : totalDividend; }
    public long getDividendPerShare() { return dividendPerShare == null ? 0 : dividendPerShare; }
    public boolean isBaselineHistory() { return Boolean.TRUE.equals(baselineHistory); }
}
