package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 기준 시가총액 가중치와 현재 주가를 사용해 종합지수를 계산하는 순수 계산기다. */
@Component
public class StockMarketIndexCalculator {
    public static final long BASE_INDEX_BASIS_POINTS = 100_000L;
    public static final double MAX_STOCK_WEIGHT = 0.15;

    public long calculate(List<StockSpec> stocks, Map<String, Long> currentPrices) {
        Map<String, Double> weights = cappedWeights(stocks);
        double weightedPriceRatio = stocks.stream()
                .mapToDouble(stock -> weights.get(stock.key())
                        * currentPrices.getOrDefault(stock.key(), stock.basePrice())
                        / (double) stock.basePrice())
                .sum();
        return Math.max(1L, Math.round(BASE_INDEX_BASIS_POINTS * weightedPriceRatio));
    }

    Map<String, Double> cappedWeights(List<StockSpec> stocks) {
        Map<String, Double> weights = new HashMap<>();
        Set<StockSpec> remaining = new LinkedHashSet<>(stocks);
        double remainingWeight = 1.0;

        while (!remaining.isEmpty()) {
            double remainingMarketCap = remaining.stream().mapToDouble(this::baseMarketCap).sum();
            double weightToDistribute = remainingWeight;
            List<StockSpec> overCap = remaining.stream()
                    .filter(stock -> weightToDistribute * baseMarketCap(stock) / remainingMarketCap > MAX_STOCK_WEIGHT)
                    .toList();
            if (overCap.isEmpty()) {
                for (StockSpec stock : remaining) {
                    weights.put(stock.key(), remainingWeight * baseMarketCap(stock) / remainingMarketCap);
                }
                break;
            }
            for (StockSpec stock : overCap) {
                weights.put(stock.key(), MAX_STOCK_WEIGHT);
                remaining.remove(stock);
                remainingWeight -= MAX_STOCK_WEIGHT;
            }
        }
        return weights;
    }

    private double baseMarketCap(StockSpec stock) {
        return stock.basePrice() * (double) stock.issuedShares();
    }
}
