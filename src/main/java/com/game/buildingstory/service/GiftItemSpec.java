package com.game.buildingstory.service;

/**
 * 선물 아이템 원본 스펙이다.
 *
 * <p>minAffinityLevel/maxAffinityLevel은 해당 선물이 효과를 낼 수 있는 비서 호감도 구간이다.</p>
 */
public record GiftItemSpec(
        String key,
        String name,
        long price,
        String imagePath,
        int minAffinityLevel,
        int maxAffinityLevel,
        int affinityExperience
) {
}
