package com.game.buildingstory.service;

/**
 * 주식 종목 원본 스펙이다.
 *
 * <p>key는 DB와 URL에서 쓰는 안정적인 식별자이고, name은 화면 표시용 이름이다.</p>
 */
public record StockSpec(
        String key,
        String industry,
        String name,
        StockRiskType riskType,
        long basePrice
) {
}
