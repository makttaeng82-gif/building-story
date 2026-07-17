package com.game.buildingstory.domain;

/**
 * 경매 이벤트의 진행 상태다.
 *
 * <p>ACTIVE는 입찰 가능, RESULT는 결과 표시 중, COMPLETED는 더 이상 화면에 표시하지 않는 종료 상태다.</p>
 */
public enum AuctionStatus {
    ACTIVE,
    RESULT,
    COMPLETED
}
