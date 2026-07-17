package com.game.buildingstory.service;

import java.util.List;

/**
 * 주식 상세 화면과 종목 목록에 표시할 값을 모은 읽기 전용 뷰 모델이다.
 *
 * <p>엔티티는 DB 저장용 구조이고, 이 record는 화면 표시용 구조다.
 * 현재가, 직전가, 손익 텍스트, SVG 캔들 좌표처럼 템플릿이 바로 출력할 값을
 * 서비스에서 미리 계산해 담는다.</p>
 */
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
