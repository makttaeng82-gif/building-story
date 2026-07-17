package com.game.buildingstory.simulation;

import com.game.buildingstory.service.BuildingCatalog;
import com.game.buildingstory.service.SecretaryCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Gradle의 economySimulation 작업에서 Markdown 보고서를 표준 출력으로 내보낸다. */
public final class EconomyBalanceSimulationRunner {
    private EconomyBalanceSimulationRunner() {
    }

    public static void main(String[] args) throws IOException {
        int iterations = args.length == 0
                ? EconomyBalanceSimulator.DEFAULT_ITERATIONS
                : Integer.parseInt(args[0]);
        EconomyBalanceSimulator simulator = new EconomyBalanceSimulator(
                new BuildingCatalog().all(),
                new SecretaryCatalog().all()
        );
        Path reportPath = Path.of("build", "reports", "economy-simulation.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(
                reportPath,
                simulator.simulate(iterations, EconomyBalanceSimulator.DEFAULT_SEED).toMarkdown(),
                StandardCharsets.UTF_8
        );
        System.out.println("Economy simulation report: " + reportPath.toAbsolutePath());
    }
}
