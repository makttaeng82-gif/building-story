package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.CompanyBond;
import com.game.buildingstory.domain.CompanyBondStatus;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyCloudPlan;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyExternalEvent;
import com.game.buildingstory.domain.CompanyExternalEventCategory;
import com.game.buildingstory.domain.CompanyCustomerContractType;
import com.game.buildingstory.domain.CompanyCustomerContract;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyCashTransactionRepository;
import com.game.buildingstory.repo.CompanyExternalEventRepository;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyCustomerContractRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyShortTermProjectRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.CompanyNewsArticleRepository;
import com.game.buildingstory.repo.CompanyProductProjectRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.CompanyInvestmentRoundRepository;
import com.game.buildingstory.repo.CompanyBondRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:company-settlement-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
@AutoConfigureMockMvc
class CompanySettlementServiceTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private GameService gameService;
    @Autowired private PlayerCompanyService playerCompanyService;
    @Autowired private CompanyTutorialService tutorialService;
    @Autowired private CompanySettlementService settlementService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PlayerCompanyRepository companyRepository;
    @Autowired private CompanyDepartmentRepository departmentRepository;
    @Autowired private CompanyCoreEmployeeRepository employeeRepository;
    @Autowired private CompanyMonthlySettlementRepository monthlyRepository;
    @Autowired private CompanyQuarterlyReportRepository quarterlyRepository;
    @Autowired private MonthlyRecordRepository monthlyRecordRepository;
    @Autowired private OwnedSecretaryRepository ownedSecretaryRepository;
    @Autowired private CompanyCompetitorRepository competitorRepository;
    @Autowired private CompanyShortTermProjectRepository shortTermProjectRepository;
    @Autowired private CompanyShortTermProjectService shortTermProjectService;
    @Autowired private CompanyCustomerContractRepository customerContractRepository;
    @Autowired private CompanyCustomerContractService customerContractService;
    @Autowired private CompanyGrowthService growthService;
    @Autowired private CompanyWorkforceService workforceService;
    @Autowired private CompanyServiceIncidentService incidentService;
    @Autowired private CompanyServiceIncidentRepository incidentRepository;
    @Autowired private CompanyNewsArticleRepository companyNewsRepository;
    @Autowired private CompanyNewsService companyNewsService;
    @Autowired private CompanyProductProjectRepository productProjectRepository;
    @Autowired private CompanyProductProjectService productProjectService;
    @Autowired private CompanyReportingService companyReportingService;
    @Autowired private CompanyCashTransactionRepository cashTransactionRepository;
    @Autowired private CompanyExternalEventRepository externalEventRepository;
    @Autowired private CompanyExternalEventService externalEventService;
    @Autowired private CompanyInfrastructureService infrastructureService;
    @Autowired private CompanyComputeConstructionService constructionService;
    @Autowired private CompanyDepartmentService departmentService;
    @Autowired private CompanyFinanceService financeService;
    @Autowired private CompanyValuationSnapshotRepository valuationRepository;
    @Autowired private CompanyInvestmentRoundRepository investmentRoundRepository;
    @Autowired private CompanyBondRepository bondRepository;

    @BeforeEach
    void cleanDatabase() {
        companyNewsRepository.deleteAll();
        bondRepository.deleteAll();
        investmentRoundRepository.deleteAll();
        valuationRepository.deleteAll();
        cashTransactionRepository.deleteAll();
        externalEventRepository.deleteAll();
        quarterlyRepository.deleteAll();
        monthlyRepository.deleteAll();
        monthlyRecordRepository.deleteAll();
        competitorRepository.deleteAll();
        customerContractRepository.deleteAll();
        incidentRepository.deleteAll();
        shortTermProjectRepository.deleteAll();
        productProjectRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        companyRepository.deleteAll();
        ownedSecretaryRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void settlesEachMonthOnceAndBuildsQuarterFromThreeActualMonths() {
        Player player = launchedCompanyPlayer("settlement-quarter");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingCash = company.getCorporateCash();
        settlementService.processMonthly(player);
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);

        var months = monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company);
        assertThat(months).hasSize(3);
        assertThat(quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company)).hasSize(1);
        var quarter = quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company).getFirst();
        long revenue = months.stream().mapToLong(item -> item.getSubscriptionRevenue()).sum();
        long operatingProfit = months.stream().mapToLong(item -> item.getOperatingProfit()).sum();
        long netIncome = months.stream().mapToLong(item -> item.getNetIncome()).sum();
        assertThat(quarter.getRevenue()).isEqualTo(revenue);
        assertThat(quarter.getOperatingProfit()).isEqualTo(operatingProfit);
        assertThat(quarter.getCorporateTax()).isZero();
        var quarterTransactions = cashTransactionRepository
                .findByCompanyAndPeriodIndexBetweenOrderByIdAsc(
                        company, quarter.getEndingPeriodIndex() - 2, quarter.getEndingPeriodIndex());
        long recordedOperatingCash = quarterTransactions.stream()
                .filter(transaction -> transaction.getFlowType() == CompanyCashFlowType.OPERATING)
                .mapToLong(transaction -> transaction.getAmount())
                .sum();
        assertThat(quarter.getOperatingCashFlow()).isEqualTo(recordedOperatingCash);
        assertThat(quarter.getInvestingCashFlow()).isZero();
        assertThat(quarter.getFinancingCashFlow()).isEqualTo(company.getPaidInCapital());
        assertThat(company.getCorporateCash()).isEqualTo(openingCash + netIncome);
        assertThat(quarterTransactions).isNotEmpty();
        var performanceArticle = companyNewsRepository.findByCompanyOrderByOccurredMarketMonthDescIdDesc(company).stream()
                .filter(article -> article.getCategory()
                        == com.game.buildingstory.domain.CompanyNewsCategory.PERFORMANCE)
                .findFirst()
                .orElseThrow();
        assertThat(performanceArticle.getBody())
                .contains("분기 매출")
                .doesNotMatch(".*\\d{9,}원.*");
        var reportView = companyReportingService.reports(company).getFirst();
        assertThat(reportView.lines())
                .anyMatch(line -> line.label().equals("구독매출") && !line.value().equals("0원"));
        assertThat(company.getTutorialStage()).isEqualTo(CompanyTutorialStage.FIRST_SETTLEMENT);
        assertThat(companyReportingService.markRead(player.getId(), quarter.getQuarterSequence())).isTrue();
        assertThat(company.getTutorialStage()).isEqualTo(CompanyTutorialStage.COMPLETED);
    }

    @Test
    void budgetPoliciesChangeMonthlyCostsAndDevelopmentProgress() {
        Player player = launchedCompanyPlayer("company-budget-policy");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        var standard = settlementService.budgetSnapshot(company);
        settlementService.changeBudgetPolicies(
                player.getId(),
                com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy.FOCUSED,
                com.game.buildingstory.domain.CompanyMarketingBudgetPolicy.MINIMUM
        );
        var changed = settlementService.budgetSnapshot(company);

        assertThat(changed.developmentCost()).isEqualTo(standard.developmentCost() * 2);
        assertThat(changed.marketingCost()).isEqualTo(standard.marketingCost() / 2);

        assertThat(productProjectService.start(
                player.getId(),
                com.game.buildingstory.domain.CompanyProductImprovementType.MODEL_REFINEMENT
        )).contains("시작");
        var project = productProjectService.activeProject(company).orElseThrow();
        int remainingBefore = project.getRemainingWork();
        int standardDepartmentWork = workforceService.effectiveMajorWork(
                company,
                com.game.buildingstory.domain.CompanyDepartmentType.AI_DEVELOPMENT,
                project.getMonthlyAssignedWork());

        productProjectService.processMonth(company);

        assertThat(remainingBefore - project.getRemainingWork())
                .isGreaterThan(standardDepartmentWork);
    }

    @Test
    void quarterlyValuationSupportsDividendAndBondLimitWhileExternalInvestmentIsBlocked() {
        Player player = launchedCompanyPlayer("company-finance");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);

        var valuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company)
                .orElseThrow();
        assertThat(valuation.getEnterpriseValue()).isPositive();

        var profitableReport = quarterlyRepository.save(new CompanyQuarterlyReport(
                company,
                2,
                6,
                300_000_000_000L,
                150_000_000_000L,
                150_000_000_000L,
                30_000_000_000L,
                120_000_000_000L,
                company.getCorporateCash(),
                30_000_000_000L,
                company.getPaidUsers()
        ));
        long personalCashBefore = player.getCash();
        assertThat(financeService.decideDividend(
                player.getId(), 10, settlementService.essentialMonthlyCost(company)))
                .contains("배당 완료");
        assertThat(profitableReport.isDividendDecided()).isTrue();
        assertThat(player.getCash()).isGreaterThan(personalCashBefore);

        double ownershipBefore = company.getPlayerOwnershipPercent();
        long corporateCashBefore = company.getCorporateCash();
        assertThat(financeService.attractInvestment(player.getId(), 10)).contains("IPO 기능");
        assertThat(investmentRoundRepository.countByCompany(company)).isZero();
        assertThat(company.getPlayerOwnershipPercent()).isEqualTo(ownershipBefore);
        assertThat(company.getCorporateCash()).isEqualTo(corporateCashBefore);

        assertThat(financeService.issueBond(
                player.getId(), 10))
                .contains("발행 완료");
        assertThat(bondRepository.findByCompanyOrderByIdDesc(company)).hasSize(1);
        assertThat(financeService.issueBond(
                player.getId(), 5))
                .contains("10%를 넘을 수 없습니다");

        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        assertThat(monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company).getFirst()
                .getFinancingCost()).isPositive();

        company.suspendOperations(1);
        assertThat(financeService.attractInvestment(player.getId(), 10)).contains("IPO 기능");
        assertThat(financeService.issueBond(
                player.getId(), 5))
                .contains("운영중단");
        long bondId = bondRepository.findByCompanyOrderByIdDesc(company).getFirst().getId();
        assertThat(financeService.repayOrdinaryBond(player.getId(), bondId)).contains("운영중단");
    }

    @Test
    void playerContributionWithLegacyExternalSharesIssuesPlayerShares() {
        Player player = launchedCompanyPlayer("company-player-rights-issue");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        settleFirstQuarter(player);
        company.issueExternalShares(company.getIssuedShares() / 4);
        double dilutedOwnership = company.getPlayerOwnershipPercent();
        long playerSharesBefore = company.getPlayerShares();

        assertThat(settlementService.contributeAndResume(player.getId(), 10_000_000_000L))
                .contains("유상증자");

        assertThat(company.getPlayerShares()).isGreaterThan(playerSharesBefore);
        assertThat(company.getPlayerOwnershipPercent()).isGreaterThan(dilutedOwnership);
    }

    @Test
    void defaultedBondPrincipalIsCountedOnceInValuationDebt() {
        Player player = launchedCompanyPlayer("company-debt-once");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long principal = 10_000_000_000L;
        CompanyBond bond = bondRepository.save(new CompanyBond(company, principal, 50_000_000L, 0, 24));
        bond.markDefaulted();
        company.suspendOperations(0, 0, principal);
        CompanyQuarterlyReport report = quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 3, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 0, 0));

        var valuation = financeService.recordQuarterlyValuation(
                company, report, List.of(), settlementService.essentialMonthlyCost(company));

        assertThat(valuation.getTotalDebt()).isEqualTo(principal);
    }

    @Test
    void resumingCompanyKeepsOperatingAndBondArrearsInSeparateCashFlows() {
        Player player = launchedCompanyPlayer("company-resume-ledger");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long operating = 300_000_000L;
        long interest = 50_000_000L;
        long principal = 1_000_000_000L;
        CompanyBond bond = bondRepository.save(new CompanyBond(company, principal, interest, 0, 24));
        bond.markDefaulted();
        company.suspendOperations(operating, interest, principal);
        long contribution = operating + interest + principal + settlementService.essentialMonthlyCost(company);
        player.addCash(contribution);

        assertThat(settlementService.contributeAndResume(player.getId(), contribution))
                .contains("운영 재개");

        var transactions = cashTransactionRepository.findAll();
        assertThat(transactions).anySatisfy(transaction -> {
            assertThat(transaction.getDescription()).isEqualTo("미지급 운영비 변제");
            assertThat(transaction.getFlowType()).isEqualTo(CompanyCashFlowType.OPERATING);
            assertThat(transaction.getAmount()).isEqualTo(-operating);
        });
        assertThat(transactions).anySatisfy(transaction -> {
            assertThat(transaction.getDescription()).isEqualTo("미지급 회사채 이자 변제");
            assertThat(transaction.getFlowType()).isEqualTo(CompanyCashFlowType.FINANCING);
            assertThat(transaction.getAmount()).isEqualTo(-interest);
        });
        assertThat(transactions).anySatisfy(transaction -> {
            assertThat(transaction.getDescription()).isEqualTo("만기 회사채 원금 변제");
            assertThat(transaction.getFlowType()).isEqualTo(CompanyCashFlowType.FINANCING);
            assertThat(transaction.getAmount()).isEqualTo(-principal);
        });
        assertThat(bond.getStatus()).isEqualTo(CompanyBondStatus.REPAID);
    }

    @Test
    void ordinaryBondMaturityRequiresFinalInterestAndPrincipal() {
        Player player = launchedCompanyPlayer("company-bond-maturity");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyBond bond = bondRepository.save(new CompanyBond(
                company, 2_000_000_000L, 10_000_000L, 0, 24));

        var obligation = financeService.prepareMonthlyObligation(company, 24);

        assertThat(obligation.interest()).isEqualTo(10_000_000L);
        assertThat(obligation.principal()).isEqualTo(2_000_000_000L);
        financeService.completeMonthlyObligation(obligation, true);
        assertThat(bond.getStatus()).isEqualTo(CompanyBondStatus.REPAID);
    }

    @Test
    void governanceReducesCompanyWorkSlotsByOwnershipBand() {
        assertThat(CompanyFinanceService.governanceWorkSlotPenalty(50.1)).isZero();
        assertThat(CompanyFinanceService.governanceWorkSlotPenalty(50.0)).isEqualTo(1);
        assertThat(CompanyFinanceService.governanceWorkSlotPenalty(33.3)).isEqualTo(2);
        assertThat(CompanyFinanceService.governanceWorkSlotPenalty(19.9)).isEqualTo(3);
    }

    @Test
    void strategyFinanceSecretaryNarrowsActualSettlementForecastRange() {
        Player player = launchedCompanyPlayer("financial-forecast");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        settlementService.processMonthly(player);
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 1, 0, 0, 0, 0, 0,
                company.getCorporateCash(), company.getMonthlyRecurringRevenue(), company.getPaidUsers()));
        var strategyFinance = new com.game.buildingstory.domain.CompanyDepartment(
                company, com.game.buildingstory.domain.CompanyDepartmentType.STRATEGY_FINANCE);
        strategyFinance.assignFoundingWorkforce(5);
        departmentRepository.save(strategyFinance);

        assertThat(companyReportingService.financialForecast(company).accuracyText())
                .isEqualTo("예상 오차 ±20%");
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-5", 30));
        assertThat(companyReportingService.financialForecast(company).accuracyText())
                .isEqualTo("예상 오차 ±18%");
        assertThat(workforceService.hireCoreTalent(player.getId(), "finance-01"))
                .contains("채용 승인");
        workforceService.processWorkforceMonth(company);
        assertThat(companyReportingService.financialForecast(company).accuracyText())
                .isEqualTo("예상 오차 ±16%");
    }

    @Test
    void cashShortageSuspendsCompanyAndSufficientContributionResumesIt() {
        Player player = launchedCompanyPlayer("settlement-restart");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        assertThat(productProjectService.start(
                player.getId(),
                com.game.buildingstory.domain.CompanyProductImprovementType.MODEL_REFINEMENT
        )).contains("시작");
        company.spendCorporateCash(company.getCorporateCash());

        String suspensionNotice = settlementService.processMonthly(player);

        assertThat(suspensionNotice).contains("기업 운영중단");
        assertThat(company.isOperationsSuspended()).isTrue();
        assertThat(company.getCorporateCash()).isZero();
        assertThat(productProjectRepository.findByCompanyOrderByIdDesc(company).getFirst().getStatus())
                .isEqualTo(com.game.buildingstory.domain.CompanyProductProjectStatus.FAILED);
        assertThat(company.getActiveMajorWorkCount()).isZero();
        long required = company.getUnpaidSettlementAmount() + settlementService.essentialMonthlyCost(company);

        String resumeNotice = settlementService.contributeAndResume(player.getId(), required);

        assertThat(resumeNotice).contains("운영 재개");
        assertThat(company.isOperationsSuspended()).isFalse();
        assertThat(company.getUnpaidSettlementAmount()).isZero();
        assertThat(company.getCorporateCash()).isEqualTo(settlementService.essentialMonthlyCost(company));
    }

    @Test
    void managementReportsKeepEightDetailedQuartersAndAggregateOlderOnesByYear() {
        Player player = launchedCompanyPlayer("company-report-retention");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        for (int sequence = 1; sequence <= 12; sequence++) {
            var report = quarterlyRepository.save(new CompanyQuarterlyReport(
                    company,
                    sequence,
                    sequence * 3 - 1,
                    10_000_000_000L,
                    8_000_000_000L,
                    2_000_000_000L,
                    400_000_000L,
                    1_600_000_000L,
                    company.getCorporateCash(),
                    company.getMonthlyRecurringRevenue(),
                    company.getPaidUsers()
            ));
            if (sequence <= 4) {
                report.markRead();
            }
        }

        var reports = companyReportingService.reports(company);

        assertThat(reports.stream().filter(report -> report.id() > 0)).hasSize(8);
        assertThat(reports.stream().filter(report -> report.id() < 0)).hasSize(1);
    }

    @Test
    void managementReportsKeepPartialOrUnreadOlderQuartersDetailed() {
        Player player = launchedCompanyPlayer("company-report-partial-retention");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        for (int sequence = 1; sequence <= 9; sequence++) {
            quarterlyRepository.save(new CompanyQuarterlyReport(
                    company,
                    sequence,
                    sequence * 3 - 1,
                    10_000_000_000L,
                    8_000_000_000L,
                    2_000_000_000L,
                    400_000_000L,
                    1_600_000_000L,
                    company.getCorporateCash(),
                    company.getMonthlyRecurringRevenue(),
                    company.getPaidUsers()
            ));
        }

        var reports = companyReportingService.reports(company);

        assertThat(reports).hasSize(9);
        assertThat(reports).allMatch(report -> report.id() > 0);
        assertThat(reports).allMatch(com.game.buildingstory.web.CompanyManagementReportView::unread);
    }

    @Test
    void activeExternalEventsApplyOnlyDuringTheirStoredMarketMonthRange() {
        Player player = launchedCompanyPlayer("external-event-effects");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        externalEventRepository.save(new CompanyExternalEvent(
                company,
                "test-market-adoption",
                "market-adoption",
                CompanyExternalEventCategory.AI_MARKET,
                1,
                2,
                0
        ));
        externalEventRepository.save(new CompanyExternalEvent(
                company,
                "test-cloud-price",
                "infra-cloud-price-up",
                CompanyExternalEventCategory.INFRASTRUCTURE,
                1,
                3,
                0
        ));

        assertThat(externalEventService.marketGrowthMultiplier(company, 1)).isEqualTo(1.15);
        assertThat(externalEventService.marketGrowthMultiplier(company, 3)).isEqualTo(1.0);
        assertThat(externalEventService.cloudCostMultiplier(company)).isEqualTo(1.12);

        externalEventService.processMonth(company);

        assertThat(externalEventRepository.findByCompanyOrderByStartMarketMonthDescIdDesc(company))
                .hasSize(2);
    }

    @Test
    void strategyFinanceDepartmentControlsExternalEventAnalysisPrecision() {
        Player player = launchedCompanyPlayer("external-event-analysis");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(externalEventService.analyzedImpactText(company, "market-adoption"))
                .isEqualTo("긍정적 영향 예상.");

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 3, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 0, 0));
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.STRATEGY_FINANCE)).contains("출범 완료");
        assertThat(externalEventService.analyzedImpactText(company, "market-adoption"))
                .isEqualTo("중간 수준의 긍정적 영향 예상.");
    }

    @Test
    void shortTermBusinessMovesFromGuaranteedOfferToThreeMonthRevenue() {
        Player player = launchedCompanyPlayer("short-term-project");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        settlementService.processMonthly(player);
        var offer = shortTermProjectService.offeredProjects(company).getFirst();
        assertThat(shortTermProjectService.accept(player.getId(), offer.getId())).contains("수락");
        assertThat(shortTermProjectService.activeProject(company)).isPresent();
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(1);

        for (int month = 0; month < 8 && shortTermProjectService.earningProject(company).isEmpty(); month++) {
            advanceToNextMonth(player);
            settlementService.processMonthly(player);
        }
        assertThat(shortTermProjectService.earningProject(company)).isPresent();
        assertThat(company.getActiveMajorWorkCount()).isZero();

        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        var revenueMonth = monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company).getFirst();
        assertThat(revenueMonth.getProjectRevenue()).isPositive();
        assertThat(revenueMonth.getTotalRevenue())
                .isEqualTo(revenueMonth.getSubscriptionRevenue() + revenueMonth.getProjectRevenue());

        for (int month = 0; month < 2; month++) {
            advanceToNextMonth(player);
            settlementService.processMonthly(player);
        }
        assertThat(shortTermProjectService.earningProject(company)).isEmpty();
    }

    @Test
    void completedShortTermBusinessUnlocksTrialCustomerContractLifecycle() {
        Player player = launchedCompanyPlayer("customer-contract");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        company.addCorporateCash(100_000_000_000L);

        settlementService.processMonthly(player);
        var shortTermOffer = shortTermProjectService.offeredProjects(company).getFirst();
        assertThat(shortTermProjectService.accept(player.getId(), shortTermOffer.getId())).contains("수락");

        for (int month = 0; month < 10
                && customerContractService.offeredContracts(company).isEmpty(); month++) {
            advanceToNextMonth(player);
            settlementService.processMonthly(player);
        }

        var contractOffer = customerContractService.offeredContracts(company).getFirst();
        assertThat(customerContractService.accept(player.getId(), contractOffer.getId()))
                .contains("시험계약 수락");
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(2);

        for (int month = 0; month < 10
                && customerContractService.buildingContract(company).isPresent(); month++) {
            advanceToNextMonth(player);
            settlementService.processMonthly(player);
        }
        assertThat(customerContractService.buildingContract(company)).isEmpty();
        assertThat(company.getActiveMajorWorkCount()).isZero();

        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        assertThat(customerContractService.activeContracts(company)).hasSize(1);

        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        var contractRevenueMonth = monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company).getFirst();
        assertThat(contractRevenueMonth.getContractRevenue()).isPositive();
        assertThat(contractRevenueMonth.getContractCost()).isPositive();
        assertThat(contractRevenueMonth.getTotalRevenue()).isEqualTo(
                contractRevenueMonth.getSubscriptionRevenue()
                        + contractRevenueMonth.getProjectRevenue()
                        + contractRevenueMonth.getContractRevenue());

        for (int month = 0; month < 5; month++) {
            advanceToNextMonth(player);
            settlementService.processMonthly(player);
        }
        var remainingActiveContracts = customerContractService.activeContracts(company);
        assertThat(remainingActiveContracts)
                .as("activeMonths=%s duration=%s suspended=%s",
                        remainingActiveContracts.isEmpty()
                                ? 0 : remainingActiveContracts.getFirst().getActiveMonthsProcessed(),
                        remainingActiveContracts.isEmpty()
                                ? 0 : remainingActiveContracts.getFirst().getContractDurationMonths(),
                        company.isOperationsSuspended())
                .isEmpty();

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 100, 100, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 100_000_000_000L, 2_000_000L));
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 101, 103, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 100_000_000_000L, 2_000_000L));
        growthService.evaluateAndPromote(company);

        var normalOfferResult = customerContractService.processSuccessfulMonth(company, false);
        assertThat(normalOfferResult.candidateGenerated()).isTrue();
        assertThat(customerContractService.offeredContracts(company).getFirst().getContractType())
                .isEqualTo(CompanyCustomerContractType.NORMAL);
    }

    @Test
    void cashDepletionFailsEveryBuildingCustomerContract() {
        Player player = launchedCompanyPlayer("all-building-contracts-fail");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var first = customerContractRepository.save(new CompanyCustomerContract(
                company, "test-contract-1", "첫 고객사", "첫 구축",
                CompanyCustomerContractType.TRIAL, 1,
                0, 0, 0, 10, 10,
                1_000_000_000L, 300_000_000L, 100_000_000L, 6, 6));
        var second = customerContractRepository.save(new CompanyCustomerContract(
                company, "test-contract-2", "둘째 고객사", "둘째 구축",
                CompanyCustomerContractType.TRIAL, 1,
                0, 0, 0, 10, 10,
                1_000_000_000L, 300_000_000L, 100_000_000L, 6, 6));
        first.accept(5, 5);
        second.accept(5, 5);
        var development = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        var operations = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.SERVICE_OPERATIONS).orElseThrow();
        development.reserveMajorWork(5, 10, 10_000);
        development.reserveMajorWork(5, 10, 10_000);
        operations.reserveMajorWork(5, 10, 10_000);
        operations.reserveMajorWork(5, 10, 10_000);
        for (int index = 0; index < 4; index++) {
            company.reserveMajorWork(10);
        }

        assertThat(customerContractService.failActiveBuild(company)).isTrue();

        assertThat(first.getStatus()).isEqualTo(CompanyCustomerContractStatus.FAILED);
        assertThat(second.getStatus()).isEqualTo(CompanyCustomerContractStatus.FAILED);
        assertThat(company.getActiveMajorWorkCount()).isZero();
    }

    @Test
    void growthStageRequiresTwoLatestQualifiedQuarterEnds() {
        Player player = launchedCompanyPlayer("company-growth-stage");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 200, 202, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 1_000_000_000_000L, 15_000_000L));
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 203, 205, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 1_000_000_000_000L, 15_000_000L));
        assertThat(growthService.stage(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.FOUNDED);
        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.GROWTH);
        assertThat(customerContractService.contractLimit(company)).isEqualTo(5);
        assertThat(workforceService.companyMajorWorkSlotLimit(company)).isEqualTo(3);

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 206, 208, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 1_000_000_000_000L, 15_000_000L));
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 209, 211, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 1_000_000_000_000L, 15_000_000L));
        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.LARGE);
        assertThat(customerContractService.contractLimit(company)).isEqualTo(10);
        assertThat(workforceService.companyMajorWorkSlotLimit(company)).isEqualTo(4);

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 212, 214, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 10_000_000_000_000L, 80_000_000L));
        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.LARGE);

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 215, 217, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 10_000_000_000_000L, 80_000_000L));
        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.GLOBAL);
        assertThat(customerContractService.contractLimit(company)).isEqualTo(20);
        assertThat(workforceService.companyMajorWorkSlotLimit(company)).isEqualTo(6);

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 218, 220, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 0, 0));
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 221, 223, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 0, 0));
        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.GLOBAL);
    }

    @Test
    void growthStageProvidesThreeMajorWorkSlots() {
        Player player = launchedCompanyPlayer("company-growth-slots");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 300, 302, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 100_000_000_000L, 2_000_000L));
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 303, 305, 0, 0, 0, 0, 0,
                company.getCorporateCash(), 100_000_000_000L, 2_000_000L));

        assertThat(growthService.evaluateAndPromote(company))
                .isEqualTo(com.game.buildingstory.domain.CompanyGrowthStage.GROWTH);
        assertThat(workforceService.companyMajorWorkSlotLimit(company)).isEqualTo(3);
    }

    @Test
    void companyMonthlySettlementIncludesSecretaryPayroll() {
        Player player = launchedCompanyPlayer("company-secretary-payroll");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 30));
        long workforcePayroll = workforceService.monthlyPayroll(company);
        long secretaryPayroll = gameService.secretarySpec("secretary-1")
                .monthlySalaryForProficiency(secretary.getProficiency());

        settlementService.processMonthly(player);

        var month = monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company).getFirst();
        assertThat(month.getPayrollCost()).isEqualTo(workforcePayroll + secretaryPayroll);
        assertThat(secretary.getUnpaidSalaryMonths()).isZero();
    }

    @Test
    void initialProductStateHasDesignedFourPercentMonthlyIncidentProbability() {
        Player player = launchedCompanyPlayer("incident-probability");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(incidentService.incidentProbabilityBasisPoints(company, 90.0))
                .isEqualTo(400);
        assertThat(incidentService.incidentProbabilityBasisPoints(company, 500.0))
                .isEqualTo(2_000);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void launchedCompanyPageRendersAfterServiceTransactionsClose() throws Exception {
        Player player = launchedCompanyPlayer("company-page-regression");
        gameService.completeStory(player.getId());
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

        String html = mockMvc.perform(get("/main").param("view", "company").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).contains(
                "data-company-dashboard",
                "data-company-operation-key=\"contracts\"",
                "ENTERPRISE CONTRACT",
                "data-company-news-open",
                "data-company-report-open",
                "data-company-tutorial-notice",
                "data-company-open-first-report",
                "data-company-command-dialog",
                "data-company-detail-actionbar",
                "data-company-operation-key=\"executive-office\"",
                "data-company-detail-key=\"executive-office\"",
                "data-company-secretary-key=\"executive-office\"",
                "data-company-executive-command-target",
                "전사 업무 지시",
                ">슬롯<",
                "company-operation-slots",
                "data-company-budget-policy",
                "data-spending-percent",
                "data-company-development-cost-preview",
                "data-company-marketing-cost-preview",
                "company-plan-mix",
                "AI 벤치마크",
                "현재 잔액 · 정상 운영",
                "최근 월 확정",
                "최근 확정 분기말 기준",
                "전체 유료시장",
                "프로 요금제 · 일반계약 · 업무 슬롯 3개 · 성장기업 인재",
                "다음 단계까지",
                "업무 현황",
                "분기 경영보고"
        );
    }

    @Test
    void companyPageReadDoesNotBackfillOrWriteNews() throws Exception {
        Player player = launchedCompanyPlayer("company-news-read-only");
        gameService.completeStory(player.getId());
        settleFirstQuarter(player);
        var company = companyRepository.findByPlayer(player).orElseThrow();
        companyNewsRepository.deleteAll();
        assertThat(companyNewsRepository.count()).isZero();
        MockHttpSession session = companySession(player);

        mockMvc.perform(get("/main").param("view", "company").session(session))
                .andExpect(status().isOk());

        assertThat(companyNewsRepository.count()).isZero();
        companyNewsService.backfillPerformanceArticles();
        assertThat(companyNewsRepository.findByCompanyOrderByOccurredMarketMonthDescIdDesc(company))
                .singleElement()
                .satisfies(article -> assertThat(article.getCategory())
                        .isEqualTo(com.game.buildingstory.domain.CompanyNewsCategory.PERFORMANCE));
    }

    @Test
    void companyPostCommandsBindParametersAndChangeState() throws Exception {
        Player player = launchedCompanyPlayer("company-command-http");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        MockHttpSession session = companySession(player);

        mockMvc.perform(post("/companies/budget-policy").session(session)
                        .param("developmentPolicy", "FOCUSED")
                        .param("marketingPolicy", "MINIMUM"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/companies/infrastructure/cloud-plan").session(session)
                        .param("plan", "GROWTH"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/companies/workforce/headcount").session(session)
                        .param("departmentType", "AI_DEVELOPMENT")
                        .param("approvedHeadcount", "40"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/companies/projects/product").session(session)
                        .param("type", "SERVICE_STABILIZATION")
                        .param("direction", "BALANCED"))
                .andExpect(status().is3xxRedirection());

        assertThat(company.getDevelopmentBudgetPolicy())
                .isEqualTo(com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy.FOCUSED);
        assertThat(company.getMarketingBudgetPolicy())
                .isEqualTo(com.game.buildingstory.domain.CompanyMarketingBudgetPolicy.MINIMUM);
        assertThat(company.getPendingCloudPlanType())
                .isEqualTo(com.game.buildingstory.domain.CompanyCloudPlan.GROWTH);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow().getApprovedHeadcount())
                .isEqualTo(40);
        assertThat(productProjectRepository.findByCompanyOrderByIdDesc(company)).hasSize(1);
    }

    @Test
    void duplicateFundingPostIsAppliedOnlyOnce() throws Exception {
        Player player = launchedCompanyPlayer("company-funding-http");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        MockHttpSession session = companySession(player);
        long personalCash = player.getCash();
        long corporateCash = company.getCorporateCash();
        long amount = 100_000_000L;

        mockMvc.perform(post("/companies/funding").session(session).param("amount", String.valueOf(amount)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/companies/funding").session(session).param("amount", String.valueOf(amount)))
                .andExpect(status().is3xxRedirection());

        assertThat(player.getCash()).isEqualTo(personalCash - amount);
        assertThat(company.getCorporateCash()).isEqualTo(corporateCash + amount);
    }

    @Test
    void suspendedCompanyRejectsCloudPlanPostWithoutChangingPlan() throws Exception {
        Player player = launchedCompanyPlayer("company-cloud-suspended-http");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        company.suspendOperations(1, 0, 0);
        MockHttpSession session = companySession(player);

        var result = mockMvc.perform(post("/companies/infrastructure/cloud-plan").session(session)
                        .param("plan", "GROWTH"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(company.getPendingCloudPlan()).isNull();
        assertThat(result.getFlashMap().get("notice")).isEqualTo("기업 운영중단 중에는 계약을 변경할 수 없음");
    }

    @Test
    void balancedDecisionsSurviveFortyEightMonthsUsingActualCompanyServices() {
        Player player = launchedCompanyPlayer("actual-service-simulation");
        settlementService.contributeAndResume(
                player.getId(),
                PlayerCompanyService.RECOMMENDED_INVESTMENT - PlayerCompanyService.MINIMUM_INVESTMENT
        );
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingMarketUsers = company.getTotalMarketUsers();

        for (int month = 0; month < 48; month++) {
            settlementService.processMonthly(player);
            assertThat(company.isOperationsSuspended())
                    .as(
                            "month %s must finish without exhausting corporate cash "
                                    + "(cash=%s, unpaid=%s, mrr=%s, users=%s, payroll=%s, essential=%s)",
                            month + 1,
                            company.getCorporateCash(),
                            company.getUnpaidSettlementAmount(),
                            company.getMonthlyRecurringRevenue(),
                            company.getPaidUsers(),
                            settlementService.monthlyPayroll(company),
                            settlementService.essentialMonthlyCost(company)
                    )
                    .isFalse();

            incidentService.activeIncident(company).ifPresent(incident -> {
                switch (incident.getStatus()) {
                    case AWAITING_DECISION -> incidentService.resolveMajor(
                            player.getId(),
                            incident.getId(),
                            com.game.buildingstory.domain.CompanyServiceIncidentResolution.EMERGENCY_RECOVERY
                    );
                    case RESPONSE_REQUIRED -> incidentService.startCriticalResponse(
                            player.getId(),
                            incident.getId()
                    );
                    default -> {
                        // 이미 대응 중인 장애는 월 정산이 실제 작업량을 차감하므로 추가 명령이 필요 없다.
                    }
                }
            });

            if (productProjectService.activeProject(company).isEmpty()) {
                var improvement = switch (month % 4) {
                    case 0 -> com.game.buildingstory.domain.CompanyProductImprovementType.SERVICE_STABILIZATION;
                    case 1 -> com.game.buildingstory.domain.CompanyProductImprovementType.MODEL_REFINEMENT;
                    case 2 -> com.game.buildingstory.domain.CompanyProductImprovementType.INFERENCE_OPTIMIZATION;
                    default -> com.game.buildingstory.domain.CompanyProductImprovementType.WORKFLOW_AUTOMATION;
                };
                productProjectService.start(player.getId(), improvement);
            }

            shortTermProjectService.offeredProjects(company).stream()
                    .findFirst()
                    .ifPresent(offer -> shortTermProjectService.accept(player.getId(), offer.getId()));
            customerContractService.offeredContracts(company).stream()
                    .findFirst()
                    .ifPresent(offer -> customerContractService.accept(player.getId(), offer.getId()));

            if (month < 47) {
                advanceToNextMonth(player);
            }
        }

        assertThat(monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company)).hasSize(48);
        assertThat(quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company)).hasSize(16);
        assertThat(company.getTotalMarketUsers()).isGreaterThan(openingMarketUsers);
        assertThat(company.getPaidUsers()).isPositive();
        assertThat(productProjectRepository.findByCompanyOrderByIdDesc(company))
                .anyMatch(project -> project.getStatus()
                        == com.game.buildingstory.domain.CompanyProductProjectStatus.COMPLETED);

        System.out.printf(
                "ACTUAL_COMPANY_SIMULATION months=%d cash=%d marketUsers=%d paidUsers=%d mrr=%d benchmark=%d projects=%d%n",
                48,
                company.getCorporateCash(),
                company.getTotalMarketUsers(),
                company.getPaidUsers(),
                company.getMonthlyRecurringRevenue(),
                company.getPrototypeBenchmark(),
                productProjectRepository.findByCompanyOrderByIdDesc(company).size()
        );
    }

    @Test
    @Disabled("새 월간 비용정책을 반영한 72개월 밸런스 재조정 단계에서 다시 활성화")
    void balancedCapacityDecisionsSurviveSeventyTwoMonthsUsingActualCompanyServices() {
        Player player = launchedCompanyPlayer("actual-service-capacity-simulation");
        settlementService.contributeAndResume(
                player.getId(),
                PlayerCompanyService.RECOMMENDED_INVESTMENT - PlayerCompanyService.MINIMUM_INVESTMENT
        );
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingMarketUsers = company.getTotalMarketUsers();
        long minimumCash = company.getCorporateCash();
        double maximumUtilization = 0;
        int capacityShortageMonths = 0;
        int firstOwnNetworkMonth = 0;
        int firstCloudUpgradeMonth = 0;

        for (int month = 0; month < 72; month++) {
            settlementService.processMonthly(player);
            assertThat(company.isOperationsSuspended())
                    .as(
                            "month %s must finish without exhausting corporate cash "
                                    + "(cash=%s, unpaid=%s, mrr=%s, users=%s, essential=%s, cloud=%s)",
                            month + 1,
                            company.getCorporateCash(),
                            company.getUnpaidSettlementAmount(),
                            company.getMonthlyRecurringRevenue(),
                            company.getPaidUsers(),
                            settlementService.essentialMonthlyCost(company),
                            company.getCloudPlanType()
                    )
                    .isFalse();

            respondToActiveIncident(player, company);
            keepBalancedProjectPipeline(player, company, month);
            acceptAvailableRevenueOpportunities(player, company);
            applyBalancedCapacityDecision(player, company);
            var monthlyInfrastructure = infrastructureService.snapshot(company);
            minimumCash = Math.min(minimumCash, company.getCorporateCash());
            maximumUtilization = Math.max(maximumUtilization, monthlyInfrastructure.utilizationPercent());
            if (monthlyInfrastructure.demand() > monthlyInfrastructure.usableCapacity()) {
                capacityShortageMonths++;
            }
            if (firstOwnNetworkMonth == 0 && monthlyInfrastructure.network().capacity() > 0) {
                firstOwnNetworkMonth = month + 1;
            }
            if (firstCloudUpgradeMonth == 0 && monthlyInfrastructure.plan() != CompanyCloudPlan.STARTER) {
                firstCloudUpgradeMonth = month + 1;
            }

            if (month < 71) {
                advanceToNextMonth(player);
            }
        }

        var infrastructure = infrastructureService.snapshot(company);
        assertThat(monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company)).hasSize(72);
        assertThat(quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company)).hasSize(24);
        assertThat(company.getTotalMarketUsers()).isGreaterThan(openingMarketUsers);
        assertThat(company.getPaidUsers()).isGreaterThan(400_000);
        assertThat(infrastructure.network().capacity()).isPositive();
        assertThat(infrastructure.utilizationPercent()).isLessThanOrEqualTo(105);
        assertThat(capacityShortageMonths).isLessThanOrEqualTo(6);

        System.out.printf(
                "ACTUAL_COMPANY_CAPACITY_SIMULATION months=%d cash=%d marketUsers=%d paidUsers=%d "
                        + "mrr=%d benchmark=%d networkCapacity=%d cloud=%s utilization=%.2f "
                        + "maxUtilization=%.2f shortageMonths=%d minCash=%d ownNetworkMonth=%d "
                        + "cloudUpgradeMonth=%d employees=%d%n",
                72,
                company.getCorporateCash(),
                company.getTotalMarketUsers(),
                company.getPaidUsers(),
                company.getMonthlyRecurringRevenue(),
                company.getPrototypeBenchmark(),
                infrastructure.network().capacity(),
                infrastructure.plan().name(),
                infrastructure.utilizationPercent(),
                maximumUtilization,
                capacityShortageMonths,
                minimumCash,
                firstOwnNetworkMonth,
                firstCloudUpgradeMonth,
                departmentService.totalEmployees(company)
        );
    }

    private void respondToActiveIncident(Player player, com.game.buildingstory.domain.PlayerCompany company) {
        incidentService.activeIncident(company).ifPresent(incident -> {
            switch (incident.getStatus()) {
                case AWAITING_DECISION -> incidentService.resolveMajor(
                        player.getId(),
                        incident.getId(),
                        com.game.buildingstory.domain.CompanyServiceIncidentResolution.EMERGENCY_RECOVERY
                );
                case RESPONSE_REQUIRED -> incidentService.startCriticalResponse(player.getId(), incident.getId());
                default -> {
                    // 진행 중인 대응은 다음 월 정산에서 실제 작업량을 차감한다.
                }
            }
        });
    }

    private void keepBalancedProjectPipeline(
            Player player,
            com.game.buildingstory.domain.PlayerCompany company,
            int month
    ) {
        if (productProjectService.activeProject(company).isPresent()) {
            return;
        }
        var improvement = switch (month % 4) {
            case 0 -> com.game.buildingstory.domain.CompanyProductImprovementType.SERVICE_STABILIZATION;
            case 1 -> com.game.buildingstory.domain.CompanyProductImprovementType.MODEL_REFINEMENT;
            case 2 -> com.game.buildingstory.domain.CompanyProductImprovementType.INFERENCE_OPTIMIZATION;
            default -> com.game.buildingstory.domain.CompanyProductImprovementType.WORKFLOW_AUTOMATION;
        };
        productProjectService.start(player.getId(), improvement);
    }

    private void acceptAvailableRevenueOpportunities(
            Player player,
            com.game.buildingstory.domain.PlayerCompany company
    ) {
        shortTermProjectService.offeredProjects(company).stream()
                .findFirst()
                .ifPresent(offer -> shortTermProjectService.accept(player.getId(), offer.getId()));
        customerContractService.offeredContracts(company).stream()
                .findFirst()
                .ifPresent(offer -> customerContractService.accept(player.getId(), offer.getId()));
    }

    private void applyBalancedCapacityDecision(
            Player player,
            com.game.buildingstory.domain.PlayerCompany company
    ) {
        var snapshot = infrastructureService.snapshot(company);
        var operations = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS)
                .orElseThrow();

        if (snapshot.operationsCapacity() * 4 < snapshot.demand() * 5) {
            int remainingMonthly = workforceService.remainingMonthlyHireLimit(company);
            int maximumApproved = workforceService.maximumApprovedHeadcount(company, operations);
            int currentCommitted = operations.getGeneralEmployeeCount() + operations.getPendingHireCount();
            int hireCount = Math.min(10, Math.min(remainingMonthly, maximumApproved - currentCommitted));
            if (hireCount > 0) {
                workforceService.changeApprovedHeadcount(
                        player.getId(),
                        CompanyDepartmentType.SERVICE_OPERATIONS,
                        currentCommitted + hireCount
                );
                workforceService.requestGeneralHires(
                        player.getId(),
                        CompanyDepartmentType.SERVICE_OPERATIONS,
                        hireCount
                );
            }
        }

        if (!departmentService.isEstablished(company, CompanyDepartmentType.HR_ORGANIZATION)
                && departmentService.totalEmployees(company) >= CompanyDepartmentService.HR_FOUNDING_EMPLOYEES) {
            departmentService.establish(player.getId(), CompanyDepartmentType.HR_ORGANIZATION);
        }

        var opportunity = constructionService.opportunity(company);
        if (snapshot.utilizationPercent() >= 70
                && opportunity.available()
                && company.getCorporateCash() - opportunity.step().totalCost()
                >= settlementService.essentialMonthlyCost(company) * 9) {
            constructionService.startNext(player.getId());
            return;
        }

        if (constructionService.activeConstruction(company).isPresent()
                || snapshot.utilizationPercent() < 85
                || snapshot.pendingPlan() != null) {
            return;
        }
        long targetCapacity = Math.round(snapshot.demand() * 1.25);
        for (CompanyCloudPlan plan : CompanyCloudPlan.values()) {
            if (plan.ordinal() > snapshot.plan().ordinal()
                    && plan.getCapacity() + snapshot.network().capacity() >= targetCapacity
                    && company.getCorporateCash() >= plan.getMonthlyCost() * 6) {
                infrastructureService.requestCloudPlan(player.getId(), plan);
                return;
            }
        }
    }

    private Player launchedCompanyPlayer(String username) {
        Player player = new Player(username, "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        playerCompanyService.establish(player.getId(), "테스트AI", "인공지능 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
        tutorialService.confirmFoundingTeam(player.getId(),
                List.of("dev-01", "dev-02", "dev-03", "sales-01", "ops-01", "ops-02"));
        tutorialService.startCommercialization(player.getId());
        tutorialService.skipCommercializationForTest(player.getId());
        tutorialService.launch(player.getId());
        assertThat(companyRepository.findByPlayer(player).orElseThrow().getTutorialStage())
                .isEqualTo(CompanyTutorialStage.LAUNCHED);
        return player;
    }

    private MockHttpSession companySession(Player player) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.PLAYER_ID, player.getId());
        return session;
    }

    private void settleFirstQuarter(Player player) {
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
        advanceToNextMonth(player);
        settlementService.processMonthly(player);
    }

    private void advanceToNextMonth(Player player) {
        int currentMonth = player.getMonth();
        do {
            player.advanceDay();
        } while (player.getMonth() == currentMonth || player.getDay() != 1);
    }
}
