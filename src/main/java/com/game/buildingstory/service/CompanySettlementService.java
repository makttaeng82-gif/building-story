package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyMonthlySettlement;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy;
import com.game.buildingstory.domain.CompanyMarketingBudgetPolicy;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyCashTransactionRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 출시된 플레이어 기업의 월 손익, 현금 고갈과 분기 합산을 처리한다. */
@Service
public class CompanySettlementService {
    public static final long STARTER_CLOUD_MONTHLY_COST = 9_000_000_000L;
    public static final long PRE_LAUNCH_DEVELOPMENT_MONTHLY_COST = 2_000_000_000L;
    public static final long PRE_LAUNCH_MARKETING_MONTHLY_COST = 800_000_000L;
    private static final int CORPORATE_TAX_PERCENT = 20;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyWorkforceService workforceService;
    private final SecretaryOperationsService secretaryOperationsService;
    private final CompanyMonthlySettlementRepository monthlyRepository;
    private final CompanyQuarterlyReportRepository quarterlyRepository;
    private final CompanyMarketService marketService;
    private final CompanyInfrastructureService infrastructureService;
    private final CompanyProductProjectService productProjectService;
    private final CompanyComputeConstructionService constructionService;
    private final CompanyShortTermProjectService shortTermProjectService;
    private final CompanyCustomerContractService customerContractService;
    private final CompanyServiceIncidentService incidentService;
    private final CompanyNewsService newsService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyCashTransactionRepository cashTransactionRepository;
    private final CompanyExternalEventService externalEventService;
    private final CompanySecretaryService companySecretaryService;
    private final CompanyOrganizationService companyOrganizationService;
    private final CompanyFinanceService companyFinanceService;
    private final CompanyGrowthService companyGrowthService;
    private final CompanyIpoService companyIpoService;

    public CompanySettlementService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyWorkforceService workforceService,
            SecretaryOperationsService secretaryOperationsService,
            CompanyMonthlySettlementRepository monthlyRepository,
            CompanyQuarterlyReportRepository quarterlyRepository,
            CompanyMarketService marketService,
            CompanyInfrastructureService infrastructureService,
            CompanyProductProjectService productProjectService,
            CompanyComputeConstructionService constructionService,
            CompanyShortTermProjectService shortTermProjectService,
            CompanyCustomerContractService customerContractService,
            CompanyServiceIncidentService incidentService,
            CompanyNewsService newsService,
            CompanyCashLedgerService cashLedgerService,
            CompanyCashTransactionRepository cashTransactionRepository,
            CompanyExternalEventService externalEventService,
            CompanySecretaryService companySecretaryService,
            CompanyOrganizationService companyOrganizationService,
            CompanyFinanceService companyFinanceService,
            CompanyGrowthService companyGrowthService,
            CompanyIpoService companyIpoService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.workforceService = workforceService;
        this.secretaryOperationsService = secretaryOperationsService;
        this.monthlyRepository = monthlyRepository;
        this.quarterlyRepository = quarterlyRepository;
        this.marketService = marketService;
        this.infrastructureService = infrastructureService;
        this.productProjectService = productProjectService;
        this.constructionService = constructionService;
        this.shortTermProjectService = shortTermProjectService;
        this.customerContractService = customerContractService;
        this.incidentService = incidentService;
        this.newsService = newsService;
        this.cashLedgerService = cashLedgerService;
        this.cashTransactionRepository = cashTransactionRepository;
        this.externalEventService = externalEventService;
        this.companySecretaryService = companySecretaryService;
        this.companyOrganizationService = companyOrganizationService;
        this.companyFinanceService = companyFinanceService;
        this.companyGrowthService = companyGrowthService;
        this.companyIpoService = companyIpoService;
    }

    @Transactional
    public String processMonthly(Player player) {
        PlayerCompany company = companyRepository.findByPlayer(player).orElse(null);
        if (company == null || !company.getTutorialStage().isOperational()
                || company.isOperationsSuspended()) {
            return "";
        }
        int periodIndex = periodIndex(player);
        if (monthlyRepository.findByCompanyAndPeriodIndex(company, periodIndex).isPresent()) {
            return "";
        }

        company.applyPendingCloudPlan();
        marketService.initializeMarket(company);
        externalEventService.processMonth(company);
        var marketResult = marketService.processMonth(company);
        var shortTermFinancials = shortTermProjectService.monthlyFinancials(company);
        var contractFinancials = customerContractService.monthlyFinancials(company);
        var incidentFinancials = incidentService.monthlyFinancials(company);
        long subscriptionRevenue = marketResult.monthlyRecurringRevenue();
        long totalRevenue = Math.addExact(
                Math.addExact(subscriptionRevenue, shortTermFinancials.revenue()),
                contractFinancials.revenue());
        long payroll = monthlyPayroll(company);
        long cloudCost = infrastructureService.monthlyInfrastructureCost(company);
        BudgetSnapshot budgets = budgetSnapshot(company);
        long operatingExpenses = Math.addExact(
                Math.addExact(payroll, cloudCost),
                Math.addExact(
                        Math.addExact(
                                Math.addExact(budgets.developmentCost(), budgets.marketingCost()),
                                budgets.platformCost()),
                        Math.addExact(
                                Math.addExact(shortTermFinancials.cost(), contractFinancials.cost()),
                                incidentFinancials.cost())));
        long operatingProfit = totalRevenue - operatingExpenses;
        long tax = quarterlyTaxIfDue(company, operatingProfit);
        var bondObligation = companyFinanceService.prepareMonthlyObligation(company, periodIndex);
        long operatingCashOut = Math.addExact(operatingExpenses, tax);
        long openingCash = company.getCorporateCash();

        cashLedgerService.deposit(
                company,
                "monthly:" + periodIndex + ":revenue",
                CompanyCashFlowType.OPERATING,
                "월 매출 입금",
                totalRevenue
        );
        long paidOperating = Math.min(company.getCorporateCash(), operatingCashOut);
        if (paidOperating > 0) {
            cashLedgerService.withdraw(
                    company,
                    "monthly:" + periodIndex + ":cost",
                    CompanyCashFlowType.OPERATING,
                    "월 운영비 및 법인세",
                    paidOperating
            );
        }
        long unpaidOperating = operatingCashOut - paidOperating;

        long paidBondInterest = Math.min(company.getCorporateCash(), bondObligation.interest());
        if (paidBondInterest > 0) {
            cashLedgerService.withdraw(
                    company,
                    "monthly:" + periodIndex + ":bond-interest",
                    CompanyCashFlowType.FINANCING,
                    "회사채 이자",
                    paidBondInterest
            );
        }
        long unpaidBondInterest = bondObligation.interest() - paidBondInterest;

        long unpaidBondPrincipal = bondObligation.principal();
        if (bondObligation.principal() > 0
                && company.getCorporateCash() >= bondObligation.principal()) {
            cashLedgerService.withdraw(
                    company,
                    "monthly:" + periodIndex + ":bond-principal",
                    CompanyCashFlowType.FINANCING,
                    "만기 회사채 원금 상환",
                    bondObligation.principal()
            );
            unpaidBondPrincipal = 0;
        }

        long unpaidAmount = Math.addExact(
                Math.addExact(unpaidOperating, unpaidBondInterest), unpaidBondPrincipal);
        if (unpaidAmount > 0) {
            company.suspendOperations(unpaidOperating, unpaidBondInterest, unpaidBondPrincipal);
            failActiveWork(company);
        }
        companyFinanceService.completeMonthlyObligation(bondObligation, unpaidAmount == 0);
        secretaryOperationsService.recordCompanySalarySettlement(player, unpaidAmount == 0);

        var constructionResult = company.isOperationsSuspended()
                ? new CompanyComputeConstructionService.ConstructionMonthResult(false, false, "")
                : constructionService.processMonth(company);

        CompanyMonthlySettlement settlement = monthlyRepository.save(new CompanyMonthlySettlement(
                company,
                periodIndex,
                gameYear(player),
                player.getMonth(),
                subscriptionRevenue,
                shortTermFinancials.revenue(),
                contractFinancials.revenue(),
                payroll,
                cloudCost,
                budgets.developmentCost(),
                budgets.marketingCost(),
                budgets.platformCost(),
                shortTermFinancials.cost(),
                contractFinancials.cost(),
                incidentFinancials.cost(),
                operatingProfit,
                tax,
                bondObligation.interest(),
                bondObligation.principal(),
                operatingProfit - tax - bondObligation.interest(),
                openingCash,
                company.getCorporateCash(),
                unpaidAmount
        ));
        company.recordFirstSettlement();
        Optional<CompanyQuarterlyReport> quarterlyReport =
                createQuarterlyReportIfDue(company, settlement);

        if (company.isOperationsSuspended()) {
            player.pause();
            return "법인현금 고갈 · 기업 운영중단 · 미지급 " + unpaidAmount + "원";
        }
        company.applyMonthlyBudgetEffects(
                campaignStrength(company),
                Math.max(0.0, (1.0 - company.getDevelopmentBudgetPolicy().getSpendingPercent() / 100.0) * 0.5)
        );
        var projectResult = productProjectService.processMonth(company);
        var shortTermResult = shortTermProjectService.processSuccessfulMonth(company);
        var infrastructure = infrastructureService.snapshot(company);
        var incidentResult = incidentService.processSuccessfulMonth(
                company, infrastructure.utilizationPercent());
        var contractResult = customerContractService.processSuccessfulMonth(
                company,
                infrastructure.demand() > infrastructure.usableCapacity(),
                incidentResult.customerImpact());
        var workforceResult = workforceService.processWorkforceMonth(company);
        var organizationResult = companyOrganizationService.processSuccessfulMonth(
                company, essentialMonthlyCost(company));
        companySecretaryService.processSuccessfulMonth(company);
        String ipoNotice = companyIpoService.processSuccessfulMonth(company);
        newsService.recordMonthlyEvents(
                company,
                marketResult,
                projectResult,
                constructionResult,
                incidentResult,
                quarterlyReport
        );
        return "기업 월 정산 완료 · 순손익 " + settlement.getNetIncome() + "원"
                + " · 유료 이용자 " + marketResult.paidUsers() + "명"
                + (projectResult.active() ? " · " + projectResult.notice() : "")
                + (shortTermResult.active() ? " · " + shortTermResult.notice() : "")
                + (shortTermResult.candidateGenerated() ? " · 신규 단기 사업 제안" : "")
                + (contractResult.active() ? " · " + contractResult.notice() : "")
                + (contractResult.candidateGenerated() ? " · 신규 고객계약 제안" : "")
                + (incidentResult.active() ? " · " + incidentResult.notice() : "")
                + (constructionResult.active() ? " · " + constructionResult.notice() : "")
                + (workforceResult.onboardedEmployees() > 0 ? " · 일반인력 " + workforceResult.onboardedEmployees() + "명 입사" : "")
                + (workforceResult.onboardedCoreEmployees() > 0 ? " · 핵심인재 " + workforceResult.onboardedCoreEmployees() + "명 입사" : "")
                + (workforceResult.promotedCoreEmployees() > 0 ? " · 핵심인재 " + workforceResult.promotedCoreEmployees() + "명 승급" : "")
                + (workforceResult.resignedGeneralEmployees() > 0 ? " · 일반인력 " + workforceResult.resignedGeneralEmployees() + "명 퇴사" : "")
                + (workforceResult.resignedCoreEmployees() > 0 ? " · 핵심인재 " + workforceResult.resignedCoreEmployees() + "명 퇴사" : "")
                + (workforceResult.resignationWarnings() > 0 ? " · 핵심인재 퇴사 협상 " + workforceResult.resignationWarnings() + "건" : "")
                + (organizationResult.upgraded() ? " · 조직관리 시스템 구축 완료" : "")
                + (organizationResult.automaticHires() > 0 ? " · 자동채용 "
                        + organizationResult.automaticHires() + "명 승인" : "")
                + (ipoNotice.isBlank() ? "" : " · " + ipoNotice);
    }

    @Transactional
    public String contributeAndResume(long playerId, long amount) {
        if (amount <= 0) {
            return "추가 출자금은 1원 이상 필요";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!player.spendCash(amount)) {
            return "개인 현금이 부족함";
        }
        boolean wasSuspended = company.isOperationsSuspended();
        long issuedPlayerShares = companyFinanceService.applyPlayerContribution(company, amount);
        cashLedgerService.recordApplied(
                company,
                "capital:" + player.getElapsedDays() + ":" + company.getPaidInCapital(),
                CompanyCashFlowType.FINANCING,
                "플레이어 유상증자",
                amount
        );
        if (!wasSuspended) {
            return "개인 유상증자 완료 · 신주 " + issuedPlayerShares + "주";
        }
        long unpaidOperating = company.getUnpaidOperatingAmount();
        long unpaidBondInterest = company.getUnpaidBondInterestAmount();
        long unpaidBondPrincipal = company.getUnpaidBondPrincipalAmount();
        boolean resumed = company.resumeOperations(essentialMonthlyCost(company));
        if (resumed) {
            companyFinanceService.resolveDefaultedBonds(company);
            recordResumedPayment(company, player.getElapsedDays(), "operating",
                    CompanyCashFlowType.OPERATING, "미지급 운영비 변제", unpaidOperating);
            recordResumedPayment(company, player.getElapsedDays(), "bond-interest",
                    CompanyCashFlowType.FINANCING, "미지급 회사채 이자 변제", unpaidBondInterest);
            recordResumedPayment(company, player.getElapsedDays(), "bond-principal",
                    CompanyCashFlowType.FINANCING, "만기 회사채 원금 변제", unpaidBondPrincipal);
            return "유상증자 완료 · 기업 운영 재개";
        }
        return "유상증자 완료 · 재가동 필요금액 미달";
    }

    private void recordResumedPayment(
            PlayerCompany company,
            int elapsedDay,
            String key,
            CompanyCashFlowType flowType,
            String description,
            long amount
    ) {
        if (amount <= 0) {
            return;
        }
        cashLedgerService.recordApplied(
                company,
                "resume:" + elapsedDay + ":" + key,
                flowType,
                description,
                -amount
        );
    }

    @Transactional
    public String changeBudgetPolicies(
            long playerId,
            CompanyDevelopmentBudgetPolicy developmentPolicy,
            CompanyMarketingBudgetPolicy marketingPolicy
    ) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational()) {
            return "제품 출시 후 비용정책을 변경할 수 있음";
        }
        if (company.isOperationsSuspended()) {
            return "운영 재개 후 비용정책을 다시 설정할 수 있음";
        }
        company.changeBudgetPolicies(developmentPolicy, marketingPolicy);
        BudgetSnapshot snapshot = budgetSnapshot(company);
        return "다음 월 비용정책 변경 · 개발비 " + snapshot.developmentCost()
                + "원 · 마케팅비 " + snapshot.marketingCost() + "원";
    }

    @Transactional(readOnly = true)
    public List<CompanyMonthlySettlement> monthlySettlements(PlayerCompany company) {
        return monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyQuarterlyReport> quarterlyReports(PlayerCompany company) {
        return quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
    }

    @Transactional(readOnly = true)
    public long monthlyPayroll(PlayerCompany company) {
        return Math.addExact(
                workforceService.monthlyPayroll(company),
                secretaryOperationsService.companyMonthlyPayroll(company.getPlayer())
        );
    }

    @Transactional(readOnly = true)
    public long essentialMonthlyCost(PlayerCompany company) {
        var pendingPlan = company.getPendingCloudPlanType();
        long cloudCost = infrastructureService.monthlyInfrastructureCost(company);
        if (pendingPlan != null) {
            cloudCost = Math.subtractExact(cloudCost, infrastructureService.monthlyCloudCost(company));
            cloudCost = Math.addExact(cloudCost, infrastructureService.monthlyCloudCost(company, pendingPlan));
        }
        return Math.addExact(monthlyPayroll(company), cloudCost);
    }

    @Transactional(readOnly = true)
    public BudgetSnapshot budgetSnapshot(PlayerCompany company) {
        long averageRevenue = recentAverageRevenue(company);
        long standardDevelopment = percentageCost(
                averageRevenue, company.getGrowthStage().getDevelopmentCostPercent());
        long standardMarketing = percentageCost(
                averageRevenue, company.getGrowthStage().getMarketingCostPercent());
        long developmentCost = percentageCost(
                standardDevelopment, company.getDevelopmentBudgetPolicy().getSpendingPercent());
        long marketingCost = percentageCost(
                standardMarketing, company.getMarketingBudgetPolicy().getSpendingPercent());
        long platformCost = percentageCost(
                averageRevenue, company.getGrowthStage().getPlatformCostPercent());
        return new BudgetSnapshot(
                standardDevelopment,
                developmentCost,
                standardMarketing,
                marketingCost,
                platformCost
        );
    }

    private long recentAverageRevenue(PlayerCompany company) {
        List<CompanyMonthlySettlement> recent =
                monthlyRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company);
        if (recent.isEmpty()) {
            return Math.max(0, company.getMonthlyRecurringRevenue());
        }
        return Math.round(recent.stream()
                .mapToLong(CompanyMonthlySettlement::getTotalRevenue)
                .average()
                .orElse(company.getMonthlyRecurringRevenue()));
    }

    private long percentageCost(long base, int percent) {
        return Math.multiplyExact(base, percent) / 100;
    }

    private double campaignStrength(PlayerCompany company) {
        int expertise = workforceService.departmentExpertise(
                company, com.game.buildingstory.domain.CompanyDepartmentType.SALES_MARKETING);
        var load = workforceService.departmentLoad(
                company, com.game.buildingstory.domain.CompanyDepartmentType.SALES_MARKETING);
        double executionRatio = load.capacity() == 0
                ? 0.0
                : Math.min(1.0, load.capacity() / (double) Math.max(1, load.totalWorkload()));
        return Math.max(0, Math.min(100,
                50.0
                        * company.getMarketingBudgetPolicy().effectMultiplier()
                        * (0.75 + expertise / 200.0)
                        * companySecretaryService.marketingEffectMultiplier(company)
                        * executionRatio));
    }

    private long quarterlyTaxIfDue(PlayerCompany company, long currentOperatingProfit) {
        long completedMonths = monthlyRepository.countByCompany(company);
        if ((completedMonths + 1) % 3 != 0) {
            return 0;
        }
        long previousOperatingProfit = monthlyRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company).stream()
                .limit(2)
                .mapToLong(CompanyMonthlySettlement::getOperatingProfit)
                .sum();
        long quarterProfit = Math.addExact(previousOperatingProfit, currentOperatingProfit);
        return quarterProfit <= 0 ? 0 : Math.multiplyExact(quarterProfit, CORPORATE_TAX_PERCENT) / 100;
    }

    private Optional<CompanyQuarterlyReport> createQuarterlyReportIfDue(
            PlayerCompany company,
            CompanyMonthlySettlement current
    ) {
        long settledMonths = monthlyRepository.countByCompany(company);
        if (settledMonths % 3 != 0) {
            return Optional.empty();
        }
        List<CompanyMonthlySettlement> quarter = monthlyRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company);
        long revenue = quarter.stream().mapToLong(CompanyMonthlySettlement::getTotalRevenue).sum();
        long operatingProfit = quarter.stream().mapToLong(CompanyMonthlySettlement::getOperatingProfit).sum();
        long tax = quarter.stream().mapToLong(CompanyMonthlySettlement::getCorporateTax).sum();
        long netIncome = quarter.stream().mapToLong(CompanyMonthlySettlement::getNetIncome).sum();
        int firstPeriodIndex = current.getPeriodIndex() - 2;
        var cashTransactions = cashTransactionRepository
                .findByCompanyAndPeriodIndexBetweenOrderByIdAsc(
                        company, firstPeriodIndex, current.getPeriodIndex());
        long operatingCashFlow = cashTransactions.stream()
                .filter(transaction -> transaction.getFlowType() == CompanyCashFlowType.OPERATING)
                .mapToLong(transaction -> transaction.getAmount())
                .sum();
        long investingCashFlow = cashTransactions.stream()
                .filter(transaction -> transaction.getFlowType() == CompanyCashFlowType.INVESTING)
                .mapToLong(transaction -> transaction.getAmount())
                .sum();
        long financingCashFlow = cashTransactions.stream()
                .filter(transaction -> transaction.getFlowType() == CompanyCashFlowType.FINANCING)
                .mapToLong(transaction -> transaction.getAmount())
                .sum();
        CompanyQuarterlyReport report = quarterlyRepository.save(new CompanyQuarterlyReport(
                company,
                (int) (settledMonths / 3),
                current.getPeriodIndex(),
                revenue,
                revenue - operatingProfit,
                operatingProfit,
                tax,
                netIncome,
                current.getClosingCash(),
                Math.addExact(company.getMonthlyRecurringRevenue(),
                        customerContractService.activeMonthlyFee(company)),
                company.getPaidUsers(),
                operatingCashFlow,
                investingCashFlow,
                financingCashFlow
        ));
        if (netIncome > 0) {
            company.adjustBrandScore(0.5);
        }
        companyFinanceService.recordQuarterlyValuation(
                company,
                report,
                monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company),
                essentialMonthlyCost(company)
        );
        companyGrowthService.evaluateAndPromote(company);
        return Optional.of(report);
    }

    private int periodIndex(Player player) {
        return (gameYear(player) - 1) * 12 + player.getMonth() - 1;
    }

    private int gameYear(Player player) {
        return player.getYear();
    }

    private void failActiveWork(PlayerCompany company) {
        productProjectService.failActiveProject(company);
        shortTermProjectService.failActiveBusiness(company);
        customerContractService.failActiveBuild(company);
        incidentService.failActiveResponse(company);
        constructionService.failActiveConstruction(company);
    }

    public record BudgetSnapshot(
            long standardDevelopmentCost,
            long developmentCost,
            long standardMarketingCost,
            long marketingCost,
            long platformCost
    ) {
    }
}
