package com.game.buildingstory.service;

public enum StockMarketRegime {
    EXPANSION("확장"),
    NEUTRAL("중립"),
    RECESSION("침체");

    private final String label;

    StockMarketRegime(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
