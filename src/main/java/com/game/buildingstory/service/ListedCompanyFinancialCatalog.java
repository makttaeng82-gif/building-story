package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 15개 초기 상장기업의 재무 체질을 정의한다.
 *
 * <p>금액은 원 단위이고 비율은 10,000이 100%인 basis point 단위다.
 * 기업별 값을 분리해 같은 위험등급이라도 매출 규모와 비용 구조가 달라지게 한다.</p>
 */
@Component
public class ListedCompanyFinancialCatalog {
    private final List<ListedCompanyFinancialProfile> profiles = List.of(
            profile("bytecore", 3_000_000_000_000L, 3_000_000_000_000L, 10_000_000_000_000L, 2_000_000_000_000L, 2_000_000_000_000L, 500, 5_800, 4_000, 500, 400, 2_500, 120),
            profile("neonsoft", 1_000_000_000_000L, 1_000_000_000_000L, 2_000_000_000_000L, 500_000_000_000L, 500_000_000_000L, 900, 7_000, 5_000, 600, 500, 1_500, 260),
            profile("cloudnine", 375_000_000_000L, 500_000_000_000L, 800_000_000_000L, 200_000_000_000L, 100_000_000_000L, 1_600, 7_500, 6_000, 900, 600, 0, 550),
            profile("freshmill", 3_750_000_000_000L, 1_000_000_000_000L, 7_000_000_000_000L, 2_000_000_000_000L, 1_000_000_000_000L, 350, 3_200, 2_400, 350, 450, 3_500, 100),
            profile("goldenfood", 1_500_000_000_000L, 500_000_000_000L, 3_000_000_000_000L, 1_000_000_000_000L, 500_000_000_000L, 650, 3_500, 2_650, 400, 500, 2_500, 220),
            profile("dailybrew", 450_000_000_000L, 200_000_000_000L, 900_000_000_000L, 300_000_000_000L, 100_000_000_000L, 1_200, 4_200, 3_600, 650, 600, 500, 500),
            profile("marketway", 6_250_000_000_000L, 1_500_000_000_000L, 10_000_000_000_000L, 3_000_000_000_000L, 2_000_000_000_000L, 300, 2_400, 1_900, 300, 400, 3_500, 90),
            profile("quickbox", 1_500_000_000_000L, 500_000_000_000L, 2_500_000_000_000L, 700_000_000_000L, 300_000_000_000L, 800, 3_800, 2_900, 550, 550, 1_500, 250),
            profile("hubstore", 375_000_000_000L, 150_000_000_000L, 600_000_000_000L, 200_000_000_000L, 50_000_000_000L, 1_400, 4_500, 4_000, 800, 650, 0, 520),
            profile("ironworks", 3_500_000_000_000L, 1_000_000_000_000L, 12_000_000_000_000L, 3_000_000_000_000L, 2_000_000_000_000L, 300, 3_600, 2_500, 700, 500, 3_000, 130),
            profile("motorline", 1_500_000_000_000L, 500_000_000_000L, 4_000_000_000_000L, 1_200_000_000_000L, 300_000_000_000L, 700, 4_000, 2_900, 700, 550, 2_000, 260),
            profile("nextchem", 450_000_000_000L, 300_000_000_000L, 1_500_000_000_000L, 500_000_000_000L, 100_000_000_000L, 1_500, 4_800, 4_000, 1_000, 650, 0, 580),
            profile("signalnet", 4_500_000_000_000L, 1_500_000_000_000L, 18_000_000_000_000L, 6_000_000_000_000L, 2_000_000_000_000L, 250, 4_500, 3_200, 900, 500, 4_000, 80),
            profile("bluewave", 1_750_000_000_000L, 600_000_000_000L, 6_000_000_000_000L, 2_000_000_000_000L, 400_000_000_000L, 600, 4_800, 3_600, 850, 550, 2_500, 240),
            profile("linktel", 500_000_000_000L, 200_000_000_000L, 2_000_000_000_000L, 800_000_000_000L, 100_000_000_000L, 1_100, 4_200, 3_500, 900, 650, 500, 500)
    );

    public ListedCompanyFinancialProfile require(String stockKey) {
        return profiles.stream()
                .filter(profile -> profile.stockKey().equals(stockKey))
                .findFirst()
                .orElseThrow();
    }

    private ListedCompanyFinancialProfile profile(
            String stockKey,
            long quarterlyRevenue,
            long cash,
            long nonCashAssets,
            long debt,
            long otherLiabilities,
            int annualGrowthBasisPoints,
            int grossMarginBasisPoints,
            int operatingExpenseBasisPoints,
            int capitalExpenditureBasisPoints,
            int annualDepreciationBasisPoints,
            int dividendPayoutBasisPoints,
            int quarterlyNoiseBasisPoints
    ) {
        return new ListedCompanyFinancialProfile(
                stockKey,
                quarterlyRevenue,
                cash,
                nonCashAssets,
                debt,
                otherLiabilities,
                annualGrowthBasisPoints,
                grossMarginBasisPoints,
                operatingExpenseBasisPoints,
                capitalExpenditureBasisPoints,
                annualDepreciationBasisPoints,
                dividendPayoutBasisPoints,
                quarterlyNoiseBasisPoints
        );
    }
}
