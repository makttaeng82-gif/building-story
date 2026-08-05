package com.game.buildingstory.domain;

/** 서비스 장애가 현재 요구하는 플레이어 행동과 처리 상태다. */
public enum CompanyServiceIncidentStatus {
    RESOLVED,
    AWAITING_DECISION,
    RESPONSE_REQUIRED,
    RESPONSE_IN_PROGRESS,
    COMPLETED,
    FAILED
}
