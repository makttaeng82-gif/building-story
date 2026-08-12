package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class NpcIpoFirstCandleModelTests {
    private final NpcIpoFirstCandleModel model = new NpcIpoFirstCandleModel();

    @Test
    void normalListingRespectsOpeningAndClosingLimits() {
        var result = model.calculate(new NpcIpoFirstCandleModel.Input(
                50_000, 80_000, 3_000, 8.0, 4.0, 3.0, StockRiskType.NORMAL
        ), new Random(7));

        assertThat(result.listingImpactBasisPoints()).isEqualTo(1_200);
        assertThat(result.open()).isEqualTo(56_000);
        assertThat(result.close()).isBetween(40_000L, 60_000L);
        assertThat(result.high()).isGreaterThanOrEqualTo(Math.max(result.open(), result.close()));
        assertThat(result.low()).isLessThanOrEqualTo(Math.min(result.open(), result.close()));
    }

    @Test
    void safeListingCanOpenBelowOfferWhenDemandIsWeak() {
        var result = model.calculate(new NpcIpoFirstCandleModel.Input(
                100_000, 100_000, -1_500, 0, 0, 0, StockRiskType.SAFE
        ), new Random(11));

        assertThat(result.listingImpactBasisPoints()).isEqualTo(-800);
        assertThat(result.open()).isEqualTo(92_000);
        assertThat(result.close()).isBetween(85_000L, 115_000L);
    }
}
