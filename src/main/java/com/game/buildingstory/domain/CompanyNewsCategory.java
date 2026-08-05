package com.game.buildingstory.domain;

/** 기업 화면에 공개되는 뉴스의 분류다. */
public enum CompanyNewsCategory {
    MARKET("시장"),
    PRODUCT("제품"),
    INFRASTRUCTURE("설비"),
    INCIDENT("사건"),
    PERFORMANCE("실적"),
    AI_MARKET("AI시장"),
    COMPETITOR("경쟁사"),
    REGULATION("규제"),
    ENTERPRISE_DEMAND("기업수요");

    private final String displayName;

    CompanyNewsCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
