package com.game.buildingstory.service;

/**
 * 주식 종목의 내부 변동성 등급이다.
 *
 * <p>화면 표시용 위험 성격과 한 캔들 안의 고가·저가 경로 흔들림을 결정한다.
 * 종가의 기업 고유 변동성은 StockSpec에서 종목별로 별도 관리한다.</p>
 */
public enum StockRiskType {
    SAFE("안전", 0.006),
    NORMAL("보통", 0.011),
    AGGRESSIVE("공격", 0.018);

    private final String label;
    private final double pathNoise;

    StockRiskType(String label, double pathNoise) {
        this.label = label;
        this.pathNoise = pathNoise;
    }

    public String label() {
        return label;
    }

    public double pathNoise() {
        return pathNoise;
    }
}
