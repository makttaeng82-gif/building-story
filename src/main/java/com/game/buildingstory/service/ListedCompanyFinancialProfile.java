package com.game.buildingstory.service;

/** 상장기업을 처음 만들 때 사용하는 종목별 재무 기준값이다. */
public record ListedCompanyFinancialProfile(
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
}
