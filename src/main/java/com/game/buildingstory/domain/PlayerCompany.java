package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 플레이어가 설립한 비상장 법인의 자금과 소유구조를 저장한다. */
@Entity
@Table(name = "player_company", uniqueConstraints =
        @UniqueConstraint(name = "uk_player_company_player", columnNames = "player_id"))
public class PlayerCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @Column(nullable = false, length = 20)
    private String companyName;

    @Column(nullable = false, length = 20)
    private String serviceName;

    @Column(nullable = false, length = 20)
    private String industry;

    private long paidInCapital;
    private long corporateCash;
    private long issuedShares;
    private long playerShares;
    private boolean secretaryTrainingCompleted;
    private int establishedElapsedDay;

    @Enumerated(EnumType.STRING)
    private CompanyTutorialStage tutorialStage;
    @Enumerated(EnumType.STRING)
    private CompanyGrowthStage growthStage;
    private Integer prototypeBenchmark;
    private String cloudPlan;
    private Integer commercializationMonthsCompleted;
    private Long commercializationAccumulatedCost;
    private Boolean commercializationFundingPaused;
    private Integer productCompleteness;
    private Integer productStability;
    private Integer productSecurity;
    private Integer computeEfficiency;
    private Integer technicalDebt;
    private Double technicalDebtBudgetRemainder;
    @Enumerated(EnumType.STRING)
    private CompanyDevelopmentBudgetPolicy developmentBudgetPolicy;
    @Enumerated(EnumType.STRING)
    private CompanyMarketingBudgetPolicy marketingBudgetPolicy;
    private Double marketingEffect;
    private Double brandScore;
    private Long paidUsers;
    private Long monthlyRecurringRevenue;
    private Boolean operationsSuspended;
    private Long unpaidSettlementAmount;
    private Long unpaidOperatingAmount;
    private Long unpaidBondInterestAmount;
    private Long unpaidBondPrincipalAmount;
    private Integer activeMajorWorkCount;
    private Long totalMarketUsers;
    private Double marketShare;
    private Integer marketMonthsProcessed;
    private Long normalSubscribers;
    private Long proSubscribers;
    private Long maxSubscribers;
    private String pendingCloudPlan;
    private Long reserveComputeCapacity;
    private Long reserveComputeCost;
    private Integer reserveComputeExpiresDay;
    @Enumerated(EnumType.STRING)
    private CompanyOrganizationSystem organizationSystem;
    @Enumerated(EnumType.STRING)
    private CompanyOrganizationSystem pendingOrganizationSystem;
    private Integer organizationUpgradeMonthsRemaining;
    private Long organizationUpgradeRemainingCost;
    private Integer organizationUpgradeHrWorkRemaining;
    private Integer organizationUpgradeHrAllocation;
    private Integer organizationUpgradeStrategyWorkRemaining;
    private Integer organizationUpgradeStrategyAllocation;
    private Boolean automaticHiringEnabled;
    private Integer growthStagePromotedMarketMonth;
    private Double generalSalaryTransitionFromMultiplier;

    protected PlayerCompany() {
    }

    public PlayerCompany(
            Player player,
            String companyName,
            String serviceName,
            long paidInCapital,
            long corporateCash,
            long issuedShares,
            int establishedElapsedDay
    ) {
        this.player = player;
        this.companyName = companyName;
        this.serviceName = serviceName;
        this.industry = "IT";
        this.paidInCapital = paidInCapital;
        this.corporateCash = corporateCash;
        this.issuedShares = issuedShares;
        this.playerShares = issuedShares;
        this.secretaryTrainingCompleted = true;
        this.establishedElapsedDay = establishedElapsedDay;
        this.tutorialStage = CompanyTutorialStage.FOUNDING_HIRE;
        this.growthStage = CompanyGrowthStage.FOUNDED;
        this.prototypeBenchmark = 180;
        this.cloudPlan = "STARTER";
        this.commercializationMonthsCompleted = 0;
        this.commercializationAccumulatedCost = 0L;
        this.commercializationFundingPaused = false;
        this.operationsSuspended = false;
        this.unpaidSettlementAmount = 0L;
        this.unpaidOperatingAmount = 0L;
        this.unpaidBondInterestAmount = 0L;
        this.unpaidBondPrincipalAmount = 0L;
        this.activeMajorWorkCount = 0;
        this.totalMarketUsers = 2_000_000L;
        this.marketShare = 12.0;
        this.marketMonthsProcessed = 0;
        this.normalSubscribers = 240_000L;
        this.proSubscribers = 0L;
        this.maxSubscribers = 0L;
        this.organizationSystem = CompanyOrganizationSystem.MANUAL;
        this.organizationUpgradeMonthsRemaining = 0;
        this.organizationUpgradeRemainingCost = 0L;
        this.organizationUpgradeHrWorkRemaining = 0;
        this.organizationUpgradeHrAllocation = 0;
        this.organizationUpgradeStrategyWorkRemaining = 0;
        this.organizationUpgradeStrategyAllocation = 0;
        this.automaticHiringEnabled = false;
        this.growthStagePromotedMarketMonth = 0;
        this.generalSalaryTransitionFromMultiplier = 1.0;
        this.developmentBudgetPolicy = CompanyDevelopmentBudgetPolicy.STANDARD;
        this.marketingBudgetPolicy = CompanyMarketingBudgetPolicy.STANDARD;
        this.marketingEffect = 40.0;
        this.brandScore = 20.0;
        this.technicalDebtBudgetRemainder = 0.0;
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public String getCompanyName() { return companyName; }
    public String getServiceName() { return serviceName; }
    public String getIndustry() { return industry; }
    public long getPaidInCapital() { return paidInCapital; }
    public long getCorporateCash() { return corporateCash; }
    public long getIssuedShares() { return issuedShares; }
    public long getPlayerShares() { return playerShares; }
    public boolean isSecretaryTrainingCompleted() { return secretaryTrainingCompleted; }
    public int getEstablishedElapsedDay() { return establishedElapsedDay; }
    public CompanyTutorialStage getTutorialStage() { return tutorialStage == null ? CompanyTutorialStage.FOUNDING_HIRE : tutorialStage; }
    public CompanyGrowthStage getGrowthStage() { return growthStage == null ? CompanyGrowthStage.FOUNDED : growthStage; }
    public int getPrototypeBenchmark() { return prototypeBenchmark == null ? 180 : prototypeBenchmark; }
    public String getCloudPlan() { return cloudPlan == null ? "STARTER" : cloudPlan; }
    public int getCommercializationMonthsCompleted() { return commercializationMonthsCompleted == null ? 0 : commercializationMonthsCompleted; }
    public long getCommercializationAccumulatedCost() { return commercializationAccumulatedCost == null ? 0 : commercializationAccumulatedCost; }
    public boolean isCommercializationFundingPaused() { return Boolean.TRUE.equals(commercializationFundingPaused); }
    public int getProductCompleteness() { return productCompleteness == null ? 0 : productCompleteness; }
    public int getProductStability() { return productStability == null ? 0 : productStability; }
    public int getProductSecurity() { return productSecurity == null ? 0 : productSecurity; }
    public int getComputeEfficiency() { return computeEfficiency == null ? 0 : computeEfficiency; }
    public int getTechnicalDebt() { return technicalDebt == null ? 0 : technicalDebt; }
    public CompanyDevelopmentBudgetPolicy getDevelopmentBudgetPolicy() {
        return developmentBudgetPolicy == null ? CompanyDevelopmentBudgetPolicy.STANDARD : developmentBudgetPolicy;
    }
    public CompanyMarketingBudgetPolicy getMarketingBudgetPolicy() {
        return marketingBudgetPolicy == null ? CompanyMarketingBudgetPolicy.STANDARD : marketingBudgetPolicy;
    }
    public double getMarketingEffect() { return marketingEffect == null ? 40.0 : marketingEffect; }
    public double getBrandScore() { return brandScore == null ? 20.0 : brandScore; }
    public long getPaidUsers() { return paidUsers == null ? 0 : paidUsers; }
    public long getMonthlyRecurringRevenue() { return monthlyRecurringRevenue == null ? 0 : monthlyRecurringRevenue; }
    public boolean isOperationsSuspended() { return Boolean.TRUE.equals(operationsSuspended); }
    public long getUnpaidSettlementAmount() { return unpaidSettlementAmount == null ? 0 : unpaidSettlementAmount; }
    public long getUnpaidOperatingAmount() {
        if (unpaidOperatingAmount == null && unpaidBondInterestAmount == null && unpaidBondPrincipalAmount == null) {
            return getUnpaidSettlementAmount();
        }
        return unpaidOperatingAmount == null ? 0 : unpaidOperatingAmount;
    }
    public long getUnpaidBondInterestAmount() {
        return unpaidBondInterestAmount == null ? 0 : unpaidBondInterestAmount;
    }
    public long getUnpaidBondPrincipalAmount() {
        return unpaidBondPrincipalAmount == null ? 0 : unpaidBondPrincipalAmount;
    }
    public int getActiveMajorWorkCount() { return activeMajorWorkCount == null ? 0 : activeMajorWorkCount; }
    public long getTotalMarketUsers() { return totalMarketUsers == null ? 2_000_000L : totalMarketUsers; }
    public double getMarketShare() { return marketShare == null ? 12.0 : marketShare; }
    public int getMarketMonthsProcessed() { return marketMonthsProcessed == null ? 0 : marketMonthsProcessed; }
    public long getNormalSubscribers() { return normalSubscribers == null ? getPaidUsers() : normalSubscribers; }
    public long getProSubscribers() { return proSubscribers == null ? 0 : proSubscribers; }
    public long getMaxSubscribers() { return maxSubscribers == null ? 0 : maxSubscribers; }
    public String getPendingCloudPlan() { return pendingCloudPlan; }
    public long getReserveComputeCost() { return reserveComputeCost == null ? 0L : reserveComputeCost; }
    public int getReserveComputeExpiresDay() { return reserveComputeExpiresDay == null ? 0 : reserveComputeExpiresDay; }
    public CompanyOrganizationSystem getOrganizationSystem() {
        return organizationSystem == null ? CompanyOrganizationSystem.MANUAL : organizationSystem;
    }
    public CompanyOrganizationSystem getPendingOrganizationSystem() { return pendingOrganizationSystem; }
    public int getOrganizationUpgradeMonthsRemaining() {
        return organizationUpgradeMonthsRemaining == null ? 0 : organizationUpgradeMonthsRemaining;
    }
    public long getOrganizationUpgradeRemainingCost() {
        return organizationUpgradeRemainingCost == null ? 0L : organizationUpgradeRemainingCost;
    }
    public int getOrganizationUpgradeHrWorkRemaining() {
        return organizationUpgradeHrWorkRemaining == null ? 0 : organizationUpgradeHrWorkRemaining;
    }
    public int getOrganizationUpgradeHrAllocation() {
        return organizationUpgradeHrAllocation == null ? 0 : organizationUpgradeHrAllocation;
    }
    public int getOrganizationUpgradeStrategyWorkRemaining() {
        return organizationUpgradeStrategyWorkRemaining == null ? 0 : organizationUpgradeStrategyWorkRemaining;
    }
    public int getOrganizationUpgradeStrategyAllocation() {
        return organizationUpgradeStrategyAllocation == null ? 0 : organizationUpgradeStrategyAllocation;
    }
    public boolean isAutomaticHiringEnabled() { return Boolean.TRUE.equals(automaticHiringEnabled); }

    public long getActiveReserveComputeCapacity(int elapsedDay) {
        if (reserveComputeCapacity == null || reserveComputeExpiresDay == null
                || elapsedDay >= reserveComputeExpiresDay) {
            return 0L;
        }
        return reserveComputeCapacity;
    }

    public int getReserveComputeRemainingDays(int elapsedDay) {
        return getActiveReserveComputeCapacity(elapsedDay) == 0
                ? 0
                : Math.max(0, getReserveComputeExpiresDay() - elapsedDay);
    }

    public CompanyCloudPlan getCloudPlanType() {
        try {
            return CompanyCloudPlan.valueOf(getCloudPlan());
        } catch (IllegalArgumentException exception) {
            return CompanyCloudPlan.STARTER;
        }
    }

    public CompanyCloudPlan getPendingCloudPlanType() {
        if (pendingCloudPlan == null || pendingCloudPlan.isBlank()) {
            return null;
        }
        try {
            return CompanyCloudPlan.valueOf(pendingCloudPlan);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public double getPlayerOwnershipPercent() {
        return issuedShares == 0 ? 0.0 : playerShares * 100.0 / issuedShares;
    }

    /**
     * 성장단계는 장기 달성 보상이므로 후보 단계가 현재 단계보다 높을 때만 변경한다.
     * 최근 실적이 나빠져도 이미 개방한 기능과 조직 한도가 다시 잠기지 않는다.
     */
    public boolean promoteGrowthStage(CompanyGrowthStage candidate) {
        if (candidate == null || candidate.ordinal() <= getGrowthStage().ordinal()) {
            return false;
        }
        generalSalaryTransitionFromMultiplier = generalSalaryStageMultiplier();
        growthStagePromotedMarketMonth = getMarketMonthsProcessed();
        growthStage = candidate;
        return true;
    }

    /**
     * 성장단계가 오를 때 일반직원 급여가 즉시 뛰지 않도록 12개월에 걸쳐 새 기준으로 이동한다.
     * 기존 저장 데이터에는 전환 시작값이 없으므로 현재 단계 기준을 즉시 적용한다.
     */
    public double generalSalaryStageMultiplier() {
        double target = getGrowthStage().getGeneralSalaryMultiplier();
        if (growthStagePromotedMarketMonth == null || generalSalaryTransitionFromMultiplier == null) {
            return target;
        }
        double progress = Math.max(0.0, Math.min(
                1.0, (getMarketMonthsProcessed() - growthStagePromotedMarketMonth) / 12.0));
        return generalSalaryTransitionFromMultiplier
                + (target - generalSalaryTransitionFromMultiplier) * progress;
    }

    public boolean spendCorporateCash(long amount) {
        if (amount < 0 || corporateCash < amount) {
            return false;
        }
        corporateCash -= amount;
        return true;
    }

    public void addCorporateCash(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("법인 입금액은 음수일 수 없습니다");
        }
        corporateCash = Math.addExact(corporateCash, amount);
    }

    public void contributeCapital(long amount, long newPlayerShares) {
        if (newPlayerShares < 0) {
            throw new IllegalArgumentException("신규 플레이어 주식 수는 음수일 수 없습니다.");
        }
        addCorporateCash(amount);
        paidInCapital = Math.addExact(paidInCapital, amount);
        issuedShares = Math.addExact(issuedShares, newPlayerShares);
        playerShares = Math.addExact(playerShares, newPlayerShares);
    }

    public void issueExternalShares(long shares) {
        if (shares <= 0) {
            throw new IllegalArgumentException("발행 주식 수는 1주 이상이어야 합니다.");
        }
        issuedShares = Math.addExact(issuedShares, shares);
    }

    public void completeFoundingHire() {
        if (getTutorialStage() != CompanyTutorialStage.FOUNDING_HIRE) {
            throw new IllegalStateException("창업팀 채용 단계가 아닙니다");
        }
        tutorialStage = CompanyTutorialStage.READY_TO_DEVELOP;
    }

    public void startCommercialization() {
        if (getTutorialStage() != CompanyTutorialStage.READY_TO_DEVELOP) {
            throw new IllegalStateException("상용화 개발을 시작할 수 없는 단계입니다");
        }
        tutorialStage = CompanyTutorialStage.COMMERCIALIZATION_IN_PROGRESS;
        commercializationFundingPaused = false;
    }

    public boolean advanceCommercializationMonth(long monthlyCost) {
        if (getTutorialStage() != CompanyTutorialStage.COMMERCIALIZATION_IN_PROGRESS) {
            return false;
        }
        if (!spendCorporateCash(monthlyCost)) {
            commercializationFundingPaused = true;
            return false;
        }
        commercializationFundingPaused = false;
        commercializationAccumulatedCost = Math.addExact(getCommercializationAccumulatedCost(), monthlyCost);
        commercializationMonthsCompleted = getCommercializationMonthsCompleted() + 1;
        if (getCommercializationMonthsCompleted() >= 4) {
            tutorialStage = CompanyTutorialStage.LAUNCH_REVIEW;
            return true;
        }
        return false;
    }

    /** 개발 중 수동 검증에서 달력 4개월을 기다리지 않고 출시 검토 단계로 이동한다. */
    public void completeCommercializationForTest() {
        if (getTutorialStage() != CompanyTutorialStage.COMMERCIALIZATION_IN_PROGRESS) {
            throw new IllegalStateException("상용화 개발 진행 단계가 아닙니다");
        }
        commercializationMonthsCompleted = 4;
        commercializationFundingPaused = false;
        tutorialStage = CompanyTutorialStage.LAUNCH_REVIEW;
    }

    public void launchFirstProduct() {
        if (getTutorialStage() != CompanyTutorialStage.LAUNCH_REVIEW) {
            throw new IllegalStateException("출시 검토 단계가 아닙니다");
        }
        tutorialStage = CompanyTutorialStage.LAUNCHED;
        prototypeBenchmark = 200;
        productCompleteness = 35;
        productStability = 65;
        productSecurity = 45;
        computeEfficiency = 55;
        technicalDebt = 15;
        technicalDebtBudgetRemainder = 0.0;
        developmentBudgetPolicy = CompanyDevelopmentBudgetPolicy.STANDARD;
        marketingBudgetPolicy = CompanyMarketingBudgetPolicy.STANDARD;
        marketingEffect = 40.0;
        brandScore = 20.0;
        paidUsers = 240_000L;
        monthlyRecurringRevenue = 4_800_000_000L;
        totalMarketUsers = 2_000_000L;
        marketShare = 12.0;
        marketMonthsProcessed = 0;
        normalSubscribers = 240_000L;
        proSubscribers = 0L;
        maxSubscribers = 0L;
    }

    public void recordFirstSettlement() {
        if (getTutorialStage() == CompanyTutorialStage.LAUNCHED) {
            tutorialStage = CompanyTutorialStage.FIRST_SETTLEMENT;
        }
    }

    public boolean completeTutorialAfterFirstQuarterRead(int quarterSequence) {
        if (getTutorialStage() != CompanyTutorialStage.FIRST_SETTLEMENT || quarterSequence != 1) {
            return false;
        }
        tutorialStage = CompanyTutorialStage.COMPLETED;
        return true;
    }

    public void updateMarketResult(long totalUsers, double share, long normal, long pro, long max, long revenue) {
        if (totalUsers < 0 || share < 0 || normal < 0 || pro < 0 || max < 0 || revenue < 0) {
            throw new IllegalArgumentException("시장 계산 결과는 음수일 수 없습니다");
        }
        totalMarketUsers = totalUsers;
        marketShare = share;
        normalSubscribers = normal;
        proSubscribers = pro;
        maxSubscribers = max;
        paidUsers = Math.addExact(Math.addExact(normal, pro), max);
        monthlyRecurringRevenue = revenue;
        marketMonthsProcessed = getMarketMonthsProcessed() + 1;
    }

    /** 완료된 제품 프로젝트의 양수 효과에는 프로젝트 품질 배율을 적용한다. */
    public void applyProductImprovement(CompanyProductImprovementType type, CompanyDevelopmentDirection direction, int quality) {
        double resultMultiplier = 0.75 + Math.max(0, Math.min(100, quality)) / 200.0;
        switch (type) {
            case MODEL_REFINEMENT -> {
                int baseGain = Math.max(15, Math.min(150, (int) Math.round(getPrototypeBenchmark() * 0.08)));
                applyModelDirection(baseGain, -2, 5, direction, resultMultiplier);
            }
            case WORKFLOW_AUTOMATION -> {
                int baseGain = Math.max(3, (int) Math.round((100 - getProductCompleteness()) * 0.15));
                productCompleteness = clamp(getProductCompleteness() + scaledGain(baseGain, resultMultiplier), 0, 100);
                technicalDebt = Math.addExact(getTechnicalDebt(), 4);
            }
            case SERVICE_STABILIZATION -> {
                int stabilityGain = Math.max(2, (int) Math.round((100 - getProductStability()) * 0.12));
                int securityGain = Math.max(2, (int) Math.round((100 - getProductSecurity()) * 0.08));
                productStability = clamp(getProductStability() + scaledGain(stabilityGain, resultMultiplier), 0, 100);
                productSecurity = clamp(getProductSecurity() + scaledGain(securityGain, resultMultiplier), 0, 100);
                technicalDebt = Math.max(0, getTechnicalDebt() - 10);
            }
            case INFERENCE_OPTIMIZATION -> {
                int baseGain = Math.max(3, (int) Math.round((100 - getComputeEfficiency()) * 0.15));
                computeEfficiency = clamp(getComputeEfficiency() + scaledGain(baseGain, resultMultiplier), 0, 100);
                technicalDebt = Math.max(0, getTechnicalDebt() - 3);
            }
            case NEXT_GENERATION_MODEL -> {
                int baseGain = Math.max(60, Math.min(400, (int) Math.round(getPrototypeBenchmark() * 0.22)));
                applyModelDirection(baseGain, -5, 12, direction, resultMultiplier);
                productCompleteness = clamp(getProductCompleteness() + scaledGain(5, resultMultiplier), 0, 100);
                productStability = clamp(getProductStability() - 5, 0, 100);
            }
        }
    }

    /** 발생한 장애의 심각도와 보안사고 여부를 제품 상태에 즉시 반영한다. */
    public void applyServiceIncident(CompanyServiceIncidentSeverity severity, boolean securityIncident) {
        int stabilityLoss = switch (severity) {
            case MINOR -> 0;
            case MAJOR -> 2;
            case CRITICAL -> 5;
        };
        int securityLoss = securityIncident ? switch (severity) {
            case MINOR -> 0;
            case MAJOR -> 2;
            case CRITICAL -> 5;
        } : 0;
        int debtIncrease = switch (severity) {
            case MINOR -> 1;
            case MAJOR -> 3;
            case CRITICAL -> 8;
        };
        productStability = clamp(getProductStability() - stabilityLoss, 0, 100);
        productSecurity = clamp(getProductSecurity() - securityLoss, 0, 100);
        technicalDebt = Math.addExact(getTechnicalDebt(), debtIncrease);
    }

    /** 비용을 충분히 투입한 장애 대응이 제품 손상의 일부를 복구한다. */
    public void applyIncidentRecovery(CompanyServiceIncidentSeverity severity) {
        int stabilityRecovery = severity == CompanyServiceIncidentSeverity.CRITICAL ? 4 : 1;
        int securityRecovery = severity == CompanyServiceIncidentSeverity.CRITICAL ? 3 : 1;
        int debtReduction = severity == CompanyServiceIncidentSeverity.CRITICAL ? 6 : 2;
        productStability = clamp(getProductStability() + stabilityRecovery, 0, 100);
        productSecurity = clamp(getProductSecurity() + securityRecovery, 0, 100);
        technicalDebt = Math.max(0, getTechnicalDebt() - debtReduction);
    }

    public void changeBudgetPolicies(
            CompanyDevelopmentBudgetPolicy developmentPolicy,
            CompanyMarketingBudgetPolicy marketingPolicy
    ) {
        if (developmentPolicy == null || marketingPolicy == null) {
            throw new IllegalArgumentException("개발비와 마케팅비 정책을 모두 선택해야 합니다.");
        }
        developmentBudgetPolicy = developmentPolicy;
        marketingBudgetPolicy = marketingPolicy;
    }

    /** 확정 월의 지출 결과를 다음 달 시장효과와 기술부채에 반영한다. */
    public void applyMonthlyBudgetEffects(double campaignStrength, double technicalDebtIncrease) {
        double nextMarketingEffect = getMarketingEffect() * 0.6 + clamp(campaignStrength, 0, 100) * 0.4;
        marketingEffect = clamp(nextMarketingEffect, 0, 100);
        double rawBrandChange = (getMarketingEffect() - 50.0) / 100.0;
        double adjustedBrandChange = rawBrandChange >= 0
                ? rawBrandChange * (100.0 - getBrandScore()) / 100.0
                : rawBrandChange * getBrandScore() / 100.0;
        brandScore = clamp(getBrandScore() + adjustedBrandChange, 0, 100);
        if (technicalDebtIncrease > 0) {
            double accumulated = (technicalDebtBudgetRemainder == null ? 0.0 : technicalDebtBudgetRemainder)
                    + technicalDebtIncrease;
            int wholeDebt = (int) Math.floor(accumulated);
            technicalDebtBudgetRemainder = accumulated - wholeDebt;
            technicalDebt = Math.addExact(getTechnicalDebt(), wholeDebt);
        }
    }

    /** 제품 출시, 계약, 장애처럼 월 예산과 무관한 사건이 브랜드에 주는 영향을 반영한다. */
    public void adjustBrandScore(double change) {
        brandScore = clamp(getBrandScore() + change, 0, 100);
    }

    private void applyModelDirection(int baseBenchmarkGain, int baseEfficiencyChange, int baseDebtChange,
                                     CompanyDevelopmentDirection direction, double resultMultiplier) {
        double benchmarkMultiplier = switch (direction) {
            case PERFORMANCE -> 1.25;
            case EFFICIENCY, STABILITY -> 0.75;
            case BALANCED -> 1.0;
        };
        int benchmarkGain = Math.max(1, (int) Math.round(baseBenchmarkGain * benchmarkMultiplier));
        prototypeBenchmark = Math.addExact(getPrototypeBenchmark(), scaledGain(benchmarkGain, resultMultiplier));
        int efficiencyChange = baseEfficiencyChange;
        int debtChange = baseDebtChange;
        switch (direction) {
            case PERFORMANCE -> {
                efficiencyChange -= 3;
                debtChange += 3;
            }
            case EFFICIENCY -> efficiencyChange += 6;
            case STABILITY -> {
                productStability = clamp(getProductStability() + 4, 0, 100);
                productSecurity = clamp(getProductSecurity() + 2, 0, 100);
                debtChange -= 2;
            }
            case BALANCED -> {
            }
        }
        computeEfficiency = clamp(getComputeEfficiency() + efficiencyChange, 0, 100);
        technicalDebt = Math.max(0, Math.addExact(getTechnicalDebt(), debtChange));
    }

    private int scaledGain(int baseGain, double multiplier) {
        return Math.max(1, (int) Math.round(baseGain * multiplier));
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public void requestCloudPlan(CompanyCloudPlan plan) {
        if (plan == null || plan == getCloudPlanType()) {
            throw new IllegalArgumentException("현재와 다른 클라우드 계약을 선택해야 합니다");
        }
        pendingCloudPlan = plan.name();
    }

    public boolean applyPendingCloudPlan() {
        CompanyCloudPlan pending = getPendingCloudPlanType();
        if (pending == null) {
            return false;
        }
        cloudPlan = pending.name();
        pendingCloudPlan = null;
        return true;
    }

    public void activateReserveComputeCapacity(long capacity, long cost, int expiresDay) {
        if (capacity <= 0 || cost <= 0 || expiresDay <= player.getElapsedDays()) {
            throw new IllegalArgumentException("예비용량 구매 정보가 올바르지 않습니다");
        }
        reserveComputeCapacity = capacity;
        reserveComputeCost = cost;
        reserveComputeExpiresDay = expiresDay;
    }

    public void suspendOperations(long unpaidOperating, long unpaidBondInterest, long unpaidBondPrincipal) {
        if (unpaidOperating < 0 || unpaidBondInterest < 0 || unpaidBondPrincipal < 0) {
            throw new IllegalArgumentException("미지급액은 음수일 수 없습니다.");
        }
        operationsSuspended = true;
        unpaidOperatingAmount = unpaidOperating;
        unpaidBondInterestAmount = unpaidBondInterest;
        unpaidBondPrincipalAmount = unpaidBondPrincipal;
        unpaidSettlementAmount = Math.addExact(
                Math.addExact(unpaidOperating, unpaidBondInterest), unpaidBondPrincipal);
    }

    public void suspendOperations(long unpaidAmount) {
        suspendOperations(Math.max(0, unpaidAmount), 0, 0);
    }

    public boolean resumeOperations(long nextMonthEssentialCost) {
        long required = Math.addExact(getUnpaidSettlementAmount(), nextMonthEssentialCost);
        if (!isOperationsSuspended() || corporateCash < required) {
            return false;
        }
        corporateCash -= getUnpaidSettlementAmount();
        unpaidSettlementAmount = 0L;
        unpaidOperatingAmount = 0L;
        unpaidBondInterestAmount = 0L;
        unpaidBondPrincipalAmount = 0L;
        operationsSuspended = false;
        return true;
    }

    public void startOrganizationUpgrade(
            CompanyOrganizationSystem target,
            long upfrontCost,
            int hrWorkload,
            int hrAllocation,
            int strategyWorkload,
            int strategyAllocation
    ) {
        if (target == null || target != getOrganizationSystem().next()
                || pendingOrganizationSystem != null || upfrontCost < 0
                || hrWorkload <= 0 || hrAllocation <= 0
                || strategyWorkload < 0 || strategyAllocation < 0
                || (strategyWorkload == 0) != (strategyAllocation == 0)
                || !spendCorporateCash(upfrontCost)) {
            throw new IllegalStateException("조직관리 시스템 구축을 시작할 수 없습니다.");
        }
        pendingOrganizationSystem = target;
        organizationUpgradeMonthsRemaining = target.getConstructionMonths();
        organizationUpgradeRemainingCost = target.getConstructionCost() - upfrontCost;
        organizationUpgradeHrWorkRemaining = hrWorkload;
        organizationUpgradeHrAllocation = hrAllocation;
        organizationUpgradeStrategyWorkRemaining = strategyWorkload;
        organizationUpgradeStrategyAllocation = strategyAllocation;
    }

    public void advanceOrganizationUpgrade(long installment, int hrWork, int strategyWork) {
        if (pendingOrganizationSystem == null || getOrganizationUpgradeMonthsRemaining() <= 0
                || installment < 0 || installment > getOrganizationUpgradeRemainingCost()
                || hrWork < 0 || strategyWork < 0
                || !spendCorporateCash(installment)) {
            throw new IllegalStateException("조직관리 시스템 구축비를 지급할 수 없습니다.");
        }
        organizationUpgradeRemainingCost = getOrganizationUpgradeRemainingCost() - installment;
        organizationUpgradeMonthsRemaining = getOrganizationUpgradeMonthsRemaining() - 1;
        organizationUpgradeHrWorkRemaining = Math.max(
                0, getOrganizationUpgradeHrWorkRemaining() - hrWork);
        organizationUpgradeStrategyWorkRemaining = Math.max(
                0, getOrganizationUpgradeStrategyWorkRemaining() - strategyWork);
        if (getOrganizationUpgradeMonthsRemaining() == 0
                && getOrganizationUpgradeRemainingCost() == 0
                && getOrganizationUpgradeHrWorkRemaining() == 0
                && getOrganizationUpgradeStrategyWorkRemaining() == 0) {
            organizationSystem = pendingOrganizationSystem;
            pendingOrganizationSystem = null;
            clearOrganizationUpgradeWork();
        }
    }

    public void advanceDelayedOrganizationUpgrade(int hrWork, int strategyWork) {
        if (pendingOrganizationSystem == null || getOrganizationUpgradeMonthsRemaining() != 0
                || getOrganizationUpgradeRemainingCost() != 0
                || hrWork < 0 || strategyWork < 0) {
            throw new IllegalStateException("조직관리 시스템 지연 작업을 진행할 수 없습니다.");
        }
        organizationUpgradeHrWorkRemaining = Math.max(
                0, getOrganizationUpgradeHrWorkRemaining() - hrWork);
        organizationUpgradeStrategyWorkRemaining = Math.max(
                0, getOrganizationUpgradeStrategyWorkRemaining() - strategyWork);
        if (getOrganizationUpgradeHrWorkRemaining() == 0
                && getOrganizationUpgradeStrategyWorkRemaining() == 0) {
            organizationSystem = pendingOrganizationSystem;
            pendingOrganizationSystem = null;
            clearOrganizationUpgradeWork();
        }
    }

    private void clearOrganizationUpgradeWork() {
        organizationUpgradeHrWorkRemaining = 0;
        organizationUpgradeHrAllocation = 0;
        organizationUpgradeStrategyWorkRemaining = 0;
        organizationUpgradeStrategyAllocation = 0;
    }

    public void toggleAutomaticHiring() {
        if (getOrganizationSystem() == CompanyOrganizationSystem.MANUAL) {
            throw new IllegalStateException("채용·급여 시스템 구축 후 자동채용을 사용할 수 있습니다.");
        }
        automaticHiringEnabled = !isAutomaticHiringEnabled();
    }

    public void reserveMajorWork(int companySlotLimit) {
        if (getActiveMajorWorkCount() >= companySlotLimit) {
            throw new IllegalStateException("회사의 동시 주요 업무 슬롯이 부족합니다");
        }
        activeMajorWorkCount = getActiveMajorWorkCount() + 1;
    }

    public void releaseMajorWork() {
        if (getActiveMajorWorkCount() <= 0) {
            throw new IllegalStateException("해제할 회사 주요 업무가 없습니다");
        }
        activeMajorWorkCount = getActiveMajorWorkCount() - 1;
    }
}
