package com.game.buildingstory.domain;

/** 최초 상용화 튜토리얼의 저장 가능한 진행 단계다. */
public enum CompanyTutorialStage {
    FOUNDING_HIRE,
    READY_TO_DEVELOP,
    COMMERCIALIZATION_IN_PROGRESS,
    LAUNCH_REVIEW,
    LAUNCHED,
    FIRST_SETTLEMENT,
    COMPLETED;

    public boolean isOperational() {
        return this == LAUNCHED || this == FIRST_SETTLEMENT || this == COMPLETED;
    }
}
