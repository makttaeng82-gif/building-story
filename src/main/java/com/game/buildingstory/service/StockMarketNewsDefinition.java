package com.game.buildingstory.service;

import java.util.List;

/** 전체 상장시장에 영향을 주는 거시 사건과 기사 문구 원본이다. */
public record StockMarketNewsDefinition(
        String key,
        String family,
        StockNewsDirection direction,
        StockNewsCertainty certainty,
        String source,
        int priceImpactBasisPoints,
        int durationRefreshes,
        int confirmationChancePercent,
        boolean randomPublication,
        List<StockNewsVariant> variants
) {
    public StockMarketNewsDefinition {
        if (variants == null || variants.size() != 3) {
            throw new IllegalArgumentException("시장 뉴스는 기사 문구 3세트를 가져야 합니다.");
        }
    }

    public record StockNewsVariant(String title, String firstParagraph, String secondParagraph) {
    }
}
