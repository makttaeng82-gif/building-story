package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 플레이어 법인의 부서별 집계 인력 상태다. */
@Entity
@Table(name = "company_department", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_department_type", columnNames = {"company_id", "department_type"}))
public class CompanyDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(40)")
    private CompanyDepartmentType departmentType;

    private int generalEmployeeCount;
    private Integer averageGeneralSkill;
    private Integer approvedHeadcount;
    private Integer pendingHireCount;
    private Integer adaptingEmployeeCount;
    private Integer activeMajorWorkCount;
    private Integer allocatedMajorWorkload;
    private Double generalResignationCarry;
    private Integer lastGeneralResignations;
    private Double lastGeneralResignationRate;

    protected CompanyDepartment() {
    }

    public CompanyDepartment(PlayerCompany company, CompanyDepartmentType departmentType) {
        this.company = company;
        this.departmentType = departmentType;
        this.averageGeneralSkill = 50;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public CompanyDepartmentType getDepartmentType() { return departmentType; }
    public int getGeneralEmployeeCount() { return generalEmployeeCount; }
    public int getAverageGeneralSkill() { return averageGeneralSkill == null ? 50 : averageGeneralSkill; }
    public int getApprovedHeadcount() { return approvedHeadcount == null ? generalEmployeeCount : approvedHeadcount; }
    public int getPendingHireCount() { return pendingHireCount == null ? 0 : pendingHireCount; }
    public int getAdaptingEmployeeCount() { return adaptingEmployeeCount == null ? 0 : adaptingEmployeeCount; }
    public int getActiveMajorWorkCount() { return activeMajorWorkCount == null ? 0 : activeMajorWorkCount; }
    public int getAllocatedMajorWorkload() { return allocatedMajorWorkload == null ? 0 : allocatedMajorWorkload; }
    public int getLastGeneralResignations() { return lastGeneralResignations == null ? 0 : lastGeneralResignations; }
    public double getLastGeneralResignationRate() { return lastGeneralResignationRate == null ? 0.0 : lastGeneralResignationRate; }

    public void assignFoundingWorkforce(int count) {
        if (generalEmployeeCount != 0) {
            throw new IllegalStateException("창업 일반인력은 한 번만 배치할 수 있습니다");
        }
        generalEmployeeCount = count;
        approvedHeadcount = count;
        pendingHireCount = 0;
        adaptingEmployeeCount = 0;
        activeMajorWorkCount = 0;
        allocatedMajorWorkload = 0;
    }

    public void changeApprovedHeadcount(int count) {
        if (count < generalEmployeeCount + getPendingHireCount()) {
            throw new IllegalArgumentException("승인 정원은 재직자와 입사 대기 인원보다 적을 수 없습니다");
        }
        approvedHeadcount = count;
    }

    public void requestHires(int count) {
        if (count <= 0 || generalEmployeeCount + getPendingHireCount() + count > getApprovedHeadcount()) {
            throw new IllegalArgumentException("승인 정원 안에서 1명 이상 채용해야 합니다");
        }
        pendingHireCount = Math.addExact(getPendingHireCount(), count);
    }

    /** 지난달 적응 인력은 정상화하고, 대기 중인 채용자는 이번 달 적응 인력으로 입사시킨다. */
    public int advanceHiringMonth() {
        int onboarded = getPendingHireCount();
        adaptingEmployeeCount = onboarded;
        generalEmployeeCount = Math.addExact(generalEmployeeCount, onboarded);
        pendingHireCount = 0;
        return onboarded;
    }

    /** 일반인력을 개인화하지 않고 기대 퇴사 인원을 누적해 정수 인원만 월별로 차감한다. */
    public int applyGeneralAttrition(double monthlyRate) {
        double boundedRate = Math.max(0.001, Math.min(0.02, monthlyRate));
        double expectedResignations = (generalResignationCarry == null ? 0.0 : generalResignationCarry)
                + generalEmployeeCount * boundedRate;
        int resignations = Math.min(generalEmployeeCount, (int) Math.floor(expectedResignations));
        generalResignationCarry = expectedResignations - resignations;
        generalEmployeeCount -= resignations;
        adaptingEmployeeCount = Math.min(getAdaptingEmployeeCount(), generalEmployeeCount);
        lastGeneralResignations = resignations;
        lastGeneralResignationRate = boundedRate;
        return resignations;
    }

    /** 인사조직팀의 교육 효과를 부서 단위 평균숙련에 반영한다. */
    public void improveAverageGeneralSkill(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("숙련도 증가량은 1 이상이어야 합니다");
        }
        averageGeneralSkill = Math.min(100, getAverageGeneralSkill() + amount);
    }

    public void reserveMajorWork(int workload, int slotLimit, int maximumWorkload) {
        if (workload <= 0) {
            throw new IllegalArgumentException("업무량은 1 이상이어야 합니다");
        }
        if (getActiveMajorWorkCount() >= slotLimit) {
            throw new IllegalStateException("부서의 동시 주요 업무 슬롯이 부족합니다");
        }
        if (getAllocatedMajorWorkload() + workload > maximumWorkload) {
            throw new IllegalStateException("예상 가동률이 130%를 넘어 신규 업무를 시작할 수 없습니다");
        }
        activeMajorWorkCount = getActiveMajorWorkCount() + 1;
        allocatedMajorWorkload = Math.addExact(getAllocatedMajorWorkload(), workload);
    }

    public void releaseMajorWork(int workload) {
        if (workload <= 0 || getActiveMajorWorkCount() <= 0 || workload > getAllocatedMajorWorkload()) {
            throw new IllegalArgumentException("해제할 주요 업무가 올바르지 않습니다");
        }
        activeMajorWorkCount = getActiveMajorWorkCount() - 1;
        allocatedMajorWorkload = getAllocatedMajorWorkload() - workload;
    }
}
