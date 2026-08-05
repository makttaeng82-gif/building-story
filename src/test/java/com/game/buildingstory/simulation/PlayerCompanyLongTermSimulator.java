package com.game.buildingstory.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Runs a DB-free monthly simulation of the player-company design.
 *
 * <p>The simulator does not search for a favorable parameter set. It applies the documented
 * values and reports whether growth timing, project quality, incidents, and cash survival land
 * inside the design targets.</p>
 */
public final class PlayerCompanyLongTermSimulator {
    public static final int DEFAULT_SEEDS = 1_000;
    public static final int DEFAULT_MONTHS = 180;
    public static final long DEFAULT_SEED = 20260721L;

    private static final double INITIAL_CASH_EOK = 2_520.0;
    private static final double INITIAL_MARKET_USERS = 2_000_000.0;
    private static final double INITIAL_COMPANY_USERS = 240_000.0;
    private static final double CORPORATE_TAX_RATE = 0.20;

    private static final CloudTier[] CLOUD_TIERS = {
            new CloudTier("스타터", 400_000, 90),
            new CloudTier("성장", 2_000_000, 250),
            new CloudTier("스케일", 10_000_000, 700),
            new CloudTier("엔터프라이즈", 50_000_000, 3_000),
            new CloudTier("하이퍼스케일", 150_000_000, 9_000),
            new CloudTier("글로벌", 600_000_000, 32_000)
    };

    private static final ComputeStep[] COMPUTE_STEPS = {
            new ComputeStep(250_000, 600, 2, 18),
            new ComputeStep(500_000, 330, 1, 35),
            new ComputeStep(750_000, 300, 1, 53),
            new ComputeStep(1_000_000, 270, 1, 70),
            new ComputeStep(2_500_000, 4_800, 3, 100),
            new ComputeStep(5_000_000, 2_640, 2, 200),
            new ComputeStep(7_500_000, 2_400, 2, 300),
            new ComputeStep(10_000_000, 2_160, 2, 400),
            new ComputeStep(20_000_000, 24_000, 4, 660),
            new ComputeStep(30_000_000, 13_200, 3, 990),
            new ComputeStep(40_000_000, 12_000, 3, 1_320),
            new ComputeStep(50_000_000, 10_800, 3, 1_650),
            new ComputeStep(75_000_000, 72_000, 6, 2_500),
            new ComputeStep(100_000_000, 39_600, 4, 3_330),
            new ComputeStep(125_000_000, 36_000, 4, 4_170),
            new ComputeStep(150_000_000, 32_400, 4, 5_000),
            new ComputeStep(250_000_000, 280_000, 8, 7_500),
            new ComputeStep(350_000_000, 154_000, 6, 10_500),
            new ComputeStep(450_000_000, 140_000, 6, 13_500),
            new ComputeStep(600_000_000, 126_000, 6, 18_000)
    };

    public Report simulate() {
        return simulate(DEFAULT_SEEDS, DEFAULT_MONTHS, DEFAULT_SEED);
    }

    public Report simulate(int seedCount, int months, long baseSeed) {
        if (seedCount <= 0 || months <= 0) {
            throw new IllegalArgumentException("seedCount and months must be positive");
        }
        Map<Strategy, List<RunResult>> runs = new EnumMap<>(Strategy.class);
        for (Strategy strategy : Strategy.values()) {
            List<RunResult> strategyRuns = new ArrayList<>(seedCount);
            for (int run = 0; run < seedCount; run++) {
                long sharedSeed = baseSeed + run * 104_729L;
                strategyRuns.add(simulateRun(strategy, months, sharedSeed));
            }
            runs.put(strategy, strategyRuns);
        }
        Map<Strategy, StrategyResult> summaries = new EnumMap<>(Strategy.class);
        runs.forEach((strategy, results) -> summaries.put(strategy, summarize(results, months)));
        return new Report(seedCount, months, baseSeed, summaries);
    }

    private RunResult simulateRun(Strategy strategy, int months, long sharedSeed) {
        Random marketRandom = new Random(sharedSeed);
        Random operationRandom = new Random(sharedSeed ^ (strategy.ordinal() + 1L) * 1_000_003L);
        State state = new State(strategy, operationRandom);

        for (int month = 1; month <= months && !state.stopped; month++) {
            state.month = month;
            if (month % 3 == 1 && month > 1) {
                updateCompetitors(state, marketRandom);
            }
            updateMarket(state, marketRandom);
            updateMarketingAndBrand(state);
            updateMarketShares(state);
            updateCustomersAndCapacityDemand(state);
            manageInfrastructure(state);
            settleCustomersAndRevenue(state);
            updateWorkforce(state);
            progressProductProject(state);
            processIncident(state);
            settleMonthlyFinance(state);
            if (!state.stopped) {
                evaluateCompanyStage(state);
            }
        }
        return state.toResult(months);
    }

    private void updateMarket(State state, Random random) {
        double baseGrowth = interpolate(state.marketUsers,
                new double[]{2_000_000, 20_000_000, 150_000_000, 600_000_000, 800_000_000},
                new double[]{0.05, 0.08, 0.04, 0.01, 0.002});
        double requiredBenchmark = interpolate(state.marketUsers,
                new double[]{2_000_000, 20_000_000, 150_000_000, 600_000_000, 800_000_000},
                new double[]{200, 500, 1_200, 2_200, 2_500});
        double averageBenchmark = weightedBenchmark(state);
        double technologyFactor = clamp(0.7 + 0.3 * averageBenchmark / requiredBenchmark, 0.85, 1.15);
        double newsFactor = random.nextDouble() < 0.08 ? 0.8 + random.nextDouble() * 0.4 : 1.0;
        state.marketUsers = Math.min(800_000_000, state.marketUsers * (1 + baseGrowth * technologyFactor * newsFactor));
    }

    private void updateCompetitors(State state, Random random) {
        double playerScore = serviceScore(state.player, weightedBenchmark(state), state.marketPenalty);
        double leaderScore = Arrays.stream(state.competitors)
                .mapToDouble(company -> serviceScore(company, weightedBenchmark(state), 0))
                .max().orElse(0);
        double response = playerScore >= leaderScore + 5 ? 1.40 : 1.0;
        for (Company competitor : state.competitors) {
            double variation = 0.9 + random.nextDouble() * 0.2;
            double highScoreDamping = competitor.benchmark > 2_500 ? 0.4 : competitor.benchmark > 1_800 ? 0.7 : 1.0;
            competitor.benchmark *= 1 + competitor.profile.benchmarkGrowth * variation * response * highScoreDamping;
            competitor.feature = clamp(competitor.feature + competitor.profile.featureGrowth * variation * response, 0, 100);
            competitor.stability = clamp(competitor.stability + competitor.profile.stabilityGrowth * variation * response, 0, 100);
            competitor.security = clamp(competitor.security + competitor.profile.securityGrowth * variation * response, 0, 100);
            competitor.brand = clamp(competitor.brand + competitor.profile.brandGrowth * variation * response, 0, 100);
            competitor.marketing = clamp(competitor.marketing * 0.6
                    + competitor.profile.marketingTarget * variation * 0.4, 0, 100);
        }
    }

    private void updateMarketingAndBrand(State state) {
        double expertise = state.expertise();
        double campaign = clamp(50 * Math.sqrt(state.strategy.marketingRatio)
                * (0.75 + expertise / 200), 0, 100);
        state.player.marketing = state.player.marketing * 0.6 + campaign * 0.4;
        double rawBrandChange = (state.player.marketing - 50) / 100;
        double resistance = rawBrandChange >= 0 ? (100 - state.player.brand) / 100 : state.player.brand / 100;
        state.player.brand = clamp(state.player.brand + rawBrandChange * resistance, 0, 100);
        state.marketPenalty *= 0.5;
    }

    private void updateMarketShares(State state) {
        double averageBenchmark = weightedBenchmark(state);
        Company[] companies = state.allCompanies();
        double[] weights = new double[companies.length];
        double totalWeight = 0;
        for (int i = 0; i < companies.length; i++) {
            double penalty = i == 0 ? state.marketPenalty : 0;
            weights[i] = Math.pow(Math.max(1, serviceScore(companies[i], averageBenchmark, penalty)), 3);
            totalWeight += weights[i];
        }
        double shareTotal = 0;
        for (int i = 0; i < companies.length; i++) {
            double target = weights[i] / totalWeight;
            double convergence = i == 0 ? 0.05 + state.player.marketing * 0.001 : 0.10;
            companies[i].share += clamp((target - companies[i].share) * convergence, -0.01, 0.01);
            shareTotal += companies[i].share;
        }
        for (Company company : companies) {
            company.share /= shareTotal;
        }
    }

    private void updateCustomersAndCapacityDemand(State state) {
        state.targetUsers = state.marketUsers * state.player.share;
        PlanMix mix = planMix(state);
        state.averageFeeWon = mix.general * 20_000 + mix.pro * 200_000 + mix.max * 1_000_000;
        double baseLoad = mix.general + mix.pro * 6 + mix.max * 20;
        double efficiencyLoad = 1.25 - state.player.efficiency / 200;
        state.processingDemand = state.targetUsers * baseLoad * efficiencyLoad;
    }

    private void manageInfrastructure(State state) {
        if (state.buildMonthsRemaining > 0) {
            state.buildMonthsRemaining--;
            if (state.buildMonthsRemaining == 0) {
                ComputeStep completed = COMPUTE_STEPS[state.buildStepIndex];
                state.cashEok -= completed.capexEok / 2;
                state.capexPaidEok += completed.capexEok / 2;
                state.ownCapacity = completed.capacity;
                state.ownOperatingCostEok = completed.monthlyCostEok;
                state.nextComputeStep = state.buildStepIndex + 1;
                state.buildStepIndex = -1;
            }
        }

        double forecastResidual = Math.max(0, state.lastProcessingDemand - state.ownCapacity);
        int desiredCloud = cloudTierFor(forecastResidual);
        if (state.strategy == Strategy.NEGLECT) {
            if (state.cloudTier < 0) {
                state.cloudTier = 0;
            } else if (forecastResidual > CLOUD_TIERS[state.cloudTier].capacity * 1.15 && state.cloudTier < CLOUD_TIERS.length - 1) {
                state.cloudTier++;
            }
        } else {
            state.cloudTier = desiredCloud;
        }

        if (state.buildMonthsRemaining == 0 && state.nextComputeStep < COMPUTE_STEPS.length
                && state.strategy != Strategy.NEGLECT) {
            ComputeStep next = COMPUTE_STEPS[state.nextComputeStep];
            double currentFixedCost = Math.max(1, state.lastMandatoryCostEok);
            double reserve = currentFixedCost * state.strategy.reserveMonths;
            boolean capacityNeed = state.lastProcessingDemand > next.capacity * state.strategy.ownBuildTrigger;
            if (capacityNeed && state.cashEok - next.capexEok > reserve) {
                state.cashEok -= next.capexEok / 2;
                state.capexPaidEok += next.capexEok / 2;
                state.buildStepIndex = state.nextComputeStep;
                state.buildMonthsRemaining = next.months;
            }
        }

        double permanentCapacity = state.ownCapacity + cloudCapacity(state.cloudTier);
        state.emergencyCapacity = 0;
        state.emergencyCostEok = 0;
        if (state.processingDemand > permanentCapacity && permanentCapacity > 0 && state.strategy != Strategy.NEGLECT) {
            double shortage = state.processingDemand - permanentCapacity;
            double selectedRatio = shortage <= permanentCapacity * 0.10 ? 0.10
                    : shortage <= permanentCapacity * 0.25 ? 0.25
                    : shortage <= permanentCapacity * 0.50 ? 0.50 : 0;
            if (selectedRatio > 0) {
                state.emergencyCapacity = permanentCapacity * selectedRatio;
                state.emergencyCostEok = emergencyCost(state.emergencyCapacity);
            }
        }
    }

    private void settleCustomersAndRevenue(State state) {
        double totalCapacity = state.ownCapacity + cloudCapacity(state.cloudTier) + state.emergencyCapacity;
        double servedRatio = state.processingDemand <= 0 ? 1 : Math.min(1, totalCapacity / state.processingDemand);
        state.servedUsers = state.targetUsers * servedRatio;
        state.utilization = totalCapacity <= 0 ? 200 : state.processingDemand / totalCapacity * 100;
        if (servedRatio < 1) {
            state.shortageMonths++;
            double shortageRatio = 1 - servedRatio;
            state.player.share = Math.max(0.001, state.player.share * (1 - Math.min(0.05, shortageRatio * 0.20)));
            state.slaBreaches++;
        }
        double subscriptionRevenue = state.servedUsers * state.averageFeeWon / 100_000_000.0;
        state.recurringRevenueEok = subscriptionRevenue * 1.20;
        state.lastProcessingDemand = state.processingDemand;
    }

    private void updateWorkforce(State state) {
        double target = interpolate(Math.max(INITIAL_COMPANY_USERS, state.servedUsers),
                new double[]{240_000, 2_000_000, 15_000_000, 80_000_000},
                new double[]{18, 500, 10_000, 100_000});
        double delta = target - state.workforce;
        double change = delta > 0 ? Math.min(delta, Math.max(10, state.workforce * state.strategy.hiringRate))
                : Math.max(delta, -Math.max(5, state.workforce * 0.20));
        if (state.strategy == Strategy.NEGLECT && delta > 0) {
            change *= 0.25;
        }
        state.newHires = Math.max(0, change);
        state.workforce = Math.max(1, state.workforce + change);
    }

    private void progressProductProject(State state) {
        if (state.strategy.developmentRatio < 1.0) {
            state.techDebt = clamp(state.techDebt + (1 - state.strategy.developmentRatio) * 0.5, 0, 100);
        }
        if (state.projectMonthsRemaining <= 0) {
            state.projectType = chooseProject(state);
            state.projectMonthsRemaining = projectDuration(state, state.projectType);
        }
        state.projectMonthsRemaining--;
        if (state.projectMonthsRemaining > 0) {
            return;
        }
        double quality = projectQuality(state);
        state.projectQualities.add(quality);
        double resultMultiplier = 0.75 + quality / 200;
        switch (state.projectType) {
            case MODEL -> {
                double gain = clamp(state.player.benchmark * 0.08, 15, 150) * resultMultiplier;
                state.player.benchmark += gain;
                state.player.efficiency = clamp(state.player.efficiency - 2, 0, 100);
                state.techDebt = clamp(state.techDebt + 5, 0, 100);
                state.player.brand = clamp(state.player.brand + 1, 0, 100);
            }
            case FEATURE -> {
                state.player.feature = clamp(state.player.feature
                        + Math.max(3, (100 - state.player.feature) * 0.15) * resultMultiplier, 0, 100);
                state.techDebt = clamp(state.techDebt + 4, 0, 100);
            }
            case STABILITY -> {
                state.player.stability = clamp(state.player.stability
                        + Math.max(2, (100 - state.player.stability) * 0.12) * resultMultiplier, 0, 100);
                state.player.security = clamp(state.player.security
                        + Math.max(2, (100 - state.player.security) * 0.08) * resultMultiplier, 0, 100);
                state.techDebt = clamp(state.techDebt - 10, 0, 100);
            }
            case EFFICIENCY -> {
                state.player.efficiency = clamp(state.player.efficiency
                        + Math.max(3, (100 - state.player.efficiency) * 0.15) * resultMultiplier, 0, 100);
                state.techDebt = clamp(state.techDebt - 3, 0, 100);
            }
        }
        state.completedProjects++;
    }

    private ProjectType chooseProject(State state) {
        if (state.strategy == Strategy.NEGLECT) {
            return state.completedProjects % 3 == 0 ? ProjectType.MODEL : ProjectType.FEATURE;
        }
        if (state.player.stability < 70 || state.techDebt > 35) {
            return ProjectType.STABILITY;
        }
        if (state.player.feature < 42) {
            return ProjectType.FEATURE;
        }
        if (state.player.efficiency < 60) {
            return ProjectType.EFFICIENCY;
        }
        return state.completedProjects % 4 == 3 ? ProjectType.STABILITY : ProjectType.MODEL;
    }

    private int projectDuration(State state, ProjectType type) {
        double workload = switch (type) {
            case STABILITY -> 180;
            case FEATURE, EFFICIENCY -> 240;
            case MODEL -> 300;
        };
        double benchmarkMultiplier = state.player.benchmark < 300 ? 1.0
                : state.player.benchmark < 600 ? 1.4
                : state.player.benchmark < 1_100 ? 2.0
                : state.player.benchmark < 1_800 ? 3.0 : 4.5;
        double aiStaff = Math.max(1, state.workforce * 0.45);
        double processing = Math.min(1_000, 100 * log2(1 + aiStaff / 10))
                * (0.8 + state.averageSkill() / 250)
                * (0.75 + state.expertise() / 200);
        processing *= state.strategy == Strategy.AGGRESSIVE ? 1.10 : state.strategy == Strategy.NEGLECT ? 0.75 : 1.0;
        int minimum = type == ProjectType.MODEL ? 2 : 1;
        int validationMonths = state.strategy == Strategy.CONSERVATIVE ? 1 : 0;
        return Math.max(minimum, Math.min(18,
                (int) Math.ceil(workload * benchmarkMultiplier / Math.max(1, processing)) + validationMonths));
    }

    private double projectQuality(State state) {
        double expertise = state.expertise() + state.operationRandom.nextGaussian() * 4;
        double infrastructureReadiness = clamp(100 - Math.max(0, state.utilization - 80) * 2.5, 0, 100);
        double budgetFactor = clamp(Math.sqrt(state.strategy.developmentRatio), 0.75, 1.25);
        double overloadPenalty = Math.min(15, Math.max(0, state.utilization - 100) * 0.5);
        double validation = state.strategy == Strategy.CONSERVATIVE ? 5
                : state.strategy == Strategy.AGGRESSIVE ? -3
                : state.strategy == Strategy.NEGLECT ? -10 : 0;
        return clamp(expertise * 0.65 + infrastructureReadiness * 0.15 + 20 * budgetFactor
                + validation - state.techDebt * 0.20 - overloadPenalty, 0, 100);
    }

    private void processIncident(State state) {
        if (state.incidentCooldown > 0) {
            state.incidentCooldown--;
        }
        double chance = incidentChance(state.techDebt, state.player.stability, state.utilization);
        if (state.operationRandom.nextDouble() >= chance) {
            return;
        }
        double roll = state.operationRandom.nextDouble();
        IncidentSeverity severity = roll < 0.70 ? IncidentSeverity.MINOR
                : roll < 0.95 ? IncidentSeverity.MAJOR : IncidentSeverity.CRITICAL;
        if (state.incidentCooldown > 0 && severity != IncidentSeverity.MINOR) {
            severity = IncidentSeverity.MINOR;
        }
        state.incidents++;
        switch (severity) {
            case MINOR -> state.techDebt = clamp(state.techDebt + 1, 0, 100);
            case MAJOR -> {
                state.majorIncidents++;
                state.player.stability = clamp(state.player.stability - 2, 0, 100);
                state.player.brand = clamp(state.player.brand - 2, 0, 100);
                state.marketPenalty += 5;
                state.incidentCostEok += state.recurringRevenueEok * 0.05;
                state.incidentCooldown = 2;
            }
            case CRITICAL -> {
                state.majorIncidents++;
                state.criticalIncidents++;
                state.player.stability = clamp(state.player.stability - 5, 0, 100);
                state.player.brand = clamp(state.player.brand - 5, 0, 100);
                state.marketPenalty += 10;
                state.incidentCostEok += state.recurringRevenueEok * 0.20;
                state.incidentCooldown = 2;
            }
        }
    }

    static double incidentChance(double techDebt, double stability, double utilization) {
        double percent = 1 + techDebt * 0.10
                + Math.max(0, 75 - stability) * 0.15
                + Math.max(0, utilization - 90) * 0.10;
        return clamp(percent, 0.5, 20) / 100;
    }

    private void settleMonthlyFinance(State state) {
        double recentRevenue = state.revenueAverage();
        double developmentRate = marketRate(state.marketUsers, 0.35, 0.22, 0.15, 0.12);
        double marketingRate = marketRate(state.marketUsers, 0.15, 0.10, 0.08, 0.06);
        double developmentCost = Math.max(state.month <= 3 ? 20 : 0, recentRevenue * developmentRate)
                * state.strategy.developmentRatio;
        double marketingCost = Math.max(state.month <= 3 ? 8 : 0, recentRevenue * marketingRate)
                * state.strategy.marketingRatio;
        double salaryScale = state.growthMonth == 0 ? 1.0 : state.largeMonth == 0 ? 1.15
                : state.globalMonth == 0 ? 1.35 : 2.50;
        double averageMonthlySalaryEok = 0.03 * salaryScale;
        double payroll = state.workforce * averageMonthlySalaryEok * 1.20 + 1.0;
        double recruiting = state.newHires * averageMonthlySalaryEok * 12 * 0.05;
        double cloudCost = state.cloudTier < 0 ? 0 : CLOUD_TIERS[state.cloudTier].monthlyCostEok;
        double platformOperatingRate = marketRate(state.marketUsers, 0.08, 0.12, 0.20, 0.25);
        double platformOperatingCost = state.recurringRevenueEok * platformOperatingRate;
        double operatingCost = developmentCost + marketingCost + payroll + recruiting + cloudCost
                + platformOperatingCost
                + state.ownOperatingCostEok + state.emergencyCostEok + state.incidentCostEok;
        double pretaxProfit = state.recurringRevenueEok - operatingCost;
        double tax = Math.max(0, pretaxProfit * CORPORATE_TAX_RATE);
        state.cashEok += pretaxProfit - tax;
        state.lastMandatoryCostEok = payroll + cloudCost + state.ownOperatingCostEok;
        state.lastOperatingMargin = state.recurringRevenueEok <= 0 ? -1
                : (pretaxProfit / state.recurringRevenueEok);
        state.recordRevenue(state.recurringRevenueEok);
        state.incidentCostEok = 0;
        if (state.cashEok < 0) {
            state.stopped = true;
            state.stopMonth = state.month;
        }
    }

    private void evaluateCompanyStage(State state) {
        if (state.month % 3 != 0) {
            return;
        }
        if (state.growthMonth == 0) {
            state.growthQuarterStreak = state.recurringRevenueEok >= 1_000 && state.servedUsers >= 2_000_000
                    ? state.growthQuarterStreak + 1 : 0;
            if (state.growthQuarterStreak >= 2) {
                state.growthMonth = state.month;
            }
            return;
        }
        if (state.largeMonth == 0) {
            state.largeQuarterStreak = state.recurringRevenueEok >= 10_000 && state.servedUsers >= 15_000_000
                    ? state.largeQuarterStreak + 1 : 0;
            if (state.largeQuarterStreak >= 2) {
                state.largeMonth = state.month;
            }
            return;
        }
        if (state.globalMonth == 0) {
            state.globalQuarterStreak = state.recurringRevenueEok >= 100_000 && state.servedUsers >= 80_000_000
                    ? state.globalQuarterStreak + 1 : 0;
            if (state.globalQuarterStreak >= 2) {
                state.globalMonth = state.month;
            }
        }
    }

    private StrategyResult summarize(List<RunResult> runs, int months) {
        List<Integer> growth = reachedMonths(runs, Stage.GROWTH);
        List<Integer> large = reachedMonths(runs, Stage.LARGE);
        List<Integer> global = reachedMonths(runs, Stage.GLOBAL);
        List<Integer> stopMonths = runs.stream().filter(RunResult::stopped)
                .map(RunResult::stopMonth).sorted().toList();
        List<Double> quality = runs.stream().flatMap(run -> run.projectQualities.stream()).sorted().toList();
        double activeYears = runs.stream().mapToInt(RunResult::activeMonths).sum() / 12.0;
        List<RunResult> survivors = runs.stream().filter(run -> !run.stopped).toList();
        return new StrategyResult(
                stageStats(growth, runs.size(), 60),
                stageStats(large, runs.size(), 108),
                stageStats(global, runs.size(), 180),
                percent(runs.stream().filter(RunResult::stopped).count(), runs.size()),
                percentileInt(stopMonths, 0.50),
                runs.stream().filter(RunResult::stopped).mapToDouble(RunResult::capexPaidEok).average().orElse(0),
                percentile(quality, 0.10), percentile(quality, 0.50), percentile(quality, 0.90),
                activeYears == 0 ? 0 : runs.stream().mapToInt(RunResult::incidents).sum() / activeYears,
                activeYears == 0 ? 0 : runs.stream().mapToInt(RunResult::majorIncidents).sum() / activeYears,
                activeYears == 0 ? 0 : runs.stream().mapToInt(RunResult::criticalIncidents).sum() / activeYears,
                runs.stream().mapToDouble(run -> run.shortageMonths).average().orElse(0),
                survivors.stream().mapToDouble(run -> run.finalRevenueEok).average().orElse(0),
                survivors.stream().mapToDouble(run -> run.finalOperatingMargin).average().orElse(0) * 100,
                survivors.stream().mapToDouble(run -> run.finalCashEok).average().orElse(0),
                survivors.stream().mapToDouble(run -> run.finalUsers).average().orElse(0),
                survivors.stream().mapToDouble(run -> run.finalShare).average().orElse(0) * 100,
                runs.stream().mapToInt(run -> run.slaBreaches).sum()
        );
    }

    private List<Integer> reachedMonths(List<RunResult> runs, Stage stage) {
        return runs.stream().map(run -> switch (stage) {
            case GROWTH -> run.growthMonth;
            case LARGE -> run.largeMonth;
            case GLOBAL -> run.globalMonth;
        }).filter(month -> month > 0).sorted().toList();
    }

    private StageStats stageStats(List<Integer> reached, int total, int deadline) {
        return new StageStats(
                percentileInt(reached, 0.10), percentileInt(reached, 0.50), percentileInt(reached, 0.90),
                percent(reached.size(), total),
                percent(reached.stream().filter(month -> month <= deadline).count(), total)
        );
    }

    private PlanMix planMix(State state) {
        double relativeTechnology = state.player.benchmark / weightedBenchmark(state);
        if (state.player.feature >= 90 && state.player.stability >= 90 && state.player.security >= 90
                && relativeTechnology >= 1.05) {
            return new PlanMix(0.70, 0.25, 0.05);
        }
        if (state.player.feature >= 80 && state.player.stability >= 80 && state.player.security >= 75) {
            return new PlanMix(0.80, 0.18, 0.02);
        }
        if (state.player.feature >= 70 && state.player.stability >= 75 && state.player.security >= 70
                && relativeTechnology >= 1.0) {
            return new PlanMix(0.88, 0.11, 0.01);
        }
        if (state.player.feature >= 40 && relativeTechnology >= 0.9) {
            return new PlanMix(0.90, 0.10, 0);
        }
        return new PlanMix(1, 0, 0);
    }

    private double weightedBenchmark(State state) {
        double total = state.player.benchmark * state.player.share;
        for (Company competitor : state.competitors) {
            total += competitor.benchmark * competitor.share;
        }
        return total;
    }

    private double serviceScore(Company company, double averageBenchmark, double penalty) {
        double technology = clamp(50 + 125 * (company.benchmark / averageBenchmark - 1), 0, 100);
        return clamp(technology * 0.35 + company.feature * 0.25 + company.stability * 0.15
                + company.brand * 0.15 + company.marketing * 0.10 - penalty, 1, 100);
    }

    private int cloudTierFor(double residualDemand) {
        if (residualDemand <= 0) {
            return -1;
        }
        for (int i = 0; i < CLOUD_TIERS.length; i++) {
            if (residualDemand <= CLOUD_TIERS[i].capacity) {
                return i;
            }
        }
        return CLOUD_TIERS.length - 1;
    }

    private double cloudCapacity(int index) {
        return index < 0 ? 0 : CLOUD_TIERS[index].capacity;
    }

    private double emergencyCost(double capacity) {
        int reference = cloudTierFor(capacity);
        CloudTier tier = CLOUD_TIERS[Math.max(0, reference)];
        return capacity * tier.monthlyCostEok / tier.capacity * 1.8;
    }

    private double marketRate(double users, double introduction, double growth, double mass, double mature) {
        return interpolate(users,
                new double[]{2_000_000, 20_000_000, 150_000_000, 600_000_000},
                new double[]{introduction, growth, mass, mature});
    }

    private static double interpolate(double value, double[] points, double[] values) {
        if (value <= points[0]) {
            return values[0];
        }
        for (int i = 1; i < points.length; i++) {
            if (value <= points[i]) {
                double ratio = (value - points[i - 1]) / (points[i] - points[i - 1]);
                return values[i - 1] + (values[i] - values[i - 1]) * ratio;
            }
        }
        return values[values.length - 1];
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.round((sorted.size() - 1) * percentile);
        return sorted.get(index);
    }

    private static int percentileInt(List<Integer> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.round((sorted.size() - 1) * percentile);
        return sorted.get(index);
    }

    private static double percent(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator * 100.0 / denominator;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum Strategy {
        CONSERVATIVE("보수", 1.0, 1.0, 0.08, 12, 0.85),
        BALANCED("균형", 1.0, 1.0, 0.15, 6, 0.70),
        AGGRESSIVE("공세", 1.25, 1.25, 0.20, 3, 0.55),
        NEGLECT("방치", 0.6, 0.5, 0.02, 0, Double.POSITIVE_INFINITY);

        private final String label;
        private final double developmentRatio;
        private final double marketingRatio;
        private final double hiringRate;
        private final int reserveMonths;
        private final double ownBuildTrigger;

        Strategy(String label, double developmentRatio, double marketingRatio, double hiringRate,
                 int reserveMonths, double ownBuildTrigger) {
            this.label = label;
            this.developmentRatio = developmentRatio;
            this.marketingRatio = marketingRatio;
            this.hiringRate = hiringRate;
            this.reserveMonths = reserveMonths;
            this.ownBuildTrigger = ownBuildTrigger;
        }
    }

    private enum Stage { GROWTH, LARGE, GLOBAL }
    private enum ProjectType { MODEL, FEATURE, STABILITY, EFFICIENCY }
    private enum IncidentSeverity { MINOR, MAJOR, CRITICAL }

    private enum CompetitorProfile {
        TECHNOLOGY(0.08, 1, 1, 1, 0.5, 35),
        MASS(0.05, 3, 1, 1, 3, 60),
        TRUST(0.06, 1, 3, 4, 1.5, 38);

        private final double benchmarkGrowth;
        private final double featureGrowth;
        private final double stabilityGrowth;
        private final double securityGrowth;
        private final double brandGrowth;
        private final double marketingTarget;

        CompetitorProfile(double benchmarkGrowth, double featureGrowth, double stabilityGrowth,
                          double securityGrowth, double brandGrowth, double marketingTarget) {
            this.benchmarkGrowth = benchmarkGrowth;
            this.featureGrowth = featureGrowth;
            this.stabilityGrowth = stabilityGrowth;
            this.securityGrowth = securityGrowth;
            this.brandGrowth = brandGrowth;
            this.marketingTarget = marketingTarget;
        }
    }

    private static final class State {
        private final Strategy strategy;
        private final Random operationRandom;
        private final Company player = new Company(200, 35, 65, 45, 55, 20, 40, 0.12, null);
        private final Company[] competitors = {
                new Company(230, 32, 58, 42, 55, 18, 35, 0.26, CompetitorProfile.TECHNOLOGY),
                new Company(185, 42, 62, 40, 55, 35, 60, 0.36, CompetitorProfile.MASS),
                new Company(195, 34, 78, 72, 55, 27, 38, 0.26, CompetitorProfile.TRUST)
        };
        private final List<Double> projectQualities = new ArrayList<>();
        private final double[] revenueHistory = new double[3];

        private int month;
        private double cashEok;
        private double marketUsers = INITIAL_MARKET_USERS;
        private double targetUsers = INITIAL_COMPANY_USERS;
        private double servedUsers = INITIAL_COMPANY_USERS;
        private double averageFeeWon = 20_000;
        private double recurringRevenueEok = 48;
        private double processingDemand = INITIAL_COMPANY_USERS;
        private double lastProcessingDemand = INITIAL_COMPANY_USERS;
        private double utilization = 60;
        private double workforce = 18;
        private double newHires;
        private double techDebt = 15;
        private double marketPenalty;
        private double incidentCostEok;
        private int cloudTier = 0;
        private int nextComputeStep;
        private int buildStepIndex = -1;
        private int buildMonthsRemaining;
        private double ownCapacity;
        private double ownOperatingCostEok;
        private double emergencyCapacity;
        private double emergencyCostEok;
        private double lastMandatoryCostEok = 120;
        private double lastOperatingMargin;
        private double capexPaidEok;
        private int projectMonthsRemaining;
        private ProjectType projectType;
        private int completedProjects;
        private int incidentCooldown;
        private int incidents;
        private int majorIncidents;
        private int criticalIncidents;
        private int shortageMonths;
        private int slaBreaches;
        private boolean stopped;
        private int stopMonth;
        private int growthQuarterStreak;
        private int largeQuarterStreak;
        private int globalQuarterStreak;
        private int growthMonth;
        private int largeMonth;
        private int globalMonth;

        private State(Strategy strategy, Random operationRandom) {
            this.strategy = strategy;
            this.operationRandom = operationRandom;
            this.cashEok = strategy == Strategy.CONSERVATIVE || strategy == Strategy.AGGRESSIVE
                    ? 4_520.0 : INITIAL_CASH_EOK;
            Arrays.fill(revenueHistory, recurringRevenueEok);
        }

        private Company[] allCompanies() {
            return new Company[]{player, competitors[0], competitors[1], competitors[2]};
        }

        private double expertise() {
            double base = switch (strategy) {
                case CONSERVATIVE -> 48 + month * 0.20;
                case BALANCED -> 45 + month * 0.25;
                case AGGRESSIVE -> 43 + month * 0.30;
                case NEGLECT -> 40 + month * 0.08;
            };
            return clamp(base, 30, strategy == Strategy.NEGLECT ? 62 : 95);
        }

        private double averageSkill() {
            return clamp(45 + month * (strategy == Strategy.NEGLECT ? 0.08 : 0.22), 40, 90);
        }

        private double revenueAverage() {
            return Arrays.stream(revenueHistory).average().orElse(recurringRevenueEok);
        }

        private void recordRevenue(double revenue) {
            revenueHistory[0] = revenueHistory[1];
            revenueHistory[1] = revenueHistory[2];
            revenueHistory[2] = revenue;
        }

        private RunResult toResult(int plannedMonths) {
            return new RunResult(growthMonth, largeMonth, globalMonth, stopped, stopMonth,
                    List.copyOf(projectQualities), incidents, majorIncidents, criticalIncidents,
                    shortageMonths, slaBreaches, recurringRevenueEok, lastOperatingMargin,
                    cashEok, servedUsers, player.share, Math.min(month, plannedMonths), capexPaidEok);
        }
    }

    private static final class Company {
        private double benchmark;
        private double feature;
        private double stability;
        private double security;
        private double efficiency;
        private double brand;
        private double marketing;
        private double share;
        private final CompetitorProfile profile;

        private Company(double benchmark, double feature, double stability, double security,
                        double efficiency, double brand, double marketing, double share,
                        CompetitorProfile profile) {
            this.benchmark = benchmark;
            this.feature = feature;
            this.stability = stability;
            this.security = security;
            this.efficiency = efficiency;
            this.brand = brand;
            this.marketing = marketing;
            this.share = share;
            this.profile = profile;
        }
    }

    private record CloudTier(String name, double capacity, double monthlyCostEok) { }
    private record ComputeStep(double capacity, double capexEok, int months, double monthlyCostEok) { }
    private record PlanMix(double general, double pro, double max) { }

    private record RunResult(int growthMonth, int largeMonth, int globalMonth, boolean stopped, int stopMonth,
                             List<Double> projectQualities, int incidents, int majorIncidents,
                             int criticalIncidents, int shortageMonths, int slaBreaches,
                             double finalRevenueEok, double finalOperatingMargin, double finalCashEok,
                             double finalUsers, double finalShare, int activeMonths, double capexPaidEok) { }

    public record StageStats(int p10Month, int medianMonth, int p90Month,
                             double reachedPercent, double deadlinePercent) { }

    public record StrategyResult(StageStats growth, StageStats large, StageStats global,
                                 double stoppedPercent,
                                 int medianStopMonth, double stoppedRunAverageCapexEok,
                                 double qualityP10, double qualityMedian, double qualityP90,
                                 double annualIncidents, double annualMajorIncidents,
                                 double annualCriticalIncidents, double averageShortageMonths,
                                 double finalRevenueEok, double finalOperatingMarginPercent,
                                 double finalCashEok, double finalUsers, double finalSharePercent,
                                 long totalSlaBreaches) { }

    public record Report(int seedCount, int months, long baseSeed,
                         Map<Strategy, StrategyResult> strategies) {
        public String toMarkdown() {
            StringBuilder out = new StringBuilder("# 플레이어 기업 장기 시뮬레이션\n\n")
                    .append("- 전략별 시드: ").append(seedCount).append("개\n")
                    .append("- 기간: ").append(months).append("개월\n")
                    .append("- 기준 시드: `").append(baseSeed).append("`\n")
                    .append("- 단위: 금액은 억원, 이용자는 명\n\n")
                    .append("## 성장 단계\n\n")
                    .append("| 전략 | 성장기업 P10/중앙/P90 | 60개월 도달 | 대기업 P10/중앙/P90 | 108개월 도달 | 글로벌 P10/중앙/P90 | 180개월 도달 | 운영중단 | 중단 중앙월 | 중단 실행 평균 설비투자 |\n")
                    .append("| --- | --- | ---: | --- | ---: | --- | ---: | ---: | ---: | ---: |\n");
            strategies.forEach((strategy, result) -> out.append("| ").append(strategy.label)
                    .append(" | ").append(stageText(result.growth))
                    .append(" | ").append(format(result.growth.deadlinePercent)).append("%")
                    .append(" | ").append(stageText(result.large))
                    .append(" | ").append(format(result.large.deadlinePercent)).append("%")
                    .append(" | ").append(stageText(result.global))
                    .append(" | ").append(format(result.global.deadlinePercent)).append("%")
                    .append(" | ").append(format(result.stoppedPercent)).append("%")
                    .append(" | ").append(result.medianStopMonth == 0 ? "-" : result.medianStopMonth)
                    .append(" | ").append(format(result.stoppedRunAverageCapexEok)).append("억원 |\n"));

            out.append("\n## 품질과 장애\n\n")
                    .append("| 전략 | 품질 P10/중앙/P90 | 연간 장애 | 연간 중대 이상 | 연간 치명 | 평균 용량부족 개월 | SLA 위반 합계 |\n")
                    .append("| --- | --- | ---: | ---: | ---: | ---: | ---: |\n");
            strategies.forEach((strategy, result) -> out.append("| ").append(strategy.label)
                    .append(" | ").append(format(result.qualityP10)).append(" / ")
                    .append(format(result.qualityMedian)).append(" / ").append(format(result.qualityP90))
                    .append(" | ").append(format(result.annualIncidents))
                    .append(" | ").append(format(result.annualMajorIncidents))
                    .append(" | ").append(format(result.annualCriticalIncidents))
                    .append(" | ").append(format(result.averageShortageMonths))
                    .append(" | ").append(result.totalSlaBreaches).append(" |\n"));

            out.append("\n## 180개월 시점 평균\n\n")
                    .append("| 전략 | 월 반복매출 | 영업이익률 | 법인현금 | 유료 이용자 | 점유율 |\n")
                    .append("| --- | ---: | ---: | ---: | ---: | ---: |\n");
            strategies.forEach((strategy, result) -> out.append("| ").append(strategy.label)
                    .append(" | ").append(format(result.finalRevenueEok)).append("억원")
                    .append(" | ").append(format(result.finalOperatingMarginPercent)).append("%")
                    .append(" | ").append(format(result.finalCashEok)).append("억원")
                    .append(" | ").append(String.format(Locale.US, "%,.0f", result.finalUsers))
                    .append(" | ").append(format(result.finalSharePercent)).append("% |\n"));
            return out.toString();
        }

        private static String stageText(StageStats stats) {
            if (stats.reachedPercent == 0) {
                return "미도달";
            }
            return stats.p10Month + " / " + stats.medianMonth + " / " + stats.p90Month
                    + " (" + format(stats.reachedPercent) + "%)";
        }

        private static String format(double value) {
            return String.format(Locale.US, "%,.1f", value);
        }
    }
}
