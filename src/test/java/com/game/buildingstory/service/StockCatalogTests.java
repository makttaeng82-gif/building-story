package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockCatalogTests {
    private final StockCatalog catalog = new StockCatalog();

    @Test
    void everyStockHasPositiveMarketIndustryAndIdiosyncraticParameters() {
        assertThat(catalog.initial()).hasSize(15);
        assertThat(catalog.all()).hasSize(31).allSatisfy(stock -> {
            assertThat(stock.beta()).isPositive();
            assertThat(stock.industryBeta()).isPositive();
            assertThat(stock.idiosyncraticVolatilityPercent()).isPositive();
        });
    }

    @Test
    void companiesInSameIndustryDoNotShareOneSensitivityValue() {
        for (String industry : catalog.initial().stream().map(StockSpec::industry).distinct().toList()) {
            assertThat(catalog.initial().stream()
                    .filter(stock -> stock.industry().equals(industry))
                    .map(StockSpec::industryBeta)
                    .distinct())
                    .hasSize(3);
            assertThat(catalog.initial().stream()
                    .filter(stock -> stock.industry().equals(industry))
                    .map(StockSpec::idiosyncraticVolatilityPercent)
                    .distinct())
                    .hasSize(3);
        }
    }
}
