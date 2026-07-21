package com.game.buildingstory.service;

import com.game.buildingstory.domain.StockPriceHistory;

/** 차트 한 지점에 필요한 가격, 지수, 뉴스, 실적 사건을 묶은 내부 데이터다. */
public record StockChartPoint(
        StockPriceHistory price,
        long marketIndexBasisPoints,
        Long newsArticleId,
        String newsTitle,
        int newsCount,
        boolean earningsEvent,
        boolean dividendEvent
) {
}
