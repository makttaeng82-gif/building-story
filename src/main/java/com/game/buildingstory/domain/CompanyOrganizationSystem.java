package com.game.buildingstory.domain;

/**
 * 회사가 구축한 조직관리 자동화 단계다.
 *
 * 직원 개인을 추가로 시뮬레이션하지 않고도 이 단계 하나로 관리 가능 인원, 월간 채용량,
 * 부서 처리능력 보정을 일관되게 결정한다.
 */
public enum CompanyOrganizationSystem {
    MANUAL("대표·외부업체 수동 관리", 100, 10, 10, 0, 1.00, 0, 0, 0, 0),
    RECRUITING_PAYROLL("채용·급여 시스템", 500, 15, 50, 15, 1.05, 8_000_000_000L, 2, 50, 40),
    INTEGRATED_HR("통합 인사관리 시스템", 2_000, 50, 200, 10, 1.10, 30_000_000_000L, 3, 350, 55),
    ENTERPRISE("전사 조직관리 시스템", 10_000, 200, 800, 8, 1.15, 120_000_000_000L, 4, 1_500, 65),
    REGIONAL("지역·법인 통합 관리", 30_000, 500, 2_000, 6, 1.20, 400_000_000_000L, 5, 7_500, 75),
    INTELLIGENT("지능형 조직운영 시스템", 100_000, 1_500, 5_000, 5, 1.25, 1_200_000_000_000L, 6, 22_500, 85);

    private final String displayName;
    private final int employeeLimit;
    private final int minimumMonthlyHires;
    private final int maximumMonthlyHires;
    private final int monthlyHirePercent;
    private final double capacityMultiplier;
    private final long constructionCost;
    private final int constructionMonths;
    private final int requiredEmployees;
    private final int requiredHrExpertise;

    CompanyOrganizationSystem(
            String displayName,
            int employeeLimit,
            int minimumMonthlyHires,
            int maximumMonthlyHires,
            int monthlyHirePercent,
            double capacityMultiplier,
            long constructionCost,
            int constructionMonths,
            int requiredEmployees,
            int requiredHrExpertise
    ) {
        this.displayName = displayName;
        this.employeeLimit = employeeLimit;
        this.minimumMonthlyHires = minimumMonthlyHires;
        this.maximumMonthlyHires = maximumMonthlyHires;
        this.monthlyHirePercent = monthlyHirePercent;
        this.capacityMultiplier = capacityMultiplier;
        this.constructionCost = constructionCost;
        this.constructionMonths = constructionMonths;
        this.requiredEmployees = requiredEmployees;
        this.requiredHrExpertise = requiredHrExpertise;
    }

    public String getDisplayName() { return displayName; }
    public int getEmployeeLimit() { return employeeLimit; }
    public double getCapacityMultiplier() { return capacityMultiplier; }
    public long getConstructionCost() { return constructionCost; }
    public int getConstructionMonths() { return constructionMonths; }
    public int getRequiredEmployees() { return requiredEmployees; }
    public int getRequiredHrExpertise() { return requiredHrExpertise; }

    public int monthlyHireLimit(int currentGeneralEmployees) {
        if (this == MANUAL) {
            return maximumMonthlyHires;
        }
        int proportional = (int) Math.ceil(currentGeneralEmployees * monthlyHirePercent / 100.0);
        return Math.min(maximumMonthlyHires, Math.max(minimumMonthlyHires, proportional));
    }

    public CompanyOrganizationSystem next() {
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal >= values().length ? null : values()[nextOrdinal];
    }

    public boolean requiresStrategyFinance() {
        return ordinal() >= ENTERPRISE.ordinal();
    }

    public int requiredStrategyExpertise() {
        return switch (this) {
            case REGIONAL -> 65;
            case INTELLIGENT -> 75;
            default -> 0;
        };
    }
}
