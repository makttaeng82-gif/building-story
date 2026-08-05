package com.game.buildingstory.web;

/** 핵심제품 상세에 표시할 현재 서비스 장애와 대응 선택이다. */
public record CompanyServiceIncidentView(
        String status,
        String title,
        String detail,
        String probability,
        String tone,
        int progressPercent,
        Long incidentId,
        boolean decisionRequired,
        boolean responseRequired,
        String emergencyRecoveryCost,
        String customerCompensationCost,
        String monthlyResponseCost
) {
}
