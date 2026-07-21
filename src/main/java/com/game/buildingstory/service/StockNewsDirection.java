package com.game.buildingstory.service;

/** 업종 사건이 주가와 실적에 주는 방향을 나타낸다. */
public enum StockNewsDirection {
    POSITIVE("호재", "up", 1),
    NEGATIVE("악재", "down", -1),
    NEUTRAL("중립", "flat", 0);

    private final String label;
    private final String cssClass;
    private final int sign;

    StockNewsDirection(String label, String cssClass, int sign) {
        this.label = label;
        this.cssClass = cssClass;
        this.sign = sign;
    }

    public String label() {
        return label;
    }

    public String cssClass() {
        return cssClass;
    }

    public int signed(int absoluteValue) {
        return sign * Math.abs(absoluteValue);
    }
}
