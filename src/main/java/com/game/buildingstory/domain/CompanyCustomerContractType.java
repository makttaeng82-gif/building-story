package com.game.buildingstory.domain;

/** 고객 계약의 규모별 기본 경제 조건이다. */
public enum CompanyCustomerContractType {
    TRIAL("시험계약", 3, 5, 400, 6, 180, Integer.MAX_VALUE),
    NORMAL("일반계약", 6, 10, 300, 12, 400, 6),
    LARGE("대형계약", 12, 18, 250, 18, 900, 9),
    STRATEGIC("전략계약", 20, 30, 200, 24, 2_000, 12);

    private final String displayName;
    private final int minimumFeePercent;
    private final int maximumFeePercent;
    private final int constructionFeePercent;
    private final int durationMonths;
    private final int totalWork;
    private final int buildDeadlineMonths;

    CompanyCustomerContractType(
            String displayName,
            int minimumFeePercent,
            int maximumFeePercent,
            int constructionFeePercent,
            int durationMonths,
            int totalWork,
            int buildDeadlineMonths
    ) {
        this.displayName = displayName;
        this.minimumFeePercent = minimumFeePercent;
        this.maximumFeePercent = maximumFeePercent;
        this.constructionFeePercent = constructionFeePercent;
        this.durationMonths = durationMonths;
        this.totalWork = totalWork;
        this.buildDeadlineMonths = buildDeadlineMonths;
    }

    public String getDisplayName() { return displayName; }
    public int getMinimumFeePercent() { return minimumFeePercent; }
    public int getMaximumFeePercent() { return maximumFeePercent; }
    public int getConstructionFeePercent() { return constructionFeePercent; }
    public int getDurationMonths() { return durationMonths; }
    public int getTotalWork() { return totalWork; }
    public int getBuildDeadlineMonths() { return buildDeadlineMonths; }
    public boolean hasRiskRules() { return this != TRIAL; }
}
