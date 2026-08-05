package com.game.buildingstory.domain;

/** 다음 월 정산부터 적용할 마케팅비 지출 수준이다. */
public enum CompanyMarketingBudgetPolicy {
    MINIMUM("최소", 50),
    STANDARD("표준", 100),
    EXPANSION("확대", 150),
    AGGRESSIVE("공세", 250);

    private final String displayName;
    private final int spendingPercent;

    CompanyMarketingBudgetPolicy(String displayName, int spendingPercent) {
        this.displayName = displayName;
        this.spendingPercent = spendingPercent;
    }

    public String getDisplayName() { return displayName; }
    public int getSpendingPercent() { return spendingPercent; }
    public double effectMultiplier() { return Math.sqrt(spendingPercent / 100.0); }
}
