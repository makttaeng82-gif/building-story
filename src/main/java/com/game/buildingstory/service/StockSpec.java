package com.game.buildingstory.service;

import java.util.Locale;

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
        double beta,
        double industryBeta,
        double idiosyncraticVolatilityPercent,
        long basePrice,
        long issuedShares,
        String description
) {
    /** 시장 수익률에 대한 민감도를 화면에서 읽기 쉬운 두 자리 소수로 표시한다. */
    public String betaText() {
        return String.format(Locale.ROOT, "%.2f", beta);
    }

    public String industryBetaText() {
        return String.format(Locale.ROOT, "%.2f", industryBeta);
    }
}
