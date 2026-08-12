package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 실제 월 정산 세 건을 합산한 플레이어 기업의 확정 분기보고서다. */
@Entity
@Table(name = "company_quarterly_report", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_quarter_sequence", columnNames = {"company_id", "quarter_sequence"}))
public class CompanyQuarterlyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private int quarterSequence;
    private int endingPeriodIndex;
    private long revenue;
    private long operatingExpenses;
    private long operatingProfit;
    private long corporateTax;
    private long netIncome;
    private long closingCash;
    private Long recurringRevenueAtEnd;
    private Long paidUsersAtEnd;
    private Long operatingCashFlow;
    private Long investingCashFlow;
    private Long financingCashFlow;
    private boolean unread;
    private Boolean dividendDecided;
    private Integer dividendRate;
    private Long dividendAmount;
    private Integer publishedElapsedDay;
    private Integer dividendDecidedElapsedDay;

    protected CompanyQuarterlyReport() {
    }

    public CompanyQuarterlyReport(
            PlayerCompany company,
            int quarterSequence,
            int endingPeriodIndex,
            long revenue,
            long operatingExpenses,
            long operatingProfit,
            long corporateTax,
            long netIncome,
            long closingCash,
            long recurringRevenueAtEnd,
            long paidUsersAtEnd
    ) {
        this(company, quarterSequence, endingPeriodIndex, revenue, operatingExpenses, operatingProfit,
                corporateTax, netIncome, closingCash, recurringRevenueAtEnd, paidUsersAtEnd,
                null, null, null);
    }

    public CompanyQuarterlyReport(
            PlayerCompany company,
            int quarterSequence,
            int endingPeriodIndex,
            long revenue,
            long operatingExpenses,
            long operatingProfit,
            long corporateTax,
            long netIncome,
            long closingCash,
            long recurringRevenueAtEnd,
            long paidUsersAtEnd,
            Long operatingCashFlow,
            Long investingCashFlow,
            Long financingCashFlow
    ) {
        this.company = company;
        this.quarterSequence = quarterSequence;
        this.endingPeriodIndex = endingPeriodIndex;
        this.revenue = revenue;
        this.operatingExpenses = operatingExpenses;
        this.operatingProfit = operatingProfit;
        this.corporateTax = corporateTax;
        this.netIncome = netIncome;
        this.closingCash = closingCash;
        this.recurringRevenueAtEnd = recurringRevenueAtEnd;
        this.paidUsersAtEnd = paidUsersAtEnd;
        this.operatingCashFlow = operatingCashFlow;
        this.investingCashFlow = investingCashFlow;
        this.financingCashFlow = financingCashFlow;
        this.unread = true;
        this.dividendDecided = false;
        this.dividendRate = 0;
        this.dividendAmount = 0L;
        this.publishedElapsedDay = company.getPlayer().getElapsedDays();
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public int getQuarterSequence() { return quarterSequence; }
    public int getEndingPeriodIndex() { return endingPeriodIndex; }
    public long getRevenue() { return revenue; }
    public long getOperatingExpenses() { return operatingExpenses; }
    public long getOperatingProfit() { return operatingProfit; }
    public long getCorporateTax() { return corporateTax; }
    public long getNetIncome() { return netIncome; }
    public long getClosingCash() { return closingCash; }
    public long getRecurringRevenueAtEnd() {
        return recurringRevenueAtEnd == null ? 0 : recurringRevenueAtEnd;
    }
    public long getPaidUsersAtEnd() { return paidUsersAtEnd == null ? 0 : paidUsersAtEnd; }
    public Long getOperatingCashFlow() { return operatingCashFlow; }
    public Long getInvestingCashFlow() { return investingCashFlow; }
    public Long getFinancingCashFlow() { return financingCashFlow; }
    public boolean isUnread() { return unread; }
    public boolean isDividendDecided() { return Boolean.TRUE.equals(dividendDecided); }
    public int getDividendRate() { return dividendRate == null ? 0 : dividendRate; }
    public long getDividendAmount() { return dividendAmount == null ? 0 : dividendAmount; }
    public int getPublishedElapsedDay() { return publishedElapsedDay == null ? 0 : publishedElapsedDay; }
    public int getDividendDecidedElapsedDay() {
        return dividendDecidedElapsedDay == null ? 0 : dividendDecidedElapsedDay;
    }

    public void markRead() {
        unread = false;
    }

    public void decideDividend(int rate, long amount) {
        if (isDividendDecided()) {
            throw new IllegalStateException("이미 배당 결정을 완료한 분기입니다.");
        }
        dividendRate = rate;
        dividendAmount = amount;
        dividendDecided = true;
        dividendDecidedElapsedDay = company.getPlayer().getElapsedDays();
    }
}
