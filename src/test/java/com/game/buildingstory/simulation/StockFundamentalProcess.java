package com.game.buildingstory.simulation;

import com.game.buildingstory.service.ListedCompanyFinancialCatalog;
import com.game.buildingstory.service.ListedCompanyFinancialProfile;
import com.game.buildingstory.service.ListedCompanyValuationCatalog;
import com.game.buildingstory.service.ListedCompanyValuationRule;
import com.game.buildingstory.service.StockCompanyFinancialEffect;
import com.game.buildingstory.service.StockMarketRegime;
import com.game.buildingstory.service.StockSpec;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/** DB 없이 운영 분기 결산과 TTM 적정가 계산 순서를 재현한다. */
final class StockFundamentalProcess {
    private static final int UPDATES_PER_QUARTER = 18;
    private static final int QUARTERLY_INTEREST_BASIS_POINTS = 100;
    private static final int CORPORATE_TAX_BASIS_POINTS = 2_200;

    private final StockSpec stock;
    private final ListedCompanyFinancialProfile profile;
    private final ListedCompanyValuationRule valuationRule;
    private final Random random;
    private final Deque<QuarterResult> recentQuarters = new ArrayDeque<>();
    private long revenue;
    private long expectedRevenue;
    private long expectedNetIncome;
    private long cash;
    private long nonCashAssets;
    private long debt;
    private long otherLiabilities;
    private long netAssets;
    private long fairValue;
    private final long initialFairValue;
    private int pendingIndustryRevenueBasisPoints;
    private int pendingCompanyRevenueBasisPoints;
    private int pendingCompanyExpenseBasisPoints;
    private int pendingEarningsBasisPoints;
    private int expansionUpdates;
    private int neutralUpdates;
    private int recessionUpdates;

    StockFundamentalProcess(
            StockSpec stock,
            ListedCompanyFinancialCatalog financialCatalog,
            ListedCompanyValuationCatalog valuationCatalog,
            long seed
    ) {
        this.stock = stock;
        this.profile = financialCatalog.require(stock.key());
        this.valuationRule = valuationCatalog.require(stock.industry());
        this.random = new Random(seed ^ stock.key().hashCode());
        this.revenue = profile.quarterlyRevenue();
        this.expectedRevenue = revenue;
        this.cash = profile.cash();
        this.nonCashAssets = profile.nonCashAssets();
        this.debt = profile.debt();
        this.otherLiabilities = profile.otherLiabilities();
        this.netAssets = cash + nonCashAssets - debt - otherLiabilities;
        this.expectedNetIncome = expectedNetIncome(expectedRevenue, debt, 0);
        long baselineNetIncome = expectedNetIncome;
        long baselineOperatingProfit = operatingProfit(revenue, 0);
        for (int quarter = 0; quarter < 4; quarter++) {
            recentQuarters.addLast(new QuarterResult(revenue, baselineOperatingProfit, baselineNetIncome));
        }
        this.fairValue = calculateFairValue();
        this.initialFairValue = fairValue;
    }

    void recordRegime(StockMarketRegime regime) {
        switch (regime) {
            case EXPANSION -> expansionUpdates++;
            case NEUTRAL -> neutralUpdates++;
            case RECESSION -> recessionUpdates++;
        }
    }

    void applyIndustryFinancialImpact(int signedRevenueBasisPoints) {
        pendingIndustryRevenueBasisPoints = clamp(
                pendingIndustryRevenueBasisPoints + signedRevenueBasisPoints, -2_000, 2_000
        );
        expectedRevenue = Math.max(1, scale(expectedRevenue, 10_000 + signedRevenueBasisPoints));
        expectedNetIncome = expectedNetIncome(expectedRevenue, debt, 0);
    }

    void applyCompanyFinancialImpact(StockCompanyFinancialEffect effect) {
        pendingCompanyRevenueBasisPoints = clamp(
                pendingCompanyRevenueBasisPoints + effect.revenueImpactBasisPoints(), -2_500, 2_500
        );
        pendingCompanyExpenseBasisPoints = clamp(
                pendingCompanyExpenseBasisPoints + effect.operatingExpenseImpactBasisPoints(), -2_000, 2_000
        );
        long addedDebt = scale(revenue, Math.max(0, effect.debtFinancingBasisPoints()));
        cash += addedDebt;
        debt += addedDebt;
        long immediateResult = scale(revenue, effect.immediateCashImpactBasisPoints());
        if (immediateResult >= 0) {
            cash += immediateResult;
            netAssets += immediateResult;
        } else {
            long loss = -immediateResult;
            if (cash >= loss) {
                cash -= loss;
            } else {
                debt += loss - cash;
                cash = 0;
            }
            netAssets -= loss;
        }
        expectedRevenue = Math.max(1, scale(expectedRevenue, 10_000 + effect.revenueImpactBasisPoints()));
        expectedNetIncome = expectedNetIncome(expectedRevenue, debt, effect.operatingExpenseImpactBasisPoints());
    }

    void settleIfDue(int update) {
        if (update % UPDATES_PER_QUARTER != 0) {
            return;
        }
        RegimeImpact regimeImpact = regimeImpact();
        int noise = random.nextInt(-profile.quarterlyNoiseBasisPoints(), profile.quarterlyNoiseBasisPoints() + 1);
        int growthBasisPoints = Math.max(-9_000,
                profile.annualGrowthBasisPoints() / 4 + noise
                        + pendingIndustryRevenueBasisPoints + pendingCompanyRevenueBasisPoints
                        + regimeImpact.revenueBasisPoints());
        long nextRevenue = Math.max(1, scale(revenue, 10_000 + growthBasisPoints));
        long operatingProfit = operatingProfit(
                nextRevenue,
                pendingCompanyExpenseBasisPoints + regimeImpact.expenseBasisPoints()
        );
        long interestExpense = scale(debt, QUARTERLY_INTEREST_BASIS_POINTS);
        long pretaxIncome = operatingProfit - interestExpense;
        long taxExpense = pretaxIncome > 0 ? scale(pretaxIncome, CORPORATE_TAX_BASIS_POINTS) : 0;
        long netIncome = pretaxIncome - taxExpense;
        long depreciation = scale(nonCashAssets, profile.annualDepreciationBasisPoints() / 4);
        long capitalExpenditure = scale(nextRevenue, profile.capitalExpenditureBasisPoints());
        cash += netIncome + depreciation - capitalExpenditure;
        if (cash < 0) {
            debt += -cash;
            cash = 0;
        }
        nonCashAssets += capitalExpenditure - depreciation;
        netAssets += netIncome;

        pendingEarningsBasisPoints = ratioBasisPoints(netIncome - expectedNetIncome, Math.abs(expectedNetIncome));
        recentQuarters.addFirst(new QuarterResult(nextRevenue, operatingProfit, netIncome));
        while (recentQuarters.size() > 4) {
            recentQuarters.removeLast();
        }
        revenue = nextRevenue;
        expectedRevenue = scale(revenue, 10_000 + profile.annualGrowthBasisPoints() / 4);
        expectedNetIncome = expectedNetIncome(expectedRevenue, debt, 0);
        fairValue = calculateFairValue();
        pendingIndustryRevenueBasisPoints = 0;
        pendingCompanyRevenueBasisPoints = 0;
        pendingCompanyExpenseBasisPoints = 0;
        expansionUpdates = 0;
        neutralUpdates = 0;
        recessionUpdates = 0;
    }

    int consumeEarningsBasisPoints() {
        int value = pendingEarningsBasisPoints;
        pendingEarningsBasisPoints = 0;
        return value;
    }

    long fairValue() {
        return fairValue;
    }

    long initialFairValue() {
        return initialFairValue;
    }

    String industry() {
        return stock.industry();
    }

    private long operatingProfit(long targetRevenue, int expenseImpactBasisPoints) {
        long grossProfit = scale(targetRevenue, profile.grossMarginBasisPoints());
        long operatingExpenses = scale(
                targetRevenue,
                Math.max(0, profile.operatingExpenseBasisPoints() + expenseImpactBasisPoints)
        );
        return grossProfit - operatingExpenses;
    }

    private long expectedNetIncome(long targetRevenue, long targetDebt, int expenseImpactBasisPoints) {
        long pretaxIncome = operatingProfit(targetRevenue, expenseImpactBasisPoints)
                - scale(targetDebt, QUARTERLY_INTEREST_BASIS_POINTS);
        long tax = pretaxIncome > 0 ? scale(pretaxIncome, CORPORATE_TAX_BASIS_POINTS) : 0;
        return pretaxIncome - tax;
    }

    private RegimeImpact regimeImpact() {
        int total = expansionUpdates + neutralUpdates + recessionUpdates;
        if (total == 0) {
            return RegimeImpact.NONE;
        }
        int[] sensitivity = switch (stock.industry()) {
            case "IT" -> new int[]{140, -20, -180, 30};
            case "식품" -> new int[]{60, -10, -70, 15};
            case "유통" -> new int[]{110, -15, -150, 30};
            case "제조" -> new int[]{180, -25, -240, 50};
            case "통신" -> new int[]{50, -10, -60, 15};
            default -> new int[]{0, 0, 0, 0};
        };
        int revenueImpact = (int) Math.round(
                (sensitivity[0] * expansionUpdates + sensitivity[2] * recessionUpdates) / (double) total
        );
        int expenseImpact = (int) Math.round(
                (sensitivity[1] * expansionUpdates + sensitivity[3] * recessionUpdates) / (double) total
        );
        return new RegimeImpact(revenueImpact, expenseImpact);
    }

    private long calculateFairValue() {
        long trailingRevenue = recentQuarters.stream().mapToLong(QuarterResult::revenue).sum();
        long trailingNetIncome = recentQuarters.stream().mapToLong(QuarterResult::netIncome).sum();
        long earningsPerShare = trailingNetIncome / stock.issuedShares();
        long bookValuePerShare = Math.max(0, netAssets / stock.issuedShares());
        long salesPerShare = trailingRevenue / stock.issuedShares();
        int earningsWeight = valuationRule.earningsWeightBasisPoints();
        int bookWeight = valuationRule.bookWeightBasisPoints();
        int salesWeight = valuationRule.salesWeightBasisPoints();
        if (earningsPerShare <= 0) {
            bookWeight += earningsWeight * 4 / 10;
            salesWeight += earningsWeight - earningsWeight * 4 / 10;
            earningsWeight = 0;
        }
        long earningsValue = earningsPerShare > 0
                ? multiplyDivide(earningsPerShare, valuationRule.perHundredths(), 100) : 0;
        long bookValue = multiplyDivide(bookValuePerShare, valuationRule.pbrHundredths(), 100);
        long salesValue = multiplyDivide(salesPerShare, valuationRule.psrHundredths(), 100);
        long weightedValue = multiplyDivide(earningsValue, earningsWeight, 10_000)
                + multiplyDivide(bookValue, bookWeight, 10_000)
                + multiplyDivide(salesValue, salesWeight, 10_000);
        long assets = Math.max(1, cash + nonCashAssets);
        int debtDiscount = (int) Math.min(2_000, multiplyDivide(debt, 1_500, assets));
        return Math.max(1, multiplyDivide(weightedValue, 10_000 - debtDiscount, 10_000));
    }

    private static long scale(long amount, int basisPoints) {
        return BigInteger.valueOf(amount).multiply(BigInteger.valueOf(basisPoints))
                .divide(BigInteger.valueOf(10_000)).longValueExact();
    }

    private static long multiplyDivide(long value, long multiplier, long divisor) {
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier))
                .divide(BigInteger.valueOf(divisor)).longValueExact();
    }

    private static int ratioBasisPoints(long numerator, long denominator) {
        if (denominator == 0) {
            return numerator == 0 ? 0 : numerator > 0 ? 10_000 : -10_000;
        }
        return BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(10_000))
                .divide(BigInteger.valueOf(denominator)).intValue();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record QuarterResult(long revenue, long operatingProfit, long netIncome) {
    }

    private record RegimeImpact(int revenueBasisPoints, int expenseBasisPoints) {
        private static final RegimeImpact NONE = new RegimeImpact(0, 0);
    }
}
