package com.game.buildingstory.service;

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
