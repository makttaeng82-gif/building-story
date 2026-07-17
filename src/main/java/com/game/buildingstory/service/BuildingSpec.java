package com.game.buildingstory.service;

/**
 * 건물 카탈로그의 원본 스펙이다.
 *
 * <p>아직 플레이어가 소유하지 않은 "기본 설계도"이며, 매물 생성과 경매 생성 시
 * 이 값에서 도시, 슬롯, 가격, 월세, 거래 대기일을 복사한다.</p>
 */
public record BuildingSpec(
        String city,
        int slot,
        String typeName,
        String name,
        long marketPrice,
        long monthlyRent,
        int tradeCooldownDays
) {
}
