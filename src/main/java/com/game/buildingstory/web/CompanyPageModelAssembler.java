package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyDevelopmentDirection;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.CompanyProductImprovementType;
import com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy;
import com.game.buildingstory.domain.CompanyMarketingBudgetPolicy;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.PlayerCompanyService;
import com.game.buildingstory.service.CompanyTutorialService;
import com.game.buildingstory.service.CompanySettlementService;
import com.game.buildingstory.service.CompanyWorkforceService;
import com.game.buildingstory.service.CompanyMarketService;
import com.game.buildingstory.service.CompanyInfrastructureService;
import com.game.buildingstory.service.CompanyProductProjectService;
import com.game.buildingstory.service.CompanyComputeConstructionService;
import com.game.buildingstory.service.CompanyShortTermProjectService;
import com.game.buildingstory.service.CompanyCustomerContractService;
import com.game.buildingstory.service.CompanyGrowthService;
import com.game.buildingstory.service.CompanyServiceIncidentService;
import com.game.buildingstory.service.CompanyDepartmentService;
import com.game.buildingstory.service.CompanySecretaryService;
import com.game.buildingstory.service.CompanyOrganizationService;
import com.game.buildingstory.service.CompanyFinanceService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.game.buildingstory.web.CompanyViewText.departmentKey;
import static com.game.buildingstory.web.CompanyViewText.formatPeople;
import static com.game.buildingstory.web.CompanyViewText.growthSpeedText;
import static com.game.buildingstory.web.CompanyViewText.progressPercent;
import static com.game.buildingstory.web.CompanyViewText.stabilityStatus;
import static com.game.buildingstory.web.CompanyViewText.stabilityTone;
import static com.game.buildingstory.web.CompanyViewText.technicalDebtStatus;
import static com.game.buildingstory.web.CompanyViewText.technicalDebtTone;

/** 기업 도메인이 완성되기 전까지 화면 검증용 데이터를 조립한다. */
@Component
public class CompanyPageModelAssembler {
    private final GameService gameService;
    private final PlayerCompanyService playerCompanyService;
    private final MoneyText moneyText;
    private final CompanyTutorialService companyTutorialService;
    private final CompanySettlementService companySettlementService;
    private final CompanyWorkforceService companyWorkforceService;
    private final CompanyMarketService companyMarketService;
    private final CompanyInfrastructureService companyInfrastructureService;
    private final CompanyProductProjectService companyProductProjectService;
    private final CompanyComputeConstructionService companyComputeConstructionService;
    private final CompanyShortTermProjectService companyShortTermProjectService;
    private final CompanyCustomerContractService companyCustomerContractService;
    private final CompanyGrowthService companyGrowthService;
    private final CompanyServiceIncidentService companyServiceIncidentService;
    private final CompanyReportingModelAssembler companyReportingModelAssembler;
    private final CompanyDepartmentService companyDepartmentService;
    private final CompanySecretaryService companySecretaryService;
    private final CompanyOrganizationService companyOrganizationService;
    private final CompanyFinanceService companyFinanceService;

    public CompanyPageModelAssembler(
            GameService gameService,
            PlayerCompanyService playerCompanyService,
            MoneyText moneyText,
            CompanyTutorialService companyTutorialService,
            CompanySettlementService companySettlementService,
            CompanyWorkforceService companyWorkforceService,
            CompanyMarketService companyMarketService,
            CompanyInfrastructureService companyInfrastructureService,
            CompanyProductProjectService companyProductProjectService,
            CompanyComputeConstructionService companyComputeConstructionService,
            CompanyShortTermProjectService companyShortTermProjectService,
            CompanyCustomerContractService companyCustomerContractService,
            CompanyGrowthService companyGrowthService,
            CompanyServiceIncidentService companyServiceIncidentService,
            CompanyReportingModelAssembler companyReportingModelAssembler,
            CompanyDepartmentService companyDepartmentService,
            CompanySecretaryService companySecretaryService,
            CompanyOrganizationService companyOrganizationService,
            CompanyFinanceService companyFinanceService
    ) {
        this.gameService = gameService;
        this.playerCompanyService = playerCompanyService;
        this.moneyText = moneyText;
        this.companyTutorialService = companyTutorialService;
        this.companySettlementService = companySettlementService;
        this.companyWorkforceService = companyWorkforceService;
        this.companyMarketService = companyMarketService;
        this.companyInfrastructureService = companyInfrastructureService;
        this.companyProductProjectService = companyProductProjectService;
        this.companyComputeConstructionService = companyComputeConstructionService;
        this.companyShortTermProjectService = companyShortTermProjectService;
        this.companyCustomerContractService = companyCustomerContractService;
        this.companyGrowthService = companyGrowthService;
        this.companyServiceIncidentService = companyServiceIncidentService;
        this.companyReportingModelAssembler = companyReportingModelAssembler;
        this.companyDepartmentService = companyDepartmentService;
        this.companySecretaryService = companySecretaryService;
        this.companyOrganizationService = companyOrganizationService;
        this.companyFinanceService = companyFinanceService;
    }

    public void addCompanyPageAttributes(Player player, Model model) {
        var company = playerCompanyService.company(player);
        if (company.isPresent()) {
            addEstablishedCompanyAttributes(player, companyTutorialService.initializeCompany(player), model);
            return;
        }
        CompanyPreparationView preparation = preparation(player);
        model.addAttribute("companyPreparation", preparation);
        model.addAttribute("playerCompany", null);
        model.addAttribute("companyDashboard", previewDashboard());
        model.addAttribute("companyNews", List.of());
        model.addAttribute("companyManagementReports", List.of());
        model.addAttribute("companyFinancialForecast",
                CompanyFinancialForecastView.unavailable("기업 설립 후 전망을 제공합니다."));
        model.addAttribute("companyGrowthProgress", null);
        model.addAttribute("companyFinance", null);
        model.addAttribute("companyOverallUtilizationText", "최고 81% · 정상");
        model.addAttribute("companyOverallUtilizationTone", "");
        model.addAttribute("companyActiveMajorWorkCount", 4);
        model.addAttribute("companyMajorWorkSlotLimit", 6);
        model.addAttribute("companyDepartmentSlots", Map.of(
                "development", "0/1",
                "sales", "0/1",
                "operations", "0/1",
                "hr", "-",
                "finance", "-"
        ));
        model.addAttribute("companyTopMetrics", List.of(
                metric("최소 출자금", moneyText.format(preparation.minimumInvestment()), "개인 현금에서 이전", ""),
                metric("관리직원", preparation.propertyManagerCount() + " / 6명", "개발 중 조건 미적용", preparation.propertyManagerCount() >= 6 ? "good" : "warn"),
                metric("비서 성장", preparation.readySecretaryCount() + " / 6명", "숙련도·호감도 최대", preparation.readySecretaryCount() >= 6 ? "good" : "warn")
        ));
    }

    private void addEstablishedCompanyAttributes(Player player, PlayerCompany company, Model model) {
        boolean operational = company.getTutorialStage().isOperational();
        var growthStage = operational
                ? companyGrowthService.stage(company) : null;
        model.addAttribute("companyPreparation", null);
        model.addAttribute("playerCompany", company);
        model.addAttribute("companyTopMetrics", List.of(
                metric("법인 현금", moneyText.format(company.getCorporateCash()), "교육비 차감 완료", ""),
                metric("발행 지분", "1,000만주", "플레이어 100%", "good"),
                metric("기업 단계", growthStage == null ? "설립 완료" : growthStage.getDisplayName(),
                        operational ? "제품 운영 중" : "상용화 준비",
                        operational ? "good" : "warn")
        ));
        model.addAttribute("companyEstablishedMonth", Math.max(1,
                (player.getElapsedDays() - company.getEstablishedElapsedDay()) / 30 + 1));
        model.addAttribute("companyCandidates", companyTutorialService.candidates());
        model.addAttribute("companyCoreEmployees", companyTutorialService.coreEmployees(company));
        model.addAttribute("companyDepartments", companyTutorialService.departments(company));
        model.addAttribute("companyDepartmentOpportunities",
                operational
                        ? companyDepartmentService.opportunities(company)
                        : List.of());
        model.addAttribute("companyMonthlyTutorialCost", companyTutorialService.monthlyCommercializationCost(company));
        model.addAttribute("companyCommercializationProgress", company.getCommercializationMonthsCompleted() * 25);
        var monthlySettlements = companySettlementService.monthlySettlements(company);
        CompanyDashboardView dashboard = operational
                ? liveDashboard(player, company, monthlySettlements)
                : previewDashboard();
        model.addAttribute("companyDashboard", dashboard);
        model.addAttribute("companyDepartmentSlots",
                operational ? departmentSlotUsage(company) : Map.of());
        model.addAttribute("companyExecutiveOffice",
                operational ? executiveOffice(player, company, dashboard) : null);
        model.addAttribute("companyWorkforceByKey", operational
                ? workforceViews(company)
                : Map.of());
        model.addAttribute("companyCoreTalentByKey", operational
                ? coreTalentViews(company, player.getElapsedDays())
                : Map.of());
        model.addAttribute("companyMonthlySettlements", monthlySettlements);
        model.addAttribute("companyDevelopmentBudgetPolicies", CompanyDevelopmentBudgetPolicy.values());
        model.addAttribute("companyMarketingBudgetPolicies", CompanyMarketingBudgetPolicy.values());
        model.addAttribute("companyBudget",
                operational ? companySettlementService.budgetSnapshot(company) : null);
        model.addAttribute("companyTutorialNotice", tutorialNotice(company));
        model.addAttribute("companyTutorialReportAction",
                operational && company.getTutorialStage() == CompanyTutorialStage.FIRST_SETTLEMENT);
        model.addAttribute("companyOrganization",
                operational
                        ? companyOrganizationService.view(company)
                        : null);
        model.addAttribute("companyLatestSettlement", monthlySettlements.isEmpty() ? null : monthlySettlements.getFirst());
        model.addAttribute("companyQuarterlyReports", companySettlementService.quarterlyReports(company));
        model.addAttribute("companyFinance",
                operational
                        ? companyFinanceService.view(
                                company, companySettlementService.essentialMonthlyCost(company))
                        : null);
        model.addAttribute("companyGrowthProgress",
                operational
                        ? growthProgress(company)
                        : null);
        companyReportingModelAssembler.addAttributes(company, operational, model);
        if (operational) {
            var infrastructure = companyInfrastructureService.snapshot(company, player.getElapsedDays());
            model.addAttribute("companyMarket", marketView(companyMarketService.snapshot(company)));
            model.addAttribute("companyInfrastructure", infrastructureView(
                    company, infrastructure, player.getElapsedDays()));
            model.addAttribute("companyProductProject", productProjectView(company));
            model.addAttribute("companyShortTermProject", shortTermProjectView(company));
            model.addAttribute("companyCustomerContract", customerContractView(company));
            model.addAttribute("companyServiceIncident",
                    serviceIncidentView(company, infrastructure.utilizationPercent()));
            model.addAttribute("companyRecentProductResult", recentProductResult(company));
            model.addAttribute("companyRecentShortTermResult", recentShortTermResult(company));
            model.addAttribute("companyRecentContractResult", recentContractResult(company));
        }
        model.addAttribute("companyResumeRequiredAmount", Math.addExact(
                company.getUnpaidSettlementAmount(), companySettlementService.essentialMonthlyCost(company)));
        var highestLoad = companyTutorialService.departments(company).stream()
                .map(department -> companyWorkforceService.departmentLoad(company, department))
                .max(java.util.Comparator.comparingDouble(CompanyWorkforceService.DepartmentLoad::utilizationPercent));
        model.addAttribute("companyOverallUtilizationText", highestLoad
                .map(load -> "최고 " + Math.round(load.utilizationPercent()) + "% · " + load.status())
                .orElse("업무량 없음"));
        model.addAttribute("companyOverallUtilizationTone", highestLoad
                .map(CompanyWorkforceService.DepartmentLoad::tone)
                .orElse("muted"));
        model.addAttribute("companyActiveMajorWorkCount", company.getActiveMajorWorkCount());
        model.addAttribute("companyMajorWorkSlotLimit", companyWorkforceService.companyMajorWorkSlotLimit(company));
    }

    private Map<String, String> departmentSlotUsage(PlayerCompany company) {
        Map<String, String> slots = new LinkedHashMap<>();
        for (CompanyDepartment department : companyTutorialService.departments(company)) {
            var load = companyWorkforceService.departmentLoad(company, department);
            slots.put(
                    departmentKey(department.getDepartmentType()),
                    load.usedSlots() + "/" + load.slotLimit()
            );
        }
        return slots;
    }

    private CompanyDashboardView liveDashboard(Player player, PlayerCompany company, List<com.game.buildingstory.domain.CompanyMonthlySettlement> settlements) {
        var growthStage = companyGrowthService.stage(company);
        var market = companyMarketService.snapshot(company);
        var infrastructure = companyInfrastructureService.snapshot(company, player.getElapsedDays());
        var departments = companyTutorialService.departments(company).stream()
                .map(department -> liveDepartment(player, company, department))
                .toList();
        var latest = settlements.isEmpty() ? null : settlements.getFirst();
        var previous = settlements.size() < 2 ? null : settlements.get(1);
        long latestRevenue = latest == null ? company.getMonthlyRecurringRevenue() : latest.getTotalRevenue();
        long latestProfit = latest == null ? 0 : latest.getOperatingProfit();
        long previousRevenue = previous == null ? 0 : previous.getTotalRevenue();
        long previousProfit = previous == null ? 0 : previous.getOperatingProfit();
        int totalCore = companyTutorialService.departments(company).stream()
                .mapToInt(department -> companyWorkforceService.employees(company, department.getDepartmentType()).size())
                .sum();
        int totalGeneral = companyTutorialService.departments(company).stream()
                .mapToInt(CompanyDepartment::getGeneralEmployeeCount)
                .sum();
        int chiefEfficiencyPercent = (int) Math.round(
                (companySecretaryService.chiefEfficiencyMultiplier(company) - 1.0) * 100);

        var developmentSecretary = companySecretary(player, company, CompanyDepartmentType.AI_DEVELOPMENT);
        var salesSecretary = companySecretary(player, company, CompanyDepartmentType.SALES_MARKETING);
        var operationsSecretary = companySecretary(player, company, CompanyDepartmentType.SERVICE_OPERATIONS);
        var activeProductProject = companyProductProjectService.activeProject(company);
        var activeShortTermProject = companyShortTermProjectService.activeProject(company);
        var earningShortTermProject = companyShortTermProjectService.earningProject(company);
        int offeredShortTermProjects = companyShortTermProjectService.offeredProjects(company).size();
        var buildingCustomerContract = companyCustomerContractService.buildingContract(company);
        var activeCustomerContracts = companyCustomerContractService.activeContracts(company);
        var renewalCustomerContracts = companyCustomerContractService.renewalContracts(company);
        int offeredCustomerContracts = companyCustomerContractService.offeredContracts(company).size();
        var activeConstruction = companyComputeConstructionService.activeConstruction(company);
        var activeIncident = companyServiceIncidentService.activeIncident(company);
        String productStatus = company.isOperationsSuspended()
                ? "운영중단"
                : activeProductProject.map(project -> "개선 진행").orElse("정상 운영");
        String productTone = company.isOperationsSuspended()
                ? "danger"
                : activeProductProject.map(project -> "warn").orElse("good");
        String productResult = activeProductProject
                .map(project -> project.getImprovementType().getDisplayName() + " " + project.progressPercent() + "%")
                .orElse("유료 이용자 " + String.format("%,d명", company.getPaidUsers()));
        int productProgress = activeProductProject
                .map(com.game.buildingstory.domain.CompanyProductProject::progressPercent)
                .orElse(company.getProductCompleteness());
        List<CompanyDashboardView.OperationItem> businesses = List.of(
                operation("core-product", "사업", "핵심제품", company.getServiceName() + " · 구독형 AI 플랫폼",
                        productStatus, productTone, "AI개발팀", "상시 운영",
                        productResult, productProgress,
                        metrics(
                                metric("AI 벤치마크", company.getPrototypeBenchmark() + "점", "최초 상용화 기준", "good"),
                                metric("유료 이용자", String.format("%,d명", company.getPaidUsers()), "일반 요금제", ""),
                                metric("월 반복매출", moneyText.format(company.getMonthlyRecurringRevenue()), "실제 정산 연결", "good"),
                                metric("시장점유율", String.format("%.2f%%", market.playerMarketShare()),
                                        "전체 시장 " + String.format("%,d명", market.totalMarketUsers()), ""),
                                metric("안정성", company.getProductStability() + " · " + stabilityStatus(company.getProductStability()),
                                        "서비스 운영·제품 개선으로 상승", stabilityTone(company.getProductStability())),
                                metric("기술부채", company.getTechnicalDebt() + " · " + technicalDebtStatus(company.getTechnicalDebt()),
                                        "제품 개선 프로젝트로 감소", technicalDebtTone(company.getTechnicalDebt()))
                        ), developmentSecretary),
                operation("projects", "사업", "단기 프로젝트", "시장 기회를 활용하는 3개월 수익 사업",
                        activeShortTermProject.isPresent() ? "수행 중"
                                : earningShortTermProject.isPresent() ? "수익 정산"
                                : offeredShortTermProjects > 0 ? "제안 도착" : "후보 대기",
                        activeShortTermProject.isPresent() || offeredShortTermProjects > 0 ? "warn"
                                : earningShortTermProject.isPresent() ? "good" : "muted",
                        "영업마케팅팀", "초기 동시 1건",
                        activeShortTermProject.map(project -> project.getName() + " " + project.progressPercent() + "%")
                                .orElseGet(() -> earningShortTermProject
                                        .map(project -> project.getName() + " 수익 "
                                                + (project.getRevenueMonthsProcessed() + 1) + "/3개월")
                                        .orElse(offeredShortTermProjects + "건 제안")),
                        activeShortTermProject.map(com.game.buildingstory.domain.CompanyShortTermProject::progressPercent)
                                .orElseGet(() -> earningShortTermProject
                                        .map(project -> project.getRevenueMonthsProcessed() * 100 / 3)
                                        .orElse(0)),
                        metrics(
                                metric("진행 사업", activeShortTermProject.isPresent() ? "1건" : "0건", "초기 한도 1건", ""),
                                metric("수익 사업", earningShortTermProject.isPresent() ? "1건" : "0건", "완료 후 3개월", "good"),
                                metric("신규 제안", offeredShortTermProjects + "건", "유효기간 2개월", offeredShortTermProjects > 0 ? "warn" : ""),
                                metric("월 가용 업무", companyShortTermProjectService.availableMonthlyWork(company) + "점",
                                        "영업마케팅팀", "")
                        ), salesSecretary),
                operation("contracts", "사업", "기업계약", "기업 고객용 AI 구축과 월 이용 계약",
                        buildingCustomerContract.isPresent() ? "구축 중"
                                : !renewalCustomerContracts.isEmpty() ? "갱신 결정"
                                : offeredCustomerContracts > 0 ? "제안 도착"
                                : !activeCustomerContracts.isEmpty() ? "계약 운영" : "후보 대기",
                        buildingCustomerContract.isPresent() || offeredCustomerContracts > 0
                                || !renewalCustomerContracts.isEmpty() ? "warn"
                                : !activeCustomerContracts.isEmpty() ? "good" : "muted",
                        "AI개발팀 · 서비스운영팀",
                        growthStage.getDisplayName() + " 최대 " + growthStage.getContractLimit() + "건",
                        buildingCustomerContract
                                .map(contract -> contract.getClientName() + " " + contract.buildProgressPercent() + "%")
                                .orElseGet(() -> !activeCustomerContracts.isEmpty()
                                        ? activeCustomerContracts.getFirst().getClientName() + " "
                                                + (activeCustomerContracts.getFirst().getActiveMonthsProcessed() + 1)
                                                + "/" + activeCustomerContracts.getFirst().getContractDurationMonths() + "개월"
                                        : offeredCustomerContracts + "건 제안"),
                        buildingCustomerContract
                                .map(com.game.buildingstory.domain.CompanyCustomerContract::buildProgressPercent)
                                .orElseGet(() -> !activeCustomerContracts.isEmpty()
                                        ? activeCustomerContracts.getFirst().getActiveMonthsProcessed() * 100
                                                / activeCustomerContracts.getFirst().getContractDurationMonths()
                                        : 0),
                        metrics(
                                metric("구축 계약", buildingCustomerContract.isPresent() ? "1건" : "0건",
                                        "업무 슬롯 2개 사용", ""),
                                metric("활성 계약", activeCustomerContracts.size() + "건",
                                        growthStage.getDisplayName() + " 한도 "
                                                + growthStage.getContractLimit() + "건", "good"),
                                metric("월 계약매출", moneyText.format(
                                                companyCustomerContractService.activeMonthlyFee(company)),
                                        "유지비 15% 별도", "good"),
                                metric("SLA 위반", activeCustomerContracts.stream()
                                                .mapToInt(com.game.buildingstory.domain.CompanyCustomerContract::getSlaViolations)
                                                .sum() + "회",
                                        renewalCustomerContracts.isEmpty() ? "운영 계약 기준" : "갱신 결정 필요",
                                        renewalCustomerContracts.isEmpty() ? "" : "warn"),
                                metric("신규 제안", offeredCustomerContracts + "건", "유효기간 3개월",
                                        offeredCustomerContracts > 0 ? "warn" : "")
                        ), salesSecretary),
                operation("compute", "설비", "연산인프라", "클라우드 " + infrastructure.plan().getDisplayName()
                                + " · 자체망 " + infrastructure.network().name(),
                        company.isOperationsSuspended() ? "계약 중단" : activeConstruction
                                .map(construction -> construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE ? "대금 대기" : "공사 중")
                                .orElse(infrastructure.status()),
                        company.isOperationsSuspended() ? "danger" : activeConstruction.map(construction -> "warn").orElse(infrastructure.tone()),
                        "서비스운영팀", "클라우드·자체망", activeConstruction
                                .map(construction -> construction.displayName() + " " + construction.progressPercent() + "%")
                                .orElse("가동률 " + Math.round(infrastructure.utilizationPercent()) + "%"),
                        activeConstruction.map(com.game.buildingstory.domain.CompanyComputeConstruction::progressPercent)
                                .orElse((int) Math.min(100, Math.round(infrastructure.utilizationPercent()))),
                        metrics(
                                metric("운영 방식",
                                        infrastructure.network().capacity() > 0 ? "혼합 운용" : "외부 클라우드",
                                        infrastructure.network().capacity() > 0
                                                ? "클라우드와 자체 연산망 병행" : "초기 설비투자 없음", ""),
                                metric("월 운영비",
                                        moneyText.format(companyInfrastructureService.monthlyInfrastructureCost(company)),
                                        "현재 시장이슈 보정 반영", "warn"),
                                metric("처리수요", String.format("%,d", infrastructure.demand()),
                                        "가용 " + String.format("%,d", infrastructure.usableCapacity()), infrastructure.tone()),
                                metric("자체망 용량", String.format("%,d", infrastructure.network().capacity()), infrastructure.network().name(), ""),
                                metric("연산효율", String.valueOf(company.getComputeEfficiency()), "제품 기준", "")
                        ), operationsSecretary)
        );

        List<CompanyDashboardView.WorkItem> workQueue = new java.util.ArrayList<>();
        long essentialMonthlyCost = companySettlementService.essentialMonthlyCost(company);
        if (essentialMonthlyCost > 0 && company.getCorporateCash() < essentialMonthlyCost * 3) {
            long runway = company.getCorporateCash() / essentialMonthlyCost;
            workQueue.add(new CompanyDashboardView.WorkItem(
                    runway < 1 ? "긴급" : "높음",
                    "danger",
                    "법인현금 소진 위험",
                    "전략재무팀",
                    runway + "개월",
                    "추가 출자·비용 조정 필요",
                    "finance"
            ));
        }
        if (infrastructure.demand() > infrastructure.usableCapacity()) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "긴급", "danger", "연산 처리용량 초과",
                    "서비스운영팀", "즉시", "클라우드·예비용량 검토", "compute"
            ));
        }
        if (company.getPendingOrganizationSystem() != null) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "보통",
                    "warn",
                    company.getPendingOrganizationSystem().getDisplayName() + " 구축",
                    "인사조직팀",
                    company.getOrganizationUpgradeMonthsRemaining() + "개월",
                    "잔여 " + moneyText.format(company.getOrganizationUpgradeRemainingCost()),
                    "hr"
            ));
        }
        companyTutorialService.departments(company).forEach(department -> {
            var load = companyWorkforceService.departmentLoad(company, department);
            if (load.utilizationPercent() >= 115) {
                workQueue.add(new CompanyDashboardView.WorkItem(
                        "높음", "danger", department.getDepartmentType().displayName() + " 과부하",
                        department.getDepartmentType().displayName(), "지속",
                        Math.round(load.utilizationPercent()) + "%",
                        departmentKey(department.getDepartmentType())
                ));
            }
        });
        activeProductProject.ifPresent(project -> workQueue.add(new CompanyDashboardView.WorkItem(
                "보통", "warn", project.getImprovementType().getDisplayName(), "AI개발팀",
                (int) Math.ceil(project.getRemainingWork() / (double) project.getMonthlyAssignedWork()) + "개월",
                project.progressPercent() + "%",
                "core-product"
        )));
        activeShortTermProject.ifPresent(project -> workQueue.add(new CompanyDashboardView.WorkItem(
                "보통", "warn", project.getName(), "영업마케팅팀",
                (int) Math.ceil(project.getRemainingWork() / (double) project.getMonthlyAssignedWork()) + "개월",
                project.progressPercent() + "%",
                "projects"
        )));
        earningShortTermProject.filter(
                com.game.buildingstory.domain.CompanyShortTermProject::isRiskDecisionRequired
        ).ifPresent(project -> workQueue.add(new CompanyDashboardView.WorkItem(
                "높음", "danger", project.getName() + " 환불 대응",
                "영업마케팅팀", "결정 필요", "수익 정산 보류", "projects"
        )));
        buildingCustomerContract.ifPresent(contract -> workQueue.add(new CompanyDashboardView.WorkItem(
                "높음", "warn", contract.getClientName() + " 구축",
                "AI개발팀 · 서비스운영팀",
                Math.max(
                        (int) Math.ceil(contract.getRemainingDevelopmentWork()
                                / (double) contract.getAssignedDevelopmentWork()),
                        (int) Math.ceil(contract.getRemainingOperationsWork()
                                / (double) contract.getAssignedOperationsWork())
                ) + "개월",
                contract.buildProgressPercent() + "%",
                "contracts"
        )));
        activeConstruction.ifPresent(construction -> workQueue.add(new CompanyDashboardView.WorkItem(
                construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE ? "높음" : "보통",
                construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE ? "danger" : "warn",
                construction.displayName(), "서비스운영팀",
                construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE ? "대금 지급" : (construction.getTotalMonths() - construction.getElapsedMonths()) + "개월",
                construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE ? "완공 대기" : construction.progressPercent() + "%",
                "compute"
        )));
        companyWorkforceService.resignationWarnings(company).forEach(employee -> workQueue.add(
                new CompanyDashboardView.WorkItem(
                        "높음", "danger", employee.getName() + " 잔류 협상",
                        employee.getDepartmentType().displayName(), "1개월",
                        "퇴사 위험 " + employee.getResignationRisk(),
                        departmentKey(employee.getDepartmentType())
                )
        ));
        activeIncident.ifPresent(incident -> workQueue.add(new CompanyDashboardView.WorkItem(
                "긴급",
                "danger",
                incidentTitle(incident)
                        + " · " + incident.getSeverity().getDisplayName(),
                "AI개발팀 · 서비스운영팀",
                incident.getStatus() == com.game.buildingstory.domain.CompanyServiceIncidentStatus.AWAITING_DECISION
                        ? "결정 필요"
                        : incident.getStatus() == com.game.buildingstory.domain.CompanyServiceIncidentStatus.RESPONSE_REQUIRED
                        ? "착수 필요" : "대응 중",
                incident.getStatus() == com.game.buildingstory.domain.CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS
                        ? incident.progressPercent() + "%" : "미처리",
                "core-product"
        )));
        if (!renewalCustomerContracts.isEmpty()) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "높음", "danger", "기업계약 갱신 결정",
                    "영업마케팅팀", "결정 필요",
                    renewalCustomerContracts.size() + "건 갱신 대기", "contracts"
            ));
        }
        if (offeredCustomerContracts > 0) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "보통", "warn", "신규 기업계약 검토",
                    "영업마케팅팀", "유효기간 3개월",
                    offeredCustomerContracts + "건 제안", "contracts"
            ));
        }
        if (offeredShortTermProjects > 0) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "보통", "warn", "신규 단기 프로젝트 검토",
                    "영업마케팅팀", "유효기간 2개월",
                    offeredShortTermProjects + "건 제안", "projects"
            ));
        }
        var financeView = companyFinanceService.view(company, essentialMonthlyCost);
        if (financeView.dividendPending()) {
            workQueue.add(new CompanyDashboardView.WorkItem(
                    "높음", "warn", "분기 배당 결정",
                    "전략재무팀", "결정 필요",
                    "배당 가능 " + moneyText.format(financeView.maximumDividend()), "finance"
            ));
        }
        workQueue.sort(java.util.Comparator.comparingInt(item -> switch (item.priority()) {
            case "긴급" -> 0;
            case "높음" -> 1;
            default -> 2;
        }));

        List<CompanyDashboardView.ReportItem> reports = latest == null ? List.of() : List.of(
                report("매출", latestRevenue, previousRevenue),
                report("영업이익", latestProfit, previousProfit),
                report("인건비", latest.getPayrollCost(), previous == null ? 0 : previous.getPayrollCost()),
                report("기말 법인현금", latest.getClosingCash(), previous == null ? latest.getOpeningCash() : previous.getClosingCash())
        );

        return new CompanyDashboardView(
                new CompanyDashboardView.Identity(company.getCompanyName(), growthStage.getDisplayName(), company.getServiceName(),
                        "설립 " + Math.max(1, (player.getElapsedDays() - company.getEstablishedElapsedDay()) / 30 + 1) + "개월차", "서울", "플레이어"),
                List.of(
                        metric("법인현금", moneyText.format(company.getCorporateCash()), company.isOperationsSuspended() ? "현재 잔액 · 운영중단" : "현재 잔액 · 정상 운영", company.isOperationsSuspended() ? "danger" : ""),
                        metric(latest == null ? "월 반복매출" : "월간매출", moneyText.format(latestRevenue),
                                latest == null ? "현재값 · 첫 정산 전" : "최근 월 확정", "good"),
                        metric("영업이익",
                                latest == null ? moneyText.format(latestProfit)
                                        : (latestProfit >= 0 ? "▲ " : "▼ ") + moneyText.format(latestProfit),
                                latest == null ? "첫 정산 대기" : latestProfit >= 0 ? "최근 월 확정 · 이익" : "최근 월 확정 · 손실",
                                latestProfit >= 0 ? "good" : "danger"),
                        metric("전체인력", (totalCore + totalGeneral) + "명",
                                "핵심 " + totalCore + " · 일반 " + totalGeneral
                                        + " · 총괄 효율 +" + chiefEfficiencyPercent + "%", ""),
                        metric("AI 벤치마크", company.getPrototypeBenchmark() + "점 · " + market.benchmarkRank() + "위",
                                "경쟁 " + (market.competitors().size() + 1) + "개사 기준", "good"),
                        metric("유료 이용자", String.format("%,d명", company.getPaidUsers()), "현재값 · 전체 요금제", ""),
                        metric("시장점유율", String.format("%.2f%%", market.playerMarketShare()),
                                "전체 유료시장 " + formatPeople(market.totalMarketUsers()), ""),
                        metric("기술부채",
                                company.getTechnicalDebt() + " · " + technicalDebtStatus(company.getTechnicalDebt()),
                                "제품 개선으로 감소", technicalDebtTone(company.getTechnicalDebt()))
                ),
                "core-product",
                departments,
                businesses,
                workQueue,
                companyServiceIncidentService.incidents(company).stream()
                        .limit(3)
                        .map(incident -> new CompanyDashboardView.NewsItem(
                                incident.getOccurredMarketMonth() + "개월차",
                                incident.isSecurityIncident() ? "보안"
                                        : incident.isComputeIncident() ? "설비" : "서비스",
                                incident.getSeverity().getDisplayName() + " "
                                        + incidentTitle(incident) + " 발생",
                                "기업 운영실"
                        ))
                        .toList(),
                reports
        );
    }

    private CompanyServiceIncidentView serviceIncidentView(
            PlayerCompany company,
            double infrastructureUtilizationPercent
    ) {
        int probability = companyServiceIncidentService.currentProbabilityBasisPoints(
                company, infrastructureUtilizationPercent);
        var active = companyServiceIncidentService.activeIncident(company);
        if (active.isEmpty()) {
            return new CompanyServiceIncidentView(
                    "정상",
                    "미처리 장애 없음",
                    "안정성·기술부채·가동률 기준으로 월말에 판정",
                    String.format("%.2f%%", probability / 100.0),
                    "good",
                    0,
                    null,
                    false,
                    false,
                    "",
                    "",
                    ""
            );
        }
        var incident = active.get();
        boolean decisionRequired = incident.getStatus()
                == com.game.buildingstory.domain.CompanyServiceIncidentStatus.AWAITING_DECISION;
        boolean responseRequired = incident.getStatus()
                == com.game.buildingstory.domain.CompanyServiceIncidentStatus.RESPONSE_REQUIRED;
        String status = decisionRequired ? "대응 결정"
                : responseRequired ? "긴급 착수 필요" : "긴급 대응 중";
        String detail = decisionRequired
                ? "긴급 복구는 제품 손상을 일부 회복하고, 고객 보상은 비용을 절반으로 줄입니다."
                : responseRequired
                ? "회사와 AI개발·서비스운영 업무 슬롯이 필요합니다."
                : "AI개발팀과 서비스운영팀이 긴급 복구를 진행하고 있습니다.";
        return new CompanyServiceIncidentView(
                status,
                incidentTitle(incident)
                        + " · " + incident.getSeverity().getDisplayName(),
                detail,
                String.format("%.2f%%", probability / 100.0),
                "danger",
                incident.progressPercent(),
                incident.getId(),
                decisionRequired,
                responseRequired,
                moneyText.format(incident.getFullResponseCost()),
                moneyText.format(incident.getFullResponseCost() / 2),
                moneyText.format(incident.currentMonthlyCost())
        );
    }

    private CompanyMarketView marketView(CompanyMarketService.MarketSnapshot market) {
        return new CompanyMarketView(
                String.format("%,d명", market.totalMarketUsers()),
                String.format("%.2f%%", market.playerMarketShare()),
                String.format("%.1f점", market.marketBenchmark()),
                market.benchmarkRank() + "위",
                "일반 " + percent(market.normalSubscribers(), market.paidUsers())
                        + " · 프로 " + percent(market.proSubscribers(), market.paidUsers())
                        + " · 맥스 " + percent(market.maxSubscribers(), market.paidUsers()),
                String.format("%,d명", market.normalSubscribers()),
                String.format("%,d명", market.proSubscribers()),
                String.format("%,d명", market.maxSubscribers()),
                market.competitors().stream().map(item -> new CompanyMarketView.Competitor(
                        item.name(), item.strategy(), item.benchmark() + "점",
                        String.format("%.2f%%", item.marketShare()), String.valueOf(item.completeness()),
                        String.valueOf(item.stability()), String.valueOf(item.security())
                )).toList()
        );
    }

    private CompanyInfrastructureView infrastructureView(
            PlayerCompany company,
            CompanyInfrastructureService.InfrastructureSnapshot infrastructure,
            int currentElapsedDay
    ) {
        return new CompanyInfrastructureView(
                infrastructure.plan().getDisplayName(),
                infrastructure.pendingPlan() == null ? "없음" : infrastructure.pendingPlan().getDisplayName(),
                String.format("%,d", infrastructure.demand()),
                String.format("%,d", infrastructure.cloudCapacity()),
                infrastructure.network().name(),
                String.format("%,d", infrastructure.network().capacity()),
                moneyText.format(infrastructure.network().monthlyCost()),
                String.format("%,d", infrastructure.permanentCapacity()),
                String.format("%,d", infrastructure.reserveCapacity()),
                String.format("%,d", infrastructure.totalCapacity()),
                infrastructure.reserveRemainingDays() > 0 ? infrastructure.reserveRemainingDays() + "일" : "없음",
                infrastructure.reserveRemainingDays() > 0 ? moneyText.format(company.getReserveComputeCost()) : "0원",
                infrastructure.reserveRemainingDays() > 0,
                String.format("%,d", infrastructure.operationsCapacity()),
                String.format("%,d", infrastructure.usableCapacity()),
                String.format("%.1f%%", infrastructure.utilizationPercent()),
                infrastructure.status(),
                infrastructure.tone(),
                companyInfrastructureService.plans().stream().map(plan -> new CompanyInfrastructureView.PlanOption(
                        plan.name(), plan.getDisplayName(), String.format("%,d", plan.getCapacity()),
                        moneyText.format(companyInfrastructureService.monthlyCloudCost(company, plan)),
                        plan == company.getCloudPlanType()
                )).toList(),
                companyInfrastructureService.reserveOptions(company, currentElapsedDay).stream()
                        .map(option -> new CompanyInfrastructureView.ReserveOption(
                                option.percentage(), String.format("%,d", option.capacity()),
                                moneyText.format(option.cost()), option.available(), option.reason()
                        )).toList(),
                companyComputeConstructionService.activeConstruction(company)
                        .map(construction -> new CompanyInfrastructureView.Construction(
                                construction.displayName(),
                                construction.getElapsedMonths() + " / " + construction.getTotalMonths() + "개월",
                                construction.progressPercent(),
                                moneyText.format(construction.getCompletionPayment()),
                                construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE
                        ))
                        .orElse(null),
                nextConstructionView(company)
        );
    }

    private CompanyInfrastructureView.NextConstruction nextConstructionView(PlayerCompany company) {
        var opportunity = companyComputeConstructionService.opportunity(company);
        var step = opportunity.step();
        if (step == null) {
            return new CompanyInfrastructureView.NextConstruction(
                    "최종 증설 완료", "-", "-", "-", "-", "-", false, opportunity.reason());
        }
        return new CompanyInfrastructureView.NextConstruction(
                step.displayName(),
                String.format("%,d", step.capacity()),
                moneyText.format(step.totalCost()),
                moneyText.format(step.upfrontPayment()),
                moneyText.format(step.monthlyCost()),
                step.months() + "개월",
                opportunity.available(),
                opportunity.reason()
        );
    }

    private CompanyProductProjectView productProjectView(PlayerCompany company) {
        var active = companyProductProjectService.activeProject(company);
        if (active.isPresent()) {
            var project = active.get();
            int expectedMonths = (int) Math.ceil(
                    project.getRemainingWork() / (double) project.getMonthlyAssignedWork());
            return new CompanyProductProjectView(
                    true,
                    project.getImprovementType().getDisplayName(),
                    project.getImprovementType().supportsDevelopmentDirection()
                            ? project.getDevelopmentDirection().getDisplayName()
                            : "",
                    (project.getTotalWork() - project.getRemainingWork()) + " / " + project.getTotalWork(),
                    project.progressPercent(),
                    expectedMonths + "개월 후",
                    List.of(),
                    List.of()
            );
        }

        int monthlyWork = companyProductProjectService.availableMonthlyWork(company);
        var directions = java.util.Arrays.stream(CompanyDevelopmentDirection.values())
                .map(direction -> new CompanyProductProjectView.Direction(
                        direction.name(), direction.getDisplayName(), direction.getDescription()))
                .toList();
        return new CompanyProductProjectView(
                false,
                "",
                "",
                "",
                0,
                "",
                java.util.Arrays.stream(CompanyProductImprovementType.values())
                        .map(type -> {
                            var eligibility = companyProductProjectService.eligibility(company, type);
                            int totalWork = companyProductProjectService.totalWork(type, company.getPrototypeBenchmark());
                            boolean available = monthlyWork > 0 && eligibility.available();
                            String reason = monthlyWork <= 0 ? "AI개발팀 업무 여유 부족" : eligibility.reason();
                            return new CompanyProductProjectView.Option(
                                    type.name(),
                                    type.getDisplayName(),
                                    "작업량 " + totalWork,
                                    monthlyWork <= 0 ? "시작 불가" : (int) Math.ceil(totalWork / (double) monthlyWork) + "개월",
                                    type.getResult(),
                                    type.getRisk(),
                                    type.supportsDevelopmentDirection(),
                                    available,
                                    reason
                            );
                        })
                        .toList(),
                directions
        );
    }

    private String tutorialNotice(PlayerCompany company) {
        return switch (company.getTutorialStage()) {
            case LAUNCHED -> "첫 월 결산을 기다리는 중";
            case FIRST_SETTLEMENT -> "첫 분기보고서를 열람하면 기업 튜토리얼 완료";
            default -> "";
        };
    }

    private String recentProductResult(PlayerCompany company) {
        return companyProductProjectService.projects(company).stream()
                .filter(project -> project.getStatus()
                        != com.game.buildingstory.domain.CompanyProductProjectStatus.ACTIVE)
                .findFirst()
                .map(project -> project.getImprovementType().getDisplayName() + " · "
                        + switch (project.getStatus()) {
                            case COMPLETED -> "완료 품질 " + project.getCompletionQuality();
                            case CANCELLED -> "취소";
                            case FAILED -> "자금 고갈로 실패";
                            default -> project.getStatus().name();
                        })
                .orElse("");
    }

    private String recentShortTermResult(PlayerCompany company) {
        return companyShortTermProjectService.projects(company).stream()
                .filter(project -> project.getStatus()
                        != com.game.buildingstory.domain.CompanyShortTermProjectStatus.OFFERED)
                .filter(project -> project.getStatus()
                        != com.game.buildingstory.domain.CompanyShortTermProjectStatus.ACTIVE)
                .filter(project -> project.getStatus()
                        != com.game.buildingstory.domain.CompanyShortTermProjectStatus.EARNING)
                .findFirst()
                .map(project -> project.getName() + " · " + switch (project.getStatus()) {
                    case COMPLETED -> "3개월 수익 종료 · 품질 " + project.getCompletionQuality();
                    case CANCELLED -> "수행 중 취소";
                    case FAILED -> "자금 고갈로 실패";
                    case REJECTED -> "제안 거절";
                    case EXPIRED -> "제안 만료";
                    default -> project.getStatus().name();
                })
                .orElse("");
    }

    private String recentContractResult(PlayerCompany company) {
        return companyCustomerContractService.contracts(company).stream()
                .filter(contract -> switch (contract.getStatus()) {
                    case COMPLETED, FAILED, TERMINATED, EXPIRED, REJECTED -> true;
                    default -> false;
                })
                .findFirst()
                .map(contract -> contract.getClientName() + " · " + switch (contract.getStatus()) {
                    case COMPLETED -> "계약 완료";
                    case FAILED -> "구축 실패";
                    case TERMINATED -> "중도 종료";
                    case EXPIRED -> "제안 만료";
                    case REJECTED -> "제안 거절";
                    default -> contract.getStatus().name();
                })
                .orElse("");
    }

    private CompanyShortTermProjectView shortTermProjectView(PlayerCompany company) {
        var active = companyShortTermProjectService.activeProject(company);
        if (active.isPresent()) {
            var project = active.get();
            return new CompanyShortTermProjectView(
                    "수행 중",
                    project.getName(),
                    project.progressPercent(),
                    (project.getTotalWork() - project.getRemainingWork()) + " / " + project.getTotalWork(),
                    "이번 달 비용 " + moneyText.format(project.currentMonthlyCost()),
                    project.getId(),
                    false,
                    "",
                    "",
                    "",
                    List.of()
            );
        }
        var earning = companyShortTermProjectService.earningProject(company);
        if (earning.isPresent()) {
            var project = earning.get();
            return new CompanyShortTermProjectView(
                    project.isRiskDecisionRequired() ? "대응 결정" : "수익 정산",
                    project.getName(),
                    project.getRevenueMonthsProcessed() * 100 / 3,
                    project.isRiskDecisionRequired()
                            ? "결정 전까지 수익 정산 보류"
                            : (project.getRevenueMonthsProcessed() + 1) + " / 3개월",
                    project.isRiskDecisionRequired()
                            ? "환불 요구 발생"
                            : "이번 달 예상수익 " + moneyText.format(project.currentRevenue())
                            + (project.currentMonthlyCost() > 0
                            ? " · 추가비용 " + moneyText.format(project.currentMonthlyCost()) : ""),
                    project.getId(),
                    project.isRiskDecisionRequired(),
                    project.getRisk().getDisplayName(),
                    project.isRiskDecisionRequired()
                            ? "추가 지원은 수익을 보전하고, 부분 환불은 남은 사업수익을 30% 줄입니다."
                            : "",
                    moneyText.format(project.extraSupportCost()),
                    List.of()
            );
        }

        boolean available = companyShortTermProjectService.canStartProject(company);
        String reason = available ? "수락 가능"
                : company.getActiveMajorWorkCount() >= companyWorkforceService.companyMajorWorkSlotLimit(company)
                ? "회사 주요 업무 슬롯 부족" : "영업마케팅팀 업무 여유 부족";
        return new CompanyShortTermProjectView(
                "제안 검토",
                "",
                0,
                "",
                "",
                null,
                false,
                "",
                "",
                "",
                companyShortTermProjectService.offeredProjects(company).stream()
                        .map(project -> new CompanyShortTermProjectView.Offer(
                                project.getId(),
                                project.getName(),
                                "작업량 " + project.getTotalWork(),
                                moneyText.format(project.getFirstMonthRevenue() * 210 / 100),
                                moneyText.format(project.getTotalCost()),
                                Math.max(0, project.getExpiresMarketMonth() - company.getMarketMonthsProcessed()) + "개월 남음",
                                available,
                                reason
                        ))
                        .toList()
        );
    }

    private String incidentTitle(com.game.buildingstory.domain.CompanyServiceIncident incident) {
        if (incident.isSecurityIncident()) {
            return "보안사고";
        }
        return incident.isComputeIncident() ? "연산장비 장애" : "서비스 장애";
    }

    private CompanyCustomerContractView customerContractView(PlayerCompany company) {
        var contracts = companyCustomerContractService.contracts(company);
        var contractItems = customerContractItems(contracts);
        var building = contracts.stream()
                .filter(contract -> contract.getStatus()
                        == CompanyCustomerContractStatus.BUILDING)
                .findFirst();
        if (building.isPresent()) {
            var contract = building.get();
            return new CompanyCustomerContractView(
                    "구축 중",
                    contract.getClientName() + " · " + contract.getProjectName(),
                    contract.buildProgressPercent(),
                    "개발 " + contract.getRemainingDevelopmentWork()
                            + " · 운영 " + contract.getRemainingOperationsWork() + " 작업 남음"
                            + (contract.getContractType().hasRiskRules()
                            ? " · 납기 " + contract.getBuildMonthsProcessed()
                                    + "/" + contract.getBuildDeadlineMonths() + "개월"
                            : ""),
                    "이번 달 구축비용 " + moneyText.format(contract.currentCost()),
                    null,
                    "",
                    List.of(),
                    contractItems
            );
        }
        var completionPayment = contracts.stream()
                .filter(contract -> contract.getStatus()
                        == CompanyCustomerContractStatus.COMPLETION_PAYMENT)
                .findFirst();
        if (completionPayment.isPresent()) {
            var contract = completionPayment.get();
            return new CompanyCustomerContractView(
                    "검수 통과",
                    contract.getClientName() + " · " + contract.getProjectName(),
                    100,
                    "다음 월 정산에서 구축 완료금 수령 후 서비스 시작",
                    "완료금 " + moneyText.format(contract.currentRevenue()),
                    null,
                    "",
                    List.of(),
                    contractItems
            );
        }
        var terminalPayment = contracts.stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT
                        || contract.getStatus() == CompanyCustomerContractStatus.TERMINATION_PAYMENT)
                .findFirst();
        if (terminalPayment.isPresent()) {
            var contract = terminalPayment.get();
            return new CompanyCustomerContractView(
                    contract.getStatus() == CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT
                            ? "구축 실패 정산" : "중도 종료 정산",
                    contract.getClientName() + " · " + contract.getProjectName(),
                    100,
                    "다음 월 정산에서 반환금·위약금 지급",
                    "지급 예정 " + moneyText.format(contract.currentCost()),
                    null,
                    "",
                    List.of(),
                    contractItems
            );
        }
        var renewal = companyCustomerContractService.renewalContracts(company);
        if (!renewal.isEmpty()) {
            var contract = renewal.getFirst();
            long renewalFee = contract.getSlaViolations() == 1
                    ? contract.getMonthlyFee() * 90 / 100 : contract.getMonthlyFee();
            return new CompanyCustomerContractView(
                    "갱신 결정",
                    contract.getClientName() + " · " + contract.getProjectName(),
                    100,
                    "SLA 위반 " + contract.getSlaViolations() + "회",
                    "갱신 월 이용료 " + moneyText.format(renewalFee),
                    contract.getId(),
                    contract.getSlaViolations() == 0 ? "기존 조건 정상 갱신" : "월 이용료 10% 인하 조건",
                    customerContractOffers(company),
                    contractItems
            );
        }
        var active = companyCustomerContractService.activeContracts(company);
        if (!active.isEmpty()) {
            var contract = active.getFirst();
            return new CompanyCustomerContractView(
                    "계약 운영",
                    contract.getClientName() + " · " + contract.getProjectName(),
                    contract.getActiveMonthsProcessed() * 100 / contract.getContractDurationMonths(),
                    (contract.getActiveMonthsProcessed() + 1) + " / "
                            + contract.getContractDurationMonths() + "개월"
                            + " · SLA 위반 " + contract.getSlaViolations() + "회",
                    "월 이용료 " + moneyText.format(contract.getMonthlyFee()),
                    null,
                    "",
                    customerContractOffers(company),
                    contractItems
            );
        }

        return new CompanyCustomerContractView(
                "제안 검토",
                "",
                0,
                "",
                "",
                null,
                "",
                customerContractOffers(company),
                contractItems
        );
    }

    private List<CompanyCustomerContractView.ActiveContract> customerContractItems(
            List<com.game.buildingstory.domain.CompanyCustomerContract> contracts
    ) {
        return contracts.stream()
                .filter(contract -> switch (contract.getStatus()) {
                    case BUILDING, COMPLETION_PAYMENT, ACTIVE, RENEWAL_OFFERED,
                            CONTRACT_FAILURE_PAYMENT, TERMINATION_PAYMENT -> true;
                    default -> false;
                })
                .map(contract -> {
                    String status = switch (contract.getStatus()) {
                        case BUILDING -> "구축 중";
                        case COMPLETION_PAYMENT -> "검수 통과";
                        case ACTIVE -> "운영 중";
                        case RENEWAL_OFFERED -> "갱신 결정";
                        case CONTRACT_FAILURE_PAYMENT -> "구축 실패 정산";
                        case TERMINATION_PAYMENT -> "중도 종료 정산";
                        default -> "";
                    };
                    int progress = switch (contract.getStatus()) {
                        case BUILDING -> contract.buildProgressPercent();
                        case ACTIVE -> contract.getActiveMonthsProcessed() * 100
                                / Math.max(1, contract.getContractDurationMonths());
                        default -> 100;
                    };
                    String detail = switch (contract.getStatus()) {
                        case BUILDING -> "개발 " + contract.getRemainingDevelopmentWork()
                                + " · 운영 " + contract.getRemainingOperationsWork() + " 작업 남음";
                        case ACTIVE -> (contract.getActiveMonthsProcessed() + 1) + " / "
                                + contract.getContractDurationMonths() + "개월 · SLA 위반 "
                                + contract.getSlaViolations() + "회";
                        case RENEWAL_OFFERED -> "SLA 위반 " + contract.getSlaViolations() + "회";
                        case COMPLETION_PAYMENT -> "다음 월 정산에서 서비스 시작";
                        default -> "다음 월 정산에서 반환금·위약금 지급";
                    };
                    String amount = switch (contract.getStatus()) {
                        case BUILDING -> "구축비용 " + moneyText.format(contract.currentCost());
                        case COMPLETION_PAYMENT -> "완료금 " + moneyText.format(contract.currentRevenue());
                        case ACTIVE, RENEWAL_OFFERED -> "월 이용료 " + moneyText.format(contract.getMonthlyFee());
                        default -> "지급 예정 " + moneyText.format(contract.currentCost());
                    };
                    long renewalFee = contract.getSlaViolations() == 1
                            ? contract.getMonthlyFee() * 90 / 100 : contract.getMonthlyFee();
                    return new CompanyCustomerContractView.ActiveContract(
                            contract.getId(), status,
                            contract.getClientName() + " · " + contract.getProjectName(),
                            detail, amount, progress,
                            contract.getStatus() == CompanyCustomerContractStatus.RENEWAL_OFFERED,
                            contract.getStatus() == CompanyCustomerContractStatus.RENEWAL_OFFERED
                                    ? "갱신 월 이용료 " + moneyText.format(renewalFee) : ""
                    );
                })
                .toList();
    }

    private List<CompanyCustomerContractView.Offer> customerContractOffers(PlayerCompany company) {
        return companyCustomerContractService.offeredContracts(company).stream()
                .map(contract -> {
                    boolean available = companyCustomerContractService.canAccept(company, contract);
                    String reason = available ? "수락 가능"
                            : company.getActiveMajorWorkCount() + 2
                                    > companyWorkforceService.companyMajorWorkSlotLimit(company)
                            ? "회사 주요 업무 슬롯 2개 필요"
                            : "제품 조건 또는 부서 업무 여유 부족";
                    return new CompanyCustomerContractView.Offer(
                            contract.getId(),
                            contract.getContractType().getDisplayName(),
                            contract.getClientName(),
                            contract.getProjectName(),
                            "벤치마크 " + contract.getRequiredBenchmark()
                                    + " · 안정성 " + contract.getRequiredStability()
                                    + " · 보안 " + contract.getRequiredSecurity(),
                            moneyText.format(contract.getConstructionFee()),
                            moneyText.format(contract.getMonthlyFee()),
                            contract.getContractDurationMonths() + "개월",
                            contract.getContractType().hasRiskRules()
                                    ? contract.getBuildDeadlineMonths() + "개월" : "지연 위험 없음",
                            contract.getContractType().hasRiskRules()
                                    ? "3개월 지연 시 실패 · SLA 2회 시 중도 종료" : "튜토리얼 안전계약",
                            Math.max(0, contract.getExpiresMarketMonth()
                                    - company.getMarketMonthsProcessed()) + "개월 남음",
                            available,
                            reason
                    );
                })
                .toList();
    }

    private String percent(long value, long total) {
        return total == 0 ? "0.0%" : String.format("%.1f%%", value * 100.0 / total);
    }

    private Map<String, CompanyWorkforceView> workforceViews(PlayerCompany company) {
        Map<String, CompanyWorkforceView> views = new LinkedHashMap<>();
        int remainingMonthlyHires = companyWorkforceService.remainingMonthlyHireLimit(company);
        for (CompanyDepartment department : companyTutorialService.departments(company)) {
            int vacancy = Math.max(0, department.getApprovedHeadcount()
                    - department.getGeneralEmployeeCount() - department.getPendingHireCount());
            String key = departmentKey(department.getDepartmentType());
            views.put(key, new CompanyWorkforceView(
                    department.getDepartmentType().name(),
                    department.getGeneralEmployeeCount(),
                    department.getAdaptingEmployeeCount(),
                    department.getPendingHireCount(),
                    department.getApprovedHeadcount(),
                    companyWorkforceService.maximumApprovedHeadcount(company, department),
                    Math.min(vacancy, remainingMonthlyHires),
                    moneyText.format(companyWorkforceService.hiringFeePerPerson(
                            company, department.getDepartmentType())),
                    department.getLastGeneralResignations(),
                    String.format("%.1f%%", department.getLastGeneralResignationRate() * 100)
            ));
        }
        return views;
    }

    private Map<String, CompanyCoreTalentView> coreTalentViews(PlayerCompany company, int elapsedDays) {
        Map<String, CompanyCoreTalentView> views = new LinkedHashMap<>();
        int hrExpertise = companyTutorialService.departments(company).stream()
                .anyMatch(department -> department.getDepartmentType() == CompanyDepartmentType.HR_ORGANIZATION)
                ? companyWorkforceService.departmentExpertise(company, CompanyDepartmentType.HR_ORGANIZATION)
                : -1;
        for (CompanyDepartment department : companyTutorialService.departments(company)) {
            var type = department.getDepartmentType();
            var employees = companyWorkforceService.allEmployees(company, type);
            String key = departmentKey(type);
            views.put(key, new CompanyCoreTalentView(
                    employees.size(),
                    5,
                    employees.stream().map(employee -> new CompanyCoreTalentView.Employee(
                            employee.getId(), employee.getName(), employee.getGrade(), employee.getAbility(),
                            employee.getLeadership(), growthSpeedText(employee.getGrowthSpeed(), hrExpertise),
                            employee.getTrainingMonthsRemaining() > 0
                                    ? "전문 연수 " + employee.getTrainingMonthsRemaining() + "개월"
                                    : employee.getResignationNoticeMonths() > 0
                                    ? "퇴사 협상" : (employee.isActive() ? "재직" : "입사 대기"),
                            employee.isTeamLeader(),
                            employee.isActive() && employee.getGrade() >= 5 && employee.getLeadership() >= 40,
                            employee.getResignationRisk(), employee.getResignationNoticeMonths() > 0,
                            moneyText.format(employee.retentionBonusCost()),
                            employee.canStartTraining(elapsedDays),
                            moneyText.format(employee.trainingCost())
                    )).toList(),
                    companyWorkforceService.availableCandidates(company, type).stream()
                            .map(candidate -> new CompanyCoreTalentView.Candidate(
                                    candidate.key(), candidate.name(), candidate.specialty(), candidate.grade(),
                                    candidate.ability(), candidate.leadership(),
                                    growthSpeedText(candidate.growthSpeed(), hrExpertise),
                                    moneyText.format(candidate.annualSalary()),
                                    moneyText.format(candidate.signingBonus())
                            )).toList()
            ));
        }
        return views;
    }

    private CompanyGrowthProgressView growthProgress(PlayerCompany company) {
        CompanyGrowthStage current = company.getGrowthStage();
        CompanyGrowthStage[] stages = CompanyGrowthStage.values();
        if (current == CompanyGrowthStage.GLOBAL) {
            return new CompanyGrowthProgressView(
                    current.getDisplayName(), "최고 단계",
                    moneyText.format(company.getMonthlyRecurringRevenue()), "-", "-",
                    100, formatPeople(company.getPaidUsers()), "-", "-",
                    100, 2, "현재값 · 최고 단계", "모든 성장단계 기능 개방", true
            );
        }

        CompanyGrowthStage next = stages[current.ordinal() + 1];
        var reports = companySettlementService.quarterlyReports(company);
        long recurringRevenue = reports.isEmpty()
                ? company.getMonthlyRecurringRevenue()
                : reports.getFirst().getRecurringRevenueAtEnd();
        long paidUsers = reports.isEmpty()
                ? company.getPaidUsers()
                : reports.getFirst().getPaidUsersAtEnd();
        int qualifiedQuarters = (int) reports.stream().limit(2)
                .filter(report -> next.qualifies(
                        report.getRecurringRevenueAtEnd(), report.getPaidUsersAtEnd()))
                .count();
        String basisText = reports.isEmpty() ? "현재값 · 첫 분기 확정 전" : "최근 확정 분기말 기준";
        return new CompanyGrowthProgressView(
                current.getDisplayName(),
                next.getDisplayName(),
                moneyText.format(recurringRevenue),
                moneyText.format(next.getRequiredRecurringRevenue()),
                moneyText.format(Math.max(0, next.getRequiredRecurringRevenue() - recurringRevenue)),
                progressPercent(recurringRevenue, next.getRequiredRecurringRevenue()),
                formatPeople(paidUsers),
                formatPeople(next.getRequiredPaidUsers()),
                formatPeople(Math.max(0, next.getRequiredPaidUsers() - paidUsers)),
                progressPercent(paidUsers, next.getRequiredPaidUsers()),
                qualifiedQuarters,
                basisText,
                next.getUnlockSummary(),
                false
        );
    }

    private CompanyDashboardView.OperationItem liveDepartment(Player player, PlayerCompany company, CompanyDepartment department) {
        CompanyDepartmentType type = department.getDepartmentType();
        var employees = companyWorkforceService.employees(company, type);
        int expertise = companyWorkforceService.departmentExpertise(company, type);
        var load = companyWorkforceService.departmentLoad(company, department);
        String key = departmentKey(type);
        String description = switch (type) {
            case AI_DEVELOPMENT -> "모델 성능·제품 개발";
            case SALES_MARKETING -> "이용자·기업계약 확보";
            case SERVICE_OPERATIONS -> "서비스 안정성·고객지원";
            case HR_ORGANIZATION -> "채용·교육·조직 효율";
            case STRATEGY_FINANCE -> "예산·분석·계약 검토";
        };
        CompanyDashboardView.Secretary secretary = companySecretary(player, company, type);
        String teamLeader = employees.stream()
                .filter(com.game.buildingstory.domain.CompanyCoreEmployee::isTeamLeader)
                .findFirst()
                .map(employee -> employee.getName())
                .orElse("팀장 미배치");
        String leader = teamLeader + " / "
                + ("미배치".equals(secretary.name()) ? "비서 미배치" : secretary.name());
        int pendingCoreEmployees = (int) companyWorkforceService.allEmployees(company, type).stream()
                .filter(employee -> !employee.isActive())
                .count();
        int pendingEmployees = Math.addExact(department.getPendingHireCount(), pendingCoreEmployees);
        return operation(key, "부서", type.displayName(), description, load.status(), load.tone(), leader,
                "핵심 " + employees.size() + " · 일반 " + department.getGeneralEmployeeCount()
                        + " (입사대기 " + pendingEmployees + ")",
                "업무량 " + load.totalWorkload() + " / " + load.capacity(),
                (int) Math.round(load.utilizationPercent()),
                metrics(
                        metric("부서 전문성", String.valueOf(expertise), "핵심인재 가중 합산", expertise >= 50 ? "good" : "warn"),
                        metric("월 처리능력", String.valueOf(load.capacity()), "일반인력·숙련·전문성", ""),
                        metric("현재 업무량", String.valueOf(load.totalWorkload()), "기본 " + load.baseWorkload() + " · 주요 " + load.allocatedMajorWorkload(), ""),
                        metric("부서 가동률", Math.round(load.utilizationPercent()) + "%", load.status(), load.tone()),
                        metric("신규 가용능력", String.valueOf(load.availableCapacity()), "처리능력 100% 기준", ""),
                        metric("주요 업무 슬롯", load.usedSlots() + " / " + load.slotLimit(), "남은 " + load.availableSlots() + "개", ""),
                        metric("일반인력", department.getGeneralEmployeeCount() + "명", "승인 정원 " + department.getApprovedHeadcount() + "명", ""),
                        metric("일반 평균숙련", String.valueOf(department.getAverageGeneralSkill()), "초기 조직", ""),
                        metric("핵심 월급", moneyText.format(employees.stream().mapToLong(employee -> employee.getAnnualSalary() / 12).sum()), "월 환산", "")
                ), secretary);
    }

    private CompanyDashboardView.Secretary companySecretary(
            Player player,
            PlayerCompany company,
            CompanyDepartmentType type
    ) {
        String key = companySecretaryService.secretaryKey(type);
        var owned = gameService.ownedSecretaries(player).stream()
                .filter(secretary -> secretary.getSecretaryKey().equals(key))
                .findFirst();
        var spec = gameService.secretarySpec(key);
        if (owned.isEmpty()) {
            return secretary("미배치", type.displayName(), spec.imagePath(), "미배치", "효과 없음", "담당 비서를 배치하면 부서 고유 효과가 적용됩니다.");
        }
        int level = owned.get().getCompanyProficiencyLevel();
        int effectPercent = companySecretaryService.effectPercent(company, type);
        String effect = switch (type) {
            case AI_DEVELOPMENT -> "제품 개발기간 " + effectPercent + "% 감소";
            case SALES_MARKETING -> "홍보 효과 " + effectPercent + "% 증가";
            case SERVICE_OPERATIONS -> "장애 발생률 " + effectPercent + "% 감소";
            case HR_ORGANIZATION -> "핵심인재 성장속도 " + effectPercent + "% 증가";
            case STRATEGY_FINANCE -> "재무 전망 오차 " + effectPercent + "% 감소";
        };
        return secretary(spec.name(), type.displayName(), spec.imagePath(),
                "기업 숙련도 " + level + "단계 · 경력 " + owned.get().getCompanyCareerMonths() + "개월",
                effect, "정상 운영 월마다 기업 경력이 1개월 누적됩니다.");
    }

    private CompanyExecutiveOfficeView executiveOffice(
            Player player,
            PlayerCompany company,
            CompanyDashboardView dashboard
    ) {
        var bottleneck = dashboard.departments().stream()
                .max(java.util.Comparator.comparingInt(CompanyDashboardView.OperationItem::progressPercent));
        int highestUtilization = bottleneck
                .map(CompanyDashboardView.OperationItem::progressPercent)
                .orElse(0);
        int averageUtilization = (int) Math.round(dashboard.departments().stream()
                .mapToInt(CompanyDashboardView.OperationItem::progressPercent)
                .average()
                .orElse(0));
        int effectPercent = companySecretaryService.chiefEffectPercent(company);
        String status = highestUtilization >= 100 ? "조정 필요"
                : highestUtilization >= 85 ? "주의" : "정상";
        String tone = highestUtilization >= 100 ? "danger"
                : highestUtilization >= 85 ? "warn" : "good";
        String advice = bottleneck
                .map(item -> item.name() + " 가동률이 " + item.progressPercent()
                        + "%로 가장 높습니다. "
                        + executiveRecommendation(item.status(), item.progressPercent()))
                .orElse("출범한 부서가 없습니다.");
        CompanyDashboardView.Secretary chiefSecretary =
                chiefSecretary(player, company, effectPercent, advice);
        CompanyDashboardView.OperationItem operation = operation(
                "executive-office",
                "대표실",
                "대표실·총괄",
                "전사 조정·병목 분석·부서 업무 지시",
                status,
                tone,
                "플레이어 / " + chiefSecretary.name(),
                "총괄 비서 1명",
                "전체 처리능력 +" + effectPercent + "%",
                highestUtilization,
                metrics(
                        metric("최고 가동률", highestUtilization + "%",
                                bottleneck.map(CompanyDashboardView.OperationItem::name).orElse("부서 없음"), tone),
                        metric("평균 가동률", averageUtilization + "%",
                                dashboard.departments().size() + "개 부서 기준", ""),
                        metric("전사 처리능력", "+" + effectPercent + "%",
                                "모든 부서 월 처리능력", "good"),
                        metric("핵심인재 성장", "+" + effectPercent + "%",
                                "정상 운영 월 성장속도", "good")
                ),
                chiefSecretary
        );
        List<CompanyExecutiveOfficeView.DepartmentCommand> commands = dashboard.departments().stream()
                .map(item -> new CompanyExecutiveOfficeView.DepartmentCommand(
                        item.name(),
                        executiveCommandTarget(item.key()),
                        item.status(),
                        item.tone(),
                        executiveRecommendation(item.status(), item.progressPercent())
                ))
                .toList();
        return new CompanyExecutiveOfficeView(operation, commands);
    }

    private CompanyDashboardView.Secretary chiefSecretary(
            Player player,
            PlayerCompany company,
            int effectPercent,
            String advice
    ) {
        String key = "secretary-6";
        var owned = gameService.ownedSecretaries(player).stream()
                .filter(secretary -> key.equals(secretary.getSecretaryKey()))
                .findFirst();
        var spec = gameService.secretarySpec(key);
        if (owned.isEmpty()) {
            return secretary("미배치", "대표실·총괄", spec.imagePath(), "미배치",
                    "효과 없음", "총괄비서가 기업에 합류하면 전사 조정 효과가 적용됩니다.");
        }
        int level = owned.get().getCompanyProficiencyLevel();
        return secretary(
                spec.name(),
                "대표실·총괄",
                spec.imagePath(),
                "기업 숙련도 " + level + "단계 · 경력 " + owned.get().getCompanyCareerMonths() + "개월",
                "전체 부서 처리능력·핵심인재 성장속도 " + effectPercent + "% 증가",
                advice
        );
    }

    private String executiveCommandTarget(String departmentKey) {
        return switch (departmentKey) {
            case "development" -> "core-product";
            case "sales" -> "projects";
            case "operations" -> "compute";
            case "hr" -> "hr";
            case "finance" -> "finance";
            default -> departmentKey;
        };
    }

    private String executiveRecommendation(String status, int utilizationPercent) {
        if (status.contains("인력 과잉")) {
            return "여유 인력을 활용할 신규 업무를 검토하십시오.";
        }
        if (status.contains("인력 부족")) {
            return "신규 업무보다 인력 증원을 우선하십시오.";
        }
        if (utilizationPercent >= 100) {
            return "업무량 조정 또는 인력 증원이 필요합니다.";
        }
        if (utilizationPercent >= 85) {
            return "신규 착수 전에 진행 업무를 확인하십시오.";
        }
        if (utilizationPercent <= 60) {
            return "추가 업무를 배정할 여유가 있습니다.";
        }
        return "현재 운영을 유지할 수 있습니다.";
    }

    private CompanyDashboardView.ReportItem report(String label, long current, long previous) {
        if (previous == 0) {
            return new CompanyDashboardView.ReportItem(label, moneyText.format(current), "-", "첫 확정", "muted");
        }
        double change = (current - previous) * 100.0 / Math.abs(previous);
        return new CompanyDashboardView.ReportItem(label, moneyText.format(current), moneyText.format(previous),
                String.format("%+.2f%%", change), change >= 0 ? "good" : "danger");
    }

    private CompanyPreparationView preparation(Player player) {
        var secretaries = gameService.ownedSecretaries(player);
        int readySecretaries = (int) secretaries.stream()
                .filter(secretary -> secretary.getProficiency() >= 30 && secretary.getAffinity() >= 30)
                .count();
        int managerCount = gameService.propertyManagers(player).size();

        return new CompanyPreparationView(
                player.getCash(),
                moneyText.format(player.getCash()),
                PlayerCompanyService.MINIMUM_INVESTMENT,
                PlayerCompanyService.RECOMMENDED_INVESTMENT,
                PlayerCompanyService.AGGRESSIVE_INVESTMENT,
                PlayerCompanyService.SECRETARY_TRAINING_COST,
                PlayerCompanyService.ESTIMATED_MONTHLY_FIXED_COST,
                secretaries.size(),
                readySecretaries,
                managerCount,
                player.getCash() >= PlayerCompanyService.MINIMUM_INVESTMENT ? 1 : 0,
                1,
                List.of(
                        new CompanyPreparationView.Requirement("설립 자금", moneyText.format(player.getCash()), "최소 1,500억원", player.getCash() >= PlayerCompanyService.MINIMUM_INVESTMENT, true),
                        new CompanyPreparationView.Requirement("비서 성장", readySecretaries + " / 6명", "개발 중 검증 보류", true, false),
                        new CompanyPreparationView.Requirement("부동산 인계", managerCount + " / 6명", "개발 중 검증 보류", true, false)
                )
        );
    }

    private CompanyDashboardView previewDashboard() {
        var developmentSecretary = secretary(
                "설하은",
                "AI개발팀",
                "/assets/secretaries/secretary-2.png",
                "기업 숙련도 1단계",
                "제품·연구 프로젝트 시간 4% 감소",
                "차세대 모델 연구보다 현재 플랫폼의 안정성과 처리효율을 먼저 보완하는 편이 안전합니다."
        );
        var salesSecretary = secretary(
                "이다은",
                "영업마케팅팀",
                "/assets/secretaries/secretary-3.png",
                "기업 숙련도 1단계",
                "같은 홍보비의 마케팅 효과 5% 증가",
                "프로 요금제 개방 전에는 일반 이용자 유지율과 첫 기업계약 확보가 우선입니다."
        );
        var operationsSecretary = secretary(
                "설아름",
                "서비스운영팀",
                "/assets/secretaries/secretary-1.png",
                "기업 숙련도 1단계",
                "장애·서비스 결함 발생률 4% 감소",
                "가동률이 80%를 넘었습니다. 다음 이용자 증가 전에 처리용량 확보가 필요합니다."
        );
        var hrSecretary = secretary(
                "한아리",
                "인사조직팀",
                "/assets/secretaries/secretary-4.png",
                "부서 출범 준비",
                "핵심 직원 성장속도 5% 증가",
                "개발팀의 업무 여유가 낮습니다. 핵심 인재와 일반 실무인력 채용을 함께 검토해야 합니다."
        );
        var financeSecretary = secretary(
                "김채린",
                "전략재무팀",
                "/assets/secretaries/secretary-5.png",
                "부서 출범 준비",
                "재무 전망의 예측 오차 10% 감소",
                "현재 투자계획을 반영해도 필수 고정비 18개월분을 보유하고 있습니다."
        );

        List<CompanyDashboardView.OperationItem> departments = List.of(
                operation("development", "부서", "AI개발팀", "모델 성능·제품 개발", "우수", "good", "이준호 · 설하은", "2,156 / 2,500명", "AI 성능 +18.7%", 78,
                        metrics(metric("부서 전문성", "82", "핵심 인재 4명", "good"), metric("업무 가동률", "78%", "정상 범위", ""), metric("기술부채", "12", "낮음", "good"), metric("진행 업무", "2 / 3", "1개 여유", "")), developmentSecretary),
                operation("sales", "부서", "영업마케팅팀", "이용자·기업계약 확보", "우수", "good", "최민수 · 이다은", "2,340 / 2,800명", "매출 성장 +15.3%", 72,
                        metrics(metric("부서 전문성", "76", "핵심 인재 3명", "good"), metric("업무 가동률", "72%", "정상 범위", ""), metric("브랜드", "68", "상승 중", "good"), metric("계약 파이프라인", "4건", "검토 2건", "")), salesSecretary),
                operation("operations", "부서", "서비스운영팀", "서비스 안정성·고객지원", "양호", "good", "박지은 · 설아름", "2,987 / 3,200명", "서비스 안정성 +12.3%", 81,
                        metrics(metric("부서 전문성", "73", "핵심 인재 3명", ""), metric("업무 가동률", "81%", "증원 검토", "warn"), metric("서비스 안정성", "94", "양호", "good"), metric("미처리 장애", "0건", "정상", "good")), operationsSecretary),
                operation("hr", "부서", "인사조직팀", "채용·성장·조직 관리", "출범 준비", "muted", "정우혁 · 한아리", "1,245 / 1,500명", "조직 효율 +10.4%", 67,
                        metrics(metric("부서 전문성", "58", "출범 준비", "warn"), metric("업무 가동률", "67%", "정상 범위", ""), metric("채용 여력", "255명", "이번 달", ""), metric("이직 위험", "낮음", "핵심 인재 1명 주의", "good")), hrSecretary),
                operation("finance", "부서", "전략재무팀", "재무·법무·경영 분석", "출범 준비", "muted", "강이든 · 김채린", "1,114 / 1,500명", "재무 건전성 +14.2%", 75,
                        metrics(metric("부서 전문성", "64", "출범 준비", ""), metric("업무 가동률", "75%", "정상 범위", ""), metric("현금 소진기간", "18개월", "안정", "good"), metric("예측 신뢰도", "보통", "오차 ±12%", "warn")), financeSecretary)
        );

        List<CompanyDashboardView.OperationItem> businesses = List.of(
                operation("core-product", "사업", "핵심제품", "생성형 AI 플랫폼 · 일반 요금제", "정상 운영", "good", "AI개발팀 · 설하은", "상시 배치", "시장점유율 24.7%", 82,
                        metrics(metric("AI 벤치마크", "182점", "경쟁사 평균 176점", "good"), metric("유료 이용자", "186만명", "전월 +4.3%", "good"), metric("월 반복매출", "372억원", "일반 요금제", "good"), metric("처리용량", "82.3%", "증설 검토", "warn"), metric("서비스 안정성", "94", "치명 장애 없음", "good"), metric("다음 목표", "프로 요금제", "성장기업에서 개방", "")), developmentSecretary),
                operation("short-project", "사업", "단기프로젝트", "차세대 추론모델 최적화", "진행 중", "warn", "AI개발팀 · 설하은", "856 / 1,200명", "개발 진행도 63%", 63,
                        metrics(metric("예상 완료", "2개월 후", "일정 정상", ""), metric("누적 개발비", "46억원", "예산의 58%", ""), metric("예상 품질", "74~82", "현재 조직 기준", "good"), metric("완료 후 수익", "3개월", "월 42~58억원", "good")), developmentSecretary),
                operation("contract", "사업", "기업계약", "엔터프라이즈 지식검색 구축", "협상 중", "warn", "영업마케팅팀 · 이다은", "623 / 800명", "계약 성사율 78.4%", 74,
                        metrics(metric("계약금", "180억원", "체결 시 수령", "good"), metric("월 이용료", "24억원", "계약기간 24개월", "good"), metric("필요 처리용량", "8.4%", "현재 수용 가능", ""), metric("위약 위험", "보통", "납기 지연 주의", "warn")), salesSecretary),
                operation("compute", "설비", "연산센터", "외부 클라우드 · 스타터 계약", "정상", "good", "서비스운영팀 · 설아름", "914 / 1,000명", "인프라 가동률 82%", 82,
                        metrics(metric("처리용량", "200 PFLOPS", "가동률 82.3%", "warn"), metric("월 운영비", "90억원", "외부 클라우드", ""), metric("서비스 안정성", "92", "정상", "good"), metric("자체 연산망", "미보유", "건설 검토 가능", "")), operationsSecretary)
        );

        return new CompanyDashboardView(
                new CompanyDashboardView.Identity("건물주이야기 주식회사", "설립기업", "생성형 AI 플랫폼", "설립 9개월차", "서울", "플레이어"),
                List.of(
                        metric("법인현금", "2,364억 7,890만원", "필수 고정비 18개월", ""),
                        metric("월간매출", "2,147억 8,900만원", "전월 +8.6%", "good"),
                        metric("영업이익", "641억 2,300만원", "영업이익률 29.8%", "good"),
                        metric("전체인력", "12,842명", "승인 정원 15,000명", ""),
                        metric("AI 벤치마크", "182점", "경쟁사 평균 176점", "good"),
                        metric("시장점유율", "24.7%", "시장 2위", "good"),
                        metric("인프라용량", "82.3%", "200 PFLOPS", "warn")
                ),
                "core-product",
                departments,
                businesses,
                List.of(
                        new CompanyDashboardView.WorkItem("높음", "danger", "차세대 모델 연구 인력 충원", "이준호", "D-2", "검토 필요"),
                        new CompanyDashboardView.WorkItem("높음", "danger", "연산센터 증설 투자 검토", "강이든", "D-3", "대기"),
                        new CompanyDashboardView.WorkItem("보통", "warn", "기업계약 최종 조건 승인", "최민수", "D-5", "승인 필요"),
                        new CompanyDashboardView.WorkItem("보통", "warn", "프로 요금제 출시 계획", "박지은", "D-8", "기획 중"),
                        new CompanyDashboardView.WorkItem("낮음", "muted", "사내 교육 프로그램 개편", "정우혁", "D-12", "대기")
                ),
                List.of(
                        new CompanyDashboardView.NewsItem("05.18", "시장", "생성형 AI 기업용 수요, 전분기 대비 11% 증가", "테크 브리핑"),
                        new CompanyDashboardView.NewsItem("05.17", "기업", "핵심제품 벤치마크 180점 돌파", "사내 소식"),
                        new CompanyDashboardView.NewsItem("05.16", "경쟁", "경쟁사 오로라AI, 프로 요금제 가격 인하 검토", "시장 정보"),
                        new CompanyDashboardView.NewsItem("05.14", "인프라", "클라우드 연산비용 안정세 지속", "디지털 데일리"),
                        new CompanyDashboardView.NewsItem("05.12", "조직", "개발 핵심인재 채용 경쟁 심화", "HR 인사이트")
                ),
                List.of(
                        new CompanyDashboardView.ReportItem("매출", "2,147억 8,900만원", "1,979억 5,600만원", "+8.50%", "good"),
                        new CompanyDashboardView.ReportItem("영업이익", "641억 2,300만원", "570억 9,800만원", "+12.29%", "good"),
                        new CompanyDashboardView.ReportItem("연구개발비", "356억 7,800만원", "312억 4,500만원", "+14.18%", "warn"),
                        new CompanyDashboardView.ReportItem("인건비", "289억 3,000만원", "273억 4,400만원", "+5.81%", "warn"),
                        new CompanyDashboardView.ReportItem("현금흐름", "+728억 9,100만원", "+612억 3,400만원", "+19.05%", "good")
                )
        );
    }

    private CompanyDashboardView.OperationItem operation(
            String key,
            String group,
            String name,
            String description,
            String status,
            String tone,
            String leader,
            String workforce,
            String result,
            int progressPercent,
            List<CompanyDashboardView.Metric> detailMetrics,
            CompanyDashboardView.Secretary secretary
    ) {
        return new CompanyDashboardView.OperationItem(key, group, name, description, status, tone, leader, workforce, result, progressPercent, detailMetrics, secretary);
    }

    private CompanyDashboardView.Secretary secretary(String name, String role, String imagePath, String proficiency, String effect, String advice) {
        return new CompanyDashboardView.Secretary(name, role, imagePath, proficiency, effect, advice);
    }

    private List<CompanyDashboardView.Metric> metrics(CompanyDashboardView.Metric... metrics) {
        return List.of(metrics);
    }

    private CompanyDashboardView.Metric metric(String label, String value, String note, String tone) {
        return new CompanyDashboardView.Metric(label, value, note, tone);
    }
}
