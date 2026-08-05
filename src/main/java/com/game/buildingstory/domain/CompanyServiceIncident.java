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

/** 제품 상태에서 실제로 발생한 서비스 장애와 대응 진행상태를 보존한다. */
@Entity
@Table(name = "company_service_incident")
public class CompanyServiceIncident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Enumerated(EnumType.STRING)
    private CompanyServiceIncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    private CompanyServiceIncidentStatus status;

    @Enumerated(EnumType.STRING)
    private CompanyServiceIncidentResolution resolution;

    private boolean securityIncident;
    private Boolean computeIncident;
    private int occurredMarketMonth;
    private int probabilityBasisPoints;
    private long fullResponseCost;
    private int totalDevelopmentWork;
    private int totalOperationsWork;
    private int remainingDevelopmentWork;
    private int remainingOperationsWork;
    private int assignedDevelopmentWork;
    private int assignedOperationsWork;
    private long monthlyResponseCost;

    protected CompanyServiceIncident() {
    }

    public CompanyServiceIncident(
            PlayerCompany company,
            CompanyServiceIncidentSeverity severity,
            boolean securityIncident,
            int occurredMarketMonth,
            int probabilityBasisPoints,
            long fullResponseCost,
            int developmentWork,
            int operationsWork,
            long monthlyResponseCost
    ) {
        this(company, severity, securityIncident, false, occurredMarketMonth,
                probabilityBasisPoints, fullResponseCost, developmentWork,
                operationsWork, monthlyResponseCost);
    }

    public CompanyServiceIncident(
            PlayerCompany company,
            CompanyServiceIncidentSeverity severity,
            boolean securityIncident,
            boolean computeIncident,
            int occurredMarketMonth,
            int probabilityBasisPoints,
            long fullResponseCost,
            int developmentWork,
            int operationsWork,
            long monthlyResponseCost
    ) {
        this.company = company;
        this.severity = severity;
        this.securityIncident = securityIncident;
        this.computeIncident = computeIncident;
        this.occurredMarketMonth = occurredMarketMonth;
        this.probabilityBasisPoints = probabilityBasisPoints;
        this.fullResponseCost = fullResponseCost;
        this.totalDevelopmentWork = developmentWork;
        this.totalOperationsWork = operationsWork;
        this.remainingDevelopmentWork = developmentWork;
        this.remainingOperationsWork = operationsWork;
        this.monthlyResponseCost = monthlyResponseCost;
        this.status = switch (severity) {
            case MINOR -> CompanyServiceIncidentStatus.RESOLVED;
            case MAJOR -> CompanyServiceIncidentStatus.AWAITING_DECISION;
            case CRITICAL -> CompanyServiceIncidentStatus.RESPONSE_REQUIRED;
        };
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public CompanyServiceIncidentSeverity getSeverity() { return severity; }
    public CompanyServiceIncidentStatus getStatus() { return status; }
    public CompanyServiceIncidentResolution getResolution() { return resolution; }
    public boolean isSecurityIncident() { return securityIncident; }
    public boolean isComputeIncident() { return Boolean.TRUE.equals(computeIncident); }
    public int getOccurredMarketMonth() { return occurredMarketMonth; }
    public int getProbabilityBasisPoints() { return probabilityBasisPoints; }
    public long getFullResponseCost() { return fullResponseCost; }
    public int getRemainingDevelopmentWork() { return remainingDevelopmentWork; }
    public int getRemainingOperationsWork() { return remainingOperationsWork; }
    public int getAssignedDevelopmentWork() { return assignedDevelopmentWork; }
    public int getAssignedOperationsWork() { return assignedOperationsWork; }

    public long resolutionCost(CompanyServiceIncidentResolution selected) {
        return selected == CompanyServiceIncidentResolution.EMERGENCY_RECOVERY
                ? fullResponseCost : fullResponseCost / 2;
    }

    public void resolveMajor(CompanyServiceIncidentResolution selected) {
        if (status != CompanyServiceIncidentStatus.AWAITING_DECISION) {
            throw new IllegalStateException("대응을 선택할 수 있는 중대 장애가 아닙니다");
        }
        resolution = selected;
        status = CompanyServiceIncidentStatus.RESOLVED;
    }

    public void startCriticalResponse(int developmentWork, int operationsWork) {
        if (status != CompanyServiceIncidentStatus.RESPONSE_REQUIRED
                || developmentWork <= 0 || operationsWork <= 0) {
            throw new IllegalStateException("긴급 대응을 시작할 수 없습니다");
        }
        assignedDevelopmentWork = developmentWork;
        assignedOperationsWork = operationsWork;
        status = CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS;
    }

    public boolean advanceResponseMonth() {
        if (status != CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS) {
            return false;
        }
        remainingDevelopmentWork = Math.max(0, remainingDevelopmentWork - assignedDevelopmentWork);
        remainingOperationsWork = Math.max(0, remainingOperationsWork - assignedOperationsWork);
        if (remainingDevelopmentWork == 0 && remainingOperationsWork == 0) {
            status = CompanyServiceIncidentStatus.COMPLETED;
            return true;
        }
        return false;
    }

    public void failResponse() {
        if (status == CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS) {
            status = CompanyServiceIncidentStatus.FAILED;
        }
    }

    public long currentMonthlyCost() {
        return status == CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS ? monthlyResponseCost : 0;
    }

    public boolean hasCustomerImpact() {
        return status == CompanyServiceIncidentStatus.AWAITING_DECISION
                || status == CompanyServiceIncidentStatus.RESPONSE_REQUIRED
                || status == CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS;
    }

    public int progressPercent() {
        int total = totalDevelopmentWork + totalOperationsWork;
        if (total == 0) {
            return status == CompanyServiceIncidentStatus.COMPLETED ? 100 : 0;
        }
        return Math.max(0, Math.min(100,
                100 - (remainingDevelopmentWork + remainingOperationsWork) * 100 / total));
    }
}
