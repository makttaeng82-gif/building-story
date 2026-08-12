package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCoreEmployee;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyOrganizationSystem;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** 핵심인재 성장과 부서 전문성·처리능력·인건비를 한 곳에서 계산한다. */
@Service
public class CompanyWorkforceService {
    private static final double[] EXPERTISE_WEIGHTS = {0.40, 0.25, 0.15, 0.12, 0.08};
    public static final int EXTERNAL_RECRUITER_ORGANIZATION_LIMIT = 80;
    public static final int HR_ORGANIZATION_LIMIT = 100;
    public static final int EXTERNAL_RECRUITER_MONTHLY_HIRE_LIMIT = 10;
    private static final int EXTERNAL_RECRUITER_FEE_PERCENT = 20;
    private static final int INTERNAL_RECRUITER_FEE_PERCENT = 5;
    private static final double ADAPTING_CAPACITY_RATE = 0.70;

    private final CompanyCoreEmployeeRepository employeeRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyTalentCatalog talentCatalog;
    private final CompanyMonthlySettlementRepository monthlySettlementRepository;
    private final CompanyGrowthService growthService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanySecretaryService secretaryService;

    public CompanyWorkforceService(
            CompanyCoreEmployeeRepository employeeRepository,
            CompanyDepartmentRepository departmentRepository,
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyTalentCatalog talentCatalog,
            CompanyMonthlySettlementRepository monthlySettlementRepository,
            CompanyGrowthService growthService,
            CompanyCashLedgerService cashLedgerService,
            CompanySecretaryService secretaryService
    ) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.talentCatalog = talentCatalog;
        this.monthlySettlementRepository = monthlySettlementRepository;
        this.growthService = growthService;
        this.cashLedgerService = cashLedgerService;
        this.secretaryService = secretaryService;
    }

    @Transactional
    public int processNormalWorkMonth(PlayerCompany company) {
        int promotions = 0;
        double growthMultiplier = secretaryService.employeeGrowthMultiplier(company);
        for (CompanyCoreEmployee employee : employeeRepository.findByCompanyOrderById(company)) {
            employee.applyAnnualSalaryReview(company.getMarketMonthsProcessed() / 12);
            boolean promoted = employee.getTrainingMonthsRemaining() > 0
                    ? employee.advanceTrainingMonth(growthMultiplier)
                    : employee.addNormalWorkExperience(growthMultiplier);
            if (promoted) {
                promotions++;
            }
        }
        return promotions;
    }

    @Transactional
    public String startTraining(long playerId, long employeeId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 전문 연수를 시작할 수 없음";
        }
        CompanyCoreEmployee employee = employeeRepository.findById(employeeId).orElseThrow();
        if (!employee.getCompany().getId().equals(company.getId())
                || !employee.canStartTraining(player.getElapsedDays())) {
            return "현재 전문 연수를 시작할 수 없는 핵심인재";
        }
        long cost = employee.trainingCost();
        if (!cashLedgerService.withdraw(
                company,
                "core-training:" + employeeId + ":" + player.getElapsedDays(),
                CompanyCashFlowType.OPERATING,
                "핵심인재 전문 연수",
                cost)) {
            return "전문 연수비를 지불할 법인 현금이 부족함";
        }
        employee.startTraining(player.getElapsedDays());
        return employee.getName() + " 전문 연수 시작 · 3개월 · 비용 " + cost + "원";
    }

    /**
     * 프로젝트를 실제로 마친 부서의 재직 핵심인재에게 완료 경험치를 지급한다.
     * 연수 중인 직원은 프로젝트에 기여하지 않으므로 경험치 대상에서도 제외한다.
     */
    @Transactional
    public int grantDepartmentExperience(
            PlayerCompany company,
            CompanyDepartmentType type,
            double baseExperience
    ) {
        int promotions = 0;
        double growthMultiplier = secretaryService.employeeGrowthMultiplier(company);
        for (CompanyCoreEmployee employee : employees(company, type)) {
            if (employee.addWorkExperience(baseExperience, growthMultiplier)) {
                promotions++;
            }
        }
        return promotions;
    }

    @Transactional
    public WorkforceMonthResult processWorkforceMonth(PlayerCompany company) {
        int onboardedCoreEmployees = employeeRepository.findByCompanyOrderById(company).stream()
                .mapToInt(employee -> employee.advanceHiringMonth() ? 1 : 0)
                .sum();
        int onboarded = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(CompanyDepartment::advanceHiringMonth)
                .sum();
        int promotions = processNormalWorkMonth(company);
        int resignedGeneralEmployees = 0;
        int hrExpertise = hrExpertise(company);
        for (CompanyDepartment department : departmentRepository.findByCompanyOrderById(company)) {
            var load = departmentLoad(company, department);
            employees(company, department.getDepartmentType())
                    .forEach(employee -> employee.updateOverloadMonths(load.utilizationPercent()));
            double attritionRate = 0.003
                    + (load.utilizationPercent() > 100 ? 0.004 : 0.0)
                    - hrExpertise * 0.00002;
            resignedGeneralEmployees += department.applyGeneralAttrition(attritionRate);
        }
        applyGeneralWorkforceEducation(company, hrExpertise);

        int resignationWarnings = 0;
        int resignedCoreEmployees = 0;
        boolean quarterlyEvaluation = company.getMarketMonthsProcessed() > 0
                && company.getMarketMonthsProcessed() % 3 == 0;
        for (CompanyCoreEmployee employee : employeeRepository.findByCompanyOrderById(company)) {
            if (!employee.isActive()) {
                continue;
            }
            int risk = calculateResignationRisk(company, employee);
            employee.updateResignationRisk(risk);
            if (employee.advanceResignationNotice()) {
                if (risk >= 60) {
                    employee.resign(company.getPlayer().getElapsedDays());
                    resignedCoreEmployees++;
                    continue;
                }
            }
            if (quarterlyEvaluation && employee.beginResignationNotice()) {
                resignationWarnings++;
            }
        }
        return new WorkforceMonthResult(
                onboarded,
                onboardedCoreEmployees,
                promotions,
                resignedGeneralEmployees,
                resignedCoreEmployees,
                resignationWarnings
        );
    }

    @Transactional
    public String offerRetentionAgreement(long playerId, long employeeId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 잔류 협상을 진행할 수 없음";
        }
        CompanyCoreEmployee employee = employeeRepository.findById(employeeId).orElseThrow();
        if (!employee.getCompany().getId().equals(company.getId()) || employee.getResignationNoticeMonths() <= 0) {
            return "퇴사 협상 대상이 아님";
        }
        long bonus = employee.retentionBonusCost();
        if (!cashLedgerService.withdraw(
                company, "retention:" + employeeId + ":" + player.getElapsedDays(),
                CompanyCashFlowType.OPERATING, "핵심인재 잔류 보너스", bonus)) {
            return "잔류보너스를 지급할 법인 현금이 부족함";
        }
        employee.acceptRetentionAgreement(player.getElapsedDays());
        employee.updateResignationRisk(calculateResignationRisk(company, employee));
        return employee.getName() + " 잔류 합의 · 연봉 15% 인상 · 보너스 지급";
    }

    @Transactional
    public String hireCoreTalent(long playerId, String candidateKey) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 채용할 수 없음";
        }
        var candidate = talentCatalog.find(candidateKey).orElseThrow();
        var growthStage = growthService.stage(company);
        if (!talentCatalog.isAvailableAt(candidateKey, growthStage)) {
            return talentCatalog.requiredStage(candidateKey).getDisplayName() + " 단계부터 지원 가능한 후보";
        }
        if (departmentRepository.findByCompanyAndDepartmentType(
                company, candidate.departmentType()).isEmpty()) {
            return candidate.departmentType().displayName() + " 출범 후 채용 가능";
        }
        List<CompanyCoreEmployee> allEmployees = employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .toList();
        if (allEmployees.stream().anyMatch(employee -> employee.getCandidateKey().equals(candidateKey))) {
            return "이미 채용했거나 입사 대기 중인 핵심인재";
        }
        long departmentCount = allEmployees.stream()
                .filter(employee -> employee.getDepartmentType() == candidate.departmentType())
                .count();
        if (departmentCount >= 5) {
            return candidate.departmentType().displayName() + " 핵심인재 정원은 5명";
        }
        int organizationLimit = organizationLimit(company);
        if (committedGeneralEmployeeCount(company) + allEmployees.size() + 1 > organizationLimit) {
            return "조직관리 한도는 총 " + organizationLimit + "명";
        }
        if (!cashLedgerService.withdraw(
                company, "core-hire:" + candidateKey + ":" + player.getElapsedDays(),
                CompanyCashFlowType.OPERATING, "핵심인재 계약금", candidate.signingBonus())) {
            return "계약금을 지불할 법인 현금이 부족함";
        }
        employeeRepository.save(new CompanyCoreEmployee(company, candidate, true));
        return candidate.name() + " 채용 승인 · 다음 월 정산 입사";
    }

    @Transactional
    public String appointTeamLeader(long playerId, long employeeId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 팀장을 임명할 수 없음";
        }
        CompanyCoreEmployee target = employeeRepository.findById(employeeId).orElseThrow();
        if (!target.getCompany().getId().equals(company.getId())) {
            return "다른 기업의 직원은 임명할 수 없음";
        }
        if (!target.isActive() || target.getGrade() < 5 || target.getLeadership() < 40) {
            return "5등급이며 리더십 40 이상인 재직자만 팀장 임명 가능";
        }
        employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> employee.getDepartmentType() == target.getDepartmentType())
                .forEach(CompanyCoreEmployee::removeTeamLeader);
        target.appointTeamLeader();
        return target.getDepartmentType().displayName() + " 팀장으로 " + target.getName() + " 임명";
    }

    @Transactional
    public String changeApprovedHeadcount(long playerId, CompanyDepartmentType type, int count) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 승인 정원을 변경할 수 없음";
        }
        CompanyDepartment target = department(company, type);
        int otherCommittedGeneralEmployees = departmentRepository.findByCompanyOrderById(company).stream()
                .filter(department -> department.getDepartmentType() != type)
                .mapToInt(department -> department.getGeneralEmployeeCount() + department.getPendingHireCount())
                .sum();
        int coreEmployees = (int) employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .count();
        if (count < target.getGeneralEmployeeCount() + target.getPendingHireCount()) {
            return "승인 정원은 재직자와 입사 대기 인원보다 적을 수 없음";
        }
        int organizationLimit = organizationLimit(company);
        if (coreEmployees + otherCommittedGeneralEmployees + count > organizationLimit) {
            return "조직관리 한도는 총 " + organizationLimit + "명";
        }
        target.changeApprovedHeadcount(count);
        return type.displayName() + " 승인 정원 " + count + "명으로 변경";
    }

    @Transactional
    public String requestGeneralHires(long playerId, CompanyDepartmentType type, int count) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = launchedCompany(player);
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 채용할 수 없음";
        }
        CompanyDepartment target = department(company, type);
        int pendingThisMonth = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(CompanyDepartment::getPendingHireCount)
                .sum();
        int remainingHireLimit = remainingMonthlyHireLimit(company);
        if (count <= 0 || count > remainingHireLimit) {
            return "월간 채용 가능 인원은 " + remainingHireLimit + "명입니다.";
        }
        if (target.getGeneralEmployeeCount() + target.getPendingHireCount() + count > target.getApprovedHeadcount()) {
            return "부서 승인 정원이 부족함";
        }
        int coreEmployees = (int) employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .count();
        if (coreEmployees + committedGeneralEmployeeCount(company) + count > organizationLimit(company)) {
            return "조직관리 한도는 총 " + organizationLimit(company) + "명";
        }
        long fee = Math.multiplyExact(hiringFeePerPerson(company, type), count);
        if (!cashLedgerService.withdraw(
                company,
                "general-hire:" + type.name() + ":" + player.getElapsedDays() + ":" + pendingThisMonth,
                CompanyCashFlowType.OPERATING,
                "일반인력 채용비",
                fee)) {
            return "채용비를 지불할 법인 현금이 부족함";
        }
        target.requestHires(count);
        return type.displayName() + " " + count + "명 채용 승인 · 다음 월 정산 입사";
    }

    @Transactional(readOnly = true)
    public List<CompanyCoreEmployee> employees(PlayerCompany company, CompanyDepartmentType type) {
        return employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> employee.getDepartmentType() == type && employee.isContributing())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyCoreEmployee> allEmployees(PlayerCompany company, CompanyDepartmentType type) {
        return employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> employee.getDepartmentType() == type && !employee.isResigned())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyCoreEmployee> resignationWarnings(PlayerCompany company) {
        return employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> employee.isActive() && employee.getResignationNoticeMonths() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.game.buildingstory.domain.CompanyTalentCandidate> availableCandidates(
            PlayerCompany company,
            CompanyDepartmentType type
    ) {
        var hiredKeys = new HashSet<>(employeeRepository.findByCompanyOrderById(company).stream()
                .map(CompanyCoreEmployee::getCandidateKey)
                .toList());
        var growthStage = growthService.stage(company);
        return talentCatalog.all().stream()
                .filter(candidate -> candidate.departmentType() == type
                        && talentCatalog.isAvailableAt(candidate.key(), growthStage)
                        && !hiredKeys.contains(candidate.key()))
                .toList();
    }

    @Transactional(readOnly = true)
    public int departmentExpertise(PlayerCompany company, CompanyDepartmentType type) {
        List<Double> contributions = employees(company, type).stream()
                .map(employee -> employee.getAbility()
                        * employee.gradeEfficiency()
                        * (0.9 + employee.getOrganizationFit() / 500.0))
                .sorted(Comparator.reverseOrder())
                .limit(EXPERTISE_WEIGHTS.length)
                .toList();
        double weighted = 0;
        for (int index = 0; index < contributions.size(); index++) {
            weighted += contributions.get(index) * EXPERTISE_WEIGHTS[index];
        }
        return (int) Math.round(Math.max(0, Math.min(100, weighted / 1.35)));
    }

    @Transactional(readOnly = true)
    public int monthlyCapacity(PlayerCompany company, CompanyDepartment department) {
        int baseline = baselineGeneralEmployees(department.getDepartmentType());
        double effectiveEmployees = department.getGeneralEmployeeCount()
                - department.getAdaptingEmployeeCount()
                + department.getAdaptingEmployeeCount() * ADAPTING_CAPACITY_RATE;
        double organizationCapacity = Math.min(1000,
                100 * (Math.log(1 + effectiveEmployees / baseline) / Math.log(2)));
        double skillFactor = 0.8 + department.getAverageGeneralSkill() / 250.0;
        double expertiseFactor = 0.75 + departmentExpertise(company, department.getDepartmentType()) / 200.0;
        double leaderFactor = employees(company, department.getDepartmentType()).stream()
                .filter(CompanyCoreEmployee::isTeamLeader)
                .findFirst()
                .map(employee -> 1 + (5 + employee.getLeadership() / 10.0) / 100.0)
                .orElse(1.0);
        return (int) Math.round(organizationCapacity * skillFactor * expertiseFactor * leaderFactor
                * secretaryService.chiefEfficiencyMultiplier(company)
                * company.getOrganizationSystem().getCapacityMultiplier());
    }

    @Transactional(readOnly = true)
    public DepartmentLoad departmentLoad(PlayerCompany company, CompanyDepartment department) {
        int capacity = monthlyCapacity(company, department);
        int baseWorkload = baseWorkload(company, department.getDepartmentType());
        int totalWorkload = Math.addExact(baseWorkload, department.getAllocatedMajorWorkload());
        double utilization = capacity == 0 ? 0 : totalWorkload * 100.0 / capacity;
        int slotLimit = capacity == 0 ? 0 : majorWorkSlotLimit(capacity);
        return new DepartmentLoad(
                capacity,
                baseWorkload,
                department.getAllocatedMajorWorkload(),
                totalWorkload,
                utilization,
                capacity == 0 ? "운영 불가" : utilizationStatus(utilization),
                capacity == 0 ? "danger" : utilizationTone(utilization),
                slotLimit,
                department.getActiveMajorWorkCount(),
                Math.max(0, slotLimit - department.getActiveMajorWorkCount()),
                Math.max(0, capacity - totalWorkload)
        );
    }

    @Transactional(readOnly = true)
    public DepartmentLoad departmentLoad(PlayerCompany company, CompanyDepartmentType type) {
        return departmentLoad(company, department(company, type));
    }

    /** 현재 처리능력에서 기본업무를 제외한 여력을 진행 중인 주요 업무에 비례 배분한다. */
    @Transactional(readOnly = true)
    public int effectiveMajorWork(
            PlayerCompany company,
            CompanyDepartmentType type,
            int reservedWork
    ) {
        if (reservedWork <= 0) {
            return 0;
        }
        CompanyDepartment department = department(company, type);
        DepartmentLoad load = departmentLoad(company, department);
        int availableForMajorWork = Math.max(0, load.capacity() - load.baseWorkload());
        int allocated = department.getAllocatedMajorWorkload();
        if (availableForMajorWork == 0 || allocated == 0) {
            return 0;
        }
        double allocationRatio = Math.min(1.0, availableForMajorWork / (double) allocated);
        return Math.max(1, (int) Math.floor(reservedWork * allocationRatio));
    }

    @Transactional
    public void reserveMajorWork(PlayerCompany company, CompanyDepartmentType type, int workload) {
        company.reserveMajorWork(companyMajorWorkSlotLimit(company));
        CompanyDepartment department = department(company, type);
        DepartmentLoad load = departmentLoad(company, department);
        int maximumAllocatedWorkload = Math.max(0,
                (int) Math.floor(load.capacity() * 1.30) - load.baseWorkload());
        try {
            department.reserveMajorWork(workload, load.slotLimit(), maximumAllocatedWorkload);
        } catch (RuntimeException exception) {
            company.releaseMajorWork();
            throw exception;
        }
    }

    /** 명령 화면이 실제 예약과 같은 슬롯·가동률 조건으로 실행 가능 여부를 안내할 때 사용한다. */
    @Transactional(readOnly = true)
    public boolean canReserveMajorWork(
            PlayerCompany company,
            CompanyDepartmentType type,
            int workload
    ) {
        if (workload <= 0 || company.getActiveMajorWorkCount() >= companyMajorWorkSlotLimit(company)) {
            return false;
        }
        CompanyDepartment department = department(company, type);
        DepartmentLoad load = departmentLoad(company, department);
        int maximumAllocatedWorkload = Math.max(0,
                (int) Math.floor(load.capacity() * 1.30) - load.baseWorkload());
        return load.availableSlots() > 0
                && department.getAllocatedMajorWorkload() + workload <= maximumAllocatedWorkload;
    }

    @Transactional
    public void releaseMajorWork(PlayerCompany company, CompanyDepartmentType type, int workload) {
        department(company, type).releaseMajorWork(workload);
        company.releaseMajorWork();
    }

    @Transactional(readOnly = true)
    public long monthlyPayroll(PlayerCompany company) {
        long corePayroll = employeeRepository.findByCompanyOrderById(company).stream()
                .filter(CompanyCoreEmployee::isActive)
                .mapToLong(CompanyCoreEmployee::monthlySalary)
                .sum();
        long generalPayroll = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToLong(department -> (long) department.getGeneralEmployeeCount()
                        * generalMonthlySalary(company, department.getDepartmentType()))
                .sum();
        long employeePayroll = Math.addExact(corePayroll, generalPayroll);
        long benefitsAndOfficeCost = Math.multiplyExact(employeePayroll, 20L) / 100L;
        return Math.addExact(employeePayroll, benefitsAndOfficeCost);
    }

    public long generalMonthlySalary(CompanyDepartmentType type) {
        return switch (type) {
            case AI_DEVELOPMENT -> 3_300_000L;
            case SALES_MARKETING -> 3_000_000L;
            case SERVICE_OPERATIONS -> 2_800_000L;
            case HR_ORGANIZATION -> 3_000_000L;
            case STRATEGY_FINANCE -> 3_300_000L;
        };
    }

    public long generalMonthlySalary(PlayerCompany company, CompanyDepartmentType type) {
        double annualRaiseMultiplier = Math.pow(1.02, company.getMarketMonthsProcessed() / 12);
        double multiplier = annualRaiseMultiplier * company.generalSalaryStageMultiplier();
        return Math.round(generalMonthlySalary(type) * multiplier);
    }

    public long hiringFeePerPerson(CompanyDepartmentType type) {
        return Math.multiplyExact(generalMonthlySalary(type), 12) * EXTERNAL_RECRUITER_FEE_PERCENT / 100;
    }

    public long hiringFeePerPerson(PlayerCompany company, CompanyDepartmentType type) {
        int feePercent = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.HR_ORGANIZATION)
                .isPresent() ? INTERNAL_RECRUITER_FEE_PERCENT : EXTERNAL_RECRUITER_FEE_PERCENT;
        return Math.multiplyExact(generalMonthlySalary(company, type), 12) * feePercent / 100;
    }

    @Transactional(readOnly = true)
    public int remainingMonthlyHireLimit(PlayerCompany company) {
        int pending = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(CompanyDepartment::getPendingHireCount)
                .sum();
        int organizationLimit = company.getOrganizationSystem().monthlyHireLimit(totalGeneralEmployees(company));
        int processingLimit = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.HR_ORGANIZATION)
                .map(department -> departmentLoad(company, department).availableCapacity())
                .orElse(organizationLimit);
        return Math.max(0, Math.min(organizationLimit, processingLimit) - pending);
    }

    @Transactional(readOnly = true)
    public int maximumApprovedHeadcount(PlayerCompany company, CompanyDepartment target) {
        int usedByCoreAndOtherDepartments = (int) employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .count()
                + departmentRepository.findByCompanyOrderById(company).stream()
                .filter(department -> department.getDepartmentType() != target.getDepartmentType())
                .mapToInt(department -> department.getGeneralEmployeeCount() + department.getPendingHireCount())
                .sum();
        return organizationLimit(company) - usedByCoreAndOtherDepartments;
    }

    /**
     * 조직 한도는 실제 재직자와 입사 승인이 끝난 대기자만 소비한다.
     * 승인 정원은 향후 채용 계획이므로 이 값에 포함하지 않는다.
     */
    private int committedGeneralEmployeeCount(PlayerCompany company) {
        return departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(department -> department.getGeneralEmployeeCount() + department.getPendingHireCount())
                .sum();
    }

    public int organizationLimit(PlayerCompany company) {
        if (company.getOrganizationSystem() == CompanyOrganizationSystem.MANUAL
                && departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION).isEmpty()) {
            return 80;
        }
        return company.getOrganizationSystem().getEmployeeLimit();
    }

    @Transactional(readOnly = true)
    public int totalGeneralEmployees(PlayerCompany company) {
        return departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(CompanyDepartment::getGeneralEmployeeCount)
                .sum();
    }

    @Transactional(readOnly = true)
    public int totalEmployees(PlayerCompany company) {
        int core = (int) employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .count();
        return Math.addExact(core, totalGeneralEmployees(company));
    }

    private PlayerCompany launchedCompany(Player player) {
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational()) {
            throw new IllegalStateException("제품 출시 후 조직을 관리할 수 있습니다");
        }
        return company;
    }

    private CompanyDepartment department(PlayerCompany company, CompanyDepartmentType type) {
        return departmentRepository.findByCompanyAndDepartmentType(company, type).orElseThrow();
    }

    private int baselineGeneralEmployees(CompanyDepartmentType type) {
        return switch (type) {
            case AI_DEVELOPMENT -> 10;
            case SALES_MARKETING -> 3;
            case SERVICE_OPERATIONS -> 5;
            case HR_ORGANIZATION, STRATEGY_FINANCE -> 5;
        };
    }

    private int baseWorkload(PlayerCompany company, CompanyDepartmentType type) {
        return switch (type) {
            case AI_DEVELOPMENT -> 50 + company.getTechnicalDebt();
            case SALES_MARKETING -> 65 + (int) Math.min(35, company.getPaidUsers() / 20_000);
            case SERVICE_OPERATIONS -> 55 + (int) Math.min(45, company.getPaidUsers() / 8_000);
            case HR_ORGANIZATION -> 35;
            case STRATEGY_FINANCE -> 30;
        };
    }

    private int majorWorkSlotLimit(int capacity) {
        if (capacity < 180) return 1;
        if (capacity < 350) return 2;
        if (capacity < 650) return 3;
        return 4;
    }

    public int companyMajorWorkSlotLimit(PlayerCompany company) {
        int baseLimit = growthService.stage(company).getMajorWorkSlotLimit();
        return Math.max(1, baseLimit - CompanyFinanceService.governanceWorkSlotPenalty(
                company.getPlayerOwnershipPercent()));
    }

    private int calculateResignationRisk(PlayerCompany company, CompanyCoreEmployee employee) {
        int risk = 0;
        double salaryRatio = employee.getExpectedAnnualSalary() == 0
                ? 1.0
                : employee.getAnnualSalary() / (double) employee.getExpectedAnnualSalary();
        if (salaryRatio < 0.90) risk += 25;
        else if (salaryRatio < 1.0) risk += 10;
        else if (salaryRatio >= 1.10) risk -= 10;

        CompanyDepartment department = department(company, employee.getDepartmentType());
        double utilization = departmentLoad(company, department).utilizationPercent();
        if (employee.getConsecutiveOverloadMonths() >= 2) risk += 15;
        if (utilization > 120) risk += 25;
        if (employee.getOrganizationFit() < 50) risk += 15;
        else if (employee.getOrganizationFit() >= 75) risk -= 10;
        if (employee.getGrade() >= 5 && employee.prefersManagement() && !employee.isTeamLeader()) risk += 10;
        if (cashRunwayBelowThreeMonths(company)) risk += 20;
        if (profitableAndGrowing(company)) risk -= 10;
        if (employee.hasActiveRetentionAgreement(company.getPlayer().getElapsedDays())) risk -= 35;
        risk -= hrExpertise(company) / 10;
        return Math.max(0, Math.min(100, risk));
    }

    private int hrExpertise(PlayerCompany company) {
        return departmentRepository.findByCompanyAndDepartmentType(
                        company, CompanyDepartmentType.HR_ORGANIZATION)
                .map(ignored -> departmentExpertise(company, CompanyDepartmentType.HR_ORGANIZATION))
                .orElse(0);
    }

    /** 인사 전문성이 높을수록 일반인력 평균숙련이 더 짧은 주기로 상승한다. */
    private void applyGeneralWorkforceEducation(PlayerCompany company, int hrExpertise) {
        int interval = hrExpertise >= 80 ? 3 : hrExpertise >= 60 ? 6 : hrExpertise >= 40 ? 12 : 0;
        int completedMonths = company.getMarketMonthsProcessed() + 1;
        if (interval == 0 || completedMonths % interval != 0) {
            return;
        }
        departmentRepository.findByCompanyOrderById(company).stream()
                .filter(department -> department.getGeneralEmployeeCount() > 0)
                .forEach(department -> department.improveAverageGeneralSkill(1));
    }

    private boolean cashRunwayBelowThreeMonths(PlayerCompany company) {
        var settlements = monthlySettlementRepository.findByCompanyOrderByPeriodIndexDesc(company);
        if (settlements.isEmpty()) {
            return false;
        }
        var latest = settlements.getFirst();
        long monthlyCost = Math.addExact(
                Math.addExact(latest.getPayrollCost(), latest.getCloudCost()),
                Math.addExact(
                        Math.addExact(
                                Math.addExact(latest.getDevelopmentCost(), latest.getMarketingCost()),
                                latest.getPlatformCost()),
                        latest.getIncidentCost())
        );
        return monthlyCost > 0 && company.getCorporateCash() < Math.multiplyExact(monthlyCost, 3L);
    }

    private boolean profitableAndGrowing(PlayerCompany company) {
        var settlements = monthlySettlementRepository.findByCompanyOrderByPeriodIndexDesc(company);
        return settlements.size() >= 2
                && settlements.getFirst().getOperatingProfit() > 0
                && settlements.getFirst().getSubscriptionRevenue() > settlements.get(1).getSubscriptionRevenue();
    }

    private String utilizationStatus(double utilization) {
        if (utilization < 70) return "인력 과잉";
        if (utilization < 90) return "여유";
        if (utilization <= 110) return "정상";
        if (utilization <= 130) return "과부하";
        return "위험";
    }

    private String utilizationTone(double utilization) {
        if (utilization < 70) return "muted";
        if (utilization < 90) return "good";
        if (utilization <= 110) return "";
        if (utilization <= 130) return "warn";
        return "danger";
    }

    public record WorkforceMonthResult(
            int onboardedEmployees,
            int onboardedCoreEmployees,
            int promotedCoreEmployees,
            int resignedGeneralEmployees,
            int resignedCoreEmployees,
            int resignationWarnings
    ) {
    }

    public record DepartmentLoad(
            int capacity,
            int baseWorkload,
            int allocatedMajorWorkload,
            int totalWorkload,
            double utilizationPercent,
            String status,
            String tone,
            int slotLimit,
            int usedSlots,
            int availableSlots,
            int availableCapacity
    ) {
    }
}
