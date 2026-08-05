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

/** 자체 연산망 한 단계의 공사 진행과 분할 지급 상태를 저장한다. */
@Entity
@Table(name = "company_compute_construction")
public class CompanyComputeConstruction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Enumerated(EnumType.STRING)
    private CompanyComputeTier tier;
    private int phase;

    @Enumerated(EnumType.STRING)
    private CompanyComputeConstructionStatus status;

    private int totalMonths;
    private int elapsedMonths;
    private long totalCost;
    private long upfrontPayment;
    private long completionPayment;
    private int serviceOperationsWorkload;
    private int startedElapsedDay;
    private Integer completedElapsedDay;

    protected CompanyComputeConstruction() {
    }

    public CompanyComputeConstruction(PlayerCompany company, CompanyComputeTier tier, int phase,
                                      int serviceOperationsWorkload, int startedElapsedDay) {
        this(company, tier, phase, serviceOperationsWorkload, startedElapsedDay,
                tier.constructionMonths(phase), tier.constructionCost(phase));
    }

    public CompanyComputeConstruction(
            PlayerCompany company,
            CompanyComputeTier tier,
            int phase,
            int serviceOperationsWorkload,
            int startedElapsedDay,
            int totalMonths,
            long totalCost
    ) {
        this.company = company;
        this.tier = tier;
        this.phase = phase;
        this.status = CompanyComputeConstructionStatus.ACTIVE;
        this.totalMonths = totalMonths;
        this.totalCost = totalCost;
        this.upfrontPayment = totalCost / 2;
        this.completionPayment = totalCost - upfrontPayment;
        this.serviceOperationsWorkload = serviceOperationsWorkload;
        this.startedElapsedDay = startedElapsedDay;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public CompanyComputeTier getTier() { return tier; }
    public int getPhase() { return phase; }
    public CompanyComputeConstructionStatus getStatus() { return status; }
    public int getTotalMonths() { return totalMonths; }
    public int getElapsedMonths() { return elapsedMonths; }
    public long getTotalCost() { return totalCost; }
    public long getUpfrontPayment() { return upfrontPayment; }
    public long getCompletionPayment() { return completionPayment; }
    public int getServiceOperationsWorkload() { return serviceOperationsWorkload; }
    public int getStartedElapsedDay() { return startedElapsedDay; }
    public Integer getCompletedElapsedDay() { return completedElapsedDay; }
    public String displayName() { return tier.getDisplayName() + " 연산망 " + tier.phaseName(phase); }
    public int progressPercent() { return totalMonths == 0 ? 100 : (int) Math.round(elapsedMonths * 100.0 / totalMonths); }

    public boolean advanceMonth() {
        if (status != CompanyComputeConstructionStatus.ACTIVE) {
            return status == CompanyComputeConstructionStatus.PAYMENT_DUE;
        }
        elapsedMonths = Math.min(totalMonths, elapsedMonths + 1);
        if (elapsedMonths >= totalMonths) {
            status = CompanyComputeConstructionStatus.PAYMENT_DUE;
            return true;
        }
        return false;
    }

    public void complete(int elapsedDay) {
        if (status != CompanyComputeConstructionStatus.PAYMENT_DUE) {
            throw new IllegalStateException("완공 대금 지급 단계가 아닙니다");
        }
        status = CompanyComputeConstructionStatus.COMPLETED;
        completedElapsedDay = elapsedDay;
    }

    public void fail(int elapsedDay) {
        if (status != CompanyComputeConstructionStatus.ACTIVE
                && status != CompanyComputeConstructionStatus.PAYMENT_DUE) {
            return;
        }
        status = CompanyComputeConstructionStatus.FAILED;
        completedElapsedDay = elapsedDay;
    }
}
