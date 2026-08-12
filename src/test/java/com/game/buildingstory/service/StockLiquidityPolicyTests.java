package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockLiquidityPolicyTests {
    private final StockLiquidityPolicy policy = new StockLiquidityPolicy();

    @Test
    void splitOrdersKeepTheSameWeightedAverageImpact() {
        var whole = policy.priceImpact(1_000, 0, 1_000);
        var firstHalf = policy.priceImpact(1_000, 0, 500);
        var secondHalf = policy.priceImpact(1_000, 500, 500);

        assertThat(whole.averageBasisPoints()).isEqualTo(267);
        assertThat(firstHalf.averageBasisPoints() + secondHalf.averageBasisPoints())
                .isEqualTo(whole.averageBasisPoints() * 2);
        assertThat(secondHalf.finalBasisPoints()).isEqualTo(whole.finalBasisPoints()).isEqualTo(800);
    }

    @Test
    void sellOrdersCreateNegativePriceImpact() {
        var impact = policy.priceImpact(1_000, 0, -1_000);

        assertThat(impact.averageBasisPoints()).isEqualTo(-267);
        assertThat(impact.finalBasisPoints()).isEqualTo(-800);
    }

    @Test
    void capacityUsesTradableSharesInsteadOfTotalIssuedShares() {
        StockSpec stock = new StockCatalog().all().getFirst();

        assertThat(policy.capacity(stock, 120_000_000L)).isEqualTo(600_000L);
        assertThat(policy.capacity(stock)).isEqualTo(1_500_000L);
    }
}
