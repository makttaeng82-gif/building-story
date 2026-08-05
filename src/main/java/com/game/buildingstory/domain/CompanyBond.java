package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_bond")
public class CompanyBond {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private CompanyBondStatus status;

    private long principal;
    private long monthlyInterest;
    private int issuedPeriodIndex;
    private int maturityPeriodIndex;

    protected CompanyBond() {
    }

    public CompanyBond(
            PlayerCompany company,
            long principal,
            long monthlyInterest,
            int issuedPeriodIndex,
            int maturityPeriodIndex
    ) {
        this.company = company;
        this.status = CompanyBondStatus.ACTIVE;
        this.principal = principal;
        this.monthlyInterest = monthlyInterest;
        this.issuedPeriodIndex = issuedPeriodIndex;
        this.maturityPeriodIndex = maturityPeriodIndex;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public CompanyBondStatus getStatus() { return status; }
    public long getPrincipal() { return principal; }
    public long getMonthlyInterest() { return monthlyInterest; }
    public int getIssuedPeriodIndex() { return issuedPeriodIndex; }
    public int getMaturityPeriodIndex() { return maturityPeriodIndex; }

    public boolean isOutstanding() {
        return status == CompanyBondStatus.ACTIVE || status == CompanyBondStatus.DEFAULTED;
    }

    public void repay() {
        status = CompanyBondStatus.REPAID;
    }

    public void markDefaulted() {
        status = CompanyBondStatus.DEFAULTED;
    }
}
