package com.game.buildingstory.service;

/**
 * 주식 종목의 내부 변동성 등급이다.
 *
 * <p>화면에는 직접 표시하지 않지만, 가격 갱신 시 무작위 노이즈 범위를 결정한다.
 * SAFE는 작게 흔들리고, AGGRESSIVE는 크게 흔들린다.</p>
 */
public enum StockRiskType {
    SAFE("안전", -2.0, 2.0),
    NORMAL("보통", -4.0, 4.0),
    AGGRESSIVE("공격", -7.0, 7.0);

    private final String label;
    private final double minNoisePercent;
    private final double maxNoisePercent;

    StockRiskType(String label, double minNoisePercent, double maxNoisePercent) {
        this.label = label;
        this.minNoisePercent = minNoisePercent;
        this.maxNoisePercent = maxNoisePercent;
    }

    public String label() {
        return label;
    }

    public double minNoisePercent() {
        return minNoisePercent;
    }

    public double maxNoisePercent() {
        return maxNoisePercent;
    }
}
