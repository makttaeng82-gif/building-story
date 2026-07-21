package com.game.buildingstory.service;

/** 시장 뉴스 목록과 상세 팝업이 함께 사용하는 표시 전용 데이터다. */
public record StockNewsArticleView(
        long id,
        String category,
        String categoryText,
        String scopeText,
        String directionClass,
        String title,
        String source,
        String dateText,
        String firstParagraph,
        String secondParagraph,
        String certaintyText,
        String effectText,
        boolean unread
) {
}
