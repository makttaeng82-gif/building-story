package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCoreEmployee;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyTalentCandidate;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 법인 설립 직후 핵심인재 채용부터 첫 제품 출시까지의 튜토리얼을 진행한다. */
@Service
public class CompanyTutorialService {
    public static final int REQUIRED_CORE_EMPLOYEES = 6;
    public static final int COMMERCIALIZATION_MONTHS = 4;
    public static final long CLOUD_MONTHLY_COST = 9_000_000_000L;
    public static final long DEVELOPMENT_MONTHLY_COST = 2_000_000_000L;
    public static final long MARKETING_MONTHLY_COST = 800_000_000L;
    public static final long GENERAL_EMPLOYEE_MONTHLY_SALARY = 3_000_000L;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyCoreEmployeeRepository employeeRepository;
    private final CompanyTalentCatalog talentCatalog;
    private final CompanyMarketService marketService;
    private final CompanyCashLedgerService cashLedgerService;

    public CompanyTutorialService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository playerCompanyRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyCoreEmployeeRepository employeeRepository,
            CompanyTalentCatalog talentCatalog,
            CompanyMarketService marketService,
            CompanyCashLedgerService cashLedgerService
    ) {
        this.playerRepository = playerRepository;
        this.playerCompanyRepository = playerCompanyRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.talentCatalog = talentCatalog;
        this.marketService = marketService;
        this.cashLedgerService = cashLedgerService;
    }

    public List<CompanyTalentCandidate> candidates() {
        return talentCatalog.foundingCandidates();
    }

    @Transactional
    public PlayerCompany initializeCompany(Player player) {
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        ensureDepartments(company);
        if (company.getTutorialStage().isOperational()) {
            marketService.initializeMarket(company);
        }
        return company;
    }

    @Transactional(readOnly = true)
    public List<CompanyDepartment> departments(PlayerCompany company) {
        return departmentRepository.findByCompanyOrderById(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyCoreEmployee> coreEmployees(PlayerCompany company) {
        return employeeRepository.findByCompanyOrderById(company);
    }

    @Transactional
    public String confirmFoundingTeam(long playerId, List<String> requestedKeys) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        if (company.getTutorialStage() != CompanyTutorialStage.FOUNDING_HIRE) {
            return "이미 창업팀 구성이 완료됨";
        }

        List<String> uniqueKeys = new LinkedHashSet<>(requestedKeys == null ? List.of() : requestedKeys).stream().toList();
        if (uniqueKeys.size() != REQUIRED_CORE_EMPLOYEES) {
            return "핵심인재는 정확히 6명 선택";
        }
        List<CompanyTalentCandidate> selected = uniqueKeys.stream().map(key -> talentCatalog.find(key).orElse(null)).toList();
        if (selected.stream().anyMatch(candidate -> candidate == null)) {
            return "유효하지 않은 이력서가 포함됨";
        }
        if (selected.stream().anyMatch(candidate -> !talentCatalog.isFoundingCandidate(candidate.key()))) {
            return "창업 이력서에 없는 후보가 포함됨";
        }
        if (!hasMinimumDepartmentMix(selected)) {
            return "AI개발 2명·영업마케팅 1명·서비스운영 1명 이상 필요";
        }

        long signingBonus = selected.stream().mapToLong(CompanyTalentCandidate::signingBonus).sum();
        if (!cashLedgerService.withdraw(
                company, "founding:core-talent", CompanyCashFlowType.OPERATING,
                "창업 핵심인재 계약금", signingBonus)) {
            return "법인현금이 계약금보다 부족함";
        }
        selected.forEach(candidate -> employeeRepository.save(new CompanyCoreEmployee(company, candidate)));
        ensureDepartments(company);
        assignFoundingWorkforce(company);
        company.completeFoundingHire();
        player.pause();
        return "핵심인재 6명 채용 완료 · 일반인력 18명 배치";
    }

    @Transactional
    public String startCommercialization(long playerId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        if (company.getTutorialStage() != CompanyTutorialStage.READY_TO_DEVELOP) {
            return "상용화 개발을 시작할 수 없는 단계";
        }
        company.startCommercialization();
        player.resume();
        return "최초 상용화 개발 시작 · 4개월 예상";
    }

    @Transactional
    public String processMonthly(Player player) {
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElse(null);
        if (company == null || company.getTutorialStage() != CompanyTutorialStage.COMMERCIALIZATION_IN_PROGRESS) {
            return "";
        }
        long monthlyCost = monthlyCommercializationCost(company);
        long cashBefore = company.getCorporateCash();
        boolean completed = company.advanceCommercializationMonth(monthlyCost);
        if (company.getCorporateCash() < cashBefore) {
            cashLedgerService.recordApplied(
                    company,
                    "commercialization:" + (company.getCommercializationMonthsCompleted()),
                    CompanyCashFlowType.OPERATING,
                    "최초 상용화 개발비",
                    company.getCorporateCash() - cashBefore
            );
        }
        if (company.isCommercializationFundingPaused()) {
            return "법인현금 부족 · 최초 상용화 개발 중단";
        }
        if (completed) {
            player.pause();
            return "최초 상용화 개발 완료 · 출시 검토 필요";
        }
        return "최초 상용화 개발 " + company.getCommercializationMonthsCompleted() + " / 4개월 완료";
    }

    @Transactional
    public String launch(long playerId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        if (company.getTutorialStage() != CompanyTutorialStage.LAUNCH_REVIEW) {
            return "정식 출시할 수 없는 단계";
        }
        company.launchFirstProduct();
        marketService.initializeMarket(company);
        player.resume();
        return company.getServiceName() + " 정식 출시 완료";
    }

    @Transactional
    public String skipCommercializationForTest(long playerId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        if (company.getTutorialStage() != CompanyTutorialStage.COMMERCIALIZATION_IN_PROGRESS) {
            return "건너뛸 수 있는 상용화 개발이 없음";
        }
        company.completeCommercializationForTest();
        player.pause();
        return "QA 상용화 개발 건너뛰기 완료 · 출시 검토 필요";
    }

    @Transactional(readOnly = true)
    public long monthlyCommercializationCost(PlayerCompany company) {
        long corePayroll = employeeRepository.findByCompanyOrderById(company).stream()
                .mapToLong(employee -> employee.getAnnualSalary() / 12)
                .sum();
        long generalPayroll = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToLong(department -> (long) department.getGeneralEmployeeCount() * GENERAL_EMPLOYEE_MONTHLY_SALARY)
                .sum();
        return CLOUD_MONTHLY_COST + DEVELOPMENT_MONTHLY_COST + MARKETING_MONTHLY_COST + corePayroll + generalPayroll;
    }

    private boolean hasMinimumDepartmentMix(List<CompanyTalentCandidate> selected) {
        Map<CompanyDepartmentType, Integer> counts = new EnumMap<>(CompanyDepartmentType.class);
        selected.forEach(candidate -> counts.merge(candidate.departmentType(), 1, Integer::sum));
        return counts.getOrDefault(CompanyDepartmentType.AI_DEVELOPMENT, 0) >= 2
                && counts.getOrDefault(CompanyDepartmentType.SALES_MARKETING, 0) >= 1
                && counts.getOrDefault(CompanyDepartmentType.SERVICE_OPERATIONS, 0) >= 1;
    }

    private void ensureDepartments(PlayerCompany company) {
        for (CompanyDepartmentType type : List.of(
                CompanyDepartmentType.AI_DEVELOPMENT,
                CompanyDepartmentType.SALES_MARKETING,
                CompanyDepartmentType.SERVICE_OPERATIONS)) {
            if (departmentRepository.findByCompanyAndDepartmentType(company, type).isEmpty()) {
                departmentRepository.save(new CompanyDepartment(company, type));
            }
        }
    }

    private void assignFoundingWorkforce(PlayerCompany company) {
        Map<CompanyDepartmentType, Integer> workforce = Map.of(
                CompanyDepartmentType.AI_DEVELOPMENT, 8,
                CompanyDepartmentType.SALES_MARKETING, 4,
                CompanyDepartmentType.SERVICE_OPERATIONS, 6
        );
        departmentRepository.findByCompanyOrderById(company).forEach(department ->
                department.assignFoundingWorkforce(workforce.get(department.getDepartmentType())));
    }

}
