package com.game.buildingstory.service;

/** 업종별 적정 배수와 가치 산식 가중치다. 배수는 100이 1배, 가중치는 10,000이 100%다. */
public record ListedCompanyValuationRule(
        int perHundredths,
        int pbrHundredths,
        int psrHundredths,
        int earningsWeightBasisPoints,
        int bookWeightBasisPoints,
        int salesWeightBasisPoints
) {
}
