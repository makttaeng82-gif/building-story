package com.game.buildingstory.domain;

/** 기업 고객 계약이 제안부터 종료까지 거치는 상태다. */
public enum CompanyCustomerContractStatus {
    OFFERED,
    BUILDING,
    COMPLETION_PAYMENT,
    ACTIVE,
    RENEWAL_OFFERED,
    CONTRACT_FAILURE_PAYMENT,
    TERMINATION_PAYMENT,
    COMPLETED,
    FAILED,
    TERMINATED,
    EXPIRED,
    REJECTED
}
