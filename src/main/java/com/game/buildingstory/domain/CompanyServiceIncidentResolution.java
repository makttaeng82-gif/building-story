package com.game.buildingstory.domain;

/** 중대 장애에서 선택하는 단순화된 대응 방식이다. */
public enum CompanyServiceIncidentResolution {
    EMERGENCY_RECOVERY("긴급 복구"),
    CUSTOMER_COMPENSATION("고객 보상");

    private final String displayName;

    CompanyServiceIncidentResolution(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
