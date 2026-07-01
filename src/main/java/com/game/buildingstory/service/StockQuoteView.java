package com.game.buildingstory.service;

import java.util.List;

public record StockQuoteView(
        StockSpec stock,
        long currentPrice,
        long previousPrice,
        long changeAmount,
        double changePercent,
        String currentPriceText,
        String previousPriceText,
        String changePercentText,
        String changeAmountText,
        String changeDirection,
        long quantity,
        long averagePrice,
        String averagePriceText,
        long valuationProfit,
        String valuationProfitText,
        List<StockCandleView> candles,
        String minPriceText,
        String maxPriceText,
        String currentPriceY,
        String currentChartPriceText
) {
}
