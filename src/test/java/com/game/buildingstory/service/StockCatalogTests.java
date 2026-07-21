package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockCatalogTests {
    private final StockCatalog catalog = new StockCatalog();

    @Test
    void everyStockHasPositiveMarketIndustryAndIdiosyncraticParameters() {
        assertThat(catalog.all()).hasSize(15).allSatisfy(stock -> {
            assertThat(stock.beta()).isPositive();
            assertThat(stock.industryBeta()).isPositive();
            assertThat(stock.idiosyncraticVolatilityPercent()).isPositive();
        });
    }

    @Test
    void companiesInSameIndustryDoNotShareOneSensitivityValue() {
        for (String industry : catalog.all().stream().map(StockSpec::industry).distinct().toList()) {
            assertThat(catalog.all().stream()
                    .filter(stock -> stock.industry().equals(industry))
                    .map(StockSpec::industryBeta)
                    .distinct())
                    .hasSize(3);
            assertThat(catalog.all().stream()
                    .filter(stock -> stock.industry().equals(industry))
                    .map(StockSpec::idiosyncraticVolatilityPercent)
                    .distinct())
                    .hasSize(3);
        }
    }
}
