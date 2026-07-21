package com.game.buildingstory.simulation;

import com.game.buildingstory.service.ListedCompanyFinancialCatalog;
import com.game.buildingstory.service.ListedCompanyValuationCatalog;
import com.game.buildingstory.service.StockCatalog;
import com.game.buildingstory.service.StockMarketRegime;
import com.game.buildingstory.service.StockPriceModel;
import com.game.buildingstory.service.StockRiskType;
import com.game.buildingstory.service.StockSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Runs fixed-seed, DB-free experiments against the production stock price formula. */
public final class StockPriceLongTermSimulator {
    public static final int DEFAULT_SEEDS = 500;
    public static final int DEFAULT_YEARS = 10;
    public static final long DEFAULT_SEED = 20260718L;
    public static final List<Double> REVERSION_CANDIDATES = List.of(0.03, 0.04, 0.05, 0.08);

    private static final int UPDATE_DAYS = 5;
    private static final int UPDATES_PER_YEAR = 365 / UPDATE_DAYS;
    private static final double TREND_EFFECT_PERCENT = 0.5;
    private static final int GAP_HISTOGRAM_STEP_BASIS_POINTS = 25;
    private static final int GAP_HISTOGRAM_MAX_BASIS_POINTS = 30_000;
    private static final int SHOCK_MAX_UPDATES = 146;

    private final StockPriceModel priceModel;
    private final List<StockSpec> stocks;
    private final ListedCompanyFinancialCatalog financialCatalog;
    private final ListedCompanyValuationCatalog valuationCatalog;
    private final Map<String, Double> indexWeights;

    public StockPriceLongTermSimulator() {
        this(
                new StockPriceModel(),
                new StockCatalog().all(),
                new ListedCompanyFinancialCatalog(),
                new ListedCompanyValuationCatalog()
        );
    }

    StockPriceLongTermSimulator(
            StockPriceModel priceModel,
            List<StockSpec> stocks,
            ListedCompanyFinancialCatalog financialCatalog,
            ListedCompanyValuationCatalog valuationCatalog
    ) {
        this.priceModel = priceModel;
        this.stocks = List.copyOf(stocks);
        this.financialCatalog = financialCatalog;
        this.valuationCatalog = valuationCatalog;
        this.indexWeights = cappedIndexWeights(this.stocks);
    }

    public Report simulate() {
        return simulate(DEFAULT_SEEDS, DEFAULT_YEARS, DEFAULT_SEED);
    }

    public Report simulate(int seedCount, int years, long seed) {
        if (seedCount <= 0 || years <= 0) {
            throw new IllegalArgumentException("seedCount and years must be positive");
        }
        Map<Double, CandidateResult> results = new LinkedHashMap<>();
        for (double candidate : REVERSION_CANDIDATES) {
            results.put(candidate, simulateCandidate(candidate, seedCount, years, seed));
        }
        return new Report(seedCount, years, seed, results);
    }

    private CandidateResult simulateCandidate(double reversion, int seedCount, int years, long seed) {
        StockPriceModel.Parameters parameters = parameters(reversion);
        Metrics metrics = new Metrics(seedCount * stocks.size(), seedCount);
        int updates = years * UPDATES_PER_YEAR;

        for (int run = 0; run < seedCount; run++) {
            long runSeed = seed + run * 104_729L;
            Random random = new Random(runSeed);
            MarketProcess market = new MarketProcess(random);
            List<StockState> states = stocks.stream().map(StockState::new).toList();
            Map<String, StockFundamentalProcess> fundamentals = stocks.stream().collect(java.util.stream.Collectors.toMap(
                    StockSpec::key,
                    stock -> new StockFundamentalProcess(
                            stock, financialCatalog, valuationCatalog,
                            runSeed
                    )
            ));
            StockNewsProcess news = new StockNewsProcess(runSeed);
            RunDiagnostics diagnostics = new RunDiagnostics(stocks, indexWeights);

            for (int update = 1; update <= updates; update++) {
                StockMarketRegime currentRegime = market.currentRegime();
                for (StockFundamentalProcess process : fundamentals.values()) {
                    process.recordRegime(currentRegime);
                    process.settleIfDue(update);
                }
                double marketPercent = market.nextPulse() + news.marketEffectPercent();
                for (StockState state : states) {
                    StockFundamentalProcess fundamental = fundamentals.get(state.spec.key());
                    long fairValue = fundamental.fairValue();
                    double industryPercent = news.industryEffectPercent(state.spec.industry())
                            * state.spec.industryBeta();
                    StockPriceModel.Result result = priceModel.calculate(new StockPriceModel.Input(
                            state.price,
                            fairValue,
                            marketPercent,
                            industryPercent,
                            news.companyEffectPercent(state.spec.key()),
                            state.trendPercent(),
                            fundamental.consumeEarningsBasisPoints(),
                            state.spec.idiosyncraticVolatilityPercent(),
                            state.spec.riskType()
                    ), parameters, random);
                    state.record(result);
                    metrics.recordPoint(state.spec.riskType(), result, fairValue);
                }
                diagnostics.record(currentRegime, states);
                boolean regimeChanged = market.advance();
                news.afterUpdate(update, regimeChanged, fundamentals);
            }
            for (StockState state : states) {
                metrics.recordTerminal(
                        state.spec,
                        state.price,
                        fundamentals.get(state.spec.key()).fairValue(),
                        fundamentals.get(state.spec.key()).initialFairValue(),
                        state.maxDrawdownPercent,
                        years
                );
            }
            metrics.recordNewsCounts(news, years);
            metrics.recordRun(diagnostics, years);
        }

        ShockResult moderateShock = simulateShock(parameters, seedCount, seed, 125_000L);
        ShockResult severeShock = simulateShock(parameters, seedCount, seed + 31_337L, 200_000L);
        return metrics.summarize(reversion, moderateShock, severeShock);
    }

    private ShockResult simulateShock(
            StockPriceModel.Parameters parameters,
            int seedCount,
            long seed,
            long initialPrice
    ) {
        int[] recoveryDays = new int[seedCount * StockRiskType.values().length];
        int recovered = 0;
        int index = 0;
        for (int run = 0; run < seedCount; run++) {
            for (StockRiskType risk : StockRiskType.values()) {
                Random random = new Random(seed + 7_919L + run * 65_537L + risk.ordinal() * 997L);
                long fairValue = 100_000L;
                long price = initialPrice;
                double halfGap = Math.abs(Math.log(price / (double) fairValue)) / 2.0;
                int recovery = SHOCK_MAX_UPDATES + 1;
                for (int update = 1; update <= SHOCK_MAX_UPDATES; update++) {
                    StockPriceModel.Result result = priceModel.calculate(new StockPriceModel.Input(
                            price, fairValue, 0.0, 0.0, 0.0, 0.0, 0,
                            representativeIdiosyncraticVolatility(risk), risk
                    ), parameters, random);
                    price = result.close();
                    if (Math.abs(Math.log(price / (double) fairValue)) <= halfGap) {
                        recovery = update;
                        recovered++;
                        break;
                    }
                }
                recoveryDays[index++] = recovery * UPDATE_DAYS;
            }
        }
        Arrays.sort(recoveryDays);
        return new ShockResult(
                percentile(recoveryDays, 0.5),
                percentile(recoveryDays, 0.9),
                recovered * 100.0 / recoveryDays.length
        );
    }

    private StockPriceModel.Parameters parameters(double reversion) {
        StockPriceModel.Parameters production = StockPriceModel.PRODUCTION_PARAMETERS;
        return new StockPriceModel.Parameters(
                reversion,
                production.valuationLimitPercent(),
                production.pathLimitPercent(),
                production.earningsGapMultiplier(),
                production.earningsGapLimitPercent()
        );
    }

    private double representativeIdiosyncraticVolatility(StockRiskType risk) {
        return switch (risk) {
            case SAFE -> 0.9;
            case NORMAL -> 1.8;
            case AGGRESSIVE -> 3.4;
        };
    }

    private Map<String, Double> cappedIndexWeights(List<StockSpec> stocks) {
        Map<String, Double> weights = new java.util.HashMap<>();
        java.util.Set<StockSpec> remaining = new java.util.LinkedHashSet<>(stocks);
        double remainingWeight = 1.0;
        while (!remaining.isEmpty()) {
            double remainingMarketCap = remaining.stream()
                    .mapToDouble(stock -> stock.basePrice() * (double) stock.issuedShares()).sum();
            double weightToDistribute = remainingWeight;
            List<StockSpec> overCap = remaining.stream()
                    .filter(stock -> weightToDistribute * stock.basePrice() * stock.issuedShares()
                            / remainingMarketCap > 0.15)
                    .toList();
            if (overCap.isEmpty()) {
                for (StockSpec stock : remaining) {
                    weights.put(
                            stock.key(),
                            remainingWeight * stock.basePrice() * stock.issuedShares() / remainingMarketCap
                    );
                }
                break;
            }
            for (StockSpec stock : overCap) {
                weights.put(stock.key(), 0.15);
                remaining.remove(stock);
                remainingWeight -= 0.15;
            }
        }
        return Map.copyOf(weights);
    }

    private static double percentile(int[] sorted, double quantile) {
        if (sorted.length == 0) {
            return 0.0;
        }
        double position = quantile * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted[lower];
        }
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower);
    }

    private static double percentile(double[] sorted, double quantile) {
        if (sorted.length == 0) {
            return 0.0;
        }
        double position = quantile * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted[lower];
        }
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower);
    }

    private static final class StockState {
        private final StockSpec spec;
        private final boolean[] recentDirections = new boolean[3];
        private int directionCount;
        private int directionCursor;
        private long price;
        private long peakPrice;
        private double maxDrawdownPercent;
        private double lastReturn;

        private StockState(StockSpec spec) {
            this.spec = spec;
            this.price = spec.basePrice();
            this.peakPrice = price;
        }

        private double trendPercent() {
            int rising = 0;
            for (int index = 0; index < directionCount; index++) {
                if (recentDirections[index]) {
                    rising++;
                }
            }
            int falling = directionCount - rising;
            if (rising >= 2) {
                return TREND_EFFECT_PERCENT;
            }
            if (falling >= 2) {
                return -TREND_EFFECT_PERCENT;
            }
            return 0.0;
        }

        private void record(StockPriceModel.Result result) {
            recentDirections[directionCursor] = result.close() > result.open();
            directionCursor = (directionCursor + 1) % recentDirections.length;
            directionCount = Math.min(recentDirections.length, directionCount + 1);
            lastReturn = result.close() / (double) price - 1.0;
            price = result.close();
            peakPrice = Math.max(peakPrice, price);
            maxDrawdownPercent = Math.max(maxDrawdownPercent, (peakPrice - price) * 100.0 / peakPrice);
        }
    }

    private static final class MarketProcess {
        private final Random random;
        private StockMarketRegime regime = StockMarketRegime.NEUTRAL;
        private int remainingUpdates;

        private MarketProcess(Random random) {
            this.random = random;
            this.remainingUpdates = duration();
        }

        private double nextPulse() {
            return switch (regime) {
                case EXPANSION -> 0.25 + random.nextDouble(-0.50, 0.51);
                case NEUTRAL -> 0.09 + random.nextDouble(-0.50, 0.51);
                case RECESSION -> -0.25 + random.nextDouble(-0.70, 0.71);
            };
        }

        private StockMarketRegime currentRegime() {
            return regime;
        }

        private boolean advance() {
            remainingUpdates--;
            if (remainingUpdates > 0) {
                return false;
            }
            StockMarketRegime previous = regime;
            int roll = random.nextInt(100);
            regime = switch (regime) {
                case EXPANSION -> roll < 55 ? StockMarketRegime.EXPANSION
                        : roll < 90 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
                case NEUTRAL -> roll < 30 ? StockMarketRegime.EXPANSION
                        : roll < 70 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
                case RECESSION -> roll < 10 ? StockMarketRegime.EXPANSION
                        : roll < 45 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
            };
            remainingUpdates = duration();
            return previous != regime;
        }

        private int duration() {
            return random.nextInt(12, 25);
        }
    }

    private static final class RunDiagnostics {
        private final List<StockSpec> stocks;
        private final Map<String, Double> indexWeights;
        private final PairReturnCorrelation[] pairCorrelations;
        private final EnumMap<StockMarketRegime, RunningMean> regimeIndexReturns =
                new EnumMap<>(StockMarketRegime.class);
        private double previousIndex = 1_000.0;
        private double peakIndex = previousIndex;
        private double maxIndexDrawdownPercent;

        private RunDiagnostics(List<StockSpec> stocks, Map<String, Double> indexWeights) {
            this.stocks = stocks;
            this.indexWeights = indexWeights;
            this.pairCorrelations = new PairReturnCorrelation[stocks.size() * (stocks.size() - 1) / 2];
            int pairIndex = 0;
            for (int left = 0; left < stocks.size(); left++) {
                for (int right = left + 1; right < stocks.size(); right++) {
                    pairCorrelations[pairIndex++] = new PairReturnCorrelation(
                            left, right, stocks.get(left).industry().equals(stocks.get(right).industry())
                    );
                }
            }
            for (StockMarketRegime regime : StockMarketRegime.values()) {
                regimeIndexReturns.put(regime, new RunningMean());
            }
        }

        private void record(StockMarketRegime regime, List<StockState> states) {
            for (PairReturnCorrelation pair : pairCorrelations) {
                pair.add(states.get(pair.left()).lastReturn, states.get(pair.right()).lastReturn);
            }
            double index = 0.0;
            for (StockState state : states) {
                index += indexWeights.get(state.spec.key()) * state.price / (double) state.spec.basePrice();
            }
            index *= 1_000.0;
            double indexReturn = index / previousIndex - 1.0;
            regimeIndexReturns.get(regime).add(indexReturn);
            previousIndex = index;
            peakIndex = Math.max(peakIndex, index);
            maxIndexDrawdownPercent = Math.max(
                    maxIndexDrawdownPercent,
                    (peakIndex - index) * 100.0 / peakIndex
            );
        }

        private double terminalIndexRatio() {
            return previousIndex / 1_000.0;
        }

        private double averageCorrelation(boolean sameIndustry) {
            return Arrays.stream(pairCorrelations)
                    .filter(pair -> pair.sameIndustry() == sameIndustry)
                    .mapToDouble(PairReturnCorrelation::correlation)
                    .average().orElse(0.0);
        }
    }

    private static final class PairReturnCorrelation {
        private final int left;
        private final int right;
        private final boolean sameIndustry;
        private long count;
        private double leftMean;
        private double rightMean;
        private double leftSumSquares;
        private double rightSumSquares;
        private double coMoment;

        private PairReturnCorrelation(int left, int right, boolean sameIndustry) {
            this.left = left;
            this.right = right;
            this.sameIndustry = sameIndustry;
        }

        private void add(double leftValue, double rightValue) {
            count++;
            double leftDelta = leftValue - leftMean;
            double rightDelta = rightValue - rightMean;
            leftMean += leftDelta / count;
            rightMean += rightDelta / count;
            leftSumSquares += leftDelta * (leftValue - leftMean);
            rightSumSquares += rightDelta * (rightValue - rightMean);
            coMoment += leftDelta * (rightValue - rightMean);
        }

        private double correlation() {
            return leftSumSquares == 0.0 || rightSumSquares == 0.0
                    ? 0.0 : coMoment / Math.sqrt(leftSumSquares * rightSumSquares);
        }

        private int left() { return left; }
        private int right() { return right; }
        private boolean sameIndustry() { return sameIndustry; }
    }

    private static final class Metrics {
        private final long[] gapHistogram = new long[GAP_HISTOGRAM_MAX_BASIS_POINTS / GAP_HISTOGRAM_STEP_BASIS_POINTS + 2];
        private final double[] terminalRatios;
        private final double[] terminalCagrs;
        private final double[] terminalFundamentalCagrs;
        private final double[] stockMaxDrawdowns;
        private final double[] marketMaxDrawdowns;
        private final double[] marketCagrs;
        private final EnumMap<StockRiskType, RunningVariance> returnVariance = new EnumMap<>(StockRiskType.class);
        private final EnumMap<StockMarketRegime, RunningMean> regimeIndexReturns =
                new EnumMap<>(StockMarketRegime.class);
        private final RunningMean sameIndustryCorrelation = new RunningMean();
        private final RunningMean crossIndustryCorrelation = new RunningMean();
        private long observations;
        private long over10Percent;
        private long over25Percent;
        private long over50Percent;
        private long pathLimitHits;
        private long invalidPrices;
        private int terminalIndex;
        private long marketArticles;
        private long industryArticles;
        private long companyArticles;
        private long simulatedRunYears;
        private int runIndex;

        private Metrics(int terminalCount, int runCount) {
            terminalRatios = new double[terminalCount];
            terminalCagrs = new double[terminalCount];
            terminalFundamentalCagrs = new double[terminalCount];
            stockMaxDrawdowns = new double[terminalCount];
            marketMaxDrawdowns = new double[runCount];
            marketCagrs = new double[runCount];
            for (StockRiskType risk : StockRiskType.values()) {
                returnVariance.put(risk, new RunningVariance());
            }
            for (StockMarketRegime regime : StockMarketRegime.values()) {
                regimeIndexReturns.put(regime, new RunningMean());
            }
        }

        private void recordPoint(StockRiskType risk, StockPriceModel.Result result, long fairValue) {
            observations++;
            if (result.close() <= 0 || result.high() < result.low()) {
                invalidPrices++;
            }
            double gapPercent = Math.abs(result.close() / (double) fairValue - 1.0) * 100.0;
            int gapBasisPoints = (int) Math.min(Integer.MAX_VALUE, Math.round(gapPercent * 100.0));
            int bucket = Math.min(gapHistogram.length - 1, gapBasisPoints / GAP_HISTOGRAM_STEP_BASIS_POINTS);
            gapHistogram[bucket]++;
            if (gapPercent > 10.0) over10Percent++;
            if (gapPercent > 25.0) over25Percent++;
            if (gapPercent > 50.0) over50Percent++;
            if (Math.abs(result.pathImpactBasisPoints()) >= 1_199) pathLimitHits++;
            returnVariance.get(risk).add(result.pathImpactBasisPoints() / 10_000.0);
        }

        private void recordTerminal(
                StockSpec stock,
                long price,
                long fairValue,
                long initialFairValue,
                double maxDrawdownPercent,
                int years
        ) {
            terminalRatios[terminalIndex] = price / (double) fairValue;
            terminalCagrs[terminalIndex] = Math.pow(price / (double) stock.basePrice(), 1.0 / years) - 1.0;
            terminalFundamentalCagrs[terminalIndex] = Math.pow(
                    fairValue / (double) Math.max(1L, initialFairValue), 1.0 / years
            ) - 1.0;
            stockMaxDrawdowns[terminalIndex] = maxDrawdownPercent;
            terminalIndex++;
        }

        private void recordNewsCounts(StockNewsProcess news, int years) {
            marketArticles += news.marketArticleCount();
            industryArticles += news.industryArticleCount();
            companyArticles += news.companyArticleCount();
            simulatedRunYears += years;
        }

        private void recordRun(RunDiagnostics diagnostics, int years) {
            marketMaxDrawdowns[runIndex] = diagnostics.maxIndexDrawdownPercent;
            marketCagrs[runIndex] = Math.pow(diagnostics.terminalIndexRatio(), 1.0 / years) - 1.0;
            runIndex++;
            sameIndustryCorrelation.add(diagnostics.averageCorrelation(true));
            crossIndustryCorrelation.add(diagnostics.averageCorrelation(false));
            for (StockMarketRegime regime : StockMarketRegime.values()) {
                RunningMean source = diagnostics.regimeIndexReturns.get(regime);
                regimeIndexReturns.get(regime).add(source.mean(), source.count());
            }
        }

        private CandidateResult summarize(
                double reversion,
                ShockResult moderateShock,
                ShockResult severeShock
        ) {
            double fundamentalReturnCorrelation = correlation(terminalFundamentalCagrs, terminalCagrs);
            double fundamentalQuartileSpread = fundamentalQuartileSpread(terminalFundamentalCagrs, terminalCagrs);
            Arrays.sort(terminalRatios);
            Arrays.sort(terminalCagrs);
            Arrays.sort(stockMaxDrawdowns);
            Arrays.sort(marketMaxDrawdowns);
            Arrays.sort(marketCagrs);
            EnumMap<StockRiskType, Double> annualVolatility = new EnumMap<>(StockRiskType.class);
            returnVariance.forEach((risk, variance) -> annualVolatility.put(risk, variance.standardDeviation() * Math.sqrt(UPDATES_PER_YEAR) * 100.0));
            return new CandidateResult(
                    reversion,
                    moderateShock,
                    severeShock,
                    histogramPercentile(0.5),
                    histogramPercentile(0.9),
                    histogramPercentile(0.99),
                    over10Percent * 100.0 / observations,
                    over25Percent * 100.0 / observations,
                    over50Percent * 100.0 / observations,
                    pathLimitHits * 100.0 / observations,
                    percentile(terminalRatios, 0.1),
                    percentile(terminalRatios, 0.5),
                    percentile(terminalRatios, 0.9),
                    percentile(terminalCagrs, 0.5) * 100.0,
                    fundamentalReturnCorrelation,
                    fundamentalQuartileSpread * 100.0,
                    marketArticles / (double) simulatedRunYears,
                    industryArticles / (double) simulatedRunYears,
                    companyArticles / (double) simulatedRunYears,
                    percentile(marketCagrs, 0.5) * 100.0,
                    percentile(marketMaxDrawdowns, 0.5),
                    percentile(marketMaxDrawdowns, 0.9),
                    percentile(stockMaxDrawdowns, 0.5),
                    percentile(stockMaxDrawdowns, 0.9),
                    annualizedRegimeReturns(),
                    sameIndustryCorrelation.mean(),
                    crossIndustryCorrelation.mean(),
                    annualVolatility,
                    invalidPrices
            );
        }

        private Map<StockMarketRegime, Double> annualizedRegimeReturns() {
            EnumMap<StockMarketRegime, Double> result = new EnumMap<>(StockMarketRegime.class);
            regimeIndexReturns.forEach((regime, returns) ->
                    result.put(regime, returns.mean() * UPDATES_PER_YEAR * 100.0));
            return result;
        }

        private double correlation(double[] x, double[] y) {
            double xMean = Arrays.stream(x).average().orElse(0.0);
            double yMean = Arrays.stream(y).average().orElse(0.0);
            double covariance = 0.0;
            double xVariance = 0.0;
            double yVariance = 0.0;
            for (int index = 0; index < x.length; index++) {
                double xDelta = x[index] - xMean;
                double yDelta = y[index] - yMean;
                covariance += xDelta * yDelta;
                xVariance += xDelta * xDelta;
                yVariance += yDelta * yDelta;
            }
            return xVariance == 0.0 || yVariance == 0.0
                    ? 0.0 : covariance / Math.sqrt(xVariance * yVariance);
        }

        private double fundamentalQuartileSpread(double[] fundamentals, double[] prices) {
            Integer[] indexes = new Integer[fundamentals.length];
            for (int index = 0; index < indexes.length; index++) {
                indexes[index] = index;
            }
            Arrays.sort(indexes, java.util.Comparator.comparingDouble(index -> fundamentals[index]));
            int quartileSize = Math.max(1, indexes.length / 4);
            double bottom = 0.0;
            double top = 0.0;
            for (int index = 0; index < quartileSize; index++) {
                bottom += prices[indexes[index]];
                top += prices[indexes[indexes.length - 1 - index]];
            }
            return top / quartileSize - bottom / quartileSize;
        }

        private double histogramPercentile(double quantile) {
            long target = Math.max(1L, (long) Math.ceil(observations * quantile));
            long cumulative = 0L;
            for (int bucket = 0; bucket < gapHistogram.length; bucket++) {
                cumulative += gapHistogram[bucket];
                if (cumulative >= target) {
                    return bucket * GAP_HISTOGRAM_STEP_BASIS_POINTS / 100.0;
                }
            }
            return GAP_HISTOGRAM_MAX_BASIS_POINTS / 100.0;
        }
    }

    private static final class RunningVariance {
        private long count;
        private double mean;
        private double sumSquaredDifference;

        private void add(double value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            sumSquaredDifference += delta * (value - mean);
        }

        private double standardDeviation() {
            return count < 2 ? 0.0 : Math.sqrt(sumSquaredDifference / (count - 1));
        }
    }

    private static final class RunningMean {
        private long count;
        private double mean;

        private void add(double value) {
            add(value, 1L);
        }

        private void add(double value, long weight) {
            if (weight <= 0) {
                return;
            }
            long nextCount = count + weight;
            mean += (value - mean) * weight / nextCount;
            count = nextCount;
        }

        private long count() {
            return count;
        }

        private double mean() {
            return mean;
        }
    }

    public record ShockResult(double medianDays, double p90Days, double recoveredPercent) {
    }

    public record CandidateResult(
            double reversion,
            ShockResult moderateShock,
            ShockResult severeShock,
            double medianAbsoluteGapPercent,
            double p90AbsoluteGapPercent,
            double p99AbsoluteGapPercent,
            double over10PercentRate,
            double over25PercentRate,
            double over50PercentRate,
            double pathLimitHitRate,
            double terminalRatioP10,
            double terminalRatioMedian,
            double terminalRatioP90,
            double medianCagrPercent,
            double fundamentalReturnCorrelation,
            double fundamentalQuartileSpreadPercent,
            double marketArticlesPerYear,
            double industryArticlesPerYear,
            double companyArticlesPerYear,
            double marketMedianCagrPercent,
            double marketDrawdownMedianPercent,
            double marketDrawdownP90Percent,
            double stockDrawdownMedianPercent,
            double stockDrawdownP90Percent,
            Map<StockMarketRegime, Double> regimeAnnualizedReturnsPercent,
            double sameIndustryCorrelation,
            double crossIndustryCorrelation,
            Map<StockRiskType, Double> annualVolatilityPercent,
            long invalidPrices
    ) {
    }

    public record Report(int seedCount, int years, long seed, Map<Double, CandidateResult> candidates) {
        public String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Stock Price Long-Term Simulation\n\n")
                    .append("- Seeds: ").append(seedCount).append('\n')
                    .append("- Years per seed: ").append(years).append('\n')
                    .append("- Stocks: 15\n")
                    .append("- Refresh interval: 5 game days\n")
                    .append("- Base seed: ").append(seed).append("\n\n")
                    .append("|Reversion|25% shock half-life median / P90|100% shock half-life median / P90|Abs gap P50 / P90 / P99|Gap >25%|Terminal price/fair P10 / P50 / P90|Median CAGR|Path limit hits|Invalid prices|\n")
                    .append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            candidates.values().forEach(result -> markdown.append(String.format(Locale.ROOT,
                    "|%.2f|%.0f / %.0f days|%.0f / %.0f days|%.2f%% / %.2f%% / %.2f%%|%.2f%%|%.3f / %.3f / %.3f|%.2f%%|%.4f%%|%d|%n",
                    result.reversion(), result.moderateShock().medianDays(), result.moderateShock().p90Days(),
                    result.severeShock().medianDays(), result.severeShock().p90Days(),
                    result.medianAbsoluteGapPercent(), result.p90AbsoluteGapPercent(), result.p99AbsoluteGapPercent(),
                    result.over25PercentRate(), result.terminalRatioP10(), result.terminalRatioMedian(), result.terminalRatioP90(),
                    result.medianCagrPercent(), result.pathLimitHitRate(), result.invalidPrices())));
            markdown.append("\n## Annualized path volatility\n\n")
                    .append("|Reversion|Safe|Normal|Aggressive|\n|---:|---:|---:|---:|\n");
            candidates.values().forEach(result -> markdown.append(String.format(Locale.ROOT,
                    "|%.2f|%.2f%%|%.2f%%|%.2f%%|%n",
                    result.reversion(),
                    result.annualVolatilityPercent().get(StockRiskType.SAFE),
                    result.annualVolatilityPercent().get(StockRiskType.NORMAL),
                    result.annualVolatilityPercent().get(StockRiskType.AGGRESSIVE))));
            markdown.append("\n## Fundamentals and news\n\n")
                    .append("|Reversion|Fundamental/price CAGR correlation|Top-bottom fundamental quartile price CAGR spread|Primary market events/year|Primary industry events/year|Primary company events/year|\n")
                    .append("|---:|---:|---:|---:|---:|---:|\n");
            candidates.values().forEach(result -> markdown.append(String.format(Locale.ROOT,
                    "|%.2f|%.3f|%.2f%%|%.2f|%.2f|%.2f|%n",
                    result.reversion(), result.fundamentalReturnCorrelation(),
                    result.fundamentalQuartileSpreadPercent(), result.marketArticlesPerYear(),
                    result.industryArticlesPerYear(), result.companyArticlesPerYear())));
            markdown.append("\n## Market path and correlation\n\n")
                    .append("|Reversion|Market median CAGR|Market max drawdown P50 / P90|Stock max drawdown P50 / P90|Expansion / Neutral / Recession annualized return|Same / cross industry correlation|\n")
                    .append("|---:|---:|---:|---:|---:|---:|\n");
            candidates.values().forEach(result -> markdown.append(String.format(Locale.ROOT,
                    "|%.2f|%.2f%%|%.2f%% / %.2f%%|%.2f%% / %.2f%%|%.2f%% / %.2f%% / %.2f%%|%.3f / %.3f|%n",
                    result.reversion(), result.marketMedianCagrPercent(),
                    result.marketDrawdownMedianPercent(), result.marketDrawdownP90Percent(),
                    result.stockDrawdownMedianPercent(), result.stockDrawdownP90Percent(),
                    result.regimeAnnualizedReturnsPercent().get(StockMarketRegime.EXPANSION),
                    result.regimeAnnualizedReturnsPercent().get(StockMarketRegime.NEUTRAL),
                    result.regimeAnnualizedReturnsPercent().get(StockMarketRegime.RECESSION),
                    result.sameIndustryCorrelation(), result.crossIndustryCorrelation())));
            return markdown.toString();
        }
    }
}
