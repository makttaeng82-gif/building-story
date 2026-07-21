package com.game.buildingstory.service;

import java.util.List;

/** 한 상장기업에만 적용되는 사건과 기사 원본이다. */
public record StockCompanyNewsDefinition(
        String key,
        String stockKey,
        String companyName,
        String industry,
        String family,
        StockNewsDirection direction,
        StockNewsCertainty certainty,
        String source,
        int priceImpactBasisPoints,
        int durationRefreshes,
        int confirmationChancePercent,
        StockCompanyFinancialEffect financialEffect,
        List<StockNewsVariant> variants
) {
    public StockCompanyNewsDefinition {
        if (variants == null || variants.size() != 3) {
            throw new IllegalArgumentException("기업 뉴스는 기사 문구 3세트를 가져야 합니다.");
        }
    }

    public record StockNewsVariant(String title, String firstParagraph, String secondParagraph) {
    }
}
