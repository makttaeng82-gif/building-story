package com.game.buildingstory.service;

/** 상장기업의 세계관 설정과 장기적으로 변하지 않는 투자 특성이다. */
public record StockCompanyOverview(
        String stockKey,
        String chiefExecutive,
        String foundedText,
        String headquarters,
        String mainRevenueSource,
        String keyRisk,
        String cyclicality
) {
}
