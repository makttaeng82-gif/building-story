package com.game.buildingstory.web;

/** 기업 요약 아래에 표시할 현재 성장단계와 다음 승급 조건이다. */
public record CompanyGrowthProgressView(
        String currentStage,
        String nextStage,
        String recurringRevenue,
        String requiredRecurringRevenue,
        String remainingRecurringRevenue,
        int recurringRevenuePercent,
        String paidUsers,
        String requiredPaidUsers,
        String remainingPaidUsers,
        int paidUsersPercent,
        int qualifiedQuarters,
        String basisText,
        String unlockText,
        boolean maximumStage
) {
}
