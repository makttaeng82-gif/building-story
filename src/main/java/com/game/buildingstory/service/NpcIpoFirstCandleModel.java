package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

/** 공모가와 일반 5일봉 사이의 신규상장 첫 거래주간만 계산한다. */
@Component
public class NpcIpoFirstCandleModel {
    public Result calculate(Input input, RandomGenerator random) {
        int gapLimit = switch (input.riskType()) {
            case SAFE -> 800;
            case NORMAL -> 1_200;
            case AGGRESSIVE -> 1_800;
        };
        int closeLimit = switch (input.riskType()) {
            case SAFE -> 1_500;
            case NORMAL -> 2_000;
            case AGGRESSIVE -> 2_500;
        };
        int demand = clamp(input.demandBasisPoints(), -gapLimit, gapLimit);
        long open = applyBasisPoints(input.offerPrice(), demand);

        double valuationPercent = 0;
        if (input.fairValue() > 0) {
            valuationPercent = clamp(
                    Math.log(input.fairValue() / (double) open) * 100.0 * 0.02,
                    -1.0,
                    1.0
            );
        }
        double noisePercent = input.idiosyncraticVolatilityPercent() == 0
                ? 0
                : random.nextDouble(
                        -input.idiosyncraticVolatilityPercent(),
                        input.idiosyncraticVolatilityPercent()
                );
        double rawPathPercent = input.marketPercent() + input.industryPercent()
                + valuationPercent + noisePercent;
        double limitedPathPercent = clamp(rawPathPercent, -12.0, 12.0);
        long unconstrainedClose = applyPercent(open, limitedPathPercent);
        long minimumClose = applyBasisPoints(input.offerPrice(), -closeLimit);
        long maximumClose = applyBasisPoints(input.offerPrice(), closeLimit);
        long close = Math.max(minimumClose, Math.min(maximumClose, unconstrainedClose));

        PricePath path = createPricePath(open, close, input.riskType(), random);
        int realizedPath = basisPoints(open, close);
        double attributionBase = input.marketPercent() + input.industryPercent()
                + valuationPercent + noisePercent;
        double scale = Math.abs(attributionBase) < 0.000_001
                ? 0 : realizedPath / (attributionBase * 100.0);
        int market = (int) Math.round(input.marketPercent() * 100.0 * scale);
        int industry = (int) Math.round(input.industryPercent() * 100.0 * scale);
        int valuation = (int) Math.round(valuationPercent * 100.0 * scale);
        int idiosyncratic = realizedPath - market - industry - valuation;
        return new Result(open, path.high(), path.low(), close, demand,
                market, industry, valuation, idiosyncratic, realizedPath);
    }

    private PricePath createPricePath(long open, long close, StockRiskType riskType, RandomGenerator random) {
        long high = Math.max(open, close);
        long low = Math.min(open, close);
        double logOpen = Math.log(open);
        double logClose = Math.log(close);
        for (int step = 1; step < 5; step++) {
            double progress = step / 5.0;
            double bridge = Math.sqrt(progress * (1.0 - progress));
            long price = Math.max(1L, Math.round(Math.exp(
                    logOpen + (logClose - logOpen) * progress
                            + random.nextGaussian() * riskType.pathNoise() * bridge
            )));
            high = Math.max(high, price);
            low = Math.min(low, price);
        }
        return new PricePath(high, low);
    }

    private long applyBasisPoints(long price, int basisPoints) {
        return Math.max(1L, Math.round(price * (10_000.0 + basisPoints) / 10_000.0));
    }

    private long applyPercent(long price, double percent) {
        return Math.max(1L, Math.round(price * (100.0 + percent) / 100.0));
    }

    private int basisPoints(long from, long to) {
        return (int) Math.round((to - from) * 10_000.0 / from);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Input(
            long offerPrice,
            long fairValue,
            int demandBasisPoints,
            double marketPercent,
            double industryPercent,
            double idiosyncraticVolatilityPercent,
            StockRiskType riskType
    ) {
    }

    public record Result(
            long open,
            long high,
            long low,
            long close,
            int listingImpactBasisPoints,
            int marketImpactBasisPoints,
            int industryImpactBasisPoints,
            int valuationImpactBasisPoints,
            int idiosyncraticImpactBasisPoints,
            int pathImpactBasisPoints
    ) {
    }

    private record PricePath(long high, long low) {
    }
}
