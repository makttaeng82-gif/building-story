package com.game.buildingstory.service;

/**
 * 주식 보유요약 패널에 표시할 합산 값을 담는다.
 *
 * <p>개별 종목의 현재가와 평균단가를 합산해 전체 원가, 평가금액, 평가손익을 계산한 결과다.</p>
 */
public record StockHoldingSummaryView(
        long holdingCount,
        long totalQuantity,
        long totalCost,
        long totalValuation,
        long totalProfit,
        String totalCostText,
        String totalValuationText,
        String totalProfitText,
        String profitDirection
) {
}
