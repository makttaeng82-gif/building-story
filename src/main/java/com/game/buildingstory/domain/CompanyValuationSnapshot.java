package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "company_valuation_snapshot", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_valuation_quarter", columnNames = {"company_id", "quarter_sequence"}))
public class CompanyValuationSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private int quarterSequence;
    private long assetValue;
    private long incomeValue;
    private long enterpriseValue;
    private long annualRecurringRevenue;
    private long annualizedOperatingProfit;
    private int revenueMultiple;
    private int adjustmentBasisPoints;
    private long totalDebt;

    protected CompanyValuationSnapshot() {
    }

    public CompanyValuationSnapshot(
            PlayerCompany company,
            int quarterSequence,
            long assetValue,
            long incomeValue,
            long enterpriseValue,
            long annualRecurringRevenue,
            long annualizedOperatingProfit,
            int revenueMultiple,
            int adjustmentBasisPoints,
            long totalDebt
    ) {
        this.company = company;
        this.quarterSequence = quarterSequence;
        this.assetValue = assetValue;
        this.incomeValue = incomeValue;
        this.enterpriseValue = enterpriseValue;
        this.annualRecurringRevenue = annualRecurringRevenue;
        this.annualizedOperatingProfit = annualizedOperatingProfit;
        this.revenueMultiple = revenueMultiple;
        this.adjustmentBasisPoints = adjustmentBasisPoints;
        this.totalDebt = totalDebt;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public int getQuarterSequence() { return quarterSequence; }
    public long getAssetValue() { return assetValue; }
    public long getIncomeValue() { return incomeValue; }
    public long getEnterpriseValue() { return enterpriseValue; }
    public long getAnnualRecurringRevenue() { return annualRecurringRevenue; }
    public long getAnnualizedOperatingProfit() { return annualizedOperatingProfit; }
    public int getRevenueMultiple() { return revenueMultiple; }
    public int getAdjustmentBasisPoints() { return adjustmentBasisPoints; }
    public long getTotalDebt() { return totalDebt; }
}
