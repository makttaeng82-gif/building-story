package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

/** 종목 위험도별 5일 유동성과 주문량에 따른 평균·최종 가격 충격을 계산한다. */
@Component
public class StockLiquidityPolicy {
    private static final int MAX_MARGINAL_IMPACT_BASIS_POINTS = 800;

    public long capacity(StockSpec stock) {
        return capacity(stock, stock.issuedShares());
    }

    public long capacity(StockSpec stock, long tradableShares) {
        int capacityBasisPoints = switch (stock.riskType()) {
            case SAFE -> 50;
            case NORMAL -> 35;
            case AGGRESSIVE -> 20;
        };
        return Math.max(1L, Math.max(1L, tradableShares) * capacityBasisPoints / 10_000L);
    }

    /**
     * 누적 순매수 비율의 제곱을 한 주문 구간에서 적분해 평균 체결 충격을 구한다.
     * 따라서 같은 주문을 여러 번 나눠도 전체 평균 체결가는 사실상 같아진다.
     */
    public PriceImpact priceImpact(long capacity, long beforeNetBuy, long signedQuantity) {
        long afterNetBuy = Math.addExact(beforeNetBuy, signedQuantity);
        if (capacity <= 0 || signedQuantity == 0 || Math.abs(afterNetBuy) > capacity) {
            throw new IllegalArgumentException("가격 충격을 계산할 수 없는 주문입니다.");
        }
        double average = (impactIntegral(afterNetBuy, capacity) - impactIntegral(beforeNetBuy, capacity))
                / signedQuantity;
        double finalImpact = marginalImpact(afterNetBuy, capacity);
        return new PriceImpact(
                (int) Math.round(average),
                (int) Math.round(finalImpact)
        );
    }

    private double marginalImpact(long netBuy, long capacity) {
        double ratio = netBuy / (double) capacity;
        return Math.copySign(MAX_MARGINAL_IMPACT_BASIS_POINTS * ratio * ratio, ratio);
    }

    private double impactIntegral(long netBuy, long capacity) {
        double absolute = Math.abs((double) netBuy);
        return MAX_MARGINAL_IMPACT_BASIS_POINTS * absolute * absolute * absolute
                / (3.0 * capacity * capacity);
    }

    public record PriceImpact(int averageBasisPoints, int finalBasisPoints) {
    }
}
