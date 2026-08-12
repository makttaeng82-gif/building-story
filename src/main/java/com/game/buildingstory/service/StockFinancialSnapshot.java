package com.game.buildingstory.service;

import java.util.List;

/** NPC와 플레이어 기업의 서로 다른 재무 저장 모델을 주식 화면용 공통 값으로 변환한 결과다. */
public record StockFinancialSnapshot(
        boolean hasReport,
        String periodText,
        long revenue,
        long operatingProfit,
        long netIncome,
        int performanceBasisPoints,
        long dividendPerShare,
        int dividendPayoutBasisPoints,
        int nextEarningsElapsedDay,
        long fairValueLower,
        long fairValueBase,
        long fairValueUpper,
        long cash,
        long debt,
        long netAssets,
        long earningsPerShare,
        long bookValuePerShare,
        List<Quarter> recentQuarters
) {
    public StockFinancialSnapshot {
        recentQuarters = List.copyOf(recentQuarters);
    }

    public record Quarter(
            String periodText,
            long revenue,
            long operatingProfit,
            long netIncome,
            int performanceBasisPoints,
            int publishedElapsedDay,
            int dividendElapsedDay,
            long dividendPerShare
    ) {
    }
}
