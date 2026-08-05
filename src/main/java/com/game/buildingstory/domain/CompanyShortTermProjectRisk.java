package com.game.buildingstory.domain;

/** 단기 사업 완료 뒤 발생할 수 있는 사후 위험이다. */
public enum CompanyShortTermProjectRisk {
    NONE("이상 없음"),
    AFTER_SERVICE("추가 AS"),
    REFUND_REQUEST("환불 요구"),
    TREND_FADE("시장 유행 종료");

    private final String displayName;

    CompanyShortTermProjectRisk(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
