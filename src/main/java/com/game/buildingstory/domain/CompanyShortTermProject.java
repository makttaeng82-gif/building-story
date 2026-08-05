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
 * 단기 사업의 제안, 수행, 3개월 수익 일정을 하나의 생명주기로 보존한다.
 *
 * <p>일반 실무인력처럼 집계해도 되는 정보가 아니라 플레이어가 직접 수락한 사업이므로
 * 개별 엔티티로 저장한다. 완료 후에는 프로젝트를 다시 진행시키지 않고 수익 일정만 소비한다.</p>
 */
@Entity
@Table(name = "company_short_term_project")
public class CompanyShortTermProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String catalogKey;
    private String name;

    @Enumerated(EnumType.STRING)
    private CompanyShortTermProjectStatus status;

    private int offeredMarketMonth;
    private int expiresMarketMonth;
    private int totalWork;
    private int remainingWork;
    private int monthlyAssignedWork;
    private long totalCost;
    private long remainingCost;
    private long plannedMonthlyCost;
    private long firstMonthRevenue;
    private int revenueMonthsProcessed;
    private Integer completionQuality;
    @Enumerated(EnumType.STRING)
    private CompanyShortTermProjectRisk risk;
    @Enumerated(EnumType.STRING)
    private CompanyShortTermProjectRiskResolution riskResolution;
    private Boolean riskDecisionRequired;
    private Long pendingRiskCost;
    private Integer revenueMultiplierPercent;

    protected CompanyShortTermProject() {
    }

    public CompanyShortTermProject(
            PlayerCompany company,
            String catalogKey,
            String name,
            int offeredMarketMonth,
            int totalWork,
            long totalCost,
            long firstMonthRevenue
    ) {
        this.company = company;
        this.catalogKey = catalogKey;
        this.name = name;
        this.status = CompanyShortTermProjectStatus.OFFERED;
        this.offeredMarketMonth = offeredMarketMonth;
        this.expiresMarketMonth = offeredMarketMonth + 2;
        this.totalWork = totalWork;
        this.remainingWork = totalWork;
        this.totalCost = totalCost;
        this.remainingCost = totalCost;
        this.firstMonthRevenue = firstMonthRevenue;
        this.revenueMonthsProcessed = 0;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getCatalogKey() { return catalogKey; }
    public String getName() { return name; }
    public CompanyShortTermProjectStatus getStatus() { return status; }
    public int getOfferedMarketMonth() { return offeredMarketMonth; }
    public int getExpiresMarketMonth() { return expiresMarketMonth; }
    public int getTotalWork() { return totalWork; }
    public int getRemainingWork() { return remainingWork; }
    public int getMonthlyAssignedWork() { return monthlyAssignedWork; }
    public long getTotalCost() { return totalCost; }
    public long getFirstMonthRevenue() { return firstMonthRevenue; }
    public int getRevenueMonthsProcessed() { return revenueMonthsProcessed; }
    public Integer getCompletionQuality() { return completionQuality; }
    public CompanyShortTermProjectRisk getRisk() {
        return risk == null ? CompanyShortTermProjectRisk.NONE : risk;
    }
    public CompanyShortTermProjectRiskResolution getRiskResolution() { return riskResolution; }
    public boolean isRiskDecisionRequired() { return Boolean.TRUE.equals(riskDecisionRequired); }
    public long getPendingRiskCost() { return pendingRiskCost == null ? 0 : pendingRiskCost; }
    public int getRevenueMultiplierPercent() {
        return revenueMultiplierPercent == null ? 100 : revenueMultiplierPercent;
    }

    public void accept(int assignedWork) {
        if (status != CompanyShortTermProjectStatus.OFFERED || assignedWork <= 0) {
            throw new IllegalStateException("수락 가능한 단기 사업이 아닙니다");
        }
        status = CompanyShortTermProjectStatus.ACTIVE;
        monthlyAssignedWork = assignedWork;
        int expectedMonths = (int) Math.ceil(totalWork / (double) assignedWork);
        plannedMonthlyCost = (long) Math.ceil(totalCost / (double) expectedMonths);
    }

    public long currentMonthlyCost() {
        if (status == CompanyShortTermProjectStatus.ACTIVE) {
            return Math.min(remainingCost, plannedMonthlyCost);
        }
        return status == CompanyShortTermProjectStatus.EARNING ? getPendingRiskCost() : 0;
    }

    public long currentRevenue() {
        if (status != CompanyShortTermProjectStatus.EARNING || revenueMonthsProcessed >= 3) {
            return 0;
        }
        int percent = switch (revenueMonthsProcessed) {
            case 0 -> 100;
            case 1 -> 70;
            default -> 40;
        };
        int quality = completionQuality == null ? 50 : completionQuality;
        double qualityMultiplier = 0.75 + quality / 400.0;
        return Math.round(firstMonthRevenue * percent / 100.0
                * qualityMultiplier * getRevenueMultiplierPercent() / 100.0);
    }

    public boolean advanceWorkMonth(int quality) {
        return advanceWorkMonth(quality, monthlyAssignedWork);
    }

    public boolean advanceWorkMonth(int quality, int completedWork) {
        if (status != CompanyShortTermProjectStatus.ACTIVE) {
            return false;
        }
        remainingCost = Math.max(0, remainingCost - currentMonthlyCost());
        if (completedWork <= 0) {
            return false;
        }
        remainingWork = Math.max(0, remainingWork - completedWork);
        if (remainingWork > 0) {
            return false;
        }
        completionQuality = Math.max(50, Math.min(100, quality));
        status = CompanyShortTermProjectStatus.EARNING;
        return true;
    }

    public void applyPostCompletionRisk(CompanyShortTermProjectRisk selectedRisk) {
        if (status != CompanyShortTermProjectStatus.EARNING || selectedRisk == null) {
            throw new IllegalStateException("완료된 단기 사업에만 사후 위험을 적용할 수 있습니다");
        }
        risk = selectedRisk;
        revenueMultiplierPercent = 100;
        pendingRiskCost = 0L;
        riskDecisionRequired = false;
        if (selectedRisk == CompanyShortTermProjectRisk.AFTER_SERVICE) {
            pendingRiskCost = Math.max(100_000_000L, firstMonthRevenue / 10);
        } else if (selectedRisk == CompanyShortTermProjectRisk.REFUND_REQUEST) {
            riskDecisionRequired = true;
        } else if (selectedRisk == CompanyShortTermProjectRisk.TREND_FADE) {
            revenueMultiplierPercent = 60;
        }
    }

    public long extraSupportCost() {
        return Math.max(100_000_000L, firstMonthRevenue * 15 / 100);
    }

    public void resolveRisk(CompanyShortTermProjectRiskResolution selectedResolution) {
        if (!riskDecisionRequired || getRisk() != CompanyShortTermProjectRisk.REFUND_REQUEST) {
            throw new IllegalStateException("대응을 선택할 수 있는 환불 요구가 아닙니다");
        }
        riskResolution = selectedResolution;
        riskDecisionRequired = false;
        if (selectedResolution == CompanyShortTermProjectRiskResolution.PARTIAL_REFUND) {
            revenueMultiplierPercent = 70;
        }
    }

    public void clearPendingRiskCost() {
        pendingRiskCost = 0L;
    }

    public boolean advanceRevenueMonth() {
        if (status != CompanyShortTermProjectStatus.EARNING || riskDecisionRequired) {
            return false;
        }
        revenueMonthsProcessed++;
        if (revenueMonthsProcessed >= 3) {
            status = CompanyShortTermProjectStatus.COMPLETED;
            return true;
        }
        return false;
    }

    public void expire(int currentMarketMonth) {
        if (status == CompanyShortTermProjectStatus.OFFERED && currentMarketMonth >= expiresMarketMonth) {
            status = CompanyShortTermProjectStatus.EXPIRED;
        }
    }

    public void reject() {
        if (status != CompanyShortTermProjectStatus.OFFERED) {
            throw new IllegalStateException("거절 가능한 단기 사업 제안이 아닙니다");
        }
        status = CompanyShortTermProjectStatus.REJECTED;
    }

    public void cancel() {
        if (status != CompanyShortTermProjectStatus.ACTIVE) {
            throw new IllegalStateException("취소 가능한 단기 사업이 아닙니다");
        }
        status = CompanyShortTermProjectStatus.CANCELLED;
    }

    public void fail() {
        if (status != CompanyShortTermProjectStatus.ACTIVE
                && status != CompanyShortTermProjectStatus.EARNING) {
            return;
        }
        status = CompanyShortTermProjectStatus.FAILED;
        riskDecisionRequired = false;
        pendingRiskCost = 0L;
    }

    public int progressPercent() {
        return totalWork == 0 ? 100 : (int) Math.round((totalWork - remainingWork) * 100.0 / totalWork);
    }
}
