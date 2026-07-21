package com.game.buildingstory.service;

import java.util.List;

/** 종목 상세 하단의 기업 개요, 최근 실적, 재무·가치평가 표시값을 묶는다. */
public record StockCompanyDetailView(
        String chiefExecutive,
        String foundedText,
        String headquarters,
        String mainBusiness,
        String mainRevenueSource,
        String keyRisk,
        String cyclicality,
        String cashText,
        String debtText,
        String netAssetsText,
        String debtRatioText,
        String financialHealthText,
        String financialHealthDirection,
        String earningsPerShareText,
        String bookValuePerShareText,
        String priceEarningsRatioText,
        String priceBookRatioText,
        List<StockQuarterSummaryView> recentQuarters
) {
}
