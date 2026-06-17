package com.game.buildingstory.service;

public record LuxuryItemSpec(
        String key,
        String name,
        long price,
        String imagePath
) {
    public int reputationReward() {
        return Math.toIntExact(price * 3 / 100_000);
    }
}
