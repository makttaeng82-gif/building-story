package com.game.buildingstory.domain;

/** 기업이 운영하는 다섯 부서다. 지원 부서 두 곳은 출시 후 조건을 충족하면 출범한다. */
public enum CompanyDepartmentType {
    AI_DEVELOPMENT("AI개발팀"),
    SALES_MARKETING("영업마케팅팀"),
    SERVICE_OPERATIONS("서비스운영팀"),
    HR_ORGANIZATION("인사조직팀"),
    STRATEGY_FINANCE("전략재무팀");

    private final String displayName;

    CompanyDepartmentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
