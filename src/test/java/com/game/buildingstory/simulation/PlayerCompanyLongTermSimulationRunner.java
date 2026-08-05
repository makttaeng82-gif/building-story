package com.game.buildingstory.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the fixed-seed company balance report for design review. */
public final class PlayerCompanyLongTermSimulationRunner {
    private PlayerCompanyLongTermSimulationRunner() {
    }

    public static void main(String[] args) throws IOException {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : PlayerCompanyLongTermSimulator.DEFAULT_SEEDS;
        int months = args.length > 1 ? Integer.parseInt(args[1]) : PlayerCompanyLongTermSimulator.DEFAULT_MONTHS;
        PlayerCompanyLongTermSimulator.Report report = new PlayerCompanyLongTermSimulator()
                .simulate(seeds, months, PlayerCompanyLongTermSimulator.DEFAULT_SEED);
        Path reportPath = Path.of("build", "reports", "player-company-long-term-simulation.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toMarkdown(), StandardCharsets.UTF_8);
        System.out.println(report.toMarkdown());
        System.out.println("Player company simulation report: " + reportPath.toAbsolutePath());
    }
}
