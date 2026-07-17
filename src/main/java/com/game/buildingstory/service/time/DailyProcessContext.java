package com.game.buildingstory.service.time;

import com.game.buildingstory.domain.Player;

/**
 * 하루 처리 단계들이 공통으로 사용하는 입력값이다.
 *
 * @param player 날짜가 이미 하루 증가한 플레이어
 * @param deferCityEvents 주식 화면처럼 도시 이벤트 화면 전환을 미룰지 여부
 */
public record DailyProcessContext(Player player, boolean deferCityEvents) {
}
