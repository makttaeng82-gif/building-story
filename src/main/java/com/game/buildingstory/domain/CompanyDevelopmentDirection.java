package com.game.buildingstory.domain;

/** 모델 개발 프로젝트에서 성능과 운영 안정성 사이의 우선순위를 정한다. */
public enum CompanyDevelopmentDirection {
    BALANCED("균형 개발", "기본 성과와 부작용을 그대로 적용"),
    PERFORMANCE("성능 우선", "벤치마크 상승 강화 · 연산효율과 기술부채 부담 증가"),
    EFFICIENCY("효율 우선", "벤치마크 상승 감소 · 연산효율 개선"),
    STABILITY("안정성 우선", "벤치마크 상승 감소 · 안정성·보안 개선");

    private final String displayName;
    private final String description;

    CompanyDevelopmentDirection(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
