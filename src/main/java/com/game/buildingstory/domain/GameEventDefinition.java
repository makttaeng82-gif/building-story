package com.game.buildingstory.domain;

/**
 * 고정 날짜 이벤트의 원본 정의다.
 *
 * <p>이 record는 카탈로그 데이터이고 DB에 직접 저장되지 않는다. 실제 플레이어에게 표시될 때는
 * GameEvent 엔티티로 복사된다.</p>
 */
public record GameEventDefinition(
        String key,
        int month,
        int day,
        String title,
        String body,
        String imageLabel,
        String effectKey
) {
}
