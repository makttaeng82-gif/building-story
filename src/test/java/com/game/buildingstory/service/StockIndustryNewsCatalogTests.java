package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockIndustryNewsCatalogTests {
    private final StockIndustryNewsCatalog catalog = new StockIndustryNewsCatalog();

    @Test
    void catalogContainsEightDistinctEventsPerIndustryAndThreeVariantsPerEvent() {
        assertThat(catalog.all()).hasSize(40);
        assertThat(catalog.all()).extracting(StockIndustryNewsDefinition::key).doesNotHaveDuplicates();
        assertThat(catalog.all()).allSatisfy(definition -> {
            assertThat(definition.variants()).hasSize(3);
            assertThat(definition.variants())
                    .extracting(StockIndustryNewsDefinition.StockNewsVariant::title)
                    .doesNotHaveDuplicates();
        });
        assertThat(catalog.all()).filteredOn(event -> event.industry().equals("IT")).hasSize(8);
        assertThat(catalog.all()).filteredOn(event -> event.industry().equals("식품")).hasSize(8);
        assertThat(catalog.all()).filteredOn(event -> event.industry().equals("유통")).hasSize(8);
        assertThat(catalog.all()).filteredOn(event -> event.industry().equals("제조")).hasSize(8);
        assertThat(catalog.all()).filteredOn(event -> event.industry().equals("통신")).hasSize(8);
    }
}
