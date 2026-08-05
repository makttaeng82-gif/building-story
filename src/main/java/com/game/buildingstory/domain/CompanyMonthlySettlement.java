package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 한 번 확정된 플레이어 기업의 월 손익과 현금 변화를 보존한다. */
@Entity
@Table(name = "company_monthly_settlement", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_monthly_period", columnNames = {"company_id", "period_index"}))
public class CompanyMonthlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private int periodIndex;
    private int gameYear;
    private int gameMonth;
    private long subscriptionRevenue;
    private Long projectRevenue;
    private Long contractRevenue;
    private long payrollCost;
    private long cloudCost;
    private long developmentCost;
    private long marketingCost;
    private Long platformCost;
    private Long projectCost;
    private Long contractCost;
    private Long incidentCost;
    private long operatingProfit;
    private long corporateTax;
    private Long financingCost;
    private Long debtPrincipalPayment;
    private long netIncome;
    private long openingCash;
    private long closingCash;
    private long unpaidAmount;

    protected CompanyMonthlySettlement() {
    }

    public CompanyMonthlySettlement(
            PlayerCompany company,
            int periodIndex,
            int gameYear,
            int gameMonth,
            long subscriptionRevenue,
            long projectRevenue,
            long contractRevenue,
            long payrollCost,
            long cloudCost,
            long developmentCost,
            long marketingCost,
            long platformCost,
            long projectCost,
            long contractCost,
            long incidentCost,
            long operatingProfit,
            long corporateTax,
            long netIncome,
            long openingCash,
            long closingCash,
            long unpaidAmount
    ) {
        this(company, periodIndex, gameYear, gameMonth, subscriptionRevenue, projectRevenue,
                contractRevenue, payrollCost, cloudCost, developmentCost, marketingCost,
                platformCost, projectCost, contractCost, incidentCost, operatingProfit, corporateTax,
                0, 0, netIncome, openingCash, closingCash, unpaidAmount);
    }

    public CompanyMonthlySettlement(
            PlayerCompany company,
            int periodIndex,
            int gameYear,
            int gameMonth,
            long subscriptionRevenue,
            long projectRevenue,
            long contractRevenue,
            long payrollCost,
            long cloudCost,
            long developmentCost,
            long marketingCost,
            long platformCost,
            long projectCost,
            long contractCost,
            long incidentCost,
            long operatingProfit,
            long corporateTax,
            long financingCost,
            long debtPrincipalPayment,
            long netIncome,
            long openingCash,
            long closingCash,
            long unpaidAmount
    ) {
        this.company = company;
        this.periodIndex = periodIndex;
        this.gameYear = gameYear;
        this.gameMonth = gameMonth;
        this.subscriptionRevenue = subscriptionRevenue;
        this.projectRevenue = projectRevenue;
        this.contractRevenue = contractRevenue;
        this.payrollCost = payrollCost;
        this.cloudCost = cloudCost;
        this.developmentCost = developmentCost;
        this.marketingCost = marketingCost;
        this.platformCost = platformCost;
        this.projectCost = projectCost;
        this.contractCost = contractCost;
        this.incidentCost = incidentCost;
        this.operatingProfit = operatingProfit;
        this.corporateTax = corporateTax;
        this.financingCost = financingCost;
        this.debtPrincipalPayment = debtPrincipalPayment;
        this.netIncome = netIncome;
        this.openingCash = openingCash;
        this.closingCash = closingCash;
        this.unpaidAmount = unpaidAmount;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public int getPeriodIndex() { return periodIndex; }
    public int getGameYear() { return gameYear; }
    public int getGameMonth() { return gameMonth; }
    public long getSubscriptionRevenue() { return subscriptionRevenue; }
    public long getProjectRevenue() { return projectRevenue == null ? 0 : projectRevenue; }
    public long getContractRevenue() { return contractRevenue == null ? 0 : contractRevenue; }
    public long getTotalRevenue() {
        return Math.addExact(Math.addExact(subscriptionRevenue, getProjectRevenue()), getContractRevenue());
    }
    public long getPayrollCost() { return payrollCost; }
    public long getCloudCost() { return cloudCost; }
    public long getDevelopmentCost() { return developmentCost; }
    public long getMarketingCost() { return marketingCost; }
    public long getPlatformCost() { return platformCost == null ? 0 : platformCost; }
    public long getProjectCost() { return projectCost == null ? 0 : projectCost; }
    public long getContractCost() { return contractCost == null ? 0 : contractCost; }
    public long getIncidentCost() { return incidentCost == null ? 0 : incidentCost; }
    public long getOperatingProfit() { return operatingProfit; }
    public long getCorporateTax() { return corporateTax; }
    public long getFinancingCost() { return financingCost == null ? 0 : financingCost; }
    public long getDebtPrincipalPayment() {
        return debtPrincipalPayment == null ? 0 : debtPrincipalPayment;
    }
    public long getNetIncome() { return netIncome; }
    public long getOpeningCash() { return openingCash; }
    public long getClosingCash() { return closingCash; }
    public long getUnpaidAmount() { return unpaidAmount; }
}
