package com.game.buildingstory.simulation;

import com.game.buildingstory.service.BuildingCatalog;
import com.game.buildingstory.service.SecretaryCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyBalanceSimulatorTests {
    private final EconomyBalanceSimulator simulator = new EconomyBalanceSimulator(
            new BuildingCatalog().all(),
            new SecretaryCatalog().all()
    );

    @Test
    void fixedSeedProducesRepeatableResults() {
        var first = simulator.simulate(20, 12345L);
        var second = simulator.simulate(20, 12345L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void salaryAuditUsesActualCatalogSalaryFormula() {
        var report = simulator.simulate(1, 1L);

        assertThat(report.secretarySalaries()).hasSize(6);
        assertThat(report.secretarySalaries().get(0).hireSalary()).isEqualTo(500_000L);
        assertThat(report.secretarySalaries().get(0).maximumSalary()).isEqualTo(1_225_000L);
        assertThat(report.secretarySalaries().get(5).hireSalary()).isEqualTo(88_000_000L);
        assertThat(report.secretarySalaries().get(5).maximumSalary()).isEqualTo(98_000_000L);
    }
}
