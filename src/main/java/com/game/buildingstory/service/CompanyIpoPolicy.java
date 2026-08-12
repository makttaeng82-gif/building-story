package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyGrowthStage;

import java.math.BigInteger;
import java.util.Set;

/** IPO 조건과 공모 계산에서 사용하는 단일 정책 원천이다. */
public final class CompanyIpoPolicy {
    public static final String PLAYER_COMPANY_STOCK_KEY = "player-company";
    public static final CompanyGrowthStage MINIMUM_GROWTH_STAGE = CompanyGrowthStage.GROWTH;
    public static final int MINIMUM_QUARTERLY_REPORTS = 8;
    public static final int REQUIRED_PROFITABLE_QUARTERS = 2;
    public static final long MINIMUM_MONTHLY_RECURRING_REVENUE = 30_000_000_000L;
    public static final long MINIMUM_PAID_USERS = 1_000_000L;
    public static final int MINIMUM_BENCHMARK = 400;
    public static final long MINIMUM_EQUITY_VALUE = 3_000_000_000_000L;
    public static final int MINIMUM_STRATEGY_EXPERTISE = 60;
    public static final long PREPARATION_COST = 12_000_000_000L;
    public static final int PREPARATION_MONTHS = 6;
    public static final int STRATEGY_FINANCE_WORKLOAD = 20;
    public static final int MINIMUM_PLAYER_OWNERSHIP_PERCENT = 20;
    public static final int OFFER_DISCOUNT_BASIS_POINTS = 9_000;
    public static final Set<Integer> OFFER_PERCENT_OPTIONS = Set.of(15, 25, 35);

    private CompanyIpoPolicy() {
    }

    public static boolean isValidOfferPercent(int offerPercent) {
        return OFFER_PERCENT_OPTIONS.contains(offerPercent);
    }

    /** 목표 공모비율을 충족하도록 필요한 최소 신주 수를 올림 계산한다. */
    public static long calculateNewShares(long existingShares, int offerPercent) {
        if (existingShares <= 0 || !isValidOfferPercent(offerPercent)) {
            throw new IllegalArgumentException("발행주식과 공모비율이 올바르지 않습니다.");
        }
        BigInteger numerator = BigInteger.valueOf(existingShares)
                .multiply(BigInteger.valueOf(offerPercent));
        BigInteger denominator = BigInteger.valueOf(100L - offerPercent);
        return numerator.add(denominator).subtract(BigInteger.ONE)
                .divide(denominator)
                .longValueExact();
    }

    /** 최근 확정 지분가치에 10% 공모 할인을 적용하고 100원 단위로 반올림한다. */
    public static long calculateOfferPrice(long equityValue, long existingShares) {
        if (equityValue <= 0 || existingShares <= 0) {
            throw new IllegalArgumentException("지분가치와 발행주식은 양수여야 합니다.");
        }
        long discounted = BigInteger.valueOf(equityValue)
                .multiply(BigInteger.valueOf(OFFER_DISCOUNT_BASIS_POINTS))
                .divide(BigInteger.valueOf(10_000))
                .divide(BigInteger.valueOf(existingShares))
                .longValueExact();
        long rounded = discounted / 100 * 100;
        if (discounted % 100 >= 50) {
            rounded = Math.addExact(rounded, 100);
        }
        return Math.max(1_000L, rounded);
    }

    public static long calculateProceeds(long newShares, long offerPrice) {
        if (newShares <= 0 || offerPrice <= 0) {
            throw new IllegalArgumentException("신주 수와 공모가는 양수여야 합니다.");
        }
        return BigInteger.valueOf(newShares)
                .multiply(BigInteger.valueOf(offerPrice))
                .longValueExact();
    }

    public static boolean preservesMinimumPlayerOwnership(
            long playerShares,
            long issuedSharesAfterListing
    ) {
        if (playerShares < 0 || issuedSharesAfterListing <= 0) {
            return false;
        }
        return BigInteger.valueOf(playerShares)
                .multiply(BigInteger.valueOf(100))
                .compareTo(BigInteger.valueOf(issuedSharesAfterListing)
                        .multiply(BigInteger.valueOf(MINIMUM_PLAYER_OWNERSHIP_PERCENT))) >= 0;
    }
}
