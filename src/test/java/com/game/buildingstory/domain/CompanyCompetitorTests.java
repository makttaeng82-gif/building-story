package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyCompetitorTests {

    @Test
    void quarterlyBenchmarkGrowthConvergesToEachStrategyTarget() {
        CompanyCompetitor frontier = competitor("frontier", 230);
        CompanyCompetitor popular = competitor("popular", 185);
        CompanyCompetitor trust = competitor("trust", 195);

        for (int quarter = 0; quarter < 200; quarter++) {
            frontier.advanceQuarter();
            popular.advanceQuarter();
            trust.advanceQuarter();
        }

        assertThat(frontier.getBenchmark()).isEqualTo(2_600);
        assertThat(popular.getBenchmark()).isEqualTo(2_200);
        assertThat(trust.getBenchmark()).isEqualTo(2_400);
    }

    @Test
    void twentyYearGrowthRemainsBelowTargetWithoutStoppingEarlyProgress() {
        CompanyCompetitor frontier = competitor("frontier", 230);
        CompanyCompetitor popular = competitor("popular", 185);
        CompanyCompetitor trust = competitor("trust", 195);

        for (int quarter = 0; quarter < 80; quarter++) {
            frontier.advanceQuarter();
            popular.advanceQuarter();
            trust.advanceQuarter();
        }

        assertThat(frontier.getBenchmark()).isBetween(2_500, 2_600);
        assertThat(popular.getBenchmark()).isBetween(1_800, 2_200);
        assertThat(trust.getBenchmark()).isBetween(2_100, 2_400);
    }

    private CompanyCompetitor competitor(String key, int benchmark) {
        return new CompanyCompetitor(
                null, key, key, "test", benchmark,
                50, 50, 50, 50, 50, 25
        );
    }
}
