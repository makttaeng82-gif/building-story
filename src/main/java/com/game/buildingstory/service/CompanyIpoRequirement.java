package com.game.buildingstory.service;

/** IPO 신청 화면과 서버 검증이 공유하는 자격조건 식별자다. */
public enum CompanyIpoRequirement {
    NO_EXISTING_LISTING,
    GROWTH_STAGE,
    REPORT_HISTORY,
    PROFITABILITY,
    RECURRING_REVENUE,
    PAID_USERS,
    BENCHMARK,
    EQUITY_VALUE,
    STRATEGY_FINANCE,
    FINANCIAL_HEALTH,
    NO_CRITICAL_INCIDENT
}
