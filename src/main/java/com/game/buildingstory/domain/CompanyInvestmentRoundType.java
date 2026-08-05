package com.game.buildingstory.domain;

public enum CompanyInvestmentRoundType {
    GROWTH("성장 투자"),
    EXPANSION("확장 투자"),
    PRE_IPO("상장 전 투자");

    private final String displayName;

    CompanyInvestmentRoundType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
