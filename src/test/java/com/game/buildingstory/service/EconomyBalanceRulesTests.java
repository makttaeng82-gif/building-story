package com.game.buildingstory.service;

import com.game.buildingstory.domain.EconomyBalanceRules;
import com.game.buildingstory.domain.CityMarketIndex;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.ValuationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyBalanceRulesTests {

    @Test
    void buildingPricesIncreaseWhileGrossYieldDeclinesAcrossAllStages() {
        List<BuildingSpec> buildings = new BuildingCatalog().all();

        assertThat(buildings).hasSize(24);
        assertThat(buildings.getFirst().marketPrice()).isEqualTo(30_000_000L);
        assertThat(buildings.getLast().marketPrice()).isEqualTo(220_000_000_000L);

        long previousPrice = 0L;
        double previousYield = Double.MAX_VALUE;
        for (BuildingSpec building : buildings) {
            double grossYieldPercent = building.monthlyRent() * 12.0 * 100.0 / building.marketPrice();
            assertThat(building.marketPrice()).isGreaterThan(previousPrice);
            assertThat(grossYieldPercent).isBetween(9.99, 15.01);
            assertThat(grossYieldPercent).isLessThanOrEqualTo(previousYield + 0.01);
            previousPrice = building.marketPrice();
            previousYield = grossYieldPercent;
        }
    }

    @Test
    void occupancyAndRepairProbabilitiesMatchExpectedMonthlyRanges() {
        Player player = new Player("balance-probability", "hash");
        double monthlyMoveInChance = atLeastOnceAcrossTwoChecks(player.getMoveInChancePercent());
        double monthlyMoveOutChance = atLeastOnceAcrossTwoChecks(player.getMoveOutChancePercent());
        double monthlyRepairChance = atLeastOnceAcrossTwoChecks(player.getRepairRequestChancePercent());

        double protectedMonths = 2.0;
        double expectedOccupiedMonths = protectedMonths + 1.0 / monthlyMoveOutChance;
        double expectedVacantMonths = 1.0 / monthlyMoveInChance;
        double expectedOccupancyRate = expectedOccupiedMonths / (expectedOccupiedMonths + expectedVacantMonths);

        assertThat(expectedOccupancyRate).isBetween(0.73, 0.76);
        assertThat(monthlyRepairChance).isBetween(0.18, 0.20);
    }

    @Test
    void transactionCostsKeepBestValuationRoundTripBelowTwentyPercent() {
        long marketPrice = 100_000_000L;
        long buyPrice = marketPrice * ValuationStatus.UNDER.rate() / 100;
        long buyTotal = buyPrice + EconomyBalanceRules.purchaseFee(buyPrice);
        long sellPrice = marketPrice * ValuationStatus.OVER.rate() / 100;
        long sellPayout = sellPrice - EconomyBalanceRules.sellFee(sellPrice);
        double profitRate = (sellPayout - buyTotal) * 100.0 / buyTotal;

        assertThat(profitRate).isPositive().isLessThan(20.0);
    }

    @Test
    void secretaryEventPaymentsNeverExceedFortyPercentOfRequiredCash() {
        for (String secretaryKey : List.of("secretary-2", "secretary-4", "secretary-5")) {
            SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
            assertThat(scenario.requestCost()).isLessThanOrEqualTo(scenario.requiredCash() * 40 / 100);
        }
    }

    @Test
    void everySecretaryHasSameMaximumSalary() {
        SecretaryCatalog catalog = new SecretaryCatalog();
        long maximumPayroll = catalog.all().stream()
                .mapToLong(secretary -> secretary.monthlySalaryForProficiency(30))
                .sum();

        assertThat(maximumPayroll).isEqualTo(105_000_000L);
        assertThat(catalog.all()).allSatisfy(secretary ->
                assertThat(secretary.monthlySalaryForProficiency(30)).isEqualTo(17_500_000L));
    }

    @Test
    void cityMarketIndexAppliesNewsOnceAndRespectsMonthlyLimit() {
        Player player = new Player("index-rules", "hash");
        CityMarketIndex index = new CityMarketIndex(player, "청주");
        index.recordNewsImpact(150);

        assertThat(index.updateMonthly(31, 60)).isTrue();
        assertThat(index.getIndexPoints()).isEqualTo(10_200);
        assertThat(index.getPendingNewsBasisPoints()).isZero();
        assertThat(index.updateMonthly(31, -60)).isFalse();
        assertThat(index.getIndexPoints()).isEqualTo(10_200);
    }

    private double atLeastOnceAcrossTwoChecks(int chancePercent) {
        double failureChance = 1.0 - chancePercent / 100.0;
        return 1.0 - failureChance * failureChance;
    }
}
