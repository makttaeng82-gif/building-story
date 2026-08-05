package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCustomerContract;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
import com.game.buildingstory.domain.CompanyCustomerContractType;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyShortTermProjectStatus;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCustomerContractRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyShortTermProjectRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 시험·일반 고객 계약의 제안, 구축, 정산, SLA와 갱신을 처리한다.
 *
 * <p>시험계약은 계약 흐름을 익히는 안전계약이고, 성장 조건을 충족한 뒤 열리는
 * 일반계약부터 납기 지연, SLA 위반, 위약금과 갱신 선택을 적용한다.</p>
 */
@Service
public class CompanyCustomerContractService {
    private static final long MINIMUM_BASE_REVENUE = 4_800_000_000L;
    private static final List<ContractCatalogItem> TRIAL_CATALOG = List.of(
            new ContractCatalogItem("bytecore-knowledge", "바이트코어", "사내 지식검색 AI 구축"),
            new ContractCatalogItem("marketway-support", "마켓웨이", "고객상담 AI 구축"),
            new ContractCatalogItem("ironworks-quality", "아이언웍스", "품질검사 지원 AI 구축"),
            new ContractCatalogItem("hanul-public", "한울리", "공공문서 분류 AI 구축")
    );
    private static final List<ContractCatalogItem> NORMAL_CATALOG = List.of(
            new ContractCatalogItem("neonsoft-office", "네온소프트", "전사 업무자동화 AI 구축"),
            new ContractCatalogItem("quickbox-logistics", "퀵박스", "물류 수요예측 AI 구축"),
            new ContractCatalogItem("freshmeal-demand", "프레시밀", "생산·수요 최적화 AI 구축"),
            new ContractCatalogItem("motorline-support", "모터라인", "정비 지식지원 AI 구축")
    );
    private static final List<ContractCatalogItem> LARGE_CATALOG = List.of(
            new ContractCatalogItem("bytecore-global-knowledge", "바이트코어", "글로벌 사내 AI 플랫폼 구축"),
            new ContractCatalogItem("ironworks-smart-factory", "아이언웍스", "전사 스마트팩토리 AI 구축"),
            new ContractCatalogItem("marketway-commerce-ai", "마켓웨이", "통합 유통 의사결정 AI 구축"),
            new ContractCatalogItem("hanul-national-ai", "한울리", "대규모 공공업무 AI 전환")
    );
    private static final List<ContractCatalogItem> STRATEGIC_CATALOG = List.of(
            new ContractCatalogItem("bytecore-sovereign-ai", "바이트코어", "글로벌 독립형 AI 연산망 구축"),
            new ContractCatalogItem("neonsoft-alliance", "네온소프트", "차세대 업무 AI 공동사업"),
            new ContractCatalogItem("ironworks-industrial-ai", "아이언웍스", "글로벌 산업 AI 표준 플랫폼"),
            new ContractCatalogItem("hanul-public-platform", "한울리", "국가 단위 공공 AI 플랫폼")
    );

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyCustomerContractRepository contractRepository;
    private final CompanyShortTermProjectRepository shortTermProjectRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyMonthlySettlementRepository settlementRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyGrowthService growthService;
    private final CompanyExternalEventService externalEventService;

    public CompanyCustomerContractService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyCustomerContractRepository contractRepository,
            CompanyShortTermProjectRepository shortTermProjectRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyMonthlySettlementRepository settlementRepository,
            CompanyWorkforceService workforceService,
            CompanyGrowthService growthService,
            CompanyExternalEventService externalEventService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.contractRepository = contractRepository;
        this.shortTermProjectRepository = shortTermProjectRepository;
        this.departmentRepository = departmentRepository;
        this.settlementRepository = settlementRepository;
        this.workforceService = workforceService;
        this.growthService = growthService;
        this.externalEventService = externalEventService;
    }

    @Transactional
    public String accept(long playerId, long contractId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational() || company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 고객 계약을 수락할 수 있음";
        }
        CompanyCustomerContract contract = contractRepository.findById(contractId).orElseThrow();
        if (!contract.getCompany().getId().equals(company.getId())
                || contract.getStatus() != CompanyCustomerContractStatus.OFFERED) {
            return "수락 가능한 고객 계약이 아님";
        }
        if (!meetsRequirements(company, contract)) {
            return "제품 요구조건을 충족하지 못함";
        }
        int contractLimit = contractLimit(company);
        if (activeContractCount(company) >= contractLimit) {
            return "고객 계약 한도 " + contractLimit + "건에 도달";
        }
        if (company.getActiveMajorWorkCount() + 2 > workforceService.companyMajorWorkSlotLimit(company)) {
            return "회사 주요 업무 슬롯 2개가 필요함";
        }

        int developmentWork = availableMonthlyWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT,
                requiredMonthlyAssignment(
                        contract.getRemainingDevelopmentWork(), contract.getBuildDeadlineMonths(), 45));
        int operationsWork = availableMonthlyWork(
                company, CompanyDepartmentType.SERVICE_OPERATIONS,
                requiredMonthlyAssignment(
                        contract.getRemainingOperationsWork(), contract.getBuildDeadlineMonths(), 25));
        if (developmentWork <= 0 || operationsWork <= 0) {
            return "AI개발팀과 서비스운영팀의 업무 여유가 필요함";
        }

        workforceService.reserveMajorWork(company, CompanyDepartmentType.AI_DEVELOPMENT, developmentWork);
        workforceService.reserveMajorWork(company, CompanyDepartmentType.SERVICE_OPERATIONS, operationsWork);
        contract.accept(developmentWork, operationsWork);
        int expectedMonths = Math.max(
                (int) Math.ceil(contract.getRemainingDevelopmentWork() / (double) developmentWork),
                (int) Math.ceil(contract.getRemainingOperationsWork() / (double) operationsWork)
        );
        return contract.getClientName() + " " + contract.getContractType().getDisplayName()
                + " 수락 · 구축 예상 " + expectedMonths + "개월";
    }

    @Transactional
    public String renew(long playerId, long contractId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 계약을 갱신할 수 있음";
        }
        CompanyCustomerContract contract = ownedContract(company, contractId);
        if (contract.getStatus() != CompanyCustomerContractStatus.RENEWAL_OFFERED) {
            return "갱신 가능한 계약이 아님";
        }
        contract.renew();
        return contract.getClientName() + " 계약 갱신 완료 · 월 이용료 " + contract.getMonthlyFee() + "원";
    }

    @Transactional
    public String declineRenewal(long playerId, long contractId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyCustomerContract contract = ownedContract(company, contractId);
        if (contract.getStatus() != CompanyCustomerContractStatus.RENEWAL_OFFERED) {
            return "갱신 대기 계약이 아님";
        }
        contract.declineRenewal();
        return contract.getClientName() + " 계약 갱신 거절 · 계약 종료";
    }

    @Transactional
    public String rejectOffer(long playerId, long contractId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyCustomerContract contract = ownedContract(company, contractId);
        if (contract.getStatus() != CompanyCustomerContractStatus.OFFERED) {
            return "거절 가능한 고객 계약 제안이 아님";
        }
        contract.rejectOffer();
        return contract.getClientName() + " 계약 제안 거절";
    }

    @Transactional
    public boolean failActiveBuild(PlayerCompany company) {
        List<CompanyCustomerContract> buildings = buildingContracts(company);
        if (buildings.isEmpty()) {
            return false;
        }
        for (CompanyCustomerContract building : buildings) {
            releaseBuildWork(company, building);
            building.failBuildForCashDepletion();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public MonthlyFinancials monthlyFinancials(PlayerCompany company) {
        long revenue = 0;
        long cost = 0;
        for (CompanyCustomerContract contract : contracts(company)) {
            revenue = Math.addExact(revenue, contract.currentRevenue());
            cost = Math.addExact(cost, contract.currentCost());
        }
        return new MonthlyFinancials(revenue, cost);
    }

    @Transactional
    public ContractMonthResult processSuccessfulMonth(
            PlayerCompany company,
            boolean infrastructureOverCapacity
    ) {
        return processSuccessfulMonth(company, infrastructureOverCapacity, false);
    }

    @Transactional
    public ContractMonthResult processSuccessfulMonth(
            PlayerCompany company,
            boolean infrastructureOverCapacity,
            boolean unresolvedServiceIncident
    ) {
        StringBuilder notice = new StringBuilder();
        for (CompanyCustomerContract contract : contracts(company)) {
            switch (contract.getStatus()) {
                case BUILDING -> {
                    int previousDelayMonths = contract.getDelayMonths();
                    boolean inspectionPassed = contract.getContractType() == CompanyCustomerContractType.TRIAL
                            || meetsRequirements(company, contract);
                    int developmentWork = workforceService.effectiveMajorWork(
                            company, CompanyDepartmentType.AI_DEVELOPMENT,
                            contract.getAssignedDevelopmentWork());
                    int operationsWork = workforceService.effectiveMajorWork(
                            company, CompanyDepartmentType.SERVICE_OPERATIONS,
                            contract.getAssignedOperationsWork());
                    boolean completed = contract.advanceBuildMonth(
                            inspectionPassed, developmentWork, operationsWork);
                    if (completed) {
                        releaseBuildWork(company, contract);
                        double experience = switch (contract.getContractType()) {
                            case TRIAL, NORMAL -> 3.0;
                            case LARGE, STRATEGIC -> 6.0;
                        };
                        workforceService.grantDepartmentExperience(
                                company, CompanyDepartmentType.AI_DEVELOPMENT, experience);
                        workforceService.grantDepartmentExperience(
                                company, CompanyDepartmentType.SERVICE_OPERATIONS, experience);
                        appendNotice(notice, contract.getClientName() + " 구축 완료 · 검수 통과");
                    } else if (contract.getStatus()
                            == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT) {
                        releaseBuildWork(company, contract);
                        appendNotice(notice, contract.getClientName() + " 구축 실패 · 반환금·위약금 정산 예정");
                    } else if (contract.getDelayMonths() > previousDelayMonths) {
                        appendNotice(notice, contract.getClientName() + " 납기 지연 "
                                + contract.getDelayMonths() + "개월");
                    } else {
                        appendNotice(notice, contract.getClientName() + " 구축 "
                                + contract.buildProgressPercent() + "%");
                    }
                }
                case COMPLETION_PAYMENT -> {
                    contract.activate();
                    if (contract.getContractType() == CompanyCustomerContractType.LARGE
                            || contract.getContractType() == CompanyCustomerContractType.STRATEGIC) {
                        company.adjustBrandScore(0.5);
                    }
                    appendNotice(notice, contract.getClientName() + " 계약 서비스 시작");
                }
                case ACTIVE -> {
                    int previousViolations = contract.getSlaViolations();
                    boolean slaViolation = contract.getContractType().hasRiskRules()
                            && slaViolation(
                                    company, contract, infrastructureOverCapacity,
                                    unresolvedServiceIncident);
                    contract.advanceActiveMonth(slaViolation);
                    if (contract.getStatus() == CompanyCustomerContractStatus.TERMINATION_PAYMENT) {
                        appendNotice(notice, contract.getClientName() + " SLA 2회 위반 · 중도 종료");
                    } else if (contract.getStatus() == CompanyCustomerContractStatus.RENEWAL_OFFERED) {
                        appendNotice(notice, contract.getClientName() + " 계약 갱신 결정 필요");
                    } else if (contract.getStatus() == CompanyCustomerContractStatus.COMPLETED) {
                        appendNotice(notice, contract.getClientName() + " 시험계약 종료");
                    } else if (contract.getSlaViolations() > previousViolations) {
                        company.adjustBrandScore(-1.0);
                        appendNotice(notice, contract.getClientName() + " SLA 위반 · 다음 달 이용료 감면");
                    }
                }
                case CONTRACT_FAILURE_PAYMENT, TERMINATION_PAYMENT -> {
                    boolean failed = contract.getStatus()
                            == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT;
                    contract.settleTerminalPayment();
                    appendNotice(notice, contract.getClientName()
                            + (failed ? " 구축 실패 정산 완료" : " 중도 종료 위약금 정산 완료"));
                }
                default -> {
                }
            }
            contract.expire(company.getMarketMonthsProcessed());
        }

        boolean generated = generateCandidateIfDue(company);
        return new ContractMonthResult(!notice.isEmpty(), generated, notice.toString());
    }

    @Transactional(readOnly = true)
    public List<CompanyCustomerContract> contracts(PlayerCompany company) {
        return contractRepository.findByCompanyOrderByIdDesc(company);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyCustomerContract> buildingContract(PlayerCompany company) {
        return buildingContracts(company).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public List<CompanyCustomerContract> buildingContracts(PlayerCompany company) {
        return contracts(company).stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.BUILDING)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyCustomerContract> activeContracts(PlayerCompany company) {
        return contracts(company).stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.ACTIVE)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyCustomerContract> offeredContracts(PlayerCompany company) {
        return contracts(company).stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.OFFERED)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyCustomerContract> renewalContracts(PlayerCompany company) {
        return contracts(company).stream()
                .filter(contract -> contract.getStatus()
                        == CompanyCustomerContractStatus.RENEWAL_OFFERED)
                .toList();
    }

    @Transactional(readOnly = true)
    public long activeMonthlyFee(PlayerCompany company) {
        return activeContracts(company).stream()
                .mapToLong(CompanyCustomerContract::getMonthlyFee)
                .sum();
    }

    @Transactional(readOnly = true)
    public long processingDemand(PlayerCompany company) {
        return activeMonthlyFee(company) / 20_000L;
    }

    @Transactional(readOnly = true)
    public int availableMonthlyWork(
            PlayerCompany company,
            CompanyDepartmentType type,
            int maximumAssignment
    ) {
        CompanyDepartment department = departmentRepository.findByCompanyAndDepartmentType(company, type)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, department);
        return Math.min(maximumAssignment,
                Math.max(0, (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload()));
    }

    @Transactional(readOnly = true)
    public boolean canAccept(PlayerCompany company, CompanyCustomerContract contract) {
        return !company.isOperationsSuspended()
                && meetsRequirements(company, contract)
                && activeContractCount(company) < contractLimit(company)
                && company.getActiveMajorWorkCount() + 2 <= workforceService.companyMajorWorkSlotLimit(company)
                && availableMonthlyWork(
                        company, CompanyDepartmentType.AI_DEVELOPMENT,
                        requiredMonthlyAssignment(
                                contract.getRemainingDevelopmentWork(), contract.getBuildDeadlineMonths(), 45)) > 0
                && availableMonthlyWork(
                        company, CompanyDepartmentType.SERVICE_OPERATIONS,
                        requiredMonthlyAssignment(
                                contract.getRemainingOperationsWork(), contract.getBuildDeadlineMonths(), 25)) > 0;
    }

    private boolean generateCandidateIfDue(PlayerCompany company) {
        List<CompanyCustomerContract> history = contracts(company);
        boolean unavailableState = history.stream().anyMatch(contract ->
                contract.getStatus() == CompanyCustomerContractStatus.OFFERED
                        || contract.getStatus() == CompanyCustomerContractStatus.BUILDING
                        || contract.getStatus() == CompanyCustomerContractStatus.COMPLETION_PAYMENT
                        || contract.getStatus() == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT
                        || contract.getStatus() == CompanyCustomerContractStatus.TERMINATION_PAYMENT);
        if (unavailableState || activeContractCount(company) >= contractLimit(company)) {
            return false;
        }
        boolean completedShortTermBusiness = shortTermProjectRepository.findByCompanyOrderByIdDesc(company).stream()
                .anyMatch(project -> project.getStatus() == CompanyShortTermProjectStatus.COMPLETED);
        if (!completedShortTermBusiness) {
            return false;
        }

        boolean completedTrial = completedContractOfType(history, CompanyCustomerContractType.TRIAL);
        CompanyCustomerContractType contractType = highestAvailableContractType(company, completedTrial);
        if (completedTrial && contractType == CompanyCustomerContractType.TRIAL) {
            return false;
        }
        boolean guaranteedFirstOffer = history.isEmpty()
                || !completedContractOfType(history, contractType);
        int roll = Math.floorMod((int) (company.getId() * 19 + company.getMarketMonthsProcessed() * 23), 100);
        double marketingMultiplier = 0.75 + company.getMarketingEffect() / 200.0;
        int candidateChance = Math.min(100, (int) Math.round(
                28
                        * externalEventService.contractCandidateChanceMultiplier(company)
                        * marketingMultiplier));
        if (!guaranteedFirstOffer && roll >= candidateChance) {
            return false;
        }

        ContractCatalogItem item = nextAvailableCatalog(company, history, contractType);
        if (item == null) {
            return false;
        }
        long baseRevenue = baseSubscriptionRevenue(company);
        int feeRange = contractType.getMaximumFeePercent() - contractType.getMinimumFeePercent() + 1;
        int feePercent = contractType.getMinimumFeePercent()
                + Math.floorMod(company.getMarketMonthsProcessed(), feeRange);
        long monthlyFee = Math.max(1, Math.round(
                baseRevenue * feePercent / 100.0
                        * externalEventService.contractFeeMultiplier(company)));
        long constructionFee = Math.multiplyExact(
                monthlyFee, contractType.getConstructionFeePercent()) / 100;
        long buildCost = constructionFee * 60 / 100;
        int totalWork = contractType.getTotalWork();
        int developmentWork = totalWork * 70 / 100;
        int operationsWork = totalWork - developmentWork;
        contractRepository.save(new CompanyCustomerContract(
                company,
                item.key(),
                item.clientName(),
                item.projectName(),
                contractType,
                company.getMarketMonthsProcessed(),
                requiredBenchmark(company, contractType),
                requiredStability(company, contractType),
                requiredSecurity(company, contractType),
                developmentWork,
                operationsWork,
                constructionFee,
                buildCost,
                monthlyFee,
                contractType.getDurationMonths(),
                contractType.getBuildDeadlineMonths()
        ));
        return true;
    }

    private ContractCatalogItem nextAvailableCatalog(
            PlayerCompany company,
            List<CompanyCustomerContract> history,
            CompanyCustomerContractType contractType
    ) {
        List<ContractCatalogItem> catalog = switch (contractType) {
            case TRIAL -> TRIAL_CATALOG;
            case NORMAL -> NORMAL_CATALOG;
            case LARGE -> LARGE_CATALOG;
            case STRATEGIC -> STRATEGIC_CATALOG;
        };
        int start = Math.floorMod(company.getMarketMonthsProcessed(), catalog.size());
        for (int offset = 0; offset < catalog.size(); offset++) {
            ContractCatalogItem item = catalog.get((start + offset) % catalog.size());
            boolean coolingDown = history.stream()
                    .filter(contract -> contract.getCatalogKey().equals(item.key()))
                    .anyMatch(contract -> company.getMarketMonthsProcessed()
                            - contract.getOfferedMarketMonth() < 12);
            if (!coolingDown) {
                return item;
            }
        }
        return null;
    }

    private boolean meetsRequirements(PlayerCompany company, CompanyCustomerContract contract) {
        return company.getPrototypeBenchmark() >= contract.getRequiredBenchmark()
                && company.getProductStability() >= contract.getRequiredStability()
                && company.getProductSecurity() >= contract.getRequiredSecurity();
    }

    private int activeContractCount(PlayerCompany company) {
        return (int) contracts(company).stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.ACTIVE
                        || contract.getStatus() == CompanyCustomerContractStatus.BUILDING
                        || contract.getStatus() == CompanyCustomerContractStatus.COMPLETION_PAYMENT
                        || contract.getStatus() == CompanyCustomerContractStatus.RENEWAL_OFFERED)
                .count();
    }

    @Transactional(readOnly = true)
    public int contractLimit(PlayerCompany company) {
        return growthService.stage(company).getContractLimit();
    }

    private CompanyCustomerContractType highestAvailableContractType(
            PlayerCompany company,
            boolean completedTrial
    ) {
        if (!completedTrial) {
            return CompanyCustomerContractType.TRIAL;
        }
        return growthService.stage(company).getHighestContractType();
    }

    private boolean completedContractOfType(
            List<CompanyCustomerContract> history,
            CompanyCustomerContractType type
    ) {
        return history.stream().anyMatch(contract ->
                contract.getContractType() == type
                        && contract.getStatus() == CompanyCustomerContractStatus.COMPLETED);
    }

    private int requiredMonthlyAssignment(int remainingWork, int deadlineMonths, int trialMaximum) {
        if (deadlineMonths == Integer.MAX_VALUE) {
            return trialMaximum;
        }
        return Math.max(1, (int) Math.ceil(remainingWork / (double) deadlineMonths));
    }

    private int requiredBenchmark(PlayerCompany company, CompanyCustomerContractType type) {
        return switch (type) {
            case TRIAL -> Math.max(150, company.getPrototypeBenchmark() - 15);
            case NORMAL -> Math.max(220, company.getPrototypeBenchmark() - 5);
            case LARGE -> Math.max(600, company.getPrototypeBenchmark() - 3);
            case STRATEGIC -> Math.max(1_200, company.getPrototypeBenchmark() - 2);
        };
    }

    private int requiredStability(PlayerCompany company, CompanyCustomerContractType type) {
        return switch (type) {
            case TRIAL -> Math.max(50, company.getProductStability() - 5);
            case NORMAL -> Math.max(65, company.getProductStability() - 3);
            case LARGE -> Math.max(75, company.getProductStability() - 2);
            case STRATEGIC -> Math.max(85, company.getProductStability() - 1);
        };
    }

    private int requiredSecurity(PlayerCompany company, CompanyCustomerContractType type) {
        int base = switch (type) {
            case TRIAL -> Math.max(45, company.getProductSecurity() - 5);
            case NORMAL -> Math.max(60, company.getProductSecurity() - 3);
            case LARGE -> Math.max(75, company.getProductSecurity() - 2);
            case STRATEGIC -> Math.max(85, company.getProductSecurity() - 1);
        };
        return Math.min(100, base + externalEventService.contractSecurityRequirementBonus(company));
    }

    private boolean slaViolation(
            PlayerCompany company,
            CompanyCustomerContract contract,
            boolean infrastructureOverCapacity,
            boolean unresolvedServiceIncident
    ) {
        CompanyDepartment operations = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS)
                .orElseThrow();
        return unresolvedServiceIncident
                || infrastructureOverCapacity
                || company.getProductStability() < contract.getRequiredStability()
                || company.getProductSecurity() < contract.getRequiredSecurity()
                || workforceService.departmentLoad(company, operations).utilizationPercent() > 115;
    }

    private void releaseBuildWork(PlayerCompany company, CompanyCustomerContract contract) {
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT,
                contract.getAssignedDevelopmentWork());
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.SERVICE_OPERATIONS,
                contract.getAssignedOperationsWork());
    }

    private CompanyCustomerContract ownedContract(PlayerCompany company, long contractId) {
        CompanyCustomerContract contract = contractRepository.findById(contractId).orElseThrow();
        if (!contract.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("다른 기업의 계약에는 접근할 수 없음");
        }
        return contract;
    }

    private long baseSubscriptionRevenue(PlayerCompany company) {
        var settlements = settlementRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company);
        if (settlements.isEmpty()) {
            return Math.max(MINIMUM_BASE_REVENUE, company.getMonthlyRecurringRevenue());
        }
        long average = Math.round(settlements.stream()
                .mapToLong(item -> item.getSubscriptionRevenue())
                .average()
                .orElse(MINIMUM_BASE_REVENUE));
        return Math.max(MINIMUM_BASE_REVENUE, average);
    }

    private void appendNotice(StringBuilder notice, String value) {
        if (!notice.isEmpty()) {
            notice.append(" · ");
        }
        notice.append(value);
    }

    private record ContractCatalogItem(String key, String clientName, String projectName) {
    }

    public record MonthlyFinancials(long revenue, long cost) {
    }

    public record ContractMonthResult(boolean active, boolean candidateGenerated, String notice) {
    }
}
