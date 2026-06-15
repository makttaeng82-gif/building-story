package com.game.buildingstory.service;

public record SecretarySpec(
        String name,
        String origin,
        String imagePath,
        int requiredReputation,
        long monthlySalary,
        int baseProficiency,
        String effect,
        String growthBonus,
        String unlockNote
) {
}
