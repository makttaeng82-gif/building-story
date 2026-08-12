package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 완전한 NPC 회사 대신 시장 경쟁에 필요한 제품 지표만 저장한다. */
@Entity
@Table(name = "company_competitor", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_competitor_key", columnNames = {"company_id", "competitor_key"}))
public class CompanyCompetitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String competitorKey;
    private String name;
    private String strategy;
    private int benchmark;
    private int completeness;
    private int stability;
    private int security;
    private double brand;
    private double marketing;
    private double marketShare;

    protected CompanyCompetitor() {
    }

    public CompanyCompetitor(PlayerCompany company, String competitorKey, String name, String strategy,
                             int benchmark, int completeness, int stability, int security,
                             double brand, double marketing, double marketShare) {
        this.company = company;
        this.competitorKey = competitorKey;
        this.name = name;
        this.strategy = strategy;
        this.benchmark = benchmark;
        this.completeness = completeness;
        this.stability = stability;
        this.security = security;
        this.brand = brand;
        this.marketing = marketing;
        this.marketShare = marketShare;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getCompetitorKey() { return competitorKey; }
    public String getName() { return name; }
    public String getStrategy() { return strategy; }
    public int getBenchmark() { return benchmark; }
    public int getCompleteness() { return completeness; }
    public int getStability() { return stability; }
    public int getSecurity() { return security; }
    public double getBrand() { return brand; }
    public double getMarketing() { return marketing; }
    public double getMarketShare() { return marketShare; }

    public void changeMarketShare(double value) {
        marketShare = Math.max(0.0, value);
    }

    public void improveBenchmark(double rate) {
        benchmark = Math.max(1, (int) Math.round(benchmark * (1.0 + rate)));
    }

    public void improveSecurity(int amount) {
        security = Math.min(100, Math.max(0, security + amount));
    }

    /** 경쟁사는 분기마다 유형에 맞는 집계 지표만 성장시킨다. */
    public void advanceQuarter() {
        double benchmarkRate;
        int benchmarkTarget;
        int completenessGrowth;
        int stabilityGrowth;
        int securityGrowth;
        double brandGrowth;
        double marketingGrowth;
        switch (competitorKey) {
            case "frontier" -> {
                benchmarkRate = 0.08;
                benchmarkTarget = 2_600;
                completenessGrowth = 1;
                stabilityGrowth = 1;
                securityGrowth = 1;
                brandGrowth = 0.5;
                marketingGrowth = 1;
            }
            case "popular" -> {
                benchmarkRate = 0.05;
                benchmarkTarget = 2_200;
                completenessGrowth = 3;
                stabilityGrowth = 1;
                securityGrowth = 1;
                brandGrowth = 3;
                marketingGrowth = 6;
            }
            default -> {
                benchmarkRate = 0.06;
                benchmarkTarget = 2_400;
                completenessGrowth = 1;
                stabilityGrowth = 3;
                securityGrowth = 4;
                brandGrowth = 1.5;
                marketingGrowth = 2;
            }
        }
        if (benchmark < benchmarkTarget) {
            double remainingRatio = (benchmarkTarget - benchmark) / (double) benchmarkTarget;
            int growth = Math.max(1, (int) Math.round(benchmark * benchmarkRate * remainingRatio));
            benchmark = Math.min(benchmarkTarget, benchmark + growth);
        }
        completeness = Math.min(100, completeness + completenessGrowth);
        stability = Math.min(100, stability + stabilityGrowth);
        security = Math.min(100, security + securityGrowth);
        brand = Math.min(100.0, brand + brandGrowth);
        marketing = Math.min(100.0, marketing + marketingGrowth);
    }
}
