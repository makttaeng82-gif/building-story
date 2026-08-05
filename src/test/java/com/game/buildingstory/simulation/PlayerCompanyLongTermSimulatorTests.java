package com.game.buildingstory.simulation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerCompanyLongTermSimulatorTests {
    @Test
    void sameSeedProducesSameReport() {
        PlayerCompanyLongTermSimulator simulator = new PlayerCompanyLongTermSimulator();

        String first = simulator.simulate(12, 36, 77L).toMarkdown();
        String second = simulator.simulate(12, 36, 77L).toMarkdown();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void incidentRiskRespondsToDebtStabilityAndOverload() {
        double stable = PlayerCompanyLongTermSimulator.incidentChance(10, 85, 80);
        double overloaded = PlayerCompanyLongTermSimulator.incidentChance(60, 50, 130);

        assertThat(stable).isBetween(0.005, 0.03);
        assertThat(overloaded).isGreaterThan(stable);
        assertThat(overloaded).isLessThanOrEqualTo(0.20);
    }

    @Test
    void reportContainsEveryOperatingStrategyAndFiniteMetrics() {
        PlayerCompanyLongTermSimulator.Report report = new PlayerCompanyLongTermSimulator()
                .simulate(20, 60, 20260721L);

        assertThat(report.strategies()).containsOnlyKeys(PlayerCompanyLongTermSimulator.Strategy.values());
        assertThat(report.strategies().values()).allSatisfy(result -> {
            assertThat(result.stoppedPercent()).isBetween(0.0, 100.0);
            assertThat(result.qualityMedian()).isGreaterThanOrEqualTo(0);
            assertThat(result.annualIncidents()).isGreaterThanOrEqualTo(0);
        });
    }
}
