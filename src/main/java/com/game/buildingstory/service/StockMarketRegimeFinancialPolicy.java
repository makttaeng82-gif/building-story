package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/** 분기 동안 누적된 경기 국면을 업종별 매출·영업비용 보정치로 변환한다. */
@Component
public class StockMarketRegimeFinancialPolicy {
    private static final Map<String, RegimeSensitivity> SENSITIVITIES = Map.of(
            "IT", new RegimeSensitivity(140, -20, -180, 30),
            "식품", new RegimeSensitivity(60, -10, -70, 15),
            "유통", new RegimeSensitivity(110, -15, -150, 30),
            "제조", new RegimeSensitivity(180, -25, -240, 50),
            "통신", new RegimeSensitivity(50, -10, -60, 15)
    );

    /**
     * 확장·침체 보정치를 분기 내 관측 횟수로 가중 평균한다.
     * 중립은 보정값이 0이며, 아직 주가 갱신을 한 번도 겪지 않은 분기도 0으로 처리한다.
     */
    public FinancialImpact impact(
            StockMarketRegimeService.RegimeExposure exposure,
            String industry
    ) {
        RegimeSensitivity sensitivity = SENSITIVITIES.get(industry);
        if (sensitivity == null || exposure.totalUpdates() == 0) {
            return FinancialImpact.NONE;
        }
        int revenue = weightedAverage(
                sensitivity.expansionRevenueBasisPoints(),
                sensitivity.recessionRevenueBasisPoints(),
                exposure
        );
        int operatingExpense = weightedAverage(
                sensitivity.expansionOperatingExpenseBasisPoints(),
                sensitivity.recessionOperatingExpenseBasisPoints(),
                exposure
        );
        return new FinancialImpact(revenue, operatingExpense);
    }

    private int weightedAverage(
            int expansionValue,
            int recessionValue,
            StockMarketRegimeService.RegimeExposure exposure
    ) {
        long weighted = (long) expansionValue * exposure.expansionUpdates()
                + (long) recessionValue * exposure.recessionUpdates();
        return (int) Math.round(weighted / (double) exposure.totalUpdates());
    }

    public record FinancialImpact(int revenueBasisPoints, int operatingExpenseBasisPoints) {
        private static final FinancialImpact NONE = new FinancialImpact(0, 0);
    }

    private record RegimeSensitivity(
            int expansionRevenueBasisPoints,
            int expansionOperatingExpenseBasisPoints,
            int recessionRevenueBasisPoints,
            int recessionOperatingExpenseBasisPoints
    ) {
    }
}
