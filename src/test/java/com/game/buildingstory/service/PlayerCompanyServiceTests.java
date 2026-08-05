package com.game.buildingstory.service;

import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:player-company-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PlayerCompanyServiceTests {
    @Autowired
    private PlayerCompanyService playerCompanyService;
    @Autowired
    private SecretaryOperationsService secretaryOperationsService;
    @Autowired
    private SecretaryCatalog secretaryCatalog;
    @Autowired
    private PlayerCompanyRepository playerCompanyRepository;
    @Autowired
    private OwnedSecretaryRepository ownedSecretaryRepository;
    @Autowired
    private MonthlyRecordRepository monthlyRecordRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private CompanyDepartmentRepository companyDepartmentRepository;
    @Autowired
    private CompanyCoreEmployeeRepository companyCoreEmployeeRepository;

    @BeforeEach
    void cleanDatabase() {
        monthlyRecordRepository.deleteAll();
        companyCoreEmployeeRepository.deleteAll();
        companyDepartmentRepository.deleteAll();
        playerCompanyRepository.deleteAll();
        ownedSecretaryRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void establishesCompanyAtomicallyWithoutDevelopmentOnlyReadinessConditions() {
        Player player = new Player("company-establish-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        OwnedSecretary secretary = new OwnedSecretary(player, "secretary-1", 1);
        secretary.assignTo("청주");
        ownedSecretaryRepository.save(secretary);

        String result = playerCompanyService.establish(
                player.getId(), "테스트에이아이", "테스트 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);

        assertThat(result).contains("설립 완료");
        assertThat(player.getCash()).isEqualTo(
                PlayerCompanyService.RECOMMENDED_INVESTMENT - PlayerCompanyService.MINIMUM_INVESTMENT);
        var company = playerCompanyRepository.findByPlayer(player).orElseThrow();
        assertThat(company.getCorporateCash()).isEqualTo(
                PlayerCompanyService.MINIMUM_INVESTMENT - PlayerCompanyService.SECRETARY_TRAINING_COST);
        assertThat(company.getIssuedShares()).isEqualTo(PlayerCompanyService.INITIAL_ISSUED_SHARES);
        assertThat(company.getPlayerShares()).isEqualTo(PlayerCompanyService.INITIAL_ISSUED_SHARES);
        assertThat(company.getPlayerOwnershipPercent()).isEqualTo(100.0);
        assertThat(secretary.getAssignedCity()).isNull();

        long personalCashBeforePayroll = player.getCash();
        long corporateCashBeforePayroll = company.getCorporateCash();
        long secretarySalary = secretaryCatalog.find("secretary-1").orElseThrow()
                .monthlySalaryForProficiency(secretary.getProficiency());
        secretaryOperationsService.processSalaries(player);
        assertThat(player.getCash()).isEqualTo(personalCashBeforePayroll);
        assertThat(company.getCorporateCash()).isEqualTo(corporateCashBeforePayroll - secretarySalary);
    }

    @Test
    void duplicateSubmissionAndInvalidInvestmentDoNotChargeCashAgain() {
        Player player = new Player("company-duplicate-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);

        assertThat(playerCompanyService.establish(
                player.getId(), "테스트클라우드", "테스트 서비스", PlayerCompanyService.MINIMUM_INVESTMENT - 1))
                .contains("최소 출자금");
        assertThat(player.getCash()).isEqualTo(PlayerCompanyService.RECOMMENDED_INVESTMENT);

        assertThat(playerCompanyService.establish(
                player.getId(), "테스트클라우드", "테스트 서비스", PlayerCompanyService.MINIMUM_INVESTMENT))
                .contains("설립 완료");
        long cashAfterEstablishment = player.getCash();

        assertThat(playerCompanyService.establish(
                player.getId(), "두번째기업", "두번째서비스", PlayerCompanyService.MINIMUM_INVESTMENT))
                .contains("이미 설립된 기업");
        assertThat(player.getCash()).isEqualTo(cashAfterEstablishment);
        assertThat(playerCompanyRepository.findAll()).hasSize(1);
    }

    @Test
    void pausedPlayerCanEstablishCompany() {
        Player player = new Player("paused-company-establish-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player.pause();
        player = playerRepository.save(player);

        String result = playerCompanyService.establish(
                player.getId(), "일시정지 기업", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);

        assertThat(result).contains("설립 완료");
        assertThat(playerCompanyRepository.findByPlayer(player)).isPresent();
    }
}
