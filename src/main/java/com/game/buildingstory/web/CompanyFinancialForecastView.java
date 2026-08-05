package com.game.buildingstory.web;

/**
 * 경영 보고 영역에 표시할 다음 달 재무 전망이다.
 *
 * 전망 계산 자체는 서비스 계층에서 수행하고, 화면에는 이미 서식이 적용된 문자열만 전달한다.
 * 이를 통해 Thymeleaf 템플릿이 금액 계산이나 오차 범위 계산을 직접 담당하지 않게 한다.
 */
public record CompanyFinancialForecastView(
        boolean available,
        String revenueRange,
        String operatingProfitRange,
        String closingCashRange,
        String accuracyText,
        String basisText
) {
    public static CompanyFinancialForecastView unavailable(String basisText) {
        return new CompanyFinancialForecastView(false, "-", "-", "-", "전망 준비 중", basisText);
    }
}
