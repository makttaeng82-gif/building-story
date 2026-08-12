package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:company-workforce-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class CompanyWorkforceServiceTests {
    @Autowired private PlayerCompanyService companyService;
    @Autowired private CompanyTutorialService tutorialService;
    @Autowired private CompanyWorkforceService workforceService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PlayerCompanyRepository companyRepository;
    @Autowired private CompanyDepartmentRepository departmentRepository;
    @Autowired private CompanyCoreEmployeeRepository employeeRepository;
    @Autowired private CompanyMonthlySettlementRepository monthlyRepository;
    @Autowired private CompanyQuarterlyReportRepository quarterlyRepository;
    @Autowired private MonthlyRecordRepository monthlyRecordRepository;
    @Autowired private OwnedSecretaryRepository ownedSecretaryRepository;
    @Autowired private CompanyCompetitorRepository competitorRepository;
    @Autowired private CompanyDepartmentService departmentService;
    @Autowired private CompanySecretaryService secretaryService;
    @Autowired private CompanyOrganizationService organizationService;
    @Autowired private CompanyFoundationTestSupport foundationTestSupport;

    @BeforeEach
    void cleanDatabase() {
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
    void calculatesDepartmentExpertiseCapacityPayrollAndMonthlyGrowth() {
        Player player = new Player("workforce-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        foundationTestSupport.prepare(player);
        companyService.establish(player.getId(), "조직테스트", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
        foundationTestSupport.clearPreparationStaff(player);
        tutorialService.confirmFoundingTeam(player.getId(),
                List.of("dev-01", "dev-02", "dev-03", "sales-01", "ops-01", "ops-02"));
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        long corePayroll = employeeRepository.findByCompanyOrderById(company).stream()
                .mapToLong(employee -> employee.getAnnualSalary() / 12)
                .sum();
        long expectedGeneralPayroll = 8 * 3_300_000L + 4 * 3_000_000L + 6 * 2_800_000L;

        assertThat(workforceService.departmentExpertise(company, CompanyDepartmentType.AI_DEVELOPMENT))
                .isBetween(1, 100);
        assertThat(workforceService.monthlyCapacity(company, development)).isPositive();
        long employeePayroll = corePayroll + expectedGeneralPayroll;
        assertThat(workforceService.monthlyPayroll(company))
                .isEqualTo(employeePayroll + employeePayroll * 20 / 100);

        var gradeTwoEmployee = employeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> employee.getCandidateKey().equals("dev-01"))
                .findFirst().orElseThrow();
        for (int month = 0; month < 16; month++) {
            workforceService.processNormalWorkMonth(company);
        }
        assertThat(gradeTwoEmployee.getGrade()).isEqualTo(3);
        assertThat(gradeTwoEmployee.getExpectedAnnualSalary()).isEqualTo(550_000_000L);
        assertThat(gradeTwoEmployee.getGrowthSpeed()).isEqualTo(78);
    }

    @Test
    void appliesApprovedHeadcountHiringDelayFeeAndFirstMonthAdaptation() {
        Player player = launchedPlayer("workforce-hiring-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        long openingCash = company.getCorporateCash();
        long feePerPerson = workforceService.hiringFeePerPerson(CompanyDepartmentType.AI_DEVELOPMENT);

        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, 12)).contains("12명");
        assertThat(workforceService.requestGeneralHires(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, 4)).contains("다음 월 정산 입사");
        assertThat(development.getGeneralEmployeeCount()).isEqualTo(8);
        assertThat(development.getPendingHireCount()).isEqualTo(4);
        assertThat(company.getCorporateCash()).isEqualTo(openingCash - feePerPerson * 4);

        var firstMonth = workforceService.processWorkforceMonth(company);
        int adaptingCapacity = workforceService.monthlyCapacity(company, development);
        assertThat(firstMonth.onboardedEmployees()).isEqualTo(4);
        assertThat(development.getGeneralEmployeeCount()).isEqualTo(12);
        assertThat(development.getAdaptingEmployeeCount()).isEqualTo(4);
        assertThat(development.getPendingHireCount()).isZero();

        workforceService.processWorkforceMonth(company);
        assertThat(development.getAdaptingEmployeeCount()).isZero();
        assertThat(workforceService.monthlyCapacity(company, development)).isGreaterThan(adaptingCapacity);
    }

    @Test
    void unusedApprovedHeadcountDoesNotBlockCoreTalentHiring() {
        Player player = launchedPlayer("core-hire-approved-headcount-test");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();

        development.changeApprovedHeadcount(64);
        // 전략재무팀 출범 시 추가되는 일반인력 5명을 포함해 최종 실제 인원을 66명으로 맞춘다.
        int hiresNeeded = 61 - workforceService.totalEmployees(company);
        development.requestHires(hiresNeeded);
        development.advanceHiringMonth();
        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 3, 0, 0, 0, 0, 0,
                company.getCorporateCash(), company.getMonthlyRecurringRevenue(), company.getPaidUsers()));
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.STRATEGY_FINANCE)).contains("출범 완료");

        assertThat(workforceService.totalEmployees(company)).isEqualTo(66);
        assertThat(workforceService.hireCoreTalent(player.getId(), "finance-01"))
                .contains("채용 승인");
    }

    @Test
    void enforcesExternalRecruiterOrganizationAndMonthlyLimits() {
        Player player = launchedPlayer("workforce-limit-test");

        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, 70)).contains("총 80명");
        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, 18)).contains("18명");
        assertThat(workforceService.requestGeneralHires(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, 10)).contains("10명 채용 승인");
        assertThat(workforceService.requestGeneralHires(
                player.getId(), CompanyDepartmentType.SALES_MARKETING, 1)).contains("월간 채용 가능 인원은 0명");
    }

    @Test
    void hiresCoreTalentAfterOneMonthWithoutPayingSalaryDuringWait() {
        Player player = launchedPlayer("core-hiring-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        long openingCash = company.getCorporateCash();
        long openingPayroll = workforceService.monthlyPayroll(company);
        var candidate = workforceService.availableCandidates(company, CompanyDepartmentType.AI_DEVELOPMENT).stream()
                .filter(item -> item.key().equals("dev-04"))
                .findFirst().orElseThrow();

        assertThat(workforceService.hireCoreTalent(player.getId(), candidate.key()))
                .contains("다음 월 정산 입사");
        var pendingEmployee = workforceService.allEmployees(company, CompanyDepartmentType.AI_DEVELOPMENT).stream()
                .filter(employee -> employee.getCandidateKey().equals(candidate.key()))
                .findFirst().orElseThrow();
        assertThat(pendingEmployee.isActive()).isFalse();
        assertThat(company.getCorporateCash()).isEqualTo(openingCash - candidate.signingBonus());
        assertThat(workforceService.monthlyPayroll(company)).isEqualTo(openingPayroll);

        var result = workforceService.processWorkforceMonth(company);
        assertThat(result.onboardedCoreEmployees()).isEqualTo(1);
        assertThat(pendingEmployee.isActive()).isTrue();
        long candidateMonthlyCost = candidate.annualSalary() / 12;
        assertThat(workforceService.monthlyPayroll(company))
                .isBetween(
                        openingPayroll + candidateMonthlyCost + candidateMonthlyCost * 20 / 100,
                        openingPayroll + candidateMonthlyCost + candidateMonthlyCost * 20 / 100 + 1
                );
    }

    @Test
    void professionalTrainingTemporarilyRemovesContributionAndGrantsExperience() {
        Player player = launchedPlayer("core-training-test");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        var employee = workforceService.employees(company, CompanyDepartmentType.AI_DEVELOPMENT).getFirst();
        double openingExperience = employee.getExperience();
        int openingContributors = workforceService.employees(
                company, CompanyDepartmentType.AI_DEVELOPMENT).size();

        assertThat(workforceService.startTraining(player.getId(), employee.getId()))
                .contains("전문 연수 시작");
        assertThat(employee.getTrainingMonthsRemaining()).isEqualTo(3);
        assertThat(workforceService.employees(company, CompanyDepartmentType.AI_DEVELOPMENT))
                .hasSize(openingContributors - 1);

        for (int month = 0; month < 3; month++) {
            workforceService.processNormalWorkMonth(company);
        }

        assertThat(employee.getTrainingMonthsRemaining()).isZero();
        assertThat(employee.getExperience()).isGreaterThan(openingExperience + 10);
        assertThat(workforceService.employees(company, CompanyDepartmentType.AI_DEVELOPMENT))
                .hasSize(openingContributors);
    }

    @Test
    void appointsOnlyQualifiedGradeFiveEmployeeAndAppliesLeaderBonuses() {
        Player player = launchedPlayer("team-leader-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        var employee = workforceService.employees(company, CompanyDepartmentType.AI_DEVELOPMENT).stream()
                .filter(item -> item.getCandidateKey().equals("dev-01"))
                .findFirst().orElseThrow();

        assertThat(workforceService.appointTeamLeader(player.getId(), employee.getId())).contains("5등급");
        for (int month = 0; month < 80; month++) {
            workforceService.processNormalWorkMonth(company);
        }
        int capacityBefore = workforceService.monthlyCapacity(company, development);
        long payrollBefore = workforceService.monthlyPayroll(company);

        assertThat(employee.getGrade()).isEqualTo(5);
        assertThat(workforceService.appointTeamLeader(player.getId(), employee.getId())).contains("팀장으로");
        assertThat(employee.isTeamLeader()).isTrue();
        assertThat(workforceService.monthlyCapacity(company, development)).isGreaterThan(capacityBefore);
        assertThat(workforceService.monthlyPayroll(company)).isGreaterThan(payrollBefore);
    }

    @Test
    void calculatesBaseWorkloadAndEnforcesDepartmentMajorWorkLimits() {
        Player player = launchedPlayer("department-load-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        var initialLoad = workforceService.departmentLoad(company, development);

        assertThat(initialLoad.baseWorkload()).isPositive();
        assertThat(initialLoad.utilizationPercent()).isBetween(70.0, 90.0);
        assertThat(initialLoad.status()).isEqualTo("여유");
        assertThat(initialLoad.slotLimit()).isEqualTo(1);
        assertThat(initialLoad.availableSlots()).isEqualTo(1);

        workforceService.reserveMajorWork(company, CompanyDepartmentType.AI_DEVELOPMENT, 10);
        var reservedLoad = workforceService.departmentLoad(company, development);
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(reservedLoad.usedSlots()).isEqualTo(1);
        assertThat(reservedLoad.allocatedMajorWorkload()).isEqualTo(10);
        assertThatThrownBy(() -> workforceService.reserveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("슬롯");

        workforceService.releaseMajorWork(company, CompanyDepartmentType.AI_DEVELOPMENT, 10);
        assertThat(company.getActiveMajorWorkCount()).isZero();
        assertThat(workforceService.departmentLoad(company, development).usedSlots()).isZero();
        assertThatThrownBy(() -> workforceService.reserveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, initialLoad.capacity()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("130%");
    }

    @Test
    void redistributesMajorWorkWhenReservedWorkExceedsCurrentDepartmentCapacity() {
        Player player = launchedPlayer("department-effective-work-test");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, development);
        int reservedWork = (int) Math.floor(load.capacity() * 1.30) - load.baseWorkload();

        workforceService.reserveMajorWork(company, CompanyDepartmentType.AI_DEVELOPMENT, reservedWork);

        assertThat(workforceService.effectiveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, reservedWork))
                .isPositive()
                .isLessThan(reservedWork);
    }

    @Test
    void departmentWithoutProcessingCapacityHasNoWorkSlotAndShowsUnavailable() {
        Player player = launchedPlayer("department-zero-capacity-test");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment emptyDepartment = departmentRepository.save(
                new CompanyDepartment(company, CompanyDepartmentType.STRATEGY_FINANCE));

        var load = workforceService.departmentLoad(company, emptyDepartment);

        assertThat(load.capacity()).isZero();
        assertThat(load.slotLimit()).isZero();
        assertThat(load.status()).isEqualTo("운영 불가");
        assertThat(load.tone()).isEqualTo("danger");
    }

    @Test
    void accumulatesGeneralAttritionWithoutCreatingIndividualEmployeeRecords() {
        Player player = launchedPlayer("general-attrition-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();

        for (int month = 0; month < 7; month++) {
            development.applyGeneralAttrition(0.02);
        }

        assertThat(development.getGeneralEmployeeCount()).isEqualTo(7);
        assertThat(development.getLastGeneralResignationRate()).isEqualTo(0.02);
    }

    @Test
    void retentionAgreementPaysBonusRaisesSalaryAndClearsNotice() {
        Player player = launchedPlayer("retention-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var employee = workforceService.employees(company, CompanyDepartmentType.AI_DEVELOPMENT).getFirst();
        long openingCash = company.getCorporateCash();
        long openingSalary = employee.getAnnualSalary();
        long expectedBonus = employee.retentionBonusCost();
        employee.updateResignationRisk(85);
        assertThat(employee.beginResignationNotice()).isTrue();

        workforceService.offerRetentionAgreement(player.getId(), employee.getId());

        assertThat(company.getCorporateCash()).isEqualTo(openingCash - expectedBonus);
        assertThat(employee.getAnnualSalary()).isEqualTo(openingSalary * 115 / 100);
        assertThat(employee.getResignationNoticeMonths()).isZero();
        assertThat(employee.hasActiveRetentionAgreement(player.getElapsedDays())).isTrue();
    }

    @Test
    void unlocksSupportDepartmentsOnlyAfterTheirDefinedConditions() {
        Player player = launchedPlayer("support-department-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(workforceService.organizationLimit(company)).isEqualTo(80);
        assertThat(departmentService.opportunities(company))
                .anyMatch(item -> item.type() == CompanyDepartmentType.HR_ORGANIZATION
                        && !item.available())
                .anyMatch(item -> item.type() == CompanyDepartmentType.STRATEGY_FINANCE
                        && !item.available());

        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        development.changeApprovedHeadcount(44);
        development.requestHires(36);
        development.advanceHiringMonth();

        assertThat(departmentService.totalEmployees(company)).isEqualTo(60);
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION)).contains("출범 완료");
        assertThat(workforceService.organizationLimit(company)).isEqualTo(100);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION))
                .get().extracting(department -> department.getGeneralEmployeeCount())
                .isEqualTo(5);
        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION, 15))
                .contains("승인 정원 15명");

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 3, 0, 0, 0, 0, 0,
                company.getCorporateCash(), company.getMonthlyRecurringRevenue(), company.getPaidUsers()));
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.STRATEGY_FINANCE)).contains("출범 완료");
    }

    @Test
    void suspendedCompanyCannotEstablishDepartmentOrStartOrganizationUpgrade() {
        Player player = launchedPlayer("suspended-company-controls");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        development.changeApprovedHeadcount(64);
        development.requestHires(56);
        development.advanceHiringMonth();
        company.suspendOperations(1);

        assertThat(departmentService.opportunities(company))
                .allMatch(opportunity -> !opportunity.available());
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION))
                .contains("운영중단");
        assertThat(organizationService.startNextUpgrade(player.getId()))
                .contains("운영중단");
    }

    @Test
    void hrAvailableCapacityLimitsMonthlyGeneralHiring() {
        Player player = launchedPlayer("hr-hiring-capacity");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        development.changeApprovedHeadcount(64);
        development.requestHires(56);
        development.advanceHiringMonth();
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION)).contains("출범 완료");

        workforceService.reserveMajorWork(company, CompanyDepartmentType.HR_ORGANIZATION, 35);

        assertThat(workforceService.remainingMonthlyHireLimit(company)).isEqualTo(5);
    }

    @Test
    void buildsOrganizationSystemAndAutomaticallyFillsApprovedHeadcount() {
        Player player = launchedPlayer("organization-automation-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        development.changeApprovedHeadcount(64);
        development.requestHires(56);
        development.advanceHiringMonth();
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION)).contains("출범 완료");

        for (String candidate : List.of("hr-01", "hr-02", "hr-03")) {
            assertThat(workforceService.hireCoreTalent(player.getId(), candidate)).contains("채용 승인");
        }
        workforceService.processWorkforceMonth(company);
        assertThat(workforceService.departmentExpertise(
                company, CompanyDepartmentType.HR_ORGANIZATION)).isGreaterThanOrEqualTo(40);

        assertThat(organizationService.startNextUpgrade(player.getId())).contains("구축 시작");
        var hrDepartment = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION).orElseThrow();
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(hrDepartment.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(company.getOrganizationUpgradeHrWorkRemaining()).isPositive();
        assertThat(company.getOrganizationUpgradeHrAllocation()).isPositive();
        organizationService.processSuccessfulMonth(company, 0);
        var completion = organizationService.processSuccessfulMonth(company, 0);
        assertThat(completion.upgraded()).isTrue();
        assertThat(workforceService.organizationLimit(company)).isEqualTo(500);
        assertThat(company.getActiveMajorWorkCount()).isZero();
        assertThat(hrDepartment.getActiveMajorWorkCount()).isZero();
        assertThat(hrDepartment.getAllocatedMajorWorkload()).isZero();

        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION, 20)).contains("20명");
        assertThat(organizationService.toggleAutomaticHiring(player.getId())).contains("사용");
        var hiring = organizationService.processSuccessfulMonth(company, 0);
        assertThat(hiring.automaticHires()).isEqualTo(15);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION).orElseThrow().getPendingHireCount())
                .isEqualTo(15);
    }

    @Test
    void enterpriseOrganizationUpgradeUsesHrAndStrategyWorkSlots() {
        Player player = launchedPlayer("enterprise-organization-work-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        development.changeApprovedHeadcount(64);
        development.requestHires(56);
        development.advanceHiringMonth();
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION)).contains("출범 완료");
        for (String candidate : List.of("hr-01", "hr-02", "hr-03")) {
            assertThat(workforceService.hireCoreTalent(player.getId(), candidate)).contains("채용 승인");
        }
        workforceService.processWorkforceMonth(company);

        assertThat(organizationService.startNextUpgrade(player.getId())).contains("구축 시작");
        for (int month = 0; month < 2; month++) {
            organizationService.processSuccessfulMonth(company, 0);
        }
        growToEmployeeCount(company, development, 400);
        for (int month = 0; month < 80; month++) {
            workforceService.processNormalWorkMonth(company);
        }
        assertThat(workforceService.departmentExpertise(
                company, CompanyDepartmentType.HR_ORGANIZATION)).isGreaterThanOrEqualTo(55);
        assertThat(organizationService.startNextUpgrade(player.getId())).contains("구축 시작");
        for (int month = 0; month < 3; month++) {
            organizationService.processSuccessfulMonth(company, 0);
        }
        company.promoteGrowthStage(CompanyGrowthStage.LARGE);
        assertThat(workforceService.hireCoreTalent(player.getId(), "hr-04")).contains("채용 승인");
        assertThat(workforceService.hireCoreTalent(player.getId(), "hr-05")).contains("채용 승인");
        workforceService.processWorkforceMonth(company);
        for (int month = 0; month < 100; month++) {
            workforceService.processNormalWorkMonth(company);
        }

        quarterlyRepository.save(new CompanyQuarterlyReport(
                company, 1, 3, 0, 0, 0, 0, 0,
                company.getCorporateCash(), company.getMonthlyRecurringRevenue(), company.getPaidUsers()));
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.STRATEGY_FINANCE)).contains("출범 완료");
        for (String candidate : List.of(
                "finance-01", "finance-02", "finance-03", "finance-04", "finance-05")) {
            assertThat(workforceService.hireCoreTalent(player.getId(), candidate)).contains("채용 승인");
        }
        workforceService.processWorkforceMonth(company);
        for (int month = 0; month < 100; month++) {
            workforceService.processNormalWorkMonth(company);
        }
        growToEmployeeCount(company, development, 1_600);
        assertThat(workforceService.departmentExpertise(
                company, CompanyDepartmentType.HR_ORGANIZATION)).isGreaterThanOrEqualTo(65);
        assertThat(workforceService.departmentExpertise(
                company, CompanyDepartmentType.STRATEGY_FINANCE)).isGreaterThanOrEqualTo(85);

        assertThat(organizationService.startNextUpgrade(player.getId())).contains("전사 조직관리 시스템 구축 시작");
        var hr = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.HR_ORGANIZATION).orElseThrow();
        var strategy = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.STRATEGY_FINANCE).orElseThrow();
        assertThat(company.getActiveMajorWorkCount()).isEqualTo(2);
        assertThat(hr.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(strategy.getActiveMajorWorkCount()).isEqualTo(1);
        assertThat(company.getOrganizationUpgradeStrategyWorkRemaining()).isPositive();
        assertThat(company.getOrganizationUpgradeStrategyAllocation()).isPositive();
    }

    @Test
    void unlocksLateCoreTalentWithoutChangingFoundingResumes() {
        Player player = launchedPlayer("late-core-talent-test");
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(tutorialService.candidates()).hasSize(20);
        assertThat(tutorialService.candidates())
                .noneMatch(candidate -> candidate.key().equals("dev-11")
                        || candidate.departmentType() == CompanyDepartmentType.HR_ORGANIZATION);
        assertThat(workforceService.availableCandidates(
                company, CompanyDepartmentType.AI_DEVELOPMENT))
                .noneMatch(candidate -> candidate.key().equals("dev-11"));
        assertThat(workforceService.hireCoreTalent(player.getId(), "dev-11"))
                .contains("성장기업 단계부터");

        company.promoteGrowthStage(CompanyGrowthStage.GROWTH);
        assertThat(workforceService.availableCandidates(
                company, CompanyDepartmentType.AI_DEVELOPMENT))
                .anyMatch(candidate -> candidate.key().equals("dev-11"))
                .noneMatch(candidate -> candidate.key().equals("dev-12"));

        company.promoteGrowthStage(CompanyGrowthStage.LARGE);
        assertThat(workforceService.availableCandidates(
                company, CompanyDepartmentType.AI_DEVELOPMENT))
                .anyMatch(candidate -> candidate.key().equals("dev-12"));
    }

    @Test
    void suspendedCompanyRejectsOrganizationMutations() {
        Player player = launchedPlayer("suspended-organization");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        var development = departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow();
        var employee = employeeRepository.findByCompanyOrderById(company).getFirst();
        int approvedBefore = development.getApprovedHeadcount();
        boolean automaticHiringBefore = company.isAutomaticHiringEnabled();
        company.suspendOperations(1);

        assertThat(workforceService.appointTeamLeader(player.getId(), employee.getId()))
                .contains("운영중단");
        assertThat(workforceService.changeApprovedHeadcount(
                player.getId(), CompanyDepartmentType.AI_DEVELOPMENT, approvedBefore + 1))
                .contains("운영중단");
        assertThat(organizationService.toggleAutomaticHiring(player.getId()))
                .contains("운영중단");
        assertThat(development.getApprovedHeadcount()).isEqualTo(approvedBefore);
        assertThat(company.isAutomaticHiringEnabled()).isEqualTo(automaticHiringBefore);
    }

    @Test
    void companySecretariesGainSeparateCareerAndApplyPassiveEffects() {
        Player player = launchedPlayer("company-secretary-effects");
        var company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment hr = new CompanyDepartment(company, CompanyDepartmentType.HR_ORGANIZATION);
        hr.assignFoundingWorkforce(5);
        departmentRepository.save(hr);
        CompanyDepartment finance = new CompanyDepartment(company, CompanyDepartmentType.STRATEGY_FINANCE);
        finance.assignFoundingWorkforce(5);
        departmentRepository.save(finance);
        var developmentSecretary = ownedSecretaryRepository.save(
                new OwnedSecretary(player, "secretary-2", 30));
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 30));
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-3", 30));
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-4", 30));
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-5", 30));
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-6", 30));

        assertThat(secretaryService.developmentWorkMultiplier(company)).isEqualTo(0.96);
        assertThat(secretaryService.marketingEffectMultiplier(company)).isEqualTo(1.05);
        assertThat(secretaryService.incidentProbabilityMultiplier(company)).isEqualTo(0.96);
        assertThat(secretaryService.employeeGrowthMultiplier(company))
                .isCloseTo(1.0815, offset(0.000_001));
        assertThat(secretaryService.forecastErrorMultiplier(company))
                .isCloseTo(0.90, offset(0.000_001));
        assertThat(secretaryService.chiefEfficiencyMultiplier(company)).isEqualTo(1.03);

        for (int month = 0; month < 6; month++) {
            secretaryService.processSuccessfulMonth(company);
        }

        assertThat(developmentSecretary.getCompanyCareerMonths()).isEqualTo(6);
        assertThat(developmentSecretary.getCompanyProficiencyLevel()).isEqualTo(2);
        assertThat(secretaryService.developmentWorkMultiplier(company)).isEqualTo(0.92);
    }

    @Test
    void hrSecretaryIncreasesActualCoreEmployeeMonthlyExperience() {
        Player player = launchedPlayer("company-hr-secretary-growth");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment hr = new CompanyDepartment(company, CompanyDepartmentType.HR_ORGANIZATION);
        hr.assignFoundingWorkforce(5);
        departmentRepository.save(hr);
        ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-4", 30));
        var employee = employeeRepository.findByCompanyOrderById(company).stream()
                .filter(candidate -> candidate.getGrade() < 5)
                .findFirst()
                .orElseThrow();
        double experienceBefore = employee.getExperience();
        double expectedGrowth = (0.75 + employee.getGrowthSpeed() / 200.0) * 1.05;

        workforceService.processNormalWorkMonth(company);

        assertThat(employee.getExperience() - experienceBefore)
                .isCloseTo(expectedGrowth, offset(0.000_001));
    }

    @Test
    void hrExpertiseImprovesGeneralSkillAndReducesGeneralAttrition() {
        Player player = launchedPlayer("hr-education-test");
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        growToEmployeeCount(company, development, 60);
        assertThat(departmentService.establish(
                player.getId(), CompanyDepartmentType.HR_ORGANIZATION)).contains("출범 완료");
        assertThat(workforceService.hireCoreTalent(player.getId(), "hr-01")).contains("채용 승인");
        assertThat(workforceService.hireCoreTalent(player.getId(), "hr-02")).contains("채용 승인");
        assertThat(workforceService.hireCoreTalent(player.getId(), "hr-03")).contains("채용 승인");
        workforceService.processWorkforceMonth(company);
        int hrExpertise = workforceService.departmentExpertise(
                company, CompanyDepartmentType.HR_ORGANIZATION);
        assertThat(hrExpertise).isGreaterThanOrEqualTo(40);
        for (int month = 0; month < 11; month++) {
            company.updateMarketResult(0, 0, 0, 0, 0, 0);
        }
        int openingSkill = development.getAverageGeneralSkill();

        workforceService.processWorkforceMonth(company);

        assertThat(development.getAverageGeneralSkill()).isEqualTo(openingSkill + 1);
        assertThat(development.getLastGeneralResignationRate()).isLessThan(0.003);
    }

    private Player launchedPlayer(String username) {
        Player player = new Player(username, "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        foundationTestSupport.prepare(player);
        companyService.establish(player.getId(), "조직테스트", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
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

    private void growToEmployeeCount(PlayerCompany company, CompanyDepartment department, int target) {
        int gap = target - workforceService.totalEmployees(company);
        if (gap <= 0) return;
        department.changeApprovedHeadcount(department.getApprovedHeadcount() + gap);
        department.requestHires(gap);
        department.advanceHiringMonth();
    }
}
