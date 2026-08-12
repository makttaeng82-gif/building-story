package com.game.buildingstory.domain;

/**
 * 확정된 최근 분기 실적으로 판정하는 회사 성장단계다.
 *
 * <p>일회성 구축비나 단기 사업 수익이 아니라 분기말 반복매출과 유료 이용자를
 * 함께 확인하므로 일시적인 매출 급증만으로 단계가 오르지 않는다.</p>
 *
 * <p>단계에 따라 함께 바뀌어야 하는 계약 한도, 주요 업무 슬롯, 비용 비율,
 * 일반직원 급여 배율과 요금제 개방 조건도 이 enum에서 관리한다. 각 서비스가
 * 서로 다른 단계 수치를 갖는 문제를 막기 위한 단일 정책 원천이다.</p>
 */
public enum CompanyGrowthStage {
    FOUNDED("설립기업", 0, 0, 2, 2, 35, 15, 8, 1.00,
            CompanyCustomerContractType.TRIAL, false, false, "초기 기업 운영"),
    GROWTH("성장기업", 15_000_000_000L, 750_000L, 5, 3, 22, 10, 12, 1.15,
            CompanyCustomerContractType.NORMAL, true, false,
            "프로 요금제 · 일반계약 · 업무 슬롯 3개 · 성장기업 인재"),
    LARGE("대기업", 1_000_000_000_000L, 15_000_000L, 10, 4, 15, 8, 20, 1.35,
            CompanyCustomerContractType.LARGE, true, true,
            "맥스 요금제 · 대형계약 · 업무 슬롯 4개 · 대기업 인재"),
    GLOBAL("글로벌기업", 10_000_000_000_000L, 80_000_000L, 20, 6, 12, 6, 25, 2.50,
            CompanyCustomerContractType.STRATEGIC, true, true,
            "전략계약 · 업무 슬롯 6개 · 최종 조직 확장");

    private final String displayName;
    private final long requiredRecurringRevenue;
    private final long requiredPaidUsers;
    private final int contractLimit;
    private final int majorWorkSlotLimit;
    private final int developmentCostPercent;
    private final int marketingCostPercent;
    private final int platformCostPercent;
    private final double generalSalaryMultiplier;
    private final CompanyCustomerContractType highestContractType;
    private final boolean proPlanAvailable;
    private final boolean maxPlanAvailable;
    private final String unlockSummary;

    CompanyGrowthStage(
            String displayName,
            long requiredRecurringRevenue,
            long requiredPaidUsers,
            int contractLimit,
            int majorWorkSlotLimit,
            int developmentCostPercent,
            int marketingCostPercent,
            int platformCostPercent,
            double generalSalaryMultiplier,
            CompanyCustomerContractType highestContractType,
            boolean proPlanAvailable,
            boolean maxPlanAvailable,
            String unlockSummary
    ) {
        this.displayName = displayName;
        this.requiredRecurringRevenue = requiredRecurringRevenue;
        this.requiredPaidUsers = requiredPaidUsers;
        this.contractLimit = contractLimit;
        this.majorWorkSlotLimit = majorWorkSlotLimit;
        this.developmentCostPercent = developmentCostPercent;
        this.marketingCostPercent = marketingCostPercent;
        this.platformCostPercent = platformCostPercent;
        this.generalSalaryMultiplier = generalSalaryMultiplier;
        this.highestContractType = highestContractType;
        this.proPlanAvailable = proPlanAvailable;
        this.maxPlanAvailable = maxPlanAvailable;
        this.unlockSummary = unlockSummary;
    }

    public String getDisplayName() { return displayName; }
    public long getRequiredRecurringRevenue() { return requiredRecurringRevenue; }
    public long getRequiredPaidUsers() { return requiredPaidUsers; }
    public int getContractLimit() { return contractLimit; }
    public int getMajorWorkSlotLimit() { return majorWorkSlotLimit; }
    public int getDevelopmentCostPercent() { return developmentCostPercent; }
    public int getMarketingCostPercent() { return marketingCostPercent; }
    public int getPlatformCostPercent() { return platformCostPercent; }
    public double getGeneralSalaryMultiplier() { return generalSalaryMultiplier; }
    public CompanyCustomerContractType getHighestContractType() { return highestContractType; }
    public boolean isProPlanAvailable() { return proPlanAvailable; }
    public boolean isMaxPlanAvailable() { return maxPlanAvailable; }
    public String getUnlockSummary() { return unlockSummary; }

    public boolean qualifies(long recurringRevenue, long paidUsers) {
        return recurringRevenue >= requiredRecurringRevenue && paidUsers >= requiredPaidUsers;
    }
}
