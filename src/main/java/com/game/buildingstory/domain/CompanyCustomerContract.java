package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 기업 고객의 구축 프로젝트와 구축 완료 후 이용 계약을 하나의 생명주기로 보존한다.
 *
 * <p>모든 계약은 구축비를 계약금 30%와 완료금 70%로 나누어 받고, 구축이 끝나면
 * 정해진 기간 동안 월 이용료와 유지비를 발생시킨다. 일반계약 이상은 납기와
 * SLA 위반에 따른 감면·위약금·중도 종료·갱신 조건도 함께 보존한다.</p>
 */
@Entity
@Table(name = "company_customer_contract")
public class CompanyCustomerContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String catalogKey;
    private String clientName;
    private String projectName;

    @Enumerated(EnumType.STRING)
    private CompanyCustomerContractType contractType;

    @Enumerated(EnumType.STRING)
    private CompanyCustomerContractStatus status;

    private int offeredMarketMonth;
    private int expiresMarketMonth;
    private int requiredBenchmark;
    private int requiredStability;
    private int requiredSecurity;
    private int totalDevelopmentWork;
    private int totalOperationsWork;
    private int remainingDevelopmentWork;
    private int remainingOperationsWork;
    private int assignedDevelopmentWork;
    private int assignedOperationsWork;
    private long constructionFee;
    private long remainingBuildCost;
    private long plannedMonthlyBuildCost;
    private long monthlyFee;
    private int contractDurationMonths;
    private int activeMonthsProcessed;
    private boolean depositRecognized;
    private Integer buildDeadlineMonths;
    private Integer buildMonthsProcessed;
    private Integer delayMonths;
    private Integer slaViolations;
    private Long pendingPenalty;
    private Long pendingSlaCredit;

    protected CompanyCustomerContract() {
    }

    public CompanyCustomerContract(
            PlayerCompany company,
            String catalogKey,
            String clientName,
            String projectName,
            CompanyCustomerContractType contractType,
            int offeredMarketMonth,
            int requiredBenchmark,
            int requiredStability,
            int requiredSecurity,
            int developmentWork,
            int operationsWork,
            long constructionFee,
            long totalBuildCost,
            long monthlyFee,
            int contractDurationMonths,
            int buildDeadlineMonths
    ) {
        this.company = company;
        this.catalogKey = catalogKey;
        this.clientName = clientName;
        this.projectName = projectName;
        this.contractType = contractType;
        this.status = CompanyCustomerContractStatus.OFFERED;
        this.offeredMarketMonth = offeredMarketMonth;
        this.expiresMarketMonth = offeredMarketMonth + 3;
        this.requiredBenchmark = requiredBenchmark;
        this.requiredStability = requiredStability;
        this.requiredSecurity = requiredSecurity;
        this.totalDevelopmentWork = developmentWork;
        this.totalOperationsWork = operationsWork;
        this.remainingDevelopmentWork = developmentWork;
        this.remainingOperationsWork = operationsWork;
        this.constructionFee = constructionFee;
        this.remainingBuildCost = totalBuildCost;
        this.monthlyFee = monthlyFee;
        this.contractDurationMonths = contractDurationMonths;
        this.buildDeadlineMonths = buildDeadlineMonths;
        this.buildMonthsProcessed = 0;
        this.delayMonths = 0;
        this.slaViolations = 0;
        this.pendingPenalty = 0L;
        this.pendingSlaCredit = 0L;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getCatalogKey() { return catalogKey; }
    public String getClientName() { return clientName; }
    public String getProjectName() { return projectName; }
    public CompanyCustomerContractType getContractType() {
        return contractType == null ? CompanyCustomerContractType.TRIAL : contractType;
    }
    public CompanyCustomerContractStatus getStatus() { return status; }
    public int getOfferedMarketMonth() { return offeredMarketMonth; }
    public int getExpiresMarketMonth() { return expiresMarketMonth; }
    public int getRequiredBenchmark() { return requiredBenchmark; }
    public int getRequiredStability() { return requiredStability; }
    public int getRequiredSecurity() { return requiredSecurity; }
    public int getRemainingDevelopmentWork() { return remainingDevelopmentWork; }
    public int getRemainingOperationsWork() { return remainingOperationsWork; }
    public int getAssignedDevelopmentWork() { return assignedDevelopmentWork; }
    public int getAssignedOperationsWork() { return assignedOperationsWork; }
    public long getConstructionFee() { return constructionFee; }
    public long getMonthlyFee() { return monthlyFee; }
    public int getContractDurationMonths() { return contractDurationMonths; }
    public int getActiveMonthsProcessed() { return activeMonthsProcessed; }
    public int getBuildDeadlineMonths() { return buildDeadlineMonths == null ? Integer.MAX_VALUE : buildDeadlineMonths; }
    public int getBuildMonthsProcessed() { return buildMonthsProcessed == null ? 0 : buildMonthsProcessed; }
    public int getDelayMonths() { return delayMonths == null ? 0 : delayMonths; }
    public int getSlaViolations() { return slaViolations == null ? 0 : slaViolations; }

    public void accept(int developmentWork, int operationsWork) {
        if (status != CompanyCustomerContractStatus.OFFERED
                || developmentWork <= 0 || operationsWork <= 0) {
            throw new IllegalStateException("수락 가능한 고객 계약이 아닙니다");
        }
        status = CompanyCustomerContractStatus.BUILDING;
        assignedDevelopmentWork = developmentWork;
        assignedOperationsWork = operationsWork;
        int expectedMonths = Math.max(
                (int) Math.ceil(remainingDevelopmentWork / (double) developmentWork),
                (int) Math.ceil(remainingOperationsWork / (double) operationsWork)
        );
        plannedMonthlyBuildCost = (long) Math.ceil(remainingBuildCost / (double) expectedMonths);
    }

    public long currentRevenue() {
        return switch (status) {
            case BUILDING -> depositRecognized ? 0 : constructionFee * 30 / 100;
            case COMPLETION_PAYMENT -> constructionFee - constructionFee * 30 / 100;
            case ACTIVE -> Math.max(0, monthlyFee - getPendingSlaCredit());
            default -> 0;
        };
    }

    public long currentCost() {
        return switch (status) {
            case BUILDING -> Math.addExact(
                    Math.min(remainingBuildCost, plannedMonthlyBuildCost), getPendingPenalty());
            case ACTIVE -> monthlyFee * 15 / 100;
            case CONTRACT_FAILURE_PAYMENT, TERMINATION_PAYMENT -> getPendingPenalty();
            default -> 0;
        };
    }

    public boolean advanceBuildMonth(boolean inspectionPassed) {
        return advanceBuildMonth(inspectionPassed, assignedDevelopmentWork, assignedOperationsWork);
    }

    public boolean advanceBuildMonth(
            boolean inspectionPassed,
            int completedDevelopmentWork,
            int completedOperationsWork
    ) {
        if (status != CompanyCustomerContractStatus.BUILDING) {
            return false;
        }
        depositRecognized = true;
        pendingPenalty = 0L;
        remainingBuildCost = Math.max(0,
                remainingBuildCost - Math.min(remainingBuildCost, plannedMonthlyBuildCost));
        remainingDevelopmentWork = Math.max(0, remainingDevelopmentWork - Math.max(0, completedDevelopmentWork));
        remainingOperationsWork = Math.max(0, remainingOperationsWork - Math.max(0, completedOperationsWork));
        buildMonthsProcessed = getBuildMonthsProcessed() + 1;
        boolean workCompleted = remainingDevelopmentWork == 0 && remainingOperationsWork == 0;
        if (workCompleted && inspectionPassed) {
            status = CompanyCustomerContractStatus.COMPLETION_PAYMENT;
            return true;
        }
        if (getContractType().hasRiskRules() && getBuildMonthsProcessed() > getBuildDeadlineMonths()) {
            delayMonths = getDelayMonths() + 1;
            if (getDelayMonths() >= 3) {
                pendingPenalty = constructionFee * 50 / 100;
                status = CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT;
            } else {
                pendingPenalty = constructionFee * 10 / 100;
            }
        }
        return false;
    }

    public void activate() {
        if (status != CompanyCustomerContractStatus.COMPLETION_PAYMENT) {
            throw new IllegalStateException("구축 완료금 정산 단계가 아닙니다");
        }
        status = CompanyCustomerContractStatus.ACTIVE;
    }

    public void advanceActiveMonth(boolean slaViolation) {
        if (status != CompanyCustomerContractStatus.ACTIVE) {
            return;
        }
        pendingSlaCredit = 0L;
        if (getContractType().hasRiskRules() && slaViolation) {
            slaViolations = getSlaViolations() + 1;
            if (getSlaViolations() >= 2) {
                pendingPenalty = monthlyFee * 2;
                status = CompanyCustomerContractStatus.TERMINATION_PAYMENT;
                return;
            }
            pendingSlaCredit = monthlyFee;
        }
        activeMonthsProcessed++;
        if (activeMonthsProcessed >= contractDurationMonths) {
            status = getContractType() == CompanyCustomerContractType.TRIAL
                    ? CompanyCustomerContractStatus.COMPLETED
                    : CompanyCustomerContractStatus.RENEWAL_OFFERED;
        }
    }

    public void renew() {
        if (status != CompanyCustomerContractStatus.RENEWAL_OFFERED) {
            throw new IllegalStateException("갱신 가능한 계약이 아닙니다");
        }
        if (getSlaViolations() == 1) {
            monthlyFee = monthlyFee * 90 / 100;
        }
        activeMonthsProcessed = 0;
        slaViolations = 0;
        pendingSlaCredit = 0L;
        status = CompanyCustomerContractStatus.ACTIVE;
    }

    public void declineRenewal() {
        if (status != CompanyCustomerContractStatus.RENEWAL_OFFERED) {
            throw new IllegalStateException("갱신 대기 계약이 아닙니다");
        }
        status = CompanyCustomerContractStatus.COMPLETED;
    }

    public void settleTerminalPayment() {
        if (status == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT) {
            pendingPenalty = 0L;
            status = CompanyCustomerContractStatus.FAILED;
        } else if (status == CompanyCustomerContractStatus.TERMINATION_PAYMENT) {
            pendingPenalty = 0L;
            status = CompanyCustomerContractStatus.TERMINATED;
        }
    }

    public void expire(int currentMarketMonth) {
        if (status == CompanyCustomerContractStatus.OFFERED && currentMarketMonth >= expiresMarketMonth) {
            status = CompanyCustomerContractStatus.EXPIRED;
        }
    }

    public void rejectOffer() {
        if (status != CompanyCustomerContractStatus.OFFERED) {
            throw new IllegalStateException("거절 가능한 고객계약 제안이 아닙니다");
        }
        status = CompanyCustomerContractStatus.REJECTED;
    }

    public void failBuildForCashDepletion() {
        if (status == CompanyCustomerContractStatus.BUILDING
                || status == CompanyCustomerContractStatus.COMPLETION_PAYMENT) {
            status = CompanyCustomerContractStatus.FAILED;
        }
    }

    public int buildProgressPercent() {
        int totalWork = totalDevelopmentWork + totalOperationsWork;
        if (status != CompanyCustomerContractStatus.BUILDING || totalWork == 0) {
            return status == CompanyCustomerContractStatus.OFFERED ? 0 : 100;
        }
        return Math.max(0, Math.min(99,
                100 - (remainingDevelopmentWork + remainingOperationsWork) * 100 / totalWork));
    }

    private long getPendingPenalty() { return pendingPenalty == null ? 0 : pendingPenalty; }
    private long getPendingSlaCredit() { return pendingSlaCredit == null ? 0 : pendingSlaCredit; }
}
