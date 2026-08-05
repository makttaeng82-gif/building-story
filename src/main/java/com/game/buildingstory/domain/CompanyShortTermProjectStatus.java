package com.game.buildingstory.domain;

/** 단기 사업이 제안부터 수익 종료까지 거치는 상태다. */
public enum CompanyShortTermProjectStatus {
    OFFERED,
    ACTIVE,
    EARNING,
    COMPLETED,
    EXPIRED,
    REJECTED,
    CANCELLED,
    FAILED
}
