package com.game.buildingstory.service;

public record StockMarketStatusView(
        String nextUpdateDateText,
        int daysUntilNextUpdate,
        int progressPercent
) {
}
