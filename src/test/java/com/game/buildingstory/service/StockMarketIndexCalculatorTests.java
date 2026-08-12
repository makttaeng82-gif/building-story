package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StockMarketIndexCalculatorTests {
    private final StockCatalog catalog = new StockCatalog();
    private final StockMarketIndexCalculator calculator = new StockMarketIndexCalculator();

    @Test
    void basePricesProduceBaseIndexAndWeightsRespectCap() {
        Map<String, Long> basePrices = catalog.all().stream()
                .collect(java.util.stream.Collectors.toMap(StockSpec::key, StockSpec::basePrice));

        assertThat(calculator.calculate(catalog.all(), basePrices))
                .isEqualTo(StockMarketIndexCalculator.BASE_INDEX_BASIS_POINTS);
        assertThat(calculator.cappedWeights(catalog.all()).values())
                .allMatch(weight -> weight <= StockMarketIndexCalculator.MAX_STOCK_WEIGHT + 0.000_000_1);
        assertThat(calculator.cappedWeights(catalog.all()).values().stream().mapToDouble(Double::doubleValue).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000_000_1));
    }

    @Test
    void tenPercentRiseAcrossAllStocksRaisesIndexTenPercent() {
        Map<String, Long> raisedPrices = catalog.all().stream()
                .collect(java.util.stream.Collectors.toMap(
                        StockSpec::key,
                        stock -> Math.round(stock.basePrice() * 1.1)
                ));

        assertThat(calculator.calculate(catalog.all(), raisedPrices)).isEqualTo(110_000L);
    }

    @Test
    void addingAConstituentWithoutAPriceChangeDoesNotMoveTheLinkedIndex() {
        StockSpec newListing = catalog.all().get(15);
        var constituents = new java.util.ArrayList<>(catalog.initial());
        constituents.add(newListing);
        Map<String, Long> unchangedPrices = constituents.stream()
                .collect(java.util.stream.Collectors.toMap(StockSpec::key, StockSpec::basePrice));

        assertThat(calculator.chainLinked(123_456L, constituents, unchangedPrices, unchangedPrices))
                .isEqualTo(123_456L);
    }
}
