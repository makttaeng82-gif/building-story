package com.game.buildingstory.service;

import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedPropertyManagerRepository;
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
    @Autowired
    private CompanyFoundationTestSupport foundationTestSupport;
    @Autowired
    private CompanyAccessService companyAccessService;
    @Autowired
    private EventFlowService eventFlowService;
    @Autowired
    private OwnedBuildingRepository ownedBuildingRepository;
    @Autowired
    private OwnedPropertyManagerRepository propertyManagerRepository;

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
        foundationTestSupport.prepare(player);
        OwnedSecretary secretary = ownedSecretaryRepository
                .findByPlayerAndSecretaryKey(player, "secretary-1")
                .orElseThrow();
        secretary.assignTo("청주");
        ownedSecretaryRepository.save(secretary);

        String result = playerCompanyService.establish(
                player.getId(), "테스트에이아이", "테스트 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);
        foundationTestSupport.clearPreparationStaff(player, "secretary-1");

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
        long secretarySalary = ownedSecretaryRepository.findByPlayerOrderById(player).stream()
                .mapToLong(owned -> secretaryCatalog.find(owned.getSecretaryKey()).orElseThrow()
                        .monthlySalaryForProficiency(owned.getProficiency()))
                .sum();
        secretaryOperationsService.processSalaries(player);
        assertThat(player.getCash()).isEqualTo(personalCashBeforePayroll);
        assertThat(company.getCorporateCash()).isEqualTo(corporateCashBeforePayroll - secretarySalary);
    }

    @Test
    void duplicateSubmissionAndInvalidInvestmentDoNotChargeCashAgain() {
        Player player = new Player("company-duplicate-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player = playerRepository.save(player);
        foundationTestSupport.prepare(player);

        assertThat(playerCompanyService.establish(
                player.getId(), "테스트클라우드", "테스트 서비스", PlayerCompanyService.MINIMUM_INVESTMENT - 1))
                .contains("최소 출자금");
        assertThat(player.getCash()).isEqualTo(PlayerCompanyService.RECOMMENDED_INVESTMENT);

        assertThat(playerCompanyService.establish(
                player.getId(), "테스트클라우드", "테스트 서비스", PlayerCompanyService.MINIMUM_INVESTMENT))
                .contains("설립 완료");
        foundationTestSupport.clearPreparationStaff(player);
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
        foundationTestSupport.prepare(player);

        String result = playerCompanyService.establish(
                player.getId(), "일시정지 기업", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT);

        assertThat(result).contains("설립 완료");
        assertThat(playerCompanyRepository.findByPlayer(player)).isPresent();
    }

    @Test
    void unlocksCompanyPreparationOnlyAfterFinalSeoulBuildingProposalIsAccepted() {
        Player player = playerRepository.save(new Player("company-unlock-test", "hash"));
        ownedBuildingRepository.save(new OwnedBuilding(
                player, "서울", 4, "대형 오피스복합", "여의도 대형 오피스복합",
                220_000_000_000L, 220_000_000_000L, 1_833_333_300L, 185));

        companyAccessService.ensureUnlockSchedule(player);
        assertThat(companyAccessService.isUnlocked(player)).isFalse();
        for (int day = 0; day < CompanyAccessService.PROPOSAL_DELAY_DAYS; day++) {
            player.advanceDay();
        }

        assertThat(companyAccessService.activateProposalIfDue(player)).isTrue();
        var event = eventFlowService.activeEvent(player).orElseThrow();
        eventFlowService.completeEvent(player.getId(), event.getId());

        assertThat(companyAccessService.isUnlocked(player)).isTrue();
    }

    @Test
    void rejectsFoundationWhenSecretaryOrActiveManagerRequirementIsMissing() {
        Player player = new Player("company-readiness-test", "hash");
        player.addCash(PlayerCompanyService.RECOMMENDED_INVESTMENT);
        player.unlockCompanyContent();
        player = playerRepository.save(player);

        assertThat(playerCompanyService.establish(
                player.getId(), "준비부족기업", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT))
                .contains("비서 6명");

        foundationTestSupport.prepare(player);
        propertyManagerRepository.findByPlayerAndCity(player, "서울").orElseThrow().recordUnpaidSalary();

        assertThat(playerCompanyService.establish(
                player.getId(), "관리중단기업", "AI 플랫폼", PlayerCompanyService.MINIMUM_INVESTMENT))
                .contains("정상 근무");
        assertThat(playerCompanyRepository.findByPlayer(player)).isEmpty();
    }
}
