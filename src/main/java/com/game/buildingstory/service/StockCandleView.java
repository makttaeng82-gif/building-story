package com.game.buildingstory.service;

public record StockCandleView(
        int x,
        int openY,
        int highY,
        int lowY,
        int closeY,
        int bodyY,
        int bodyHeight,
        boolean rising,
        String dateText
) {
}
