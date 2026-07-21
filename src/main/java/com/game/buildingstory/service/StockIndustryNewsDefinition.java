package com.game.buildingstory.service;

import java.util.List;

/** 코드에 고정된 업종 사건 원본이다. 실제 발행된 문장은 StockNewsArticle에 별도로 저장된다. */
public record StockIndustryNewsDefinition(
        String key,
        String industry,
        String family,
        StockNewsDirection direction,
        StockNewsCertainty certainty,
        String source,
        int priceImpactBasisPoints,
        int financialImpactBasisPoints,
        int durationRefreshes,
        int confirmationChancePercent,
        List<StockNewsVariant> variants
) {
    public StockIndustryNewsDefinition {
        if (variants == null || variants.size() != 3) {
            throw new IllegalArgumentException("업종 뉴스는 기사 문구 3세트를 가져야 합니다.");
        }
    }

    public record StockNewsVariant(String title, String firstParagraph, String secondParagraph) {
    }
}
