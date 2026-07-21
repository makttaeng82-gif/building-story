package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.random.RandomGenerator;

/**
 * DB나 플레이어 상태를 직접 조회하지 않고 한 개의 5일봉을 계산한다.
 * 운영 서비스와 장기 시뮬레이터가 같은 가격식을 공유할 수 있도록 모든 입력과 난수 생성기를 외부에서 받는다.
 */
@Component
public class StockPriceModel {
    public static final Parameters PRODUCTION_PARAMETERS = new Parameters(0.04, 2.0, 12.0, 0.0015, 10.0);

    public Result calculate(Input input, RandomGenerator random) {
        return calculate(input, PRODUCTION_PARAMETERS, random);
    }

    public Result calculate(Input input, Parameters parameters, RandomGenerator random) {
        if (input.previousClose() <= 0) {
            throw new IllegalArgumentException("직전 종가는 1원 이상이어야 합니다.");
        }
        if (input.idiosyncraticVolatilityPercent() < 0) {
            throw new IllegalArgumentException("고유 변동성은 0 이상이어야 합니다.");
        }

        double earningsGapPercent = clamp(
                input.earningsSurpriseBasisPoints() * parameters.earningsGapMultiplier(),
                -parameters.earningsGapLimitPercent(),
                parameters.earningsGapLimitPercent()
        );
        double valuationPercent = valuationEffectPercent(
                input.previousClose(), input.fairValue(), parameters.valuationReversionFactor(),
                parameters.valuationLimitPercent()
        );
        double idiosyncraticNoise = input.idiosyncraticVolatilityPercent() == 0
                ? 0.0
                : random.nextDouble(-input.idiosyncraticVolatilityPercent(), input.idiosyncraticVolatilityPercent());
        double noisePercent = input.trendPercent() + idiosyncraticNoise;
        double rawPathPercent = input.marketPercent() + input.industryPercent() + input.companyPercent()
                + valuationPercent + noisePercent;
        double limitedPathPercent = clamp(
                rawPathPercent,
                -parameters.pathLimitPercent(),
                parameters.pathLimitPercent()
        );

        long open = Math.max(1L, applyPercent(input.previousClose(), earningsGapPercent));
        long close = Math.max(1L, applyPercent(open, limitedPathPercent));
        PricePath path = createPricePath(open, close, input.riskType(), random);
        int earningsImpactBasisPoints = realizedBasisPoints(input.previousClose(), open);
        int pathImpactBasisPoints = realizedBasisPoints(open, close);
        Attribution attribution = attributePath(
                input.marketPercent(), input.industryPercent(), input.companyPercent(), valuationPercent,
                input.trendPercent(), idiosyncraticNoise,
                rawPathPercent, pathImpactBasisPoints
        );

        return new Result(
                open,
                path.high(),
                path.low(),
                close,
                attribution.marketBasisPoints(),
                attribution.industryBasisPoints(),
                attribution.companyBasisPoints(),
                earningsImpactBasisPoints,
                attribution.valuationBasisPoints(),
                attribution.trendBasisPoints(),
                attribution.idiosyncraticBasisPoints(),
                attribution.trendBasisPoints() + attribution.idiosyncraticBasisPoints(),
                pathImpactBasisPoints,
                percentChange(input.previousClose(), close)
        );
    }

    private Attribution attributePath(
            double marketPercent,
            double industryPercent,
            double companyPercent,
            double valuationPercent,
            double trendPercent,
            double idiosyncraticPercent,
            double rawPathPercent,
            int realizedPathBasisPoints
    ) {
        if (realizedPathBasisPoints == 0 || Math.abs(rawPathPercent) < 0.000_001) {
            return new Attribution(0, 0, 0, 0, 0, 0);
        }
        double scale = realizedPathBasisPoints / (rawPathPercent * 100.0);
        int market = (int) Math.round(marketPercent * 100.0 * scale);
        int industry = (int) Math.round(industryPercent * 100.0 * scale);
        int company = (int) Math.round(companyPercent * 100.0 * scale);
        int valuation = (int) Math.round(valuationPercent * 100.0 * scale);
        int trend = (int) Math.round(trendPercent * 100.0 * scale);
        // 마지막 고유 변동 항에 반올림 잔차를 모아 모든 항의 합이 실제 경로 수익률과 정확히 일치하게 한다.
        int idiosyncratic = realizedPathBasisPoints - market - industry - company - valuation - trend;
        return new Attribution(market, industry, company, valuation, trend, idiosyncratic);
    }

    private double valuationEffectPercent(long currentPrice, long fairValue, double factor, double limitPercent) {
        if (fairValue <= 0) {
            return 0.0;
        }
        double gapPercent = Math.log(fairValue / (double) currentPrice) * 100.0;
        return clamp(gapPercent * factor, -limitPercent, limitPercent);
    }

    private PricePath createPricePath(long open, long close, StockRiskType riskType, RandomGenerator random) {
        long high = Math.max(open, close);
        long low = Math.min(open, close);
        double logOpen = Math.log(open);
        double logClose = Math.log(close);
        for (int step = 1; step < 5; step++) {
            double progress = step / 5.0;
            double bridgeScale = Math.sqrt(progress * (1.0 - progress));
            double logPrice = logOpen + (logClose - logOpen) * progress
                    + random.nextGaussian() * riskType.pathNoise() * bridgeScale;
            long price = Math.max(1L, Math.round(Math.exp(logPrice)));
            high = Math.max(high, price);
            low = Math.min(low, price);
        }
        return new PricePath(high, low);
    }

    private long applyPercent(long price, double percent) {
        return Math.round(price * (100.0 + percent) / 100.0);
    }

    private int realizedBasisPoints(long from, long to) {
        return (int) Math.round((to - from) * 10_000.0 / from);
    }

    private double percentChange(long from, long to) {
        return (to - from) * 100.0 / from;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Input(
            long previousClose,
            long fairValue,
            double marketPercent,
            double industryPercent,
            double companyPercent,
            double trendPercent,
            int earningsSurpriseBasisPoints,
            double idiosyncraticVolatilityPercent,
            StockRiskType riskType
    ) {
    }

    public record Parameters(
            double valuationReversionFactor,
            double valuationLimitPercent,
            double pathLimitPercent,
            double earningsGapMultiplier,
            double earningsGapLimitPercent
    ) {
    }

    public record Result(
            long open,
            long high,
            long low,
            long close,
            int marketImpactBasisPoints,
            int industryImpactBasisPoints,
            int companyImpactBasisPoints,
            int earningsImpactBasisPoints,
            int valuationImpactBasisPoints,
            int trendImpactBasisPoints,
            int idiosyncraticImpactBasisPoints,
            int noiseImpactBasisPoints,
            int pathImpactBasisPoints,
            double totalChangePercent
    ) {
    }

    private record Attribution(
            int marketBasisPoints,
            int industryBasisPoints,
            int companyBasisPoints,
            int valuationBasisPoints,
            int trendBasisPoints,
            int idiosyncraticBasisPoints
    ) {
    }

    private record PricePath(long high, long low) {
    }
}
