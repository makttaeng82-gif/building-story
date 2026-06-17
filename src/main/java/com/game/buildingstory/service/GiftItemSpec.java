package com.game.buildingstory.service;

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
