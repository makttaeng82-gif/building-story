package com.game.buildingstory.domain;

/**
 * 최근 기록의 종류다.
 *
 * <p>MonthlyRecord가 어떤 사건에서 생성됐는지 구분한다. 화면 필터나 색상,
 * 향후 통계 기능을 만들 때 이 enum을 기준으로 분류할 수 있다.</p>
 */
public enum RecordType {
    MOVE_IN,
    MOVE_OUT,
    REPAIR_REQUEST,
    REPAIR_COMPLETE,
    RENT_INCOME,
    SALARY_INCOME,
    SECRETARY_SALARY,
    DONATION,
    LUXURY_ITEM,
    AD_COST,
    LOAN_PAYMENT,
    BUILDING_BUY,
    BUILDING_SELL,
    STOCK_EVENT
}
