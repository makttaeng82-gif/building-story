package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
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
        "spring.datasource.url=jdbc:h2:mem:company-tutorial-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class CompanyTutorialServiceTests {
    @Autowired private PlayerCompanyService playerCompanyService;
    @Autowired private CompanyTutorialService companyTutorialService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PlayerCompanyRepository companyRepository;
    @Autowired private CompanyDepartmentRepository departmentRepository;
    @Autowired private CompanyCoreEmployeeRepository employeeRepository;
    @Autowired private MonthlyRecordRepository monthlyRecordRepository;
    @Autowired private OwnedSecretaryRepository ownedSecretaryRepository;
    @Autowired private CompanyFoundationTestSupport foundationTestSupport;
    @Autowired private CompanyCompetitorRepository competitorRepository;

    @BeforeEach
    void cleanDatabase() {
        monthlyRecordRepository.deleteAll();
        competitorRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        companyRepository.deleteAll();
        ownedSecretaryRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void rejectsTeamWithoutRequiredDepartmentMix() {
        Player player = establishedPlayer("tutorial-invalid");
        long cashBefore = companyRepository.findByPlayer(player).orElseThrow().getCorporateCash();

        String notice = companyTutorialService.confirmFoundingTeam(player.getId(),
                List.of("dev-01", "dev-02", "dev-03", "dev-04", "dev-05", "sales-01"));

        assertThat(notice).contains("서비스운영 1명 이상");
        assertThat(employeeRepository.findAll()).isEmpty();
        assertThat(companyRepository.findByPlayer(player).orElseThrow().getCorporateCash()).isEqualTo(cashBefore);
    }

    @Test
    void hiresFoundingTeamDevelopsForFourMonthsAndLaunchesProduct() {
        Player player = establishedPlayer("tutorial-complete");
        String hireNotice = companyTutorialService.confirmFoundingTeam(player.getId(),
                List.of("dev-01", "dev-02", "dev-03", "sales-01", "ops-01", "ops-02"));
        var company = companyRepository.findByPlayer(player).orElseThrow();

        assertThat(hireNotice).contains("채용 완료");
        assertThat(employeeRepository.findByCompanyOrderById(company)).hasSize(6);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT).orElseThrow().getGeneralEmployeeCount()).isEqualTo(8);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(company, CompanyDepartmentType.SALES_MARKETING).orElseThrow().getGeneralEmployeeCount()).isEqualTo(4);
        assertThat(departmentRepository.findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS).orElseThrow().getGeneralEmployeeCount()).isEqualTo(6);
        assertThat(company.getTutorialStage()).isEqualTo(CompanyTutorialStage.READY_TO_DEVELOP);
        assertThat(player.isPaused()).isTrue();

        assertThat(companyTutorialService.startCommercialization(player.getId())).contains("개발 시작");
        long cashBeforeDevelopment = company.getCorporateCash();
        long monthlyCost = companyTutorialService.monthlyCommercializationCost(company);
        for (int month = 0; month < 4; month++) {
            companyTutorialService.processMonthly(player);
        }

        assertThat(company.getTutorialStage()).isEqualTo(CompanyTutorialStage.LAUNCH_REVIEW);
        assertThat(company.getCorporateCash()).isEqualTo(cashBeforeDevelopment - monthlyCost * 4);
        assertThat(player.isPaused()).isTrue();
        assertThat(companyTutorialService.launch(player.getId())).contains("정식 출시 완료");
        assertThat(company.getTutorialStage()).isEqualTo(CompanyTutorialStage.LAUNCHED);
        assertThat(company.getPrototypeBenchmark()).isEqualTo(200);
        assertThat(company.getPaidUsers()).isEqualTo(240_000L);
        assertThat(company.getMonthlyRecurringRevenue()).isEqualTo(4_800_000_000L);
        assertThat(player.isPaused()).isFalse();
    }

    private Player establishedPlayer(String username) {
        Player player = new Player(username, "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        foundationTestSupport.prepare(player);
        playerCompanyService.establish(player.getId(), "테스트AI", "인공지능 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
        foundationTestSupport.clearPreparationStaff(player);
        return player;
    }
}
