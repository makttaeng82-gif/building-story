package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCompetitor;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** AI 유료시장 성장, 경쟁사 점유율과 플레이어 구독 매출을 월 단위로 계산한다. */
@Service
public class CompanyMarketService {
    private static final long MAXIMUM_MARKET_USERS = 800_000_000L;
    private static final long NORMAL_PRICE = 20_000L;
    private static final long PRO_PRICE = 200_000L;
    private static final long MAX_PRICE = 1_000_000L;

    private final CompanyCompetitorRepository competitorRepository;
    private final CompanyInfrastructureService infrastructureService;
    private final CompanyExternalEventService externalEventService;

    public CompanyMarketService(CompanyCompetitorRepository competitorRepository,
                                CompanyInfrastructureService infrastructureService,
                                CompanyExternalEventService externalEventService) {
        this.competitorRepository = competitorRepository;
        this.infrastructureService = infrastructureService;
        this.externalEventService = externalEventService;
    }

    @Transactional
    public void initializeMarket(PlayerCompany company) {
        if (competitorRepository.existsByCompany(company)) {
            return;
        }
        competitorRepository.saveAll(List.of(
                new CompanyCompetitor(company, "frontier", "프론티어AI", "기술선도",
                        230, 32, 58, 42, 18, 35, 26),
                new CompanyCompetitor(company, "popular", "모두AI", "대중화",
                        185, 42, 62, 40, 35, 60, 36),
                new CompanyCompetitor(company, "trust", "트러스트랩", "신뢰중심",
                        195, 34, 78, 72, 27, 38, 26)
        ));
    }

    @Transactional
    public MarketSnapshot processMonth(PlayerCompany company) {
        initializeMarket(company);
        List<CompanyCompetitor> competitors = competitorRepository.findByCompanyOrderById(company);
        double marketBenchmark = marketBenchmark(company, competitors);
        double playerScore = playerCompetitiveness(company, marketBenchmark);
        int processingMarketMonth = company.getMarketMonthsProcessed() + 1;
        List<Double> competitorScores = competitors.stream()
                .map(item -> competitorCompetitiveness(item, marketBenchmark, processingMarketMonth))
                .toList();
        double totalWeight = cube(playerScore) + competitorScores.stream().mapToDouble(this::cube).sum();
        double playerTargetShare = cube(playerScore) * 100.0 / totalWeight;
        double playerMovementSpeed = Math.max(0.05, Math.min(
                0.15, 0.05 + company.getMarketingEffect() * 0.001));
        double newPlayerShare = moveShare(company.getMarketShare(), playerTargetShare, playerMovementSpeed);

        List<Double> movedCompetitorShares = new ArrayList<>();
        for (int index = 0; index < competitors.size(); index++) {
            double target = cube(competitorScores.get(index)) * 100.0 / totalWeight;
            movedCompetitorShares.add(moveShare(competitors.get(index).getMarketShare(), target, 0.10));
        }
        double movedTotal = newPlayerShare + movedCompetitorShares.stream().mapToDouble(Double::doubleValue).sum();
        newPlayerShare = newPlayerShare * 100.0 / movedTotal;
        for (int index = 0; index < competitors.size(); index++) {
            competitors.get(index).changeMarketShare(movedCompetitorShares.get(index) * 100.0 / movedTotal);
        }

        double marketGrowthRate = marketGrowthRate(company.getTotalMarketUsers(), marketBenchmark)
                * externalEventService.marketGrowthMultiplier(company, processingMarketMonth);
        long totalMarketUsers = Math.min(MAXIMUM_MARKET_USERS,
                Math.round(company.getTotalMarketUsers() * (1.0 + marketGrowthRate)));
        long targetPaidUsers = Math.round(totalMarketUsers * newPlayerShare / 100.0);
        PlanMix mix = planMix(company, marketBenchmark, targetPaidUsers);
        var accepted = infrastructureService.applyCapacity(company, mix.normal(), mix.pro(), mix.max());
        long revenue = subscriptionRevenue(accepted.normal(), accepted.pro(), accepted.max());
        company.updateMarketResult(totalMarketUsers, newPlayerShare,
                accepted.normal(), accepted.pro(), accepted.max(), revenue);

        if (company.getMarketMonthsProcessed() % 3 == 0) {
            competitors.forEach(CompanyCompetitor::advanceQuarter);
        }
        return snapshot(company);
    }

    @Transactional(readOnly = true)
    public MarketSnapshot snapshot(PlayerCompany company) {
        List<CompanyCompetitor> competitors = competitorRepository.findByCompanyOrderById(company);
        double marketBenchmark = marketBenchmark(company, competitors);
        List<CompetitorSnapshot> competitorViews = competitors.stream()
                .map(item -> new CompetitorSnapshot(item.getName(), item.getStrategy(), item.getBenchmark(),
                        item.getCompleteness(), item.getStability(), item.getSecurity(), item.getMarketShare()))
                .sorted(Comparator.comparingDouble(CompetitorSnapshot::marketShare).reversed())
                .toList();
        int rank = 1 + (int) competitors.stream().filter(item -> item.getBenchmark() > company.getPrototypeBenchmark()).count();
        return new MarketSnapshot(company.getTotalMarketUsers(), company.getMarketShare(), marketBenchmark, rank,
                company.getNormalSubscribers(), company.getProSubscribers(), company.getMaxSubscribers(),
                company.getPaidUsers(), company.getMonthlyRecurringRevenue(), competitorViews);
    }

    private PlanMix planMix(PlayerCompany company, double marketBenchmark, long targetPaidUsers) {
        double relativeTechnology = company.getPrototypeBenchmark() / Math.max(1.0, marketBenchmark);
        CompanyGrowthStage growthStage = company.getGrowthStage();
        double targetPro = 0.0;
        double targetMax = 0.0;
        if (growthStage.isProPlanAvailable()
                && company.getProductCompleteness() >= 40 && relativeTechnology >= 0.9) {
            targetPro = 0.10;
        }
        if (growthStage.isMaxPlanAvailable()
                && company.getProductCompleteness() >= 70 && company.getProductStability() >= 75
                && company.getProductSecurity() >= 70 && relativeTechnology >= 1.0) {
            targetPro = 0.11;
            targetMax = 0.01;
        }
        long currentTotal = Math.max(1, company.getPaidUsers());
        double currentPro = growthStage.isProPlanAvailable()
                ? company.getProSubscribers() / (double) currentTotal : 0.0;
        double currentMax = growthStage.isMaxPlanAvailable()
                ? company.getMaxSubscribers() / (double) currentTotal : 0.0;
        double movementSpeed = 0.20
                * externalEventService.planMixSpeedMultiplier(company, company.getMarketMonthsProcessed() + 1);
        double proRatio = currentPro + (targetPro - currentPro) * movementSpeed;
        double maxRatio = currentMax + (targetMax - currentMax) * movementSpeed;
        long max = Math.round(targetPaidUsers * maxRatio);
        long pro = Math.round(targetPaidUsers * proRatio);
        long normal = Math.max(0, targetPaidUsers - pro - max);
        return new PlanMix(normal, pro, max);
    }

    private double marketBenchmark(PlayerCompany company, List<CompanyCompetitor> competitors) {
        double weighted = company.getPrototypeBenchmark() * company.getMarketShare();
        for (CompanyCompetitor competitor : competitors) {
            weighted += competitor.getBenchmark() * competitor.getMarketShare();
        }
        return weighted / 100.0;
    }

    private double playerCompetitiveness(PlayerCompany company, double marketBenchmark) {
        return relativeTechnologyScore(company.getPrototypeBenchmark(), marketBenchmark) * 0.35
                + company.getProductCompleteness() * 0.25
                + company.getProductStability() * 0.15
                + company.getMarketingEffect() * 0.15
                + company.getBrandScore() * 0.10;
    }

    private double competitorCompetitiveness(
            CompanyCompetitor company,
            double marketBenchmark,
            int processingMarketMonth
    ) {
        return relativeTechnologyScore(company.getBenchmark(), marketBenchmark) * 0.35
                + company.getCompleteness() * 0.25
                + company.getStability() * 0.15
                + company.getBrand() * 0.15
                + company.getMarketing() * 0.10
                + externalEventService.competitorScoreModifier(
                        company.getCompany(), company.getCompetitorKey(), processingMarketMonth);
    }

    private double relativeTechnologyScore(int benchmark, double marketBenchmark) {
        return clamp(50 + 125 * (benchmark / Math.max(1.0, marketBenchmark) - 1), 0, 100);
    }

    private double marketGrowthRate(long users, double marketBenchmark) {
        double baseRate = interpolate(users,
                new long[]{2_000_000L, 20_000_000L, 150_000_000L, 600_000_000L, 800_000_000L},
                new double[]{0.05, 0.08, 0.04, 0.01, 0.002});
        double expectedBenchmark = interpolate(users,
                new long[]{2_000_000L, 20_000_000L, 150_000_000L, 600_000_000L, 800_000_000L},
                new double[]{200, 500, 1_200, 2_200, 2_500});
        double technologyModifier = clamp(0.7 + 0.3 * marketBenchmark / expectedBenchmark, 0.85, 1.15);
        return baseRate * technologyModifier;
    }

    private double interpolate(long value, long[] points, double[] values) {
        if (value <= points[0]) return values[0];
        for (int index = 1; index < points.length; index++) {
            if (value <= points[index]) {
                double ratio = (value - points[index - 1]) / (double) (points[index] - points[index - 1]);
                return values[index - 1] + (values[index] - values[index - 1]) * ratio;
            }
        }
        return values[values.length - 1];
    }

    private double moveShare(double current, double target, double speed) {
        return current + clamp((target - current) * speed, -1.0, 1.0);
    }

    private double cube(double value) { return value * value * value; }
    private double clamp(double value, double minimum, double maximum) { return Math.max(minimum, Math.min(maximum, value)); }

    private long subscriptionRevenue(long normal, long pro, long max) {
        return Math.addExact(Math.multiplyExact(normal, NORMAL_PRICE),
                Math.addExact(Math.multiplyExact(pro, PRO_PRICE), Math.multiplyExact(max, MAX_PRICE)));
    }

    private record PlanMix(long normal, long pro, long max) {
    }

    public record MarketSnapshot(long totalMarketUsers, double playerMarketShare, double marketBenchmark,
                                 int benchmarkRank, long normalSubscribers, long proSubscribers,
                                 long maxSubscribers, long paidUsers, long monthlyRecurringRevenue,
                                 List<CompetitorSnapshot> competitors) {
    }

    public record CompetitorSnapshot(String name, String strategy, int benchmark, int completeness,
                                     int stability, int security, double marketShare) {
    }
}
