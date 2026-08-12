package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyOrganizationSystem;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.web.CompanyOrganizationView;
import com.game.buildingstory.web.MoneyText;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 조직관리 시스템 구축과 승인 정원 기반 자동채용을 관리한다. */
@Service
public class CompanyOrganizationService {
    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyCashLedgerService cashLedgerService;
    private final MoneyText moneyText;

    public CompanyOrganizationService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService workforceService,
            CompanyCashLedgerService cashLedgerService,
            MoneyText moneyText
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
        this.cashLedgerService = cashLedgerService;
        this.moneyText = moneyText;
    }

    @Transactional(readOnly = true)
    public CompanyOrganizationView view(PlayerCompany company) {
        CompanyOrganizationSystem current = company.getOrganizationSystem();
        CompanyOrganizationSystem next = current.next();
        Requirement requirement = next == null ? new Requirement(false, "최종 단계") : requirement(company, next);
        String progress = upgradeProgress(company);
        List<CompanyDepartment> departments = departmentRepository.findByCompanyOrderById(company);
        int currentEmployees = workforceService.totalEmployees(company);
        int employeeLimit = workforceService.organizationLimit(company);
        int pendingHires = departments.stream().mapToInt(CompanyDepartment::getPendingHireCount).sum();
        int approvedVacancies = departments.stream().mapToInt(department -> Math.max(0,
                department.getApprovedHeadcount()
                        - department.getGeneralEmployeeCount()
                        - department.getPendingHireCount())).sum();
        int monthlyHireLimit = current.monthlyHireLimit(workforceService.totalGeneralEmployees(company));
        return new CompanyOrganizationView(
                current.getDisplayName(),
                employeeLimit + "명",
                currentEmployees + "명",
                Math.max(0, employeeLimit - currentEmployees - pendingHires) + "명",
                monthlyHireLimit + "명",
                approvedVacancies + "명",
                pendingHires + "명",
                Math.round((current.getCapacityMultiplier() - 1) * 100) + "%",
                current == CompanyOrganizationSystem.MANUAL
                        ? "조직관리 시스템 구축 후 사용 가능"
                        : company.isAutomaticHiringEnabled() ? "사용 중" : "중지",
                current == CompanyOrganizationSystem.MANUAL
                        ? "월 최대 " + monthlyHireLimit + "명"
                        : "일반인력 규모 비례 · 월 최대 " + monthlyHireLimit + "명",
                company.isAutomaticHiringEnabled(),
                current != CompanyOrganizationSystem.MANUAL,
                company.getPendingOrganizationSystem() != null,
                progress,
                next != null
                        && !company.isOperationsSuspended()
                        && requirement.met()
                        && company.getPendingOrganizationSystem() == null,
                next == null ? "최종 단계" : next.getDisplayName(),
                company.isOperationsSuspended() ? "기업 재가동 후 구축 가능" : requirement.text(),
                next == null ? "-" : moneyText.format(next.getConstructionCost())
        );
    }

    @Transactional
    public String startNextUpgrade(long playerId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) return "기업 운영중단 중에는 조직관리 시스템을 구축할 수 없음";
        CompanyOrganizationSystem next = company.getOrganizationSystem().next();
        if (next == null) return "이미 최종 조직관리 단계입니다.";
        if (company.getPendingOrganizationSystem() != null) return "조직관리 시스템 구축이 이미 진행 중입니다.";
        Requirement requirement = requirement(company, next);
        if (!requirement.met()) return requirement.text();
        long upfront = next.getConstructionCost() * 30 / 100;
        if (company.getCorporateCash() < upfront) return "계약금 " + moneyText.format(upfront) + "이 필요합니다.";
        WorkPlan workPlan = workPlan(company, next);
        try {
            workforceService.reserveMajorWork(
                    company, CompanyDepartmentType.HR_ORGANIZATION, workPlan.hrAllocation());
            if (workPlan.strategyAllocation() > 0) {
                try {
                    workforceService.reserveMajorWork(
                            company, CompanyDepartmentType.STRATEGY_FINANCE, workPlan.strategyAllocation());
                } catch (RuntimeException exception) {
                    workforceService.releaseMajorWork(
                            company, CompanyDepartmentType.HR_ORGANIZATION, workPlan.hrAllocation());
                    throw exception;
                }
            }
        } catch (IllegalStateException exception) {
            return exception.getMessage();
        }
        try {
            company.startOrganizationUpgrade(
                    next,
                    upfront,
                    workPlan.hrWorkload(),
                    workPlan.hrAllocation(),
                    workPlan.strategyWorkload(),
                    workPlan.strategyAllocation()
            );
        } catch (RuntimeException exception) {
            releaseUpgradeWork(company, workPlan.hrAllocation(), workPlan.strategyAllocation());
            throw exception;
        }
        cashLedgerService.recordApplied(
                company,
                "organization-upgrade:start:" + next.name(),
                CompanyCashFlowType.INVESTING,
                next.getDisplayName() + " 계약금",
                -upfront
        );
        return next.getDisplayName() + " 구축 시작 · 최소 " + next.getConstructionMonths()
                + "개월 · 계약금 " + moneyText.format(upfront);
    }

    @Transactional
    public String toggleAutomaticHiring(long playerId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 자동채용 설정을 변경할 수 없음";
        }
        try {
            company.toggleAutomaticHiring();
            return "자동채용 " + (company.isAutomaticHiringEnabled() ? "사용" : "중지");
        } catch (IllegalStateException exception) {
            return exception.getMessage();
        }
    }

    /**
     * 정상 월 정산 뒤 구축 분할금을 지급하고 자동채용을 요청한다.
     * 자동채용은 채용비 지급 뒤에도 필수비 3개월분이 남는 범위에서만 진행한다.
     */
    @Transactional
    public MonthResult processSuccessfulMonth(PlayerCompany company, long essentialMonthlyCost) {
        boolean upgraded = advanceUpgrade(company);
        int hires = company.isAutomaticHiringEnabled()
                ? requestAutomaticHires(company, essentialMonthlyCost)
                : 0;
        return new MonthResult(upgraded, hires);
    }

    private boolean advanceUpgrade(PlayerCompany company) {
        if (company.getPendingOrganizationSystem() == null) return false;
        int months = company.getOrganizationUpgradeMonthsRemaining();
        int hrWork = processableWork(
                company,
                CompanyDepartmentType.HR_ORGANIZATION,
                company.getOrganizationUpgradeHrAllocation(),
                company.getOrganizationUpgradeHrWorkRemaining()
        );
        int strategyWork = processableWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                company.getOrganizationUpgradeStrategyAllocation(),
                company.getOrganizationUpgradeStrategyWorkRemaining()
        );
        CompanyOrganizationSystem target = company.getPendingOrganizationSystem();
        int hrAllocation = company.getOrganizationUpgradeHrAllocation();
        int strategyAllocation = company.getOrganizationUpgradeStrategyAllocation();
        if (months == 0) {
            company.advanceDelayedOrganizationUpgrade(hrWork, strategyWork);
            boolean completed = company.getPendingOrganizationSystem() == null;
            if (completed) {
                releaseUpgradeWork(company, hrAllocation, strategyAllocation);
            }
            return completed;
        }
        long remaining = company.getOrganizationUpgradeRemainingCost();
        long installment = (remaining + months - 1) / months;
        if (company.getCorporateCash() < installment) return false;
        company.advanceOrganizationUpgrade(installment, hrWork, strategyWork);
        cashLedgerService.recordApplied(
                company,
                "organization-upgrade:month:" + target.name() + ":" + months,
                CompanyCashFlowType.INVESTING,
                target.getDisplayName() + " 구축비",
                -installment
        );
        boolean completed = company.getPendingOrganizationSystem() == null;
        if (completed) {
            releaseUpgradeWork(company, hrAllocation, strategyAllocation);
        }
        return completed;
    }

    private WorkPlan workPlan(PlayerCompany company, CompanyOrganizationSystem target) {
        var hrLoad = workforceService.departmentLoad(
                company,
                department(company, CompanyDepartmentType.HR_ORGANIZATION)
        );
        int hrAllocation = Math.max(1, hrLoad.availableCapacity());
        int hrWorkload = Math.multiplyExact(hrAllocation, target.getConstructionMonths());
        int strategyWorkload = target.requiresStrategyFinance()
                ? (int) Math.ceil(hrWorkload * 0.30)
                : 0;
        int strategyAllocation = strategyWorkload == 0
                ? 0
                : (int) Math.ceil(strategyWorkload / (double) target.getConstructionMonths());
        return new WorkPlan(hrWorkload, hrAllocation, strategyWorkload, strategyAllocation);
    }

    private int processableWork(
            PlayerCompany company,
            CompanyDepartmentType type,
            int reservedAllocation,
            int remainingWork
    ) {
        if (reservedAllocation <= 0 || remainingWork <= 0) return 0;
        var load = workforceService.departmentLoad(company, department(company, type));
        int otherMajorWork = Math.max(0, load.allocatedMajorWorkload() - reservedAllocation);
        int available = Math.max(0, load.capacity() - load.baseWorkload() - otherMajorWork);
        return Math.min(remainingWork, available);
    }

    private void releaseUpgradeWork(PlayerCompany company, int hrAllocation, int strategyAllocation) {
        if (strategyAllocation > 0) {
            workforceService.releaseMajorWork(
                    company, CompanyDepartmentType.STRATEGY_FINANCE, strategyAllocation);
        }
        if (hrAllocation > 0) {
            workforceService.releaseMajorWork(
                    company, CompanyDepartmentType.HR_ORGANIZATION, hrAllocation);
        }
    }

    private CompanyDepartment department(PlayerCompany company, CompanyDepartmentType type) {
        return departmentRepository.findByCompanyAndDepartmentType(company, type).orElseThrow();
    }

    private String upgradeProgress(PlayerCompany company) {
        if (company.getPendingOrganizationSystem() == null) {
            return "구축 중인 시스템 없음";
        }
        String minimumPeriod = company.getOrganizationUpgradeMonthsRemaining() > 0
                ? "최소 " + company.getOrganizationUpgradeMonthsRemaining() + "개월"
                : "최소 기간 충족";
        String strategy = company.getOrganizationUpgradeStrategyWorkRemaining() > 0
                ? " · 전략 작업 " + company.getOrganizationUpgradeStrategyWorkRemaining()
                : "";
        return company.getPendingOrganizationSystem().getDisplayName()
                + " · " + minimumPeriod
                + " · 인사 작업 " + company.getOrganizationUpgradeHrWorkRemaining()
                + strategy;
    }

    private int requestAutomaticHires(PlayerCompany company, long essentialMonthlyCost) {
        int remaining = workforceService.remainingMonthlyHireLimit(company);
        int hired = 0;
        long safetyCash = Math.multiplyExact(essentialMonthlyCost, 3);
        List<CompanyDepartment> departments = departmentRepository.findByCompanyOrderById(company);
        for (CompanyDepartment department : departments) {
            int gap = department.getApprovedHeadcount()
                    - department.getGeneralEmployeeCount()
                    - department.getPendingHireCount();
            int count = Math.min(gap, remaining);
            if (count <= 0) continue;
            long feePerPerson = workforceService.hiringFeePerPerson(company, department.getDepartmentType());
            long reservedPerPerson = Math.addExact(
                    feePerPerson,
                    Math.multiplyExact(workforceService.generalMonthlySalary(
                            company, department.getDepartmentType()), 3)
            );
            long affordable = Math.max(0, company.getCorporateCash() - safetyCash) / reservedPerPerson;
            count = (int) Math.min(count, affordable);
            if (count <= 0) break;
            long fee = Math.multiplyExact(feePerPerson, count);
            if (!cashLedgerService.withdraw(
                    company,
                    "auto-hire:" + department.getDepartmentType().name() + ":" + company.getMarketMonthsProcessed(),
                    CompanyCashFlowType.OPERATING,
                    "일반인력 자동채용비",
                    fee
            )) break;
            department.requestHires(count);
            hired += count;
            remaining -= count;
            if (remaining == 0) break;
        }
        return hired;
    }

    private Requirement requirement(PlayerCompany company, CompanyOrganizationSystem target) {
        if (departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION).isEmpty()) {
            return new Requirement(false, "인사조직팀 설립 필요");
        }
        int employees = workforceService.totalEmployees(company);
        int hr = workforceService.departmentExpertise(company, CompanyDepartmentType.HR_ORGANIZATION);
        if (employees < target.getRequiredEmployees() || hr < target.getRequiredHrExpertise()) {
            return new Requirement(false,
                    "직원 " + target.getRequiredEmployees() + "명 · 인사 전문성 "
                            + target.getRequiredHrExpertise() + " 필요");
        }
        if (target.requiresStrategyFinance()
                && departmentRepository.findByCompanyAndDepartmentType(
                        company, CompanyDepartmentType.STRATEGY_FINANCE).isEmpty()) {
            return new Requirement(false, "전략재무팀 설립 필요");
        }
        int requiredStrategy = target.requiredStrategyExpertise();
        if (requiredStrategy > 0
                && workforceService.departmentExpertise(
                        company, CompanyDepartmentType.STRATEGY_FINANCE) < requiredStrategy) {
            return new Requirement(false, "전략재무 전문성 " + requiredStrategy + " 필요");
        }
        return new Requirement(true, "구축 가능");
    }

    private record Requirement(boolean met, String text) {
    }

    private record WorkPlan(
            int hrWorkload,
            int hrAllocation,
            int strategyWorkload,
            int strategyAllocation
    ) {
    }

    public record MonthResult(boolean upgraded, int automaticHires) {
    }
}
