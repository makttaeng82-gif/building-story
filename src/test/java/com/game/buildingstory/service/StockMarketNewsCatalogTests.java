package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockMarketNewsCatalogTests {
    private final StockMarketNewsCatalog catalog = new StockMarketNewsCatalog();

    @Test
    void catalogContainsTwelveRandomEventsAndThreeRegimeArticles() {
        assertThat(catalog.all()).hasSize(15);
        assertThat(catalog.all()).extracting(StockMarketNewsDefinition::key).doesNotHaveDuplicates();
        assertThat(catalog.all()).filteredOn(StockMarketNewsDefinition::randomPublication).hasSize(12);
        assertThat(catalog.all()).filteredOn(event -> !event.randomPublication()).hasSize(3)
                .allSatisfy(event -> {
                    assertThat(event.priceImpactBasisPoints()).isZero();
                    assertThat(event.durationRefreshes()).isZero();
                });
        assertThat(catalog.all()).allSatisfy(event ->
                assertThat(event.variants()).hasSize(3)
        );
    }
}
