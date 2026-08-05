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

/** 핵심제품 개선의 작업량, 월 투입량과 완료 결과를 저장한다. */
@Entity
@Table(name = "company_product_project")
public class CompanyProductProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Enumerated(EnumType.STRING)
    private CompanyProductImprovementType improvementType;

    @Enumerated(EnumType.STRING)
    private CompanyDevelopmentDirection developmentDirection;

    @Enumerated(EnumType.STRING)
    private CompanyProductProjectStatus status;

    private int totalWork;
    private int remainingWork;
    private int monthlyAssignedWork;
    private int startedElapsedDay;
    private Integer completedElapsedDay;
    private Integer completionQuality;

    protected CompanyProductProject() {
    }

    public CompanyProductProject(
            PlayerCompany company,
            CompanyProductImprovementType improvementType,
            CompanyDevelopmentDirection developmentDirection,
            int totalWork,
            int monthlyAssignedWork,
            int startedElapsedDay
    ) {
        if (monthlyAssignedWork <= 0) {
            throw new IllegalArgumentException("월 투입 업무량은 1 이상이어야 합니다");
        }
        this.company = company;
        this.improvementType = improvementType;
        this.developmentDirection = developmentDirection;
        this.status = CompanyProductProjectStatus.ACTIVE;
        this.totalWork = totalWork;
        this.remainingWork = totalWork;
        this.monthlyAssignedWork = monthlyAssignedWork;
        this.startedElapsedDay = startedElapsedDay;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public CompanyProductImprovementType getImprovementType() { return improvementType; }
    public CompanyDevelopmentDirection getDevelopmentDirection() {
        return developmentDirection == null ? CompanyDevelopmentDirection.BALANCED : developmentDirection;
    }
    public CompanyProductProjectStatus getStatus() { return status; }
    public int getTotalWork() { return totalWork; }
    public int getRemainingWork() { return remainingWork; }
    public int getMonthlyAssignedWork() { return monthlyAssignedWork; }
    public int getStartedElapsedDay() { return startedElapsedDay; }
    public Integer getCompletedElapsedDay() { return completedElapsedDay; }
    public Integer getCompletionQuality() { return completionQuality; }

    public int progressPercent() {
        return totalWork == 0 ? 100 : (int) Math.round((totalWork - remainingWork) * 100.0 / totalWork);
    }

    public boolean advanceMonth() {
        return advanceMonth(monthlyAssignedWork);
    }

    public boolean advanceMonth(int completedWork) {
        if (status != CompanyProductProjectStatus.ACTIVE) {
            return false;
        }
        if (completedWork <= 0) {
            throw new IllegalArgumentException("월 완료 작업량은 1 이상이어야 합니다.");
        }
        remainingWork = Math.max(0, remainingWork - completedWork);
        return remainingWork == 0;
    }

    public void complete(int elapsedDay, int quality) {
        if (status != CompanyProductProjectStatus.ACTIVE || remainingWork != 0) {
            throw new IllegalStateException("완료 조건을 충족하지 않은 제품 프로젝트입니다");
        }
        status = CompanyProductProjectStatus.COMPLETED;
        completedElapsedDay = elapsedDay;
        completionQuality = quality;
    }

    public void cancel(int elapsedDay) {
        finishWithoutResult(CompanyProductProjectStatus.CANCELLED, elapsedDay);
    }

    public void fail(int elapsedDay) {
        finishWithoutResult(CompanyProductProjectStatus.FAILED, elapsedDay);
    }

    private void finishWithoutResult(CompanyProductProjectStatus result, int elapsedDay) {
        if (status != CompanyProductProjectStatus.ACTIVE) {
            throw new IllegalStateException("진행 중인 제품 프로젝트가 아닙니다");
        }
        status = result;
        completedElapsedDay = elapsedDay;
    }
}
