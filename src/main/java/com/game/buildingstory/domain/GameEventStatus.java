package com.game.buildingstory.domain;

/**
 * 일반 게임 이벤트 모달의 상태다.
 *
 * <p>ACTIVE 이벤트는 화면에 표시되고, COMPLETED 이벤트는 다시 표시하지 않는다.</p>
 */
public enum GameEventStatus {
    ACTIVE,
    COMPLETED
}
