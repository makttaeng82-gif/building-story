package com.game.buildingstory.domain;

/**
 * 여러 경제 기능이 함께 사용하는 비율과 계산 규칙이다.
 *
 * <p>거래비용처럼 구매, 경매, 월 정산에서 같은 값을 써야 하는 규칙을 한곳에 둔다.
 * 기능마다 숫자를 따로 적으면 일부만 변경되어 경제가 어긋날 수 있다.</p>
 */
public final class EconomyBalanceRules {
    public static final int PURCHASE_FEE_BASIS_POINTS = 150;
    public static final int SELL_FEE_BASIS_POINTS = 100;
    public static final int RENT_OPERATING_COST_BASIS_POINTS = 1_000;
    public static final int AUCTION_DEPOSIT_BASIS_POINTS = 100;

    private static final long BASIS_POINT_DENOMINATOR = 10_000L;

    private EconomyBalanceRules() {
    }

    public static long purchaseFee(long amount) {
        return basisPoints(amount, PURCHASE_FEE_BASIS_POINTS);
    }

    public static long sellFee(long amount) {
        return basisPoints(amount, SELL_FEE_BASIS_POINTS);
    }

    public static long rentOperatingCost(long amount) {
        return basisPoints(amount, RENT_OPERATING_COST_BASIS_POINTS);
    }

    public static long auctionDeposit(long amount) {
        return basisPoints(amount, AUCTION_DEPOSIT_BASIS_POINTS);
    }

    public static long governmentCityEntryGrant(String city) {
        return switch (city) {
            case "청주" -> 30_000_000L;
            case "세종" -> 150_000_000L;
            case "대전" -> 400_000_000L;
            case "부산" -> 1_000_000_000L;
            case "인천" -> 2_500_000_000L;
            case "서울" -> 10_000_000_000L;
            default -> 0L;
        };
    }

    /**
     * 과거 저장 데이터와 시뮬레이션 코드의 컴파일 호환성을 위한 메서드다.
     * 정부지원은 구매 할인에서 도시 첫 진입 현금 지급으로 변경되어 항상 0%다.
     */
    @Deprecated
    public static int governmentPurchaseSupportPercent(String city) {
        return 0;
    }

    /** 구매가격 할인은 더 이상 적용하지 않는다. */
    @Deprecated
    public static long governmentSupportedPrice(long amount, String city) {
        if (amount < 0) {
            throw new IllegalArgumentException("기준 금액은 음수일 수 없습니다");
        }
        return amount;
    }

    public static int buildingMilestoneReputation(String city, int slot) {
        int cityIndex = switch (city) {
            case "청주" -> 0;
            case "세종" -> 1;
            case "대전" -> 2;
            case "부산" -> 3;
            case "인천" -> 4;
            case "서울" -> 5;
            default -> -1;
        };
        if (cityIndex < 0 || slot < 1 || slot > 4) {
            return 0;
        }
        int[][] rewards = {
                {0, 50, 100, 180},
                {220, 300, 375, 450},
                {525, 600, 750, 900},
                {1_100, 1_200, 1_500, 1_800},
                {1_875, 2_250, 2_625, 3_000},
                {3_000, 3_750, 4_500, 6_000}
        };
        return rewards[cityIndex][slot - 1];
    }

    private static long basisPoints(long amount, int basisPoints) {
        if (amount < 0) {
            throw new IllegalArgumentException("기준 금액은 음수일 수 없습니다");
        }
        return Math.multiplyExact(amount, basisPoints) / BASIS_POINT_DENOMINATOR;
    }
}
