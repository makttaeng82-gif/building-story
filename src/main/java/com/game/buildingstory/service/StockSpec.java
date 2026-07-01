package com.game.buildingstory.service;

public record StockSpec(
        String key,
        String industry,
        String name,
        StockRiskType riskType,
        long basePrice
) {
}
