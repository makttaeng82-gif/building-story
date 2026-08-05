package com.game.buildingstory.domain;

/** 다음 월 정산부터 적용할 제품개발비 지출 수준이다. */
public enum CompanyDevelopmentBudgetPolicy {
    AUSTERITY("긴축", 60),
    STANDARD("표준", 100),
    EXPANSION("확대", 140),
    FOCUSED("집중", 200);

    private final String displayName;
    private final int spendingPercent;

    CompanyDevelopmentBudgetPolicy(String displayName, int spendingPercent) {
        this.displayName = displayName;
        this.spendingPercent = spendingPercent;
    }

    public String getDisplayName() { return displayName; }
    public int getSpendingPercent() { return spendingPercent; }
    public double effectMultiplier() { return Math.sqrt(spendingPercent / 100.0); }
}
