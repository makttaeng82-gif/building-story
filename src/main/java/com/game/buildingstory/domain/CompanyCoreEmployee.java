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

/** 플레이어가 직접 선택한 핵심 인재의 최초 고용 상태다. */
@Entity
@Table(name = "company_core_employee", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_core_employee_candidate", columnNames = {"company_id", "candidate_key"}))
public class CompanyCoreEmployee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String candidateKey;
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(40)")
    private CompanyDepartmentType departmentType;

    private int grade;
    private int ability;
    @Column(name = "growth_potential")
    private int growthSpeed;
    private int organizationFit;
    private Integer leadership;
    private long annualSalary;
    private Double experience;
    private Boolean teamLeader;
    private Integer pendingHireMonths;
    private Long expectedAnnualSalary;
    private Boolean resigned;
    private Integer resignedElapsedDay;
    private Integer resignationRisk;
    private Integer resignationNoticeMonths;
    private Integer consecutiveOverloadMonths;
    private Integer retentionAgreementExpiresDay;
    private Integer trainingMonthsRemaining;
    private Integer trainingCooldownUntilElapsedDay;
    private Integer salaryReviewYearsApplied;

    protected CompanyCoreEmployee() {
    }

    public CompanyCoreEmployee(PlayerCompany company, CompanyTalentCandidate candidate) {
        this(company, candidate, false);
    }

    public CompanyCoreEmployee(PlayerCompany company, CompanyTalentCandidate candidate, boolean pendingHire) {
        this.company = company;
        this.candidateKey = candidate.key();
        this.name = candidate.name();
        this.departmentType = candidate.departmentType();
        this.grade = candidate.grade();
        this.ability = candidate.ability();
        this.growthSpeed = candidate.growthSpeed();
        this.organizationFit = candidate.organizationFit();
        this.leadership = candidate.leadership();
        this.annualSalary = candidate.annualSalary();
        this.expectedAnnualSalary = candidate.annualSalary();
        this.experience = (double) minimumExperienceForGrade(candidate.grade());
        this.teamLeader = false;
        this.pendingHireMonths = pendingHire ? 1 : 0;
        this.resigned = false;
        this.resignationRisk = 0;
        this.resignationNoticeMonths = 0;
        this.consecutiveOverloadMonths = 0;
        this.trainingMonthsRemaining = 0;
        this.salaryReviewYearsApplied = 0;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getCandidateKey() { return candidateKey; }
    public String getName() { return name; }
    public CompanyDepartmentType getDepartmentType() { return departmentType; }
    public int getGrade() { return grade; }
    public int getAbility() { return ability; }
    public int getGrowthSpeed() { return growthSpeed; }
    public int getOrganizationFit() { return organizationFit; }
    public int getLeadership() {
        return leadership == null ? Math.max(30, Math.min(80, (ability + organizationFit) / 2)) : leadership;
    }
    public long getAnnualSalary() { return annualSalary; }
    public long getExpectedAnnualSalary() { return expectedAnnualSalary == null ? annualSalary : expectedAnnualSalary; }
    public double getExperience() { return experience == null ? minimumExperienceForGrade(grade) : experience; }
    public boolean isTeamLeader() { return Boolean.TRUE.equals(teamLeader); }
    public int getPendingHireMonths() { return pendingHireMonths == null ? 0 : pendingHireMonths; }
    public boolean isResigned() { return Boolean.TRUE.equals(resigned); }
    public int getResignationRisk() { return resignationRisk == null ? 0 : resignationRisk; }
    public int getResignationNoticeMonths() { return resignationNoticeMonths == null ? 0 : resignationNoticeMonths; }
    public int getConsecutiveOverloadMonths() { return consecutiveOverloadMonths == null ? 0 : consecutiveOverloadMonths; }
    public int getTrainingMonthsRemaining() { return trainingMonthsRemaining == null ? 0 : trainingMonthsRemaining; }
    public int getTrainingCooldownUntilElapsedDay() {
        return trainingCooldownUntilElapsedDay == null ? 0 : trainingCooldownUntilElapsedDay;
    }
    public boolean hasActiveRetentionAgreement(int elapsedDay) {
        return retentionAgreementExpiresDay != null && elapsedDay < retentionAgreementExpiresDay;
    }
    public boolean isActive() { return !isResigned() && getPendingHireMonths() == 0; }
    public boolean isContributing() { return isActive() && getTrainingMonthsRemaining() == 0; }

    public long monthlySalary() {
        long adjustedAnnualSalary = isTeamLeader() ? Math.multiplyExact(annualSalary, 115) / 100 : annualSalary;
        return adjustedAnnualSalary / 12;
    }

    public boolean advanceHiringMonth() {
        if (isResigned() || getPendingHireMonths() == 0) {
            return false;
        }
        pendingHireMonths = getPendingHireMonths() - 1;
        return pendingHireMonths == 0;
    }

    public void updateOverloadMonths(double utilizationPercent) {
        consecutiveOverloadMonths = utilizationPercent > 100
                ? getConsecutiveOverloadMonths() + 1
                : 0;
    }

    public void updateResignationRisk(int risk) {
        resignationRisk = Math.max(0, Math.min(100, risk));
    }

    public boolean beginResignationNotice() {
        if (!isActive() || getResignationRisk() < 80 || getResignationNoticeMonths() > 0) {
            return false;
        }
        resignationNoticeMonths = 1;
        return true;
    }

    public boolean advanceResignationNotice() {
        if (!isActive() || getResignationNoticeMonths() <= 0) {
            return false;
        }
        resignationNoticeMonths = getResignationNoticeMonths() - 1;
        return resignationNoticeMonths == 0;
    }

    public void acceptRetentionAgreement(int elapsedDay) {
        if (!isActive() || getResignationNoticeMonths() <= 0) {
            throw new IllegalStateException("퇴사 협상 대상이 아닙니다");
        }
        annualSalary = Math.multiplyExact(annualSalary, 115L) / 100L;
        retentionAgreementExpiresDay = Math.addExact(elapsedDay, 90);
        resignationNoticeMonths = 0;
    }

    public long retentionBonusCost() {
        return Math.multiplyExact(annualSalary, 20L) / 100L;
    }

    public long trainingCost() {
        return Math.multiplyExact(annualSalary, 30L) / 100L;
    }

    public boolean canStartTraining(int elapsedDay) {
        return isActive() && getTrainingMonthsRemaining() == 0
                && elapsedDay >= getTrainingCooldownUntilElapsedDay();
    }

    /** 전문 연수는 3개월 동안 부서 기여에서 제외되고 완료 시 큰 경험치를 지급한다. */
    public void startTraining(int elapsedDay) {
        if (!canStartTraining(elapsedDay)) {
            throw new IllegalStateException("현재 전문 연수를 시작할 수 없습니다");
        }
        trainingMonthsRemaining = 3;
        trainingCooldownUntilElapsedDay = Math.addExact(elapsedDay, 360);
    }

    public boolean advanceTrainingMonth(double growthMultiplier) {
        if (!isActive() || getTrainingMonthsRemaining() == 0) {
            return false;
        }
        trainingMonthsRemaining = getTrainingMonthsRemaining() - 1;
        return getTrainingMonthsRemaining() == 0 && addWorkExperience(12.0, growthMultiplier);
    }

    /** 회사 경과 연차만큼 2% 정기 인상을 한 번씩 적용한다. */
    public void applyAnnualSalaryReview(int completedCompanyYears) {
        if (!isActive()) {
            return;
        }
        int appliedYears = salaryReviewYearsApplied == null ? 0 : salaryReviewYearsApplied;
        while (appliedYears < completedCompanyYears) {
            annualSalary = Math.multiplyExact(annualSalary, 102L) / 100L;
            expectedAnnualSalary = Math.multiplyExact(getExpectedAnnualSalary(), 102L) / 100L;
            appliedYears++;
        }
        salaryReviewYearsApplied = appliedYears;
    }

    public void resign(int elapsedDay) {
        if (!isActive()) {
            throw new IllegalStateException("재직 중인 핵심인재만 퇴사할 수 있습니다");
        }
        resigned = true;
        resignedElapsedDay = elapsedDay;
        resignationNoticeMonths = 0;
        teamLeader = false;
    }

    public boolean prefersManagement() {
        return Math.floorMod(candidateKey.hashCode(), 100) < 15;
    }

    public void appointTeamLeader() {
        if (!isActive() || grade < 5 || getLeadership() < 40) {
            throw new IllegalStateException("5등급이며 리더십 40 이상인 재직자만 팀장이 될 수 있습니다");
        }
        teamLeader = true;
    }

    public void removeTeamLeader() {
        teamLeader = false;
    }

    /** 정상 근무 한 달의 경험치를 성장속도에 따라 반영하고 기준을 넘으면 자동 승급한다. */
    public boolean addNormalWorkExperience() {
        return addNormalWorkExperience(1.0);
    }

    public boolean addNormalWorkExperience(double multiplier) {
        return addWorkExperience(1.0, multiplier);
    }

    /**
     * 근무·프로젝트·연수에서 받은 기본 경험치에 개인 성장속도와 비서 보정을 곱한다.
     * 승급 시에는 새 등급의 시장 연봉을 기대연봉에 반영해 보상 불일치가 퇴사위험으로 이어지게 한다.
     */
    public boolean addWorkExperience(double baseExperience, double multiplier) {
        if (!isContributing() || grade >= 5 || baseExperience <= 0) {
            return false;
        }
        experience = getExperience()
                + baseExperience * (0.75 + growthSpeed / 200.0) * Math.max(0, multiplier);
        int oldGrade = grade;
        while (grade < 5 && getExperience() >= minimumExperienceForGrade(grade + 1)) {
            grade++;
        }
        if (grade > oldGrade) {
            expectedAnnualSalary = Math.max(getExpectedAnnualSalary(), expectedSalaryForGrade(grade));
        }
        return grade > oldGrade;
    }

    public double gradeEfficiency() {
        return switch (grade) {
            case 2 -> 1.08;
            case 3 -> 1.16;
            case 4 -> 1.25;
            case 5 -> 1.35;
            default -> 1.0;
        };
    }

    private static int minimumExperienceForGrade(int grade) {
        return switch (grade) {
            case 2 -> 12;
            case 3 -> 30;
            case 4 -> 55;
            case 5 -> 90;
            default -> 0;
        };
    }

    private static long expectedSalaryForGrade(int grade) {
        return switch (grade) {
            case 2 -> 325_000_000L;
            case 3 -> 550_000_000L;
            case 4 -> 950_000_000L;
            case 5 -> 1_850_000_000L;
            default -> 200_000_000L;
        };
    }
}
