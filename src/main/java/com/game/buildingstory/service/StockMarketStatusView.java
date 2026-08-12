package com.game.buildingstory.service;

/**
 * 주식 화면 상단 상태 카드에 필요한 값을 담는 뷰 모델이다.
 *
 * <p>다음 주가 갱신일, 진행률, 현재 적용 중인 업종 뉴스 배지를 한 객체에 담아
 * 템플릿이 계산 없이 표시만 하도록 만든다.</p>
 */
public record StockMarketStatusView(
        String nextUpdateDateText,
        int daysUntilNextUpdate,
        int progressPercent,
        String activeNewsText,
        String activeNewsDirection,
        String regimeText,
        String indexValueText,
        String indexChangeText,
        String indexDirection,
        int indexConstituentCount
) {
    public boolean hasActiveNews() {
        return activeNewsText != null && !activeNewsText.isBlank();
    }
}
