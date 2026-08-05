package com.game.buildingstory.domain;

/** 환불 요구가 발생한 단기 사업에서 플레이어가 선택하는 대응이다. */
public enum CompanyShortTermProjectRiskResolution {
    EXTRA_SUPPORT("추가 지원"),
    PARTIAL_REFUND("부분 환불");

    private final String displayName;

    CompanyShortTermProjectRiskResolution(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
