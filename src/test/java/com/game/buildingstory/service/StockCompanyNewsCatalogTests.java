package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockCompanyNewsCatalogTests {
    private final StockCompanyNewsCatalog catalog = new StockCompanyNewsCatalog();

    @Test
    void catalogContainsSixDistinctEventsPerCompanyAndThreeVariantsPerEvent() {
        assertThat(catalog.all()).hasSize(90);
        assertThat(catalog.all()).extracting(StockCompanyNewsDefinition::key).doesNotHaveDuplicates();
        assertThat(catalog.all()).allSatisfy(definition -> {
            assertThat(definition.variants()).hasSize(3);
            assertThat(definition.variants())
                    .extracting(StockCompanyNewsDefinition.StockNewsVariant::title)
                    .doesNotHaveDuplicates();
        });
        assertThat(catalog.all().stream().map(StockCompanyNewsDefinition::stockKey).distinct()).hasSize(15);
        catalog.all().stream().map(StockCompanyNewsDefinition::stockKey).distinct().forEach(stockKey ->
                assertThat(catalog.all()).filteredOn(event -> event.stockKey().equals(stockKey)).hasSize(6)
        );
    }
}
