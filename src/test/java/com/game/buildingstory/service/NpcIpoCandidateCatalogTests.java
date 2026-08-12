package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NpcIpoCandidateCatalogTests {
    private final StockCatalog stockCatalog = new StockCatalog();
    private final NpcIpoCandidateCatalog candidateCatalog = new NpcIpoCandidateCatalog();

    @Test
    void newCompanyMarketCapIsCloseToInitialCompanyTotal() {
        long initialTotal = stockCatalog.initial().stream()
                .mapToLong(stock -> Math.multiplyExact(stock.basePrice(), stock.issuedShares()))
                .sum();
        long candidateTotal = candidateCatalog.all().stream()
                .map(candidate -> stockCatalog.find(candidate.stockKey()).orElseThrow())
                .mapToLong(stock -> Math.multiplyExact(stock.basePrice(), stock.issuedShares()))
                .sum();

        assertThat(candidateCatalog.all()).hasSize(16);
        assertThat(candidateTotal).isBetween(
                Math.round(initialTotal * 0.98),
                Math.round(initialTotal * 1.02)
        );
    }

    @Test
    void scheduleKeepsAtLeastThreeMonthsBetweenListings() {
        int previous = 0;
        for (NpcIpoCandidate candidate : candidateCatalog.all()) {
            assertThat(candidate.targetMonthAfterUnlock() - previous).isGreaterThanOrEqualTo(3);
            previous = candidate.targetMonthAfterUnlock();
        }
    }
}
