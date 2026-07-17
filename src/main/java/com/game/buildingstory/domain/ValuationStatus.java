package com.game.buildingstory.domain;

/**
 * 부동산 가격 평가 상태다.
 *
 * <p>매물 생성이나 건물 매각 시 기준가에 rate를 곱해 실제 거래가를 만든다.</p>
 */
public enum ValuationStatus {
    UNDER("저평가", 80),
    FAIR("시장가", 100),
    OVER("고평가", 120);

    private final String label;
    private final int rate;

    ValuationStatus(String label, int rate) {
        this.label = label;
        this.rate = rate;
    }

    public String label() {
        return label;
    }

    public int rate() {
        return rate;
    }
}
