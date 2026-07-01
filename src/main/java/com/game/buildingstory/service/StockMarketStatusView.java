package com.game.buildingstory.service;

public record StockMarketStatusView(
        String nextUpdateDateText,
        int daysUntilNextUpdate,
        int progressPercent,
        String activeNewsText,
        String activeNewsDirection
) {
    public boolean hasActiveNews() {
        return activeNewsText != null && !activeNewsText.isBlank();
    }
}
