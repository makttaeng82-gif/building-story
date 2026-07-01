package com.game.buildingstory.service;

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
