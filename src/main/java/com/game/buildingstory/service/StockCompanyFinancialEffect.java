package com.game.buildingstory.service;

/**
 * 확정 기업 사건이 다음 분기와 현재 재무상태에 주는 변화다.
 * 현금·차입 값도 기업 규모에 맞게 적용되도록 직전 분기 매출 대비 basis point로 저장한다.
 */
public record StockCompanyFinancialEffect(
        int revenueImpactBasisPoints,
        int operatingExpenseImpactBasisPoints,
        int immediateCashImpactBasisPoints,
        int debtFinancingBasisPoints
) {
    public static final StockCompanyFinancialEffect NONE = new StockCompanyFinancialEffect(0, 0, 0, 0);
}
