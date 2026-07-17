package com.game.buildingstory.service;

/**
 * 명품 아이템 원본 스펙이다.
 *
 * <p>구매 가격, 표시 이름, 이미지 경로를 담는다. 실제 구매 여부는 OwnedLuxuryItem에 저장된다.</p>
 */
public record LuxuryItemSpec(
        String key,
        String name,
        long price,
        int reputationReward,
        String imagePath
) {
}
