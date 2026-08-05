package com.game.buildingstory.web;

/** 기업뉴스 목록과 상세 팝업에 사용하는 읽기 전용 모델이다. */
public record CompanyNewsView(
        long id,
        String dateText,
        String category,
        String title,
        String body,
        String source,
        boolean unread
) {
}
