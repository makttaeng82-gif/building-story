package com.game.buildingstory.service;

/** 최근 분기 실적 표의 한 행에 필요한 표시값이다. */
public record StockQuarterSummaryView(
        String periodText,
        String revenueText,
        String operatingProfitText,
        String netIncomeText,
        String surpriseText,
        String surpriseDirection
) {
}
