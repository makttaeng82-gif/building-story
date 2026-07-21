package com.game.buildingstory.service;

/**
 * 종목 목록과 보유종목 표에 필요한 값만 담는 경량 시세다.
 *
 * <p>캔들 좌표와 차트 범위는 포함하지 않는다. 모든 종목의 목록 시세를 한 번에 만들고,
 * 무거운 차트 데이터는 사용자가 선택한 한 종목에서만 조회하기 위한 모델이다.</p>
 */
public record StockListQuoteView(
        StockSpec stock,
        long currentPrice,
        long previousPrice,
        long changeAmount,
        double changePercent,
        String currentPriceText,
        String changePercentText,
        String changeAmountText,
        String changeDirection,
        long quantity,
        long averagePrice,
        String averagePriceText,
        long totalCostBasis,
        long valuationProfit,
        String valuationProfitText,
        String valuationText,
        String marketCapText,
        String ownershipPercentText,
        long remainingMarketBuyQuantity
) {
}
