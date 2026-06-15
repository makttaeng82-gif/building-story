package com.game.buildingstory.domain;

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
