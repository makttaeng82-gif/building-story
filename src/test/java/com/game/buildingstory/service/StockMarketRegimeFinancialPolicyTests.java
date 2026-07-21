package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockMarketRegimeFinancialPolicyTests {
    private final StockMarketRegimeFinancialPolicy policy = new StockMarketRegimeFinancialPolicy();

    @Test
    void manufacturingRespondsMoreStronglyToExpansionAndRecession() {
        var expansion = new StockMarketRegimeService.RegimeExposure(12, 0, 0);
        var recession = new StockMarketRegimeService.RegimeExposure(0, 0, 12);

        assertThat(policy.impact(expansion, "제조"))
                .isEqualTo(new StockMarketRegimeFinancialPolicy.FinancialImpact(180, -25));
        assertThat(policy.impact(recession, "제조"))
                .isEqualTo(new StockMarketRegimeFinancialPolicy.FinancialImpact(-240, 50));
        assertThat(policy.impact(recession, "식품").revenueBasisPoints()).isEqualTo(-70);
    }

    @Test
    void mixedQuarterUsesObservedRegimeWeights() {
        var mixed = new StockMarketRegimeService.RegimeExposure(6, 3, 3);

        assertThat(policy.impact(mixed, "IT"))
                .isEqualTo(new StockMarketRegimeFinancialPolicy.FinancialImpact(25, -2));
        assertThat(policy.impact(new StockMarketRegimeService.RegimeExposure(0, 0, 0), "IT"))
                .isEqualTo(new StockMarketRegimeFinancialPolicy.FinancialImpact(0, 0));
    }
}
