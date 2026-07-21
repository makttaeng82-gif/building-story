package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StockPriceModelTests {
    private final StockPriceModel model = new StockPriceModel();

    @Test
    void productionReversionUsesLongTermSimulationDecision() {
        assertThat(StockPriceModel.PRODUCTION_PARAMETERS.valuationReversionFactor()).isEqualTo(0.04);
    }

    @Test
    void sameSeedProducesSameCandle() {
        StockPriceModel.Input input = new StockPriceModel.Input(
                100_000L, 120_000L, 0.3, 1.2, 0.0, 0.5, 800, 1.8, StockRiskType.NORMAL
        );

        StockPriceModel.Result first = model.calculate(input, new Random(42));
        StockPriceModel.Result second = model.calculate(input, new Random(42));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void limitedPathKeepsAttributionEqualToAppliedPath() {
        StockPriceModel.Input input = new StockPriceModel.Input(
                100_000L, 300_000L, 15.0, 5.0, 0.0, 2.0, 0, 0.9, StockRiskType.SAFE
        );

        StockPriceModel.Result result = model.calculate(input, new Random(7));

        assertThat(result.pathImpactBasisPoints()).isEqualTo(1_200);
        assertThat(result.marketImpactBasisPoints()
                + result.industryImpactBasisPoints()
                + result.companyImpactBasisPoints()
                + result.valuationImpactBasisPoints()
                + result.noiseImpactBasisPoints()).isEqualTo(result.pathImpactBasisPoints());
        assertThat(result.trendImpactBasisPoints() + result.idiosyncraticImpactBasisPoints())
                .isEqualTo(result.noiseImpactBasisPoints());
        assertThat(result.high()).isGreaterThanOrEqualTo(Math.max(result.open(), result.close()));
        assertThat(result.low()).isLessThanOrEqualTo(Math.min(result.open(), result.close()));
    }

    @Test
    void industryBetaChangesOnlyIndustryContribution() {
        StockPriceModel.Result defensive = model.calculate(new StockPriceModel.Input(
                100_000L, 100_000L, 0.0, 1.0 * 0.75, 0.0, 0.0, 0, 0.0, StockRiskType.SAFE
        ), new Random(11));
        StockPriceModel.Result sensitive = model.calculate(new StockPriceModel.Input(
                100_000L, 100_000L, 0.0, 1.0 * 1.40, 0.0, 0.0, 0, 0.0, StockRiskType.SAFE
        ), new Random(11));

        assertThat(defensive.industryImpactBasisPoints()).isEqualTo(75);
        assertThat(sensitive.industryImpactBasisPoints()).isEqualTo(140);
        assertThat(defensive.marketImpactBasisPoints()).isZero();
        assertThat(sensitive.marketImpactBasisPoints()).isZero();
    }

    @Test
    void companyEventIsRecordedAsItsOwnPriceContribution() {
        StockPriceModel.Result result = model.calculate(new StockPriceModel.Input(
                100_000L, 100_000L, 0.0, 0.0, 0.65, 0.0, 0, 0.0, StockRiskType.SAFE
        ), new Random(17));

        assertThat(result.companyImpactBasisPoints()).isEqualTo(65);
        assertThat(result.marketImpactBasisPoints()).isZero();
        assertThat(result.industryImpactBasisPoints()).isZero();
        assertThat(result.pathImpactBasisPoints()).isEqualTo(65);
    }

    @Test
    void largerIdiosyncraticVolatilityCreatesLargerSameSeedNoise() {
        StockPriceModel.Result stable = model.calculate(new StockPriceModel.Input(
                100_000L, 100_000L, 0.0, 0.0, 0.0, 0.0, 0, 0.7, StockRiskType.SAFE
        ), new Random(23));
        StockPriceModel.Result volatileStock = model.calculate(new StockPriceModel.Input(
                100_000L, 100_000L, 0.0, 0.0, 0.0, 0.0, 0, 3.5, StockRiskType.SAFE
        ), new Random(23));

        assertThat(Math.abs(volatileStock.noiseImpactBasisPoints()))
                .isGreaterThan(Math.abs(stable.noiseImpactBasisPoints()));
    }
}
