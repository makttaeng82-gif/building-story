package com.game.buildingstory.web;

import java.util.List;

/** 기업 화면에 표시할 단기 사업의 현재 진행 상태와 제안 목록이다. */
public record CompanyShortTermProjectView(
        String status,
        String activeName,
        int progressPercent,
        String progressText,
        String monthlyAmount,
        Long projectId,
        boolean riskDecisionRequired,
        String riskTitle,
        String riskDetail,
        String extraSupportCost,
        List<Offer> offers
) {
    public record Offer(
            long id,
            String name,
            String work,
            String expectedRevenue,
            String totalCost,
            String expiresText,
            boolean available,
            String availabilityReason
    ) {
    }
}
