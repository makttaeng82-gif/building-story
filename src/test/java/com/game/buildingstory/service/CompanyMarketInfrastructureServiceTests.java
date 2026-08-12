package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCloudPlan;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.CompanyComputeTier;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyDevelopmentDirection;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyProductImprovementType;
import com.game.buildingstory.domain.CompanyProductProjectStatus;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyComputeConstructionRepository;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyProductProjectRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:company-market-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class CompanyMarketInfrastructureServiceTests {
    @Autowired private PlayerCompanyService companyService;
    @Autowired private CompanyTutorialService tutorialService;
    @Autowired private CompanyMarketService marketService;
    @Autowired private CompanyInfrastructureService infrastructureService;
    @Autowired private CompanyProductProjectService productProjectService;
    @Autowired private CompanyComputeConstructionService computeConstructionService;
    @Autowired private CompanySettlementService settlementService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PlayerCompanyRepository companyRepository;
    @Autowired private CompanyCompetitorRepository competitorRepository;
    @Autowired private CompanyCoreEmployeeRepository employeeRepository;
    @Autowired private CompanyDepartmentRepository departmentRepository;
    @Autowired private CompanyMonthlySettlementRepository monthlyRepository;
    @Autowired private CompanyQuarterlyReportRepository quarterlyRepository;
    @Autowired private CompanyProductProjectRepository productProjectRepository;
    @Autowired private CompanyComputeConstructionRepository computeConstructionRepository;
    @Autowired private MonthlyRecordRepository monthlyRecordRepository;
    @Autowired private OwnedSecretaryRepository ownedSecretaryRepository;
    @Autowired private CompanyFoundationTestSupport foundationTestSupport;

    @BeforeEach
    void cleanDatabase() {
        computeConstructionRepository.deleteAll();
        productProjectRepository.deleteAll();
        quarterlyRepository.deleteAll();
        monthlyRepository.deleteAll();
        monthlyRecordRepository.deleteAll();
        competitorRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        companyRepository.deleteAll();
        ownedSecretaryRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void initializesThreeCompetitorsAndCalculatesMonthlyMarketRevenue() {
        Player player = launchedPlayer("market-month-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingMarket = company.getTotalMarketUsers();

        var result = marketService.processMonth(company);

        assertThat(result.competitors()).hasSize(3);
        assertThat(result.totalMarketUsers()).isGreaterThan(openingMarket);
        assertThat(result.paidUsers()).isEqualTo(
                result.normalSubscribers() + result.proSubscribers() + result.maxSubscribers());
        assertThat(result.monthlyRecurringRevenue()).isEqualTo(
                result.normalSubscribers() * 20_000L
                        + result.proSubscribers() * 200_000L
                        + result.maxSubscribers() * 1_000_000L);
        double totalShare = result.playerMarketShare()
                + result.competitors().stream().mapToDouble(item -> item.marketShare()).sum();
        assertThat(totalShare).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void paidPlansRemainLockedUntilTheirGrowthStage() {
        Player player = launchedPlayer("growth-plan-gate-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        company.applyProductImprovement(
                CompanyProductImprovementType.WORKFLOW_AUTOMATION,
                CompanyDevelopmentDirection.BALANCED,
                100
        );

        marketService.processMonth(company);

        assertThat(company.getGrowthStage()).isEqualTo(CompanyGrowthStage.FOUNDED);
        assertThat(company.getProSubscribers()).isZero();
        assertThat(company.getMaxSubscribers()).isZero();

        company.promoteGrowthStage(CompanyGrowthStage.GROWTH);
        marketService.processMonth(company);

        assertThat(company.getProSubscribers()).isPositive();
        assertThat(company.getMaxSubscribers()).isZero();
    }

    @Test
    void appliesCloudChangeAtNextSettlementAndUsesItsMonthlyCost() {
        Player player = launchedPlayer("cloud-plan-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(infrastructureService.requestCloudPlan(player.getId(), CompanyCloudPlan.GROWTH))
                .contains("다음 월 정산");
        assertThat(company.getCloudPlanType()).isEqualTo(CompanyCloudPlan.STARTER);

        settlementService.processMonthly(player);

        assertThat(company.getCloudPlanType()).isEqualTo(CompanyCloudPlan.GROWTH);
        assertThat(monthlyRepository.findByCompanyOrderByPeriodIndexDesc(company).getFirst().getCloudCost())
                .isEqualTo(CompanyCloudPlan.GROWTH.getMonthlyCost());
    }

    @Test
    void starterCapacityLimitsSubscriberGrowthWithoutExceedingProcessingLimit() {
        Player player = launchedPlayer("capacity-limit-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        for (int month = 0; month < 18; month++) {
            marketService.processMonth(company);
        }
        var infrastructure = infrastructureService.snapshot(company);

        assertThat(infrastructure.demand()).isLessThanOrEqualTo(infrastructure.usableCapacity() + 1);
        assertThat(company.getPaidUsers()).isLessThan(company.getTotalMarketUsers());
    }

    @Test
    void productImprovementProgressesMonthlyAppliesResultAndReleasesWorkload() {
        Player player = launchedPlayer("product-project-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        int openingBenchmark = company.getPrototypeBenchmark();

        assertThat(productProjectService.start(player.getId(), CompanyProductImprovementType.MODEL_REFINEMENT))
                .contains("시작");
        assertThat(productProjectService.start(player.getId(), CompanyProductImprovementType.SERVICE_STABILIZATION))
                .contains("이미 진행 중");

        var active = productProjectService.activeProject(company).orElseThrow();
        int requiredMonths = (int) Math.ceil(active.getRemainingWork() / (double) active.getMonthlyAssignedWork());
        int actualMonths = 0;
        while (productProjectService.activeProject(company).isPresent() && actualMonths < 24) {
            productProjectService.processMonth(company);
            actualMonths++;
        }

        assertThat(productProjectService.activeProject(company)).isEmpty();
        assertThat(actualMonths).isGreaterThanOrEqualTo(requiredMonths);
        assertThat(productProjectRepository.findByCompanyOrderByIdDesc(company).getFirst().getStatus())
                .isEqualTo(CompanyProductProjectStatus.COMPLETED);
        assertThat(company.getPrototypeBenchmark()).isGreaterThan(openingBenchmark);
        assertThat(company.getActiveMajorWorkCount()).isZero();
        assertThat(departmentRepository.findByCompanyOrderById(company))
                .allMatch(department -> department.getAllocatedMajorWorkload() == 0);
    }

    @Test
    void constructsOwnComputeNetworkInOrderAndAddsCompletedCapacityAndCost() {
        Player player = launchedPlayer("compute-construction-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingCash = company.getCorporateCash();

        assertThat(computeConstructionService.startNext(player.getId()))
                .contains("소형 연산망 기반 구축 착공");
        var construction = computeConstructionService.activeConstruction(company).orElseThrow();

        assertThat(construction.getTier()).isEqualTo(CompanyComputeTier.SMALL);
        assertThat(construction.getPhase()).isZero();
        assertThat(company.getCorporateCash()).isEqualTo(openingCash - construction.getUpfrontPayment());
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS)
                .orElseThrow().getAllocatedMajorWorkload()).isEqualTo(CompanyComputeConstructionService.SERVICE_OPERATIONS_WORKLOAD);

        computeConstructionService.processMonth(company);
        var completion = computeConstructionService.processMonth(company);

        assertThat(completion.completed()).isTrue();
        assertThat(construction.getStatus()).isEqualTo(CompanyComputeConstructionStatus.COMPLETED);
        assertThat(company.getCorporateCash()).isEqualTo(openingCash - construction.getTotalCost());
        assertThat(company.getActiveMajorWorkCount()).isZero();
        assertThat(computeConstructionService.networkState(company).capacity()).isEqualTo(250_000L);
        assertThat(computeConstructionService.networkState(company).monthlyCost()).isEqualTo(1_800_000_000L);
        assertThat(infrastructureService.snapshot(company).permanentCapacity())
                .isEqualTo(company.getCloudPlanType().getCapacity() + 250_000L);
        assertThat(computeConstructionService.opportunity(company).step().phase()).isEqualTo(1);
    }

    @Test
    void modelDirectionChangesResultAndNextGenerationShowsItsUnlockReason() {
        Player performancePlayer = launchedPlayer("performance-direction-test");
        var performanceCompany = companyRepository.findByPlayer(performancePlayer).orElseThrow();
        assertThat(productProjectService.start(
                performancePlayer.getId(),
                CompanyProductImprovementType.MODEL_REFINEMENT,
                CompanyDevelopmentDirection.PERFORMANCE
        )).contains("성능 우선");
        assertThat(productProjectService.activeProject(performanceCompany).orElseThrow().getDevelopmentDirection())
                .isEqualTo(CompanyDevelopmentDirection.PERFORMANCE);
        completeActiveProductProject(performanceCompany);

        Player efficiencyPlayer = launchedPlayer("efficiency-direction-test");
        var efficiencyCompany = companyRepository.findByPlayer(efficiencyPlayer).orElseThrow();
        productProjectService.start(
                efficiencyPlayer.getId(),
                CompanyProductImprovementType.MODEL_REFINEMENT,
                CompanyDevelopmentDirection.EFFICIENCY
        );
        completeActiveProductProject(efficiencyCompany);

        assertThat(performanceCompany.getPrototypeBenchmark())
                .isGreaterThan(efficiencyCompany.getPrototypeBenchmark());
        assertThat(performanceCompany.getComputeEfficiency())
                .isLessThan(efficiencyCompany.getComputeEfficiency());
        assertThat(performanceCompany.getTechnicalDebt())
                .isGreaterThan(efficiencyCompany.getTechnicalDebt());
        assertThat(productProjectService.eligibility(
                performanceCompany, CompanyProductImprovementType.NEXT_GENERATION_MODEL).reason())
                .contains("벤치마크 300점");
        assertThat(productProjectService.totalWork(CompanyProductImprovementType.NEXT_GENERATION_MODEL, 300))
                .isEqualTo(1_050);
    }

    @Test
    void reserveCapacityIsPrepaidAppliedImmediatelyAndExpiresAfterThirtyDays() {
        Player player = launchedPlayer("reserve-capacity-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingCash = company.getCorporateCash();

        assertThat(infrastructureService.purchaseReserveCapacity(player.getId(), 50))
                .contains("30일간 200,000 추가");
        var active = infrastructureService.snapshot(company);

        assertThat(company.getCorporateCash()).isEqualTo(openingCash - 8_100_000_000L);
        assertThat(active.permanentCapacity()).isEqualTo(400_000L);
        assertThat(active.reserveCapacity()).isEqualTo(200_000L);
        assertThat(active.totalCapacity()).isEqualTo(600_000L);
        assertThat(active.reserveRemainingDays()).isEqualTo(30);
        assertThat(infrastructureService.purchaseReserveCapacity(player.getId(), 10))
                .contains("활성 예비용량");

        for (int day = 0; day < 30; day++) {
            player.advanceDay();
        }
        var expired = infrastructureService.snapshot(company);

        assertThat(expired.reserveCapacity()).isZero();
        assertThat(expired.totalCapacity()).isEqualTo(expired.permanentCapacity());
        assertThat(expired.reserveRemainingDays()).isZero();
    }

    private void completeActiveProductProject(com.game.buildingstory.domain.PlayerCompany company) {
        for (int month = 0;
             month < 24 && productProjectService.activeProject(company).isPresent();
             month++) {
            productProjectService.processMonth(company);
        }
        assertThat(productProjectService.activeProject(company)).isEmpty();
    }

    private Player launchedPlayer(String username) {
        Player player = new Player(username, "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        foundationTestSupport.prepare(player);
        companyService.establish(player.getId(), "시장테스트", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
        foundationTestSupport.clearPreparationStaff(player);
        tutorialService.confirmFoundingTeam(player.getId(),
                List.of("dev-01", "dev-02", "dev-03", "sales-01", "ops-01", "ops-02"));
        tutorialService.startCommercialization(player.getId());
        for (int month = 0; month < 4; month++) {
            tutorialService.processMonthly(player);
        }
        tutorialService.launch(player.getId());
        return playerRepository.findById(player.getId()).orElseThrow();
    }
}
