package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "company_investment_round", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_investment_round", columnNames = {"company_id", "round_number"}))
public class CompanyInvestmentRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private int roundNumber;

    @Enumerated(EnumType.STRING)
    private CompanyInvestmentRoundType roundType;

    private int targetOwnershipPercent;
    private long preMoneyValue;
    private long investmentAmount;
    private long postMoneyValue;
    private long issuedShares;
    private int completedQuarterSequence;

    protected CompanyInvestmentRound() {
    }

    public CompanyInvestmentRound(
            PlayerCompany company,
            int roundNumber,
            CompanyInvestmentRoundType roundType,
            int targetOwnershipPercent,
            long preMoneyValue,
            long investmentAmount,
            long postMoneyValue,
            long issuedShares,
            int completedQuarterSequence
    ) {
        this.company = company;
        this.roundNumber = roundNumber;
        this.roundType = roundType;
        this.targetOwnershipPercent = targetOwnershipPercent;
        this.preMoneyValue = preMoneyValue;
        this.investmentAmount = investmentAmount;
        this.postMoneyValue = postMoneyValue;
        this.issuedShares = issuedShares;
        this.completedQuarterSequence = completedQuarterSequence;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public int getRoundNumber() { return roundNumber; }
    public CompanyInvestmentRoundType getRoundType() { return roundType; }
    public int getTargetOwnershipPercent() { return targetOwnershipPercent; }
    public long getPreMoneyValue() { return preMoneyValue; }
    public long getInvestmentAmount() { return investmentAmount; }
    public long getPostMoneyValue() { return postMoneyValue; }
    public long getIssuedShares() { return issuedShares; }
    public int getCompletedQuarterSequence() { return completedQuarterSequence; }
}
