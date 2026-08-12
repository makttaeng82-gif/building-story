package com.game.buildingstory.service;

/** 공모주 청약 팝업의 최근 분기 실적 한 행이다. */
public record NpcIpoQuarterView(
        String periodText,
        String revenueText,
        String operatingProfitText,
        String netIncomeText,
        String growthText,
        String growthDirection
) {
}
