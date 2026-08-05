package com.game.buildingstory.domain;

/** 서비스 장애의 처리 수준을 결정하는 심각도다. */
public enum CompanyServiceIncidentSeverity {
    MINOR("경미"),
    MAJOR("중대"),
    CRITICAL("치명");

    private final String displayName;

    CompanyServiceIncidentSeverity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
