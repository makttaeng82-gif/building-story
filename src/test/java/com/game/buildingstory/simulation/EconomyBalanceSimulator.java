package com.game.buildingstory.simulation;

import com.game.buildingstory.domain.EconomyBalanceRules;
import com.game.buildingstory.service.BuildingSpec;
import com.game.buildingstory.service.SecretarySpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 통합 경제 재설계 수치를 고정 시드로 반복 검증하는 순수 자바 시뮬레이터다.
 *
 * <p>Spring이나 DB를 사용하지 않아 1만 회 반복을 빠르게 실행할 수 있다. 가격, 월세,
 * 쿨타임과 비서 급여는 실제 카탈로그에서 받으며 사용자 클릭과 이벤트 선택은 문서화된
 * 월 단위 전략으로 단순화한다.</p>
 */
public final class EconomyBalanceSimulator {
    public static final int DEFAULT_ITERATIONS = 10_000;
    public static final int MAX_MONTHS = 240;
    public static final long DEFAULT_SEED = 20260718L;

    private static final List<String> CITIES = List.of("청주", "세종", "대전", "부산", "인천", "서울");
    private static final Map<String, Integer> CITY_REPUTATION = Map.of(
            "청주", 0, "세종", 375, "대전", 1_725, "부산", 4_500, "인천", 10_125, "서울", 19_500
    );
    private static final long[] DONATION_THRESHOLDS = {
            1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L
    };
    private static final long[] SECRETARY_REQUEST_COSTS = {0L, 100_000_000L, 50_000_000L, 3_000_000_000L, 12_000_000_000L, 35_450_300_000L};
    private static final int[] SECRETARY_REWARDS = {100, 300, 600, 1_000, 1_500, 2_000};

    private final List<BuildingSpec> buildings;
    private final List<SecretarySpec> secretaries;

    public EconomyBalanceSimulator(List<BuildingSpec> buildings, List<SecretarySpec> secretaries) {
        this.buildings = List.copyOf(buildings);
        this.secretaries = List.copyOf(secretaries);
    }

    public SimulationReport simulate() {
        return simulate(DEFAULT_ITERATIONS, DEFAULT_SEED);
    }

    public SimulationReport simulate(int iterations, long seed) {
        EnumMap<Strategy, List<RunResult>> results = new EnumMap<>(Strategy.class);
        for (Strategy strategy : Strategy.values()) {
            List<RunResult> strategyResults = new ArrayList<>(iterations);
            for (int run = 0; run < iterations; run++) {
                long runSeed = seed + strategy.ordinal() * 1_000_003L + run * 97L;
                strategyResults.add(run(strategy, new Random(runSeed)));
            }
            results.put(strategy, strategyResults);
        }
        return new SimulationReport(iterations, seed, summarize(results), salaryAudit());
    }

    private RunResult run(Strategy strategy, Random random) {
        State state = new State();
        BuildingSpec starter = buildings.stream()
                .filter(spec -> spec.city().equals("청주") && spec.slot() == 1)
                .findFirst()
                .orElseThrow();
        state.buildings.add(new SimBuilding(starter, 0L, 0L, 0L, true, true, 0));
        state.claimedMilestones.put(key(starter), true);
        state.cityUnlockMonth.put("청주", 0);

        for (int month = 1; month <= MAX_MONTHS; month++) {
            state.month = month;
            state.employed = month <= 12;
            state.cashShortageThisMonth = false;
            updateMarketIndexes(state, random);
            updateTenants(state, random);
            collectMonthlyIncome(state, random);
            processRepairs(state, random);
            processSecretarySalaries(state);
            processLoanInterest(state);
            if (state.cashShortageThisMonth) {
                state.cashShortageMonths++;
            }
            updateStocks(state, strategy, random);
            applyDonationMilestone(state, strategy);

            if (strategy == Strategy.ACTIVE_TRADING && month % 3 == 0) {
                sellTradingBuilding(state, random);
            }
            boolean auctionPurchased = strategy == Strategy.ACTIVE_TRADING && tryAuctionPurchase(state, random);
            if (!auctionPurchased) {
                buyBuilding(state, strategy, random);
            }
            hireEligibleSecretaries(state);
            recordUnlocks(state);
            transferToSecurities(state, strategy);
            recordFinancialUnlocks(state);
            recordWealth(state);
        }
        return state.toResult();
    }

    private void updateMarketIndexes(State state, Random random) {
        for (String city : CITIES) {
            double change = -0.006 + random.nextDouble() * 0.012;
            if (random.nextInt(100) < 15) {
                change += random.nextBoolean() ? 0.015 : -0.015;
            }
            double current = state.cityIndex.getOrDefault(city, 1.0);
            state.cityIndex.put(city, clamp(current * (1.0 + change), 0.7, 1.6));
        }
    }

    private void updateTenants(State state, Random random) {
        for (SimBuilding building : state.buildings) {
            building.ageMonths++;
            if (!building.occupied) {
                if (rollAtLeastOnce(random, 35, 2)) {
                    building.occupied = true;
                    building.occupiedMonths = 0;
                }
                continue;
            }
            building.occupiedMonths++;
            if (!building.protectedTenant && building.occupiedMonths >= 2 && rollAtLeastOnce(random, 18, 2)) {
                building.occupied = false;
                building.occupiedMonths = 0;
            }
        }
    }

    private void collectMonthlyIncome(State state, Random random) {
        if (state.employed) {
            state.cash += 3_000_000L;
            state.salaryIncome += 3_000_000L;
        }
        for (SimBuilding building : state.buildings) {
            if (!building.occupied) {
                continue;
            }
            long grossRent = building.spec.monthlyRent();
            long netRent = grossRent - EconomyBalanceRules.rentOperatingCost(grossRent);
            state.cash += netRent;
            state.rentIncome += netRent;
            state.reputation += random.nextInt(3) + 2;
        }
    }

    private void processRepairs(State state, Random random) {
        boolean shortage = false;
        for (SimBuilding building : state.buildings) {
            if (!building.occupied || !rollAtLeastOnce(random, 10, 2)) {
                continue;
            }
            long repairCost = currentValue(state, building) / 1_000;
            if (state.cash >= repairCost) {
                state.cash -= repairCost;
                state.reputation += 5;
            } else {
                shortage = true;
            }
        }
        state.cashShortageThisMonth |= shortage;
    }

    private void processSecretarySalaries(State state) {
        boolean shortage = false;
        for (int index = state.hiredSecretaries.size() - 1; index >= 0; index--) {
            SimSecretary secretary = state.hiredSecretaries.get(index);
            long salary = secretary.spec.monthlySalaryForProficiency(secretary.proficiency);
            if (state.cash >= salary) {
                state.cash -= salary;
                state.secretarySalaryExpense += salary;
                secretary.unpaidMonths = 0;
                continue;
            }
            shortage = true;
            secretary.unpaidMonths++;
            if (secretary.unpaidMonths >= 2) {
                state.hiredSecretaries.remove(index);
            }
        }
        state.cashShortageThisMonth |= shortage;
    }

    private void processLoanInterest(State state) {
        boolean shortage = false;
        for (int index = state.loans.size() - 1; index >= 0; index--) {
            SimLoan loan = state.loans.get(index);
            long interest = divideRoundUp(loan.principal, 250L);
            if (state.cash >= interest) {
                state.cash -= interest;
                loan.delinquentMonths = 0;
                continue;
            }
            shortage = true;
            loan.delinquentMonths++;
            if (loan.delinquentMonths < 2) {
                continue;
            }
            long forcedSale = currentValue(state, loan.building) * 90 / 100;
            long supportClawback = loan.building.ageMonths < 13 ? loan.building.governmentSupportAmount : 0L;
            state.cash += Math.max(0L, forcedSale - loan.principal - supportClawback);
            state.buildings.remove(loan.building);
            state.loans.remove(index);
            state.foreclosures++;
            state.lastForeclosureMonth = state.month;
        }
        state.cashShortageThisMonth |= shortage;
    }

    private void updateStocks(State state, Strategy strategy, Random random) {
        if (state.stockUnlockMonth == 0 || state.stockValue == 0) {
            return;
        }
        double expectedReturn = switch (strategy) {
            case STABLE_NO_LOAN -> 0.003;
            case LOAN_GROWTH -> 0.004;
            case ACTIVE_TRADING -> 0.005;
        };
        double monthlyReturn = clamp(expectedReturn + random.nextGaussian() * 0.035, -0.15, 0.15);
        long change = Math.round(state.stockValue * monthlyReturn);
        state.stockValue = Math.max(0L, state.stockValue + change);
        state.stockProfit += change;
    }

    private void applyDonationMilestone(State state, Strategy strategy) {
        if (state.nextDonationMilestone >= DONATION_THRESHOLDS.length) {
            return;
        }
        long target = DONATION_THRESHOLDS[state.nextDonationMilestone];
        long additionalDonation = target - state.cumulativeDonation;
        long reserve = reserveCash(state, strategy);
        // 누적 기부 이정표가 보이자마자 성장 자금을 전부 소진하는 행동은 일반적인 플레이가 아니다.
        // 기부액이 순자산의 10% 이하일 때만 집행해 부동산 투자와 기부를 함께 하는 전략을 모델링한다.
        if (netWorth(state) < target * 10 || state.cash - additionalDonation < reserve) {
            return;
        }
        state.cash -= additionalDonation;
        state.cumulativeDonation = target;
        state.reputation += Math.toIntExact(additionalDonation / 300_000L);
        state.nextDonationMilestone++;
    }

    private void buyBuilding(State state, Strategy strategy, Random random) {
        List<BuildingSpec> candidates = buildings.stream()
                .filter(spec -> isUnlocked(state, spec))
                .filter(spec -> cityBuildingCount(state, spec.city()) < 8)
                .filter(spec -> state.month >= state.nextPurchaseMonth.getOrDefault(key(spec), 0))
                .sorted(purchasePriority(state))
                .toList();
        for (BuildingSpec spec : candidates) {
            int valuationRate = randomValuationRate(random);
            long originalOfferPrice = spec.marketPrice() * valuationRate / 100;
            long purchasePrice = effectivePurchasePrice(state, spec, originalOfferPrice);
            long loan = strategy == Strategy.STABLE_NO_LOAN ? 0L : loanAmount(spec, purchasePrice);
            long cashCost = purchasePrice - loan + EconomyBalanceRules.purchaseFee(purchasePrice);
            if (state.cash - cashCost < reserveCash(state, strategy)) {
                continue;
            }
            purchase(state, spec, originalOfferPrice, purchasePrice, loan, cashCost);
            return;
        }
    }

    private boolean tryAuctionPurchase(State state, Random random) {
        if (random.nextDouble() >= 1.0 - Math.pow(0.98, 30)) {
            return false;
        }
        List<BuildingSpec> candidates = buildings.stream()
                .filter(spec -> isUnlocked(state, spec))
                .filter(spec -> cityBuildingCount(state, spec.city()) < 8)
                .sorted(purchasePriority(state))
                .toList();
        for (BuildingSpec spec : candidates) {
            long bidPrice = spec.marketPrice() * 88 / 100;
            long purchasePrice = effectivePurchasePrice(state, spec, bidPrice);
            long loan = loanAmount(spec, purchasePrice);
            long cashCost = purchasePrice - loan + EconomyBalanceRules.purchaseFee(purchasePrice);
            long deposit = EconomyBalanceRules.auctionDeposit(bidPrice);
            if (state.cash - Math.max(cashCost, deposit) < reserveCash(state, Strategy.ACTIVE_TRADING)) {
                continue;
            }
            if (random.nextInt(100) >= 35) {
                state.cash -= deposit;
                state.tradeProfit -= deposit;
                return false;
            }
            purchase(state, spec, bidPrice, purchasePrice, loan, cashCost);
            return true;
        }
        return false;
    }

    private void purchase(State state, BuildingSpec spec, long originalPrice, long purchasePrice, long loan, long cashCost) {
        state.cash -= cashCost;
        long governmentSupportAmount = originalPrice - purchasePrice;
        SimBuilding building = new SimBuilding(spec, purchasePrice, cashCost, governmentSupportAmount, false, false, 0);
        state.buildings.add(building);
        if (governmentSupportAvailable(state, spec)) {
            state.governmentSupportedCities.put(spec.city(), true);
        }
        if (loan > 0) {
            state.loans.add(new SimLoan(building, loan));
        }
        state.nextPurchaseMonth.put(key(spec), state.month + Math.max(1, (int) Math.ceil(spec.tradeCooldownDays() / 30.0)));
        if (!state.claimedMilestones.containsKey(key(spec))) {
            state.claimedMilestones.put(key(spec), true);
            state.reputation += EconomyBalanceRules.buildingMilestoneReputation(spec.city(), spec.slot());
        }
    }

    private long effectivePurchasePrice(State state, BuildingSpec spec, long originalPrice) {
        return governmentSupportAvailable(state, spec)
                ? EconomyBalanceRules.governmentSupportedPrice(originalPrice, spec.city())
                : originalPrice;
    }

    private boolean governmentSupportAvailable(State state, BuildingSpec spec) {
        return EconomyBalanceRules.governmentPurchaseSupportPercent(spec.city()) > 0
                && !state.governmentSupportedCities.containsKey(spec.city());
    }

    private void sellTradingBuilding(State state, Random random) {
        SimBuilding target = state.buildings.stream()
                .filter(building -> !building.protectedTenant)
                .filter(building -> building.purchasePrice > 0)
                .filter(building -> building.ageMonths * 30 >= building.spec.tradeCooldownDays())
                .min(Comparator.comparingDouble(building -> building.spec.monthlyRent() / (double) building.spec.marketPrice()))
                .orElse(null);
        if (target == null) {
            return;
        }
        long sellPrice = currentValue(state, target) * randomValuationRate(random) / 100;
        long proceeds = sellPrice - EconomyBalanceRules.sellFee(sellPrice);
        SimLoan loan = loanFor(state, target);
        long debt = loan == null ? 0L : loan.principal;
        long supportClawback = target.ageMonths < 13 ? target.governmentSupportAmount : 0L;
        if (proceeds < debt + supportClawback) {
            return;
        }
        state.cash += proceeds - debt - supportClawback;
        state.tradeProfit += proceeds - debt - supportClawback - target.cashInvested;
        state.buildings.remove(target);
        if (loan != null) {
            state.loans.remove(loan);
        }
    }

    private void hireEligibleSecretaries(State state) {
        for (int index = 0; index < secretaries.size(); index++) {
            SecretarySpec spec = secretaries.get(index);
            if (state.hiredSecretaryKeys.containsKey(spec.key()) || !ownsSecretaryTriggerBuilding(state, index)) {
                continue;
            }
            long requestCost = SECRETARY_REQUEST_COSTS[index];
            if (!secretaryConditionMet(state, index) || state.cash < requestCost) {
                continue;
            }
            state.cash -= requestCost;
            state.hiredSecretaryKeys.put(spec.key(), true);
            state.hiredSecretaries.add(new SimSecretary(spec, spec.baseProficiency()));
            state.reputation += SECRETARY_REWARDS[index];
        }
    }

    private boolean secretaryConditionMet(State state, int index) {
        return switch (index) {
            case 0 -> state.cash >= 100_000_000L;
            case 1 -> state.reputation >= 1_275 && state.cash >= 300_000_000L;
            case 2 -> state.reputation >= 3_600;
            case 3 -> state.reputation >= 8_250 && state.cash >= 8_000_000_000L;
            case 4 -> state.reputation >= 12_000 && state.cash >= 30_000_000_000L && state.loans.isEmpty();
            case 5 -> state.cash >= 90_000_000_000L && ownsBuilding(state, "서울", 4);
            default -> false;
        };
    }

    private boolean ownsSecretaryTriggerBuilding(State state, int index) {
        String city = CITIES.get(index);
        int slot = index == 0 ? 1 : index == 5 ? 1 : 2;
        return ownsBuilding(state, city, slot);
    }

    private void recordUnlocks(State state) {
        for (String city : CITIES) {
            if (!state.cityUnlockMonth.containsKey(city)
                    && state.reputation >= CITY_REPUTATION.get(city)
                    && (!city.equals("세종") || !state.employed)) {
                state.cityUnlockMonth.put(city, state.month);
            }
        }
    }

    private void transferToSecurities(State state, Strategy strategy) {
        if (state.stockUnlockMonth == 0) {
            return;
        }
        long reserve = Math.max(10_000_000_000L, reserveCash(state, strategy));
        long excess = state.cash - reserve;
        if (excess <= 0) {
            return;
        }
        int percent = switch (strategy) {
            case STABLE_NO_LOAN -> 20;
            case LOAN_GROWTH -> 30;
            case ACTIVE_TRADING -> 40;
        };
        long transfer = excess * percent / 100;
        state.cash -= transfer;
        state.stockValue += transfer;
    }

    private void recordFinancialUnlocks(State state) {
        long netWorth = netWorth(state);
        if (state.stockUnlockMonth == 0 && state.reputation >= 8_250 && netWorth >= 3_000_000_000L) {
            state.stockUnlockMonth = state.month;
        }
        if (state.companyMonth == 0
                && state.stockUnlockMonth > 0
                && state.month - state.stockUnlockMonth >= 12
                && state.reputation >= 37_500
                && state.stockValue >= 50_000_000_000L
                && state.cash >= 10_000_000_000L
                && state.month - state.lastForeclosureMonth > 6) {
            state.companyMonth = state.month;
        }
    }

    private void recordWealth(State state) {
        long netWorth = netWorth(state);
        state.currentNetWorth = netWorth;
        state.maxNetWorth = Math.max(state.maxNetWorth, netWorth);
        long maxBuilding = state.buildings.stream().mapToLong(building -> currentValue(state, building)).max().orElse(0L);
        if (netWorth > 0) {
            state.maxSingleBuildingRatio = Math.max(state.maxSingleBuildingRatio, maxBuilding / (double) netWorth);
        }
    }

    private Map<Strategy, StrategySummary> summarize(EnumMap<Strategy, List<RunResult>> results) {
        EnumMap<Strategy, StrategySummary> summaries = new EnumMap<>(Strategy.class);
        results.forEach((strategy, runs) -> summaries.put(strategy, StrategySummary.from(runs)));
        return summaries;
    }

    private List<SecretarySalaryAudit> salaryAudit() {
        List<SecretarySalaryAudit> audits = new ArrayList<>();
        for (int index = 0; index < secretaries.size(); index++) {
            SecretarySpec secretary = secretaries.get(index);
            String city = CITIES.get(index);
            long expectedCityNetRent = Math.round(buildings.stream()
                    .filter(building -> building.city().equals(city))
                    .mapToLong(BuildingSpec::monthlyRent)
                    .sum() * 0.75 * 0.90);
            long hireSalary = secretary.monthlySalaryForProficiency(secretary.baseProficiency());
            long maximumSalary = secretary.monthlySalaryForProficiency(30);
            audits.add(new SecretarySalaryAudit(
                    secretary.name(), city, hireSalary, maximumSalary, expectedCityNetRent,
                    ratio(hireSalary, expectedCityNetRent), ratio(maximumSalary, expectedCityNetRent)
            ));
        }
        return audits;
    }

    private Comparator<BuildingSpec> purchasePriority(State state) {
        return Comparator
                .comparing((BuildingSpec spec) -> state.claimedMilestones.containsKey(key(spec)))
                .thenComparing(Comparator.comparingLong(BuildingSpec::marketPrice).reversed());
    }

    private boolean isUnlocked(State state, BuildingSpec spec) {
        int required = requiredReputation(spec.city(), spec.slot());
        return state.reputation >= required && (!(spec.city().equals("세종") && spec.slot() >= 2) || !state.employed);
    }

    private int requiredReputation(String city, int slot) {
        int[][] required = {
                {0, 40, 90, 190}, {375, 600, 900, 1_275}, {1_725, 2_250, 2_850, 3_600},
                {4_500, 5_625, 6_750, 8_250}, {10_125, 12_000, 14_250, 16_875}, {19_500, 22_500, 26_250, 31_500}
        };
        return required[CITIES.indexOf(city)][slot - 1];
    }

    private long reserveCash(State state, Strategy strategy) {
        long monthlyInterest = state.loans.stream().mapToLong(loan -> divideRoundUp(loan.principal, 250L)).sum();
        long monthlySalary = state.hiredSecretaries.stream()
                .mapToLong(secretary -> secretary.spec.monthlySalaryForProficiency(secretary.proficiency))
                .sum();
        int months = switch (strategy) {
            case STABLE_NO_LOAN -> 6;
            case LOAN_GROWTH -> 3;
            case ACTIVE_TRADING -> 2;
        };
        return (monthlyInterest + monthlySalary) * months;
    }

    private long loanAmount(BuildingSpec spec, long offerPrice) {
        long loanToValue = Math.min(offerPrice, spec.marketPrice()) * 80 / 100;
        long expectedNetRent = spec.monthlyRent() * 75 / 100 * 90 / 100;
        long cashFlowLimit = expectedNetRent * 100 / 120 * 250;
        return Math.min(loanToValue, cashFlowLimit);
    }

    private long currentValue(State state, SimBuilding building) {
        return Math.round(building.spec.marketPrice() * state.cityIndex.getOrDefault(building.spec.city(), 1.0));
    }

    private long netWorth(State state) {
        long property = state.buildings.stream().mapToLong(building -> currentValue(state, building)).sum();
        long debt = state.loans.stream().mapToLong(loan -> loan.principal).sum();
        return state.cash + state.stockValue + property - debt;
    }

    private int cityBuildingCount(State state, String city) {
        return (int) state.buildings.stream().filter(building -> building.spec.city().equals(city)).count();
    }

    private boolean ownsBuilding(State state, String city, int slot) {
        return state.buildings.stream().anyMatch(building -> building.spec.city().equals(city) && building.spec.slot() == slot);
    }

    private SimLoan loanFor(State state, SimBuilding building) {
        return state.loans.stream().filter(loan -> loan.building == building).findFirst().orElse(null);
    }

    private int randomValuationRate(Random random) {
        int roll = random.nextInt(100);
        return roll < 25 ? 94 : roll < 75 ? 100 : 106;
    }

    private boolean rollAtLeastOnce(Random random, int percent, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (random.nextInt(100) < percent) {
                return true;
            }
        }
        return false;
    }

    private String key(BuildingSpec spec) {
        return spec.city() + ':' + spec.slot();
    }

    private long divideRoundUp(long value, long divisor) {
        return value / divisor + (value % divisor == 0 ? 0 : 1);
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : numerator / (double) denominator;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum Strategy {
        STABLE_NO_LOAN("무대출 안정형"),
        LOAN_GROWTH("담보대출 성장형"),
        ACTIVE_TRADING("매매·경매 적극형");

        private final String label;

        Strategy(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record SimulationReport(
            int iterations,
            long seed,
            Map<Strategy, StrategySummary> strategies,
            List<SecretarySalaryAudit> secretarySalaries
    ) {
        public String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# 경제 밸런스 시뮬레이션 결과\n\n")
                    .append("- 반복: 전략별 ").append(String.format(Locale.US, "%,d", iterations)).append("회\n")
                    .append("- 고정 시드: `").append(seed).append("`\n")
                    .append("- 최대 기간: ").append(MAX_MONTHS).append("게임월\n\n")
                    .append("## 모델 가정\n\n")
                    .append("- 가격·월세·쿨타임·거래비용·대출이자·비서 급여는 실제 카탈로그와 계산식을 사용한다.\n")
                    .append("- 첫 유상 취득 정부지원은 청주·세종·대전 40%, 부산 30%, 인천 20%, 서울 10%이며 1년 내 매각 시 전액 환수한다.\n")
                    .append("- 플레이어는 매월 최대 한 번 부동산 투자 결정을 한다. 부업과 선물 구매는 제외한다.\n")
                    .append("- 입주·퇴거·수리는 실제 월 2회 판정 확률을 사용하고, 도시지수는 월 변동 범위를 적용한다.\n")
                    .append("- 주식 해금 후 여유 현금의 20~40%를 투자하며 월 수익률은 평균 0.3~0.5%, 표준편차 3.5%로 가정한다.\n")
                    .append("- 비서는 조건 충족 시 고용하고 고용 시 숙련도 월급을 낸다. 선물·숙련도 성장·특수효과는 제외해 최대 월급은 별도 표로 검토한다.\n")
                    .append("- 기업 설립은 평판 37,500, 금융자산 500억원, 현금 100억원, 주식 해금 후 12개월 조건으로 판정한다.\n\n")
                    .append("## 전략 결과\n\n")
                    .append("| 전략 | 기업 설립 중앙값 | 미도달 | 최종 평판 | 최종 금융자산 | 최종 순자산 | 현금부족 월 | 강제매각률 | 매매수익 비중 | 단일건물 최대비중 |\n")
                    .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            strategies.forEach((strategy, summary) -> markdown.append('|').append(strategy.label())
                    .append('|').append(monthText(summary.companyMedianMonth))
                    .append('|').append(percent(summary.companyUnreachedRate))
                    .append('|').append(Math.round(summary.medianFinalReputation))
                    .append('|').append(won(Math.round(summary.medianFinalStockValue)))
                    .append('|').append(won(Math.round(summary.medianFinalNetWorth)))
                    .append('|').append(decimal(summary.medianCashShortageMonths))
                    .append('|').append(percent(summary.foreclosureRunRate))
                    .append('|').append(percent(summary.tradeIncomeShare))
                    .append('|').append(percent(summary.medianMaxSingleBuildingRatio)).append("|\n"));
            markdown.append("\n## 도시·주식 해금 시점\n\n")
                    .append("값은 `10백분위 / 중앙값 / 90백분위` 게임월이다. 241은 240개월 내 미도달을 뜻한다.\n\n")
                    .append("| 전략 | 청주 | 세종 | 대전 | 부산 | 인천 | 서울 | 주식 |\n")
                    .append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
            strategies.forEach((strategy, summary) -> {
                markdown.append('|').append(strategy.label());
                for (String city : CITIES) {
                    markdown.append('|').append(percentileText(summary.cityUnlockMonths.get(city)));
                }
                markdown.append('|').append(percentileText(summary.stockUnlockMonths)).append("|\n");
            });
            markdown.append("\n## 비서 월급\n\n")
                    .append("예상 도시 순월세는 도시 4개 건물을 모두 보유하고 평균 입주율 75%, 운영비 10%를 적용한 값이다.\n\n")
                    .append("| 비서 | 도시 | 고용 시 월급 | 숙련도 30 월급 | 예상 도시 순월세 | 고용 시 비중 | 최대 비중 |\n")
                    .append("|---|---|---:|---:|---:|---:|---:|\n");
            secretarySalaries.forEach(audit -> markdown.append('|').append(audit.name)
                    .append('|').append(audit.city)
                    .append('|').append(won(audit.hireSalary))
                    .append('|').append(won(audit.maximumSalary))
                    .append('|').append(won(audit.expectedCityNetRent))
                    .append('|').append(percent(audit.hireSalaryRatio))
                    .append('|').append(percent(audit.maximumSalaryRatio)).append("|\n"));
            return markdown.toString();
        }

        private String monthText(double month) {
            return month > MAX_MONTHS ? "240월 내 미도달" : decimal(month) + "개월";
        }

        private String percent(double value) {
            return String.format(Locale.US, "%.1f%%", value * 100.0);
        }

        private String decimal(double value) {
            return String.format(Locale.US, "%.1f", value);
        }

        private String won(long value) {
            return String.format(Locale.US, "%,d원", value);
        }

        private String percentileText(Percentiles value) {
            return Math.round(value.p10) + " / " + Math.round(value.median) + " / " + Math.round(value.p90);
        }
    }

    public record StrategySummary(
            double companyMedianMonth,
            double companyUnreachedRate,
            double medianCashShortageMonths,
            double foreclosureRunRate,
            double tradeIncomeShare,
            double medianMaxSingleBuildingRatio,
            double medianFinalReputation,
            double medianFinalStockValue,
            double medianFinalNetWorth,
            Map<String, Percentiles> cityUnlockMonths,
            Percentiles stockUnlockMonths
    ) {
        static StrategySummary from(List<RunResult> runs) {
            return new StrategySummary(
                    percentile(runs.stream().mapToInt(run -> normalizedMonth(run.companyMonth)).sorted().toArray(), 0.5),
                    runs.stream().filter(run -> run.companyMonth == 0).count() / (double) runs.size(),
                    percentile(runs.stream().mapToInt(RunResult::cashShortageMonths).sorted().toArray(), 0.5),
                    runs.stream().filter(run -> run.foreclosures > 0).count() / (double) runs.size(),
                    incomeShare(runs, true),
                    percentile(runs.stream().mapToInt(run -> (int) Math.round(run.maxSingleBuildingRatio * 10_000)).sorted().toArray(), 0.5) / 10_000.0,
                    percentile(runs.stream().mapToInt(RunResult::finalReputation).sorted().toArray(), 0.5),
                    percentileLong(runs.stream().mapToLong(RunResult::finalStockValue).sorted().toArray(), 0.5),
                    percentileLong(runs.stream().mapToLong(RunResult::finalNetWorth).sorted().toArray(), 0.5),
                    cityPercentiles(runs),
                    monthPercentiles(runs.stream().mapToInt(RunResult::stockUnlockMonth).toArray())
            );
        }

        private static Map<String, Percentiles> cityPercentiles(List<RunResult> runs) {
            Map<String, Percentiles> result = new LinkedHashMap<>();
            for (String city : CITIES) {
                int[] months = runs.stream().mapToInt(run -> run.cityUnlockMonth.getOrDefault(city, 0)).toArray();
                result.put(city, city.equals("청주") ? zeroBasedMonthPercentiles(months) : monthPercentiles(months));
            }
            return result;
        }

        private static Percentiles zeroBasedMonthPercentiles(int[] rawMonths) {
            int[] months = java.util.Arrays.stream(rawMonths).sorted().toArray();
            return new Percentiles(percentile(months, 0.1), percentile(months, 0.5), percentile(months, 0.9));
        }

        private static Percentiles monthPercentiles(int[] rawMonths) {
            int[] months = java.util.Arrays.stream(rawMonths).map(StrategySummary::normalizedMonth).sorted().toArray();
            return new Percentiles(percentile(months, 0.1), percentile(months, 0.5), percentile(months, 0.9));
        }

        private static int normalizedMonth(int month) {
            return month == 0 ? MAX_MONTHS + 1 : month;
        }

        private static double incomeShare(List<RunResult> runs, boolean trade) {
            long salary = runs.stream().mapToLong(RunResult::salaryIncome).sum();
            long rent = runs.stream().mapToLong(RunResult::rentIncome).sum();
            long trading = runs.stream().mapToLong(RunResult::tradeProfit).sum();
            long stocks = runs.stream().mapToLong(RunResult::stockProfit).sum();
            long positiveTotal = Math.max(1L, salary + rent + Math.max(0L, trading) + Math.max(0L, stocks));
            return (trade ? Math.max(0L, trading) : Math.max(0L, stocks)) / (double) positiveTotal;
        }

        private static double percentile(int[] sorted, double quantile) {
            if (sorted.length == 0) {
                return 0.0;
            }
            int index = (int) Math.ceil(quantile * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
        }

        private static double percentileLong(long[] sorted, double quantile) {
            if (sorted.length == 0) {
                return 0.0;
            }
            int index = (int) Math.ceil(quantile * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
        }
    }

    public record Percentiles(double p10, double median, double p90) {
    }

    public record SecretarySalaryAudit(
            String name,
            String city,
            long hireSalary,
            long maximumSalary,
            long expectedCityNetRent,
            double hireSalaryRatio,
            double maximumSalaryRatio
    ) {
    }

    private record RunResult(
            Map<String, Integer> cityUnlockMonth,
            int stockUnlockMonth,
            int companyMonth,
            int cashShortageMonths,
            int foreclosures,
            long maxNetWorth,
            double maxSingleBuildingRatio,
            long salaryIncome,
            long rentIncome,
            long tradeProfit,
            long stockProfit,
            long secretarySalaryExpense,
            int finalReputation,
            long finalStockValue,
            long finalNetWorth
    ) {
    }

    private static final class State {
        private int month;
        private long cash = 2_000_000L;
        private int reputation;
        private boolean employed = true;
        private final List<SimBuilding> buildings = new ArrayList<>();
        private final List<SimLoan> loans = new ArrayList<>();
        private final List<SimSecretary> hiredSecretaries = new ArrayList<>();
        private final Map<String, Boolean> hiredSecretaryKeys = new HashMap<>();
        private final Map<String, Boolean> claimedMilestones = new HashMap<>();
        private final Map<String, Boolean> governmentSupportedCities = new HashMap<>();
        private final Map<String, Integer> nextPurchaseMonth = new HashMap<>();
        private final Map<String, Double> cityIndex = new HashMap<>();
        private final Map<String, Integer> cityUnlockMonth = new LinkedHashMap<>();
        private long cumulativeDonation;
        private int nextDonationMilestone;
        private long stockValue;
        private int stockUnlockMonth;
        private int companyMonth;
        private int cashShortageMonths;
        private boolean cashShortageThisMonth;
        private int foreclosures;
        private int lastForeclosureMonth = -100;
        private long maxNetWorth;
        private long currentNetWorth;
        private double maxSingleBuildingRatio;
        private long salaryIncome;
        private long rentIncome;
        private long tradeProfit;
        private long stockProfit;
        private long secretarySalaryExpense;

        private RunResult toResult() {
            return new RunResult(Map.copyOf(cityUnlockMonth), stockUnlockMonth, companyMonth,
                    cashShortageMonths, foreclosures, maxNetWorth, maxSingleBuildingRatio,
                    salaryIncome, rentIncome, tradeProfit, stockProfit, secretarySalaryExpense,
                    reputation, stockValue, currentNetWorth);
        }
    }

    private static final class SimBuilding {
        private final BuildingSpec spec;
        private final long purchasePrice;
        private final long cashInvested;
        private final long governmentSupportAmount;
        private boolean occupied;
        private final boolean protectedTenant;
        private int occupiedMonths;
        private int ageMonths;

        private SimBuilding(BuildingSpec spec, long purchasePrice, long cashInvested, long governmentSupportAmount,
                            boolean occupied, boolean protectedTenant, int ageMonths) {
            this.spec = spec;
            this.purchasePrice = purchasePrice;
            this.cashInvested = cashInvested;
            this.governmentSupportAmount = governmentSupportAmount;
            this.occupied = occupied;
            this.protectedTenant = protectedTenant;
            this.ageMonths = ageMonths;
        }
    }

    private static final class SimLoan {
        private final SimBuilding building;
        private final long principal;
        private int delinquentMonths;

        private SimLoan(SimBuilding building, long principal) {
            this.building = building;
            this.principal = principal;
        }
    }

    private static final class SimSecretary {
        private final SecretarySpec spec;
        private final int proficiency;
        private int unpaidMonths;

        private SimSecretary(SecretarySpec spec, int proficiency) {
            this.spec = spec;
            this.proficiency = proficiency;
        }
    }
}
