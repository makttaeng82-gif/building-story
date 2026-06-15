package com.game.buildingstory.service;

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
