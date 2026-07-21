package com.game.buildingstory.simulation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockPriceLongTermSimulatorTests {
    @Test
    void strongerReversionShortensShockRecoveryAndNarrowsLongTermGap() {
        StockPriceLongTermSimulator.Report report = new StockPriceLongTermSimulator()
                .simulate(12, 2, 20260718L);

        StockPriceLongTermSimulator.CandidateResult weak = report.candidates().get(0.03);
        StockPriceLongTermSimulator.CandidateResult strong = report.candidates().get(0.08);

        assertThat(strong.moderateShock().medianDays()).isLessThan(weak.moderateShock().medianDays());
        assertThat(strong.p90AbsoluteGapPercent()).isLessThan(weak.p90AbsoluteGapPercent());
        assertThat(report.candidates().values())
                .allSatisfy(result -> assertThat(result.invalidPrices()).isZero());
    }

    @Test
    void sameSeedProducesSameReport() {
        StockPriceLongTermSimulator simulator = new StockPriceLongTermSimulator();

        String first = simulator.simulate(3, 1, 77L).toMarkdown();
        String second = simulator.simulate(3, 1, 77L).toMarkdown();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void productionReversionKeepsRiskOrderAndRewardsFundamentalImprovement() {
        StockPriceLongTermSimulator.CandidateResult result = new StockPriceLongTermSimulator()
                .simulate(24, 3, 20260718L)
                .candidates().get(0.04);

        assertThat(result.invalidPrices()).isZero();
        assertThat(result.annualVolatilityPercent().get(com.game.buildingstory.service.StockRiskType.SAFE))
                .isLessThan(result.annualVolatilityPercent().get(com.game.buildingstory.service.StockRiskType.NORMAL));
        assertThat(result.annualVolatilityPercent().get(com.game.buildingstory.service.StockRiskType.NORMAL))
                .isLessThan(result.annualVolatilityPercent().get(com.game.buildingstory.service.StockRiskType.AGGRESSIVE));
        assertThat(result.fundamentalReturnCorrelation()).isPositive();
        assertThat(result.fundamentalQuartileSpreadPercent()).isPositive();
        assertThat(result.over25PercentRate()).isLessThan(5.0);
    }
}
