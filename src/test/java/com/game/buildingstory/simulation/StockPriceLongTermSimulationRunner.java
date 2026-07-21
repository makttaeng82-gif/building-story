package com.game.buildingstory.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the repeatable long-term stock simulation report used for balance decisions. */
public final class StockPriceLongTermSimulationRunner {
    private StockPriceLongTermSimulationRunner() {
    }

    public static void main(String[] args) throws IOException {
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : StockPriceLongTermSimulator.DEFAULT_SEEDS;
        int years = args.length > 1 ? Integer.parseInt(args[1]) : StockPriceLongTermSimulator.DEFAULT_YEARS;
        StockPriceLongTermSimulator.Report report = new StockPriceLongTermSimulator()
                .simulate(seeds, years, StockPriceLongTermSimulator.DEFAULT_SEED);
        Path reportPath = Path.of("build", "reports", "stock-price-long-term-simulation.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toMarkdown(), StandardCharsets.UTF_8);
        System.out.println(report.toMarkdown());
        System.out.println("Stock simulation report: " + reportPath.toAbsolutePath());
    }
}
