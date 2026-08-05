package com.game.buildingstory.service;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedPropertyManagerRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:property-manager-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PropertyManagementServiceTests {
    private static final String CHEONGJU = "\uCCAD\uC8FC";
    private static final String SEJONG = "\uC138\uC885";

    @Autowired
    private PropertyManagementService propertyManagementService;

    @Autowired
    private SecretaryOperationsService secretaryOperationsService;

    @Autowired
    private OwnedPropertyManagerRepository propertyManagerRepository;

    @Autowired
    private OwnedSecretaryRepository ownedSecretaryRepository;

    @Autowired
    private OwnedBuildingRepository ownedBuildingRepository;

    @Autowired
    private MonthlyRecordRepository monthlyRecordRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void cleanDatabase() {
        monthlyRecordRepository.deleteAll();
        ownedBuildingRepository.deleteAll();
        ownedSecretaryRepository.deleteAll();
        propertyManagerRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void hiresOnlyOnePropertyManagerPerCity() {
        Player player = playerRepository.save(new Player("manager-hire-test", "hash"));
        prepareHandoff(player);
        OwnedSecretary secretary = ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-1").orElseThrow();
        secretary.assignTo(CHEONGJU);

        assertThat(propertyManagementService.hire(player.getId(), CHEONGJU)).contains("채용 완료", "비서 배치 해제");
        assertThat(propertyManagementService.hire(player.getId(), CHEONGJU)).contains("이미 채용됨");
        assertThat(propertyManagerRepository.findByPlayerOrderById(player)).hasSize(1);
        assertThat(secretary.getAssignedCity()).isNull();
        assertThat(secretaryOperationsService.canAssignSecretaryToCity(player, secretary, CHEONGJU)).isFalse();
        assertThat(secretaryOperationsService.assignSecretary(player.getId(), secretary.getId(), CHEONGJU))
                .contains("관리직원이 배치된 도시");
    }

    @Test
    void salarySettlementSuspendsAndRecoversEveryManagerTogether() {
        Player player = new Player("manager-salary-test", "hash");
        player.addCash(5_000_000L);
        player = playerRepository.save(player);
        prepareHandoff(player);
        propertyManagementService.hire(player.getId(), CHEONGJU);
        propertyManagementService.hire(player.getId(), SEJONG);

        assertThat(propertyManagementService.processSalaries(player)).contains("미지급");
        assertThat(propertyManagerRepository.findByPlayerOrderById(player))
                .allSatisfy(manager -> {
                    assertThat(manager.isActive()).isFalse();
                    assertThat(manager.getUnpaidSalaryMonths()).isEqualTo(1);
                    assertThat(propertyManagementService.salaryDue(manager)).isEqualTo(10_000_000L);
                });

        player.addCash(15_000_000L);
        assertThat(propertyManagementService.processSalaries(player)).contains("지급");
        assertThat(player.getCash()).isZero();
        assertThat(propertyManagerRepository.findByPlayerOrderById(player))
                .allSatisfy(manager -> {
                    assertThat(manager.isActive()).isTrue();
                    assertThat(manager.getUnpaidSalaryMonths()).isZero();
                });
    }

    @Test
    void repairsOldestRequestFirstAndPreventsSecretaryFallback() {
        Player player = new Player("manager-repair-test", "hash");
        player.addCash(1_000_000L);
        player = playerRepository.save(player);
        prepareHandoff(player);
        propertyManagementService.hire(player.getId(), CHEONGJU);

        OwnedBuilding older = ownedBuildingRepository.save(new OwnedBuilding(
                player, CHEONGJU, 1, "테스트", "오래된 수리", 1_000_000_000L, 0L, 0L, 5));
        older.moveIn();
        older.requestRepair();
        older.advanceRepairNeglectMonth();
        OwnedBuilding newer = ownedBuildingRepository.save(new OwnedBuilding(
                player, CHEONGJU, 2, "테스트", "새 수리", 500_000_000L, 0L, 0L, 5));
        newer.moveIn();
        newer.requestRepair();

        OwnedSecretary secretary = ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-1").orElseThrow();
        secretary.assignTo(CHEONGJU);

        assertThat(propertyManagementService.processAutoRepairs(player)).contains("1건", "수리비 부족");
        assertThat(older.isRepairRequested()).isFalse();
        assertThat(newer.isRepairRequested()).isTrue();

        player.addCash(500_000L);
        assertThat(secretaryOperationsService.processAutoRepairs(
                player, propertyManagementService.managedCities(player))).isBlank();
        assertThat(newer.isRepairRequested()).isTrue();

        assertThat(propertyManagementService.processAutoRepairs(player)).contains("1건");
        assertThat(newer.isRepairRequested()).isFalse();
        assertThat(player.getReputation()).isEqualTo(1_000_003);
        assertThat(propertyManagementService.recentRepairCountsByCity(player))
                .containsEntry(CHEONGJU, 2L);
    }

    @Test
    void handoffRequiresSeoulUnlockAndAllSixSecretaries() {
        Player player = playerRepository.save(new Player("manager-condition-test", "hash"));

        assertThat(propertyManagementService.isFeatureVisible(player)).isFalse();
        assertThat(propertyManagementService.hire(player.getId(), CHEONGJU)).contains("서울 해금 후");

        player.setReputationForTest(1_000_000);
        player.resign();
        assertThat(propertyManagementService.isFeatureVisible(player)).isTrue();
        assertThat(propertyManagementService.hire(player.getId(), CHEONGJU)).contains("비서 6명 고용 후");

        prepareHandoff(player);
        assertThat(propertyManagementService.isHandoffReady(player)).isTrue();
    }

    private void prepareHandoff(Player player) {
        long cashBefore = player.getCash();
        player.setReputationForTest(1_000_000);
        player.resign();
        player.spendCash(player.getCash() - cashBefore);
        for (int index = 1; index <= 6; index++) {
            String key = "secretary-" + index;
            if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, key).isEmpty()) {
                ownedSecretaryRepository.save(new OwnedSecretary(player, key, 30));
            }
        }
    }
}
