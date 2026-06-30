package com.game.buildingstory;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import com.game.buildingstory.domain.ValuationStatus;
import com.game.buildingstory.repo.AuctionEventRepository;
import com.game.buildingstory.repo.BuildingOfferRepository;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedGiftItemRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.QaService;
import com.game.buildingstory.service.SecretaryCatalog;
import com.game.buildingstory.service.SecretaryOperationsService;
import com.game.buildingstory.repo.OwnedLuxuryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:building-story-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class BuildingStoryApplicationTests {

	@Autowired
	private GameService gameService;

	@Autowired
	private QaService qaService;

	@Autowired
	private SecretaryOperationsService secretaryOperationsService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private OwnedSecretaryRepository ownedSecretaryRepository;

	@Autowired
	private OwnedBuildingRepository ownedBuildingRepository;

	@Autowired
	private MonthlyRecordRepository monthlyRecordRepository;

	@Autowired
	private OwnedLuxuryItemRepository ownedLuxuryItemRepository;

	@Autowired
	private OwnedGiftItemRepository ownedGiftItemRepository;

	@Autowired
	private AuctionEventRepository auctionEventRepository;

	@Autowired
	private GameEventRepository gameEventRepository;

	@Autowired
	private BuildingOfferRepository buildingOfferRepository;

	@Autowired
	private SecretaryTenantEventRepository secretaryTenantEventRepository;

	@Autowired
	private SecretaryCatalog secretaryCatalog;

	@BeforeEach
	void cleanDatabase() {
		monthlyRecordRepository.deleteAll();
		auctionEventRepository.deleteAll();
		gameEventRepository.deleteAll();
		buildingOfferRepository.deleteAll();
		secretaryTenantEventRepository.deleteAll();
		ownedGiftItemRepository.deleteAll();
		ownedLuxuryItemRepository.deleteAll();
		ownedBuildingRepository.deleteAll();
		ownedSecretaryRepository.deleteAll();
		playerRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void cannotAssignTwoSecretariesToSameCity() {
		Player player = playerRepository.save(new Player("assignment-test", "hash"));
		OwnedSecretary first = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		OwnedSecretary second = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 5));

		assertThat(gameService.assignSecretary(player.getId(), first.getId(), "청주")).isEqualTo("비서 배치 완료");
		assertThat(gameService.assignSecretary(player.getId(), second.getId(), "청주")).isEqualTo("이미 다른 비서가 배치된 도시");
		assertThat(ownedSecretaryRepository.findById(second.getId()).orElseThrow().getAssignedCity()).isNull();
	}

	@Test
	@Transactional
	void updatesSecretaryProficiencyForTestBySecretary() {
		Player player = playerRepository.save(new Player("proficiency-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 5));

		assertThat(qaService.updateTestSecretaryProficiency(player.getId(), "secretary-2", 17)).isEqualTo("설하은 숙련도 변경 완료");
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getProficiency()).isEqualTo(17);

		qaService.updateTestSecretaryProficiency(player.getId(), "secretary-2", 99);
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getProficiency()).isEqualTo(30);
	}

	@Test
	void qaCashChangePersistsWithoutCallerTransaction() {
		Player player = playerRepository.save(new Player("qa-cash-test", "hash"));

		assertThat(qaService.addTestCash(player.getId())).isEqualTo("테스트 현금 30,000,000원 지급");

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(30_000_000L);
	}

	@Test
	@Transactional
	void secretaryAutoRepairAddsProficiencyExperience() {
		Player player = playerRepository.save(new Player("auto-repair-test", "hash"));
		player.addCash(1_000_000L);
		player.updateTestChances(0, 0, 0);
		OwnedBuilding building = new OwnedBuilding(player, "청주", "원룸", "테스트 원룸", 30_000_000L, 0L, 200_000L, 4);
		building.moveIn();
		building.requestRepair();
		ownedBuildingRepository.save(building);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.assignTo("청주");

		gameService.tick(player.getId());

		OwnedBuilding repairedBuilding = ownedBuildingRepository.findById(building.getId()).orElseThrow();
		OwnedSecretary experiencedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(repairedBuilding.isRepairRequested()).isFalse();
		assertThat(experiencedSecretary.getProficiencyExperience()).isBetween(1, 2);
		assertThat(experiencedSecretary.getRequiredProficiencyExperience()).isEqualTo(3);
		assertThat(experiencedSecretary.canAutoRepair(player.getElapsedDays())).isFalse();
		assertThat(monthlyRecordRepository.findAll())
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서수리");
					assertThat(record.getMemo()).contains("숙련도 경험치 +");
				});
	}

	@Test
	@Transactional
	void lowProficiencySecretaryAutoRepairsOnlyFirstTwoManagedBuildings() {
		Player player = playerRepository.save(new Player("managed-repair-limit-test", "hash"));
		player.addCash(1_000_000L);
		for (int i = 1; i <= 3; i++) {
			OwnedBuilding building = new OwnedBuilding(player, "청주", i, "원룸", "테스트 원룸 " + i, 30_000_000L, 0L, 200_000L, 4);
			building.moveIn();
			if (i == 3) {
				building.requestRepair();
			}
			ownedBuildingRepository.save(building);
		}
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.assignTo("청주");

		assertThat(secretaryOperationsService.processAutoRepairs(player)).isBlank();

		var buildings = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주");
		assertThat(buildings.get(0).isRepairRequested()).isFalse();
		assertThat(buildings.get(1).isRepairRequested()).isFalse();
		assertThat(buildings.get(2).isRepairRequested()).isTrue();
	}

	@Test
	@Transactional
	void proficiencyElevenSecretaryCanAutoRepairTwoBuildingsPerCooldown() {
		Player player = playerRepository.save(new Player("double-auto-repair-test", "hash"));
		player.addCash(1_000_000L);
		for (int i = 1; i <= 3; i++) {
			OwnedBuilding building = new OwnedBuilding(player, "청주", i, "원룸", "테스트 원룸 " + i, 30_000_000L, 0L, 200_000L, 4);
			building.moveIn();
			building.requestRepair();
			ownedBuildingRepository.save(building);
		}
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 11));
		secretary.assignTo("청주");

		assertThat(secretaryOperationsService.processAutoRepairs(player)).contains("비서수리 1건");

		var buildings = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주");
		assertThat(buildings.get(0).isRepairRequested()).isFalse();
		assertThat(buildings.get(1).isRepairRequested()).isFalse();
		assertThat(buildings.get(2).isRepairRequested()).isTrue();
	}

	@Test
	@Transactional
	void secretaryOneAutoRepairCostUsesAffinityDiscount() {
		Player player = playerRepository.save(new Player("auto-repair-discount-test", "hash"));
		player.addCash(1_000_000L);
		OwnedBuilding building = new OwnedBuilding(player, "청주", 1, "원룸", "테스트 원룸", 30_000_000L, 0L, 200_000L, 4);
		building.moveIn();
		building.requestRepair();
		ownedBuildingRepository.save(building);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.addAffinityExperience(63);
		assertThat(secretary.getAffinity()).isEqualTo(10);
		secretary.assignTo("청주");

		secretaryOperationsService.processAutoRepairs(player);

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(973_000L);
		assertThat(monthlyRecordRepository.findAll())
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서수리");
					assertThat(record.getAmount()).isEqualTo(-27_000L);
				});
	}

	@Test
	@Transactional
	void secretaryAbilitySummariesShowCurrentUnlockedAbilities() {
		Player player = playerRepository.save(new Player("secretary-ability-summary-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 16));
		secretary.addAffinityExperience(168);
		assertThat(secretary.getAffinity()).isEqualTo(17);
		secretary.assignTo("청주");

		assertThat(gameService.activeSecretaryAbilitySummaries(secretary))
				.contains(
						"자동수리 수리비 감소 17%",
						"관리 가능 건물 8채",
						"쿨타임 내 자동수리 최대 2건",
						"자동수리 시 평판 +1 추가 증가",
						"매월 1일 배치 평판 +1~+3"
				);
	}

	@Test
	void secretarySalaryIncreasesByProficiencyTier() {
		var secretary = secretaryCatalog.find("secretary-1").orElseThrow();

		assertThat(secretary.monthlySalaryForProficiency(1)).isEqualTo(2_000_000L);
		assertThat(secretary.monthlySalaryForProficiency(2)).isEqualTo(2_160_000L);
		assertThat(secretary.monthlySalaryForProficiency(11)).isEqualTo(4_469_000L);
		assertThat(secretaryCatalog.find("secretary-6").orElseThrow().monthlySalaryForProficiency(1)).isEqualTo(2_000_000L);
		assertThat(secretaryCatalog.find("secretary-6").orElseThrow().monthlySalaryForProficiency(25)).isEqualTo(36_729_000L);
	}

	@Test
	void secretarySpecialEffectAndDefaultMoveOutChanceAreUpdated() {
		Player player = new Player("default-chance-test", "hash");

		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		assertThat(player.getMoveOutChancePercent()).isEqualTo(25);
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(35);
		player.updateTestChances(40, 20, 30);
		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		assertThat(player.getMoveOutChancePercent()).isEqualTo(25);
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(35);
		assertThat(gameService.baseMoveInChancePercent(player)).isEqualTo(35);
		assertThat(gameService.baseMoveOutChancePercent(player)).isEqualTo(25);
		assertThat(gameService.baseRepairRequestChancePercent(player)).isEqualTo(35);
		assertThat(secretaryCatalog.find("secretary-2").orElseThrow().specialEffectSummary()).isEqualTo("퇴거확률 감소 0.3%");
	}

	@Test
	void randomBuildingEventsAllowTwoRepairDaysPerMonth() {
		Player player = new Player("repair-schedule-test", "hash");

		player.scheduleMonthlyRandomEvents(2, 3, 4, 5, 6, 7);

		assertThat(player.hasEventScheduleForCurrentMonth()).isTrue();
		assertThat(player.isRepairEventDay()).isFalse();
		for (int i = 0; i < 5; i++) {
			player.advanceDay();
		}
		assertThat(player.getDay()).isEqualTo(6);
		assertThat(player.isRepairEventDay()).isTrue();
		player.advanceDay();
		assertThat(player.getDay()).isEqualTo(7);
		assertThat(player.isRepairEventDay()).isTrue();
	}

	@Test
	@Transactional
	void donationAddsOneReputationPerFiftyThousandWon() {
		Player player = playerRepository.save(new Player("donation-test", "hash"));
		player.addCash(5_000_000L);

		assertThat(gameService.donate(player.getId(), 100)).isEqualTo("기부 완료 · 평판 +100");
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isZero();
		assertThat(updatedPlayer.getReputation()).isEqualTo(100);
	}

	@Test
	@Transactional
	void luxuryItemCanBeBoughtOnceAndHasDonationEfficiencyBonus() {
		Player player = playerRepository.save(new Player("luxury-test", "hash"));
		player.addCash(1_000_000L);

		assertThat(gameService.buyLuxuryItem(player.getId(), "bicycle")).isEqualTo("자전거 구매 완료 · 평판 +9");
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isEqualTo(700_000L);
		assertThat(updatedPlayer.getReputation()).isEqualTo(9);
		assertThat(gameService.buyLuxuryItem(player.getId(), "bicycle")).isEqualTo("이미 구매한 아이템");
	}

	@Test
	@Transactional
	void giftItemCanBeBoughtInQuantityAndGivenByAffinityRange() {
		Player player = playerRepository.save(new Player("gift-test", "hash"));
		player.addCash(1_000_000L);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));

		assertThat(gameService.buyGiftItem(player.getId(), "coffee-beans", 2)).isEqualTo("고급 원두세트 2개 구매 완료");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(2);

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 2))
				.isEqualTo("설아름에게 고급 원두세트 2개 선물 완료");
		OwnedSecretary updatedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(updatedSecretary.getAffinity()).isEqualTo(1);
		assertThat(updatedSecretary.getAffinityExperience()).isEqualTo(2);
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isZero();
	}

	@Test
	@Transactional
	void giftRequiresMatchingAffinityRange() {
		Player player = playerRepository.save(new Player("gift-range-test", "hash"));
		player.addCash(2_000_000L);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));

		gameService.buyGiftItem(player.getId(), "fountain-pen", 1);

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "fountain-pen", 1))
				.isEqualTo("호감도 구간에 맞지 않는 선물");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "fountain-pen").orElseThrow().getQuantity()).isEqualTo(1);
	}

	@Test
	void giftBalancePricesAreUpdated() {
		assertThat(gameService.giftItems().stream()
				.filter(gift -> "jewelry".equals(gift.key()))
				.findFirst()
				.orElseThrow()
				.price()).isEqualTo(80_000_000L);
		assertThat(gameService.giftItems().stream()
				.filter(gift -> "incentive".equals(gift.key()))
				.findFirst()
				.orElseThrow()
				.price()).isEqualTo(500_000_000L);
	}

	@Test
	@Transactional
	void giftQuantityCannotExceedOwnedQuantity() {
		Player player = playerRepository.save(new Player("gift-owned-quantity-test", "hash"));
		player.addCash(1_000_000L);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));

		gameService.buyGiftItem(player.getId(), "coffee-beans", 1);

		assertThat(gameService.maxGiftQuantityForSecretary(player, secretary, gameService.giftItems().getFirst())).isEqualTo(1);
		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 2))
				.isEqualTo("선물 수량 부족");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(1);
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getAffinityExperience()).isZero();
	}

	@Test
	@Transactional
	void giftQuantityCannotContinuePastGiftAffinityRange() {
		Player player = playerRepository.save(new Player("gift-affinity-boundary-test", "hash"));
		player.addCash(5_000_000L);
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.addAffinityExperience(63);
		assertThat(secretary.getAffinity()).isEqualTo(10);

		gameService.buyGiftItem(player.getId(), "coffee-beans", 20);
		var coffeeBeans = gameService.giftItems().stream()
				.filter(gift -> "coffee-beans".equals(gift.key()))
				.findFirst()
				.orElseThrow();

		assertThat(gameService.maxGiftQuantityForSecretary(player, secretary, coffeeBeans)).isEqualTo(12);
		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 13))
				.isEqualTo("현재 호감도 구간에서 선물 가능한 수량 초과");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(20);

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 12))
				.isEqualTo("설아름에게 고급 원두세트 12개 선물 완료");
		OwnedSecretary updatedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(updatedSecretary.getAffinity()).isEqualTo(11);
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(8);
	}

	@Test
	@Transactional
	void loanPurchaseUsesSixtyPercentLoanAndFortyPercentCash() {
		Player player = playerRepository.save(new Player("loan-ratio-test", "hash"));
		player.addCash(20_000_000L);
		player.setReputationForTest(1_000);
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
				player,
				"청주",
				1,
				"원룸",
				"대출 테스트 원룸",
				30_000_000L,
				200_000L,
				4,
				ValuationStatus.FAIR
		));

		assertThat(offer.loanAmount()).isEqualTo(18_000_000L);
		assertThat(offer.cashForLoanPurchase()).isEqualTo(12_000_000L);
		assertThat(gameService.buyOffer(player.getId(), offer.getId(), true)).isEqualTo("대출구매 완료");
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(8_000_000L);
	}

	@Test
	@Transactional
	void loanPurchaseRejectsLoanLimitOverflow() {
		Player player = playerRepository.save(new Player("loan-limit-test", "hash"));
		player.addCash(20_000_000L);
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
				player,
				"청주",
				1,
				"원룸",
				"한도 테스트 원룸",
				30_000_000L,
				200_000L,
				4,
				ValuationStatus.FAIR
		));

		assertThat(gameService.buyOffer(player.getId(), offer.getId(), true)).isEqualTo("대출 한도 초과");
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(20_000_000L);
	}

	@Test
	@Transactional
	void buyingOfferDoesNotRefreshMarketBeforeRefreshDay() {
		Player player = playerRepository.save(new Player("offer-refresh-test", "hash"));
		player.addCash(100_000_000L);
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
				player,
				"청주",
				1,
				"원룸",
				"갱신 테스트 원룸",
				30_000_000L,
				200_000L,
				4,
				ValuationStatus.FAIR
		));

		assertThat(gameService.buyOffer(player.getId(), offer.getId(), false)).isEqualTo("현금구매 완료");
		gameService.ensureOffers(player);

		BuildingOffer remainingOffer = buildingOfferRepository.findById(offer.getId()).orElseThrow();
		assertThat(remainingOffer.getName()).isEqualTo("갱신 테스트 원룸");
		assertThat(gameService.purchaseCooldownDaysLeft(player, remainingOffer)).isPositive();
		assertThat(buildingOfferRepository.findByPlayerAndCityOrderById(player, "청주")).hasSize(1);
	}

	@Test
	void buildingCatalogRentAndCooldownBalanceAreUpdated() {
		var cheongjuRoom = gameService.buildingSpecs().stream()
				.filter(spec -> spec.city().equals("청주") && spec.slot() == 1)
				.findFirst()
				.orElseThrow();
		var seoulFinal = gameService.buildingSpecs().stream()
				.filter(spec -> spec.city().equals("서울") && spec.slot() == 4)
				.findFirst()
				.orElseThrow();

		assertThat(cheongjuRoom.monthlyRent()).isEqualTo(300_000L);
		assertThat(cheongjuRoom.tradeCooldownDays()).isEqualTo(5);
		assertThat(seoulFinal.monthlyRent()).isEqualTo(27_000_000_000L);
		assertThat(seoulFinal.tradeCooldownDays()).isEqualTo(264);
	}

	@Test
	@Transactional
	void secretaryRentBonusAppliesOnlyToAssignedCity() {
		Player player = playerRepository.save(new Player("secretary-rent-bonus-test", "hash"));
		OwnedBuilding assignedCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
				player, "test-city", "office", "assigned city office", 100_000_000L, 0L, 1_000_000L, 10));
		OwnedBuilding otherCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
				player, "other-city", "office", "other city office", 100_000_000L, 0L, 1_000_000L, 10));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-4", 15));
		secretary.assignTo("test-city");

		assertThat(gameService.effectiveMonthlyRent(player, assignedCityBuilding)).isEqualTo(1_005_000L);
		assertThat(gameService.effectiveMonthlyRent(player, otherCityBuilding)).isEqualTo(1_000_000L);
	}

	@Test
	@Transactional
	void cityPanelChanceTextUsesSecretaryEffectForAssignedCityOnly() {
		Player player = playerRepository.save(new Player("secretary-chance-text-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-3", 20));
		secretary.assignTo("test-city");

		assertThat(gameService.effectiveMoveInChancePercentText(player, "test-city")).isEqualTo("35.5%");
		assertThat(gameService.effectiveMoveInChancePercentText(player, "other-city")).isEqualTo("35%");
		assertThat(gameService.effectiveMoveOutChancePercentText(player, "test-city")).isEqualTo("25%");
		assertThat(gameService.effectiveRepairRequestChancePercentText(player, "test-city")).isEqualTo("35%");
	}

	@Test
	@Transactional
	void assignedSecretaryAddsMonthlyReputationRecordOnFirstDay() {
		Player player = playerRepository.save(new Player("secretary-monthly-reputation-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.assignTo("청주");

		secretaryOperationsService.processMonthlyReputation(player);

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getReputation()).isBetween(1, 3);
		assertThat(monthlyRecordRepository.findAll())
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서 관리");
					assertThat(record.getReputationChange()).isBetween(1, 3);
				});
	}

	@Test
	@Transactional
	void secretaryBuildingWaitReductionAppliesToPurchaseAndSaleCooldownCity() {
		Player player = playerRepository.save(new Player("secretary-wait-reduction-test", "hash"));
		OwnedBuilding assignedCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
				player, "test-city", "office", "assigned city office", 100_000_000L, 0L, 1_000_000L, 100));
		OwnedBuilding otherCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
				player, "other-city", "office", "other city office", 100_000_000L, 0L, 1_000_000L, 100));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-6", 25));
		secretary.assignTo("test-city");

		assertThat(gameService.daysUntilSellable(player, assignedCityBuilding)).isEqualTo(99);
		assertThat(gameService.daysUntilSellable(player, otherCityBuilding)).isEqualTo(100);
	}

	@Test
	@Transactional
	void cityPanelCanDisplayRentBonusAndBuildingWaitReductionText() {
		Player player = playerRepository.save(new Player("secretary-city-effect-text-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-5", 20));
		secretary.assignTo("test-city");

		assertThat(gameService.rentBonusPercent(player, "test-city")).isEqualTo(0.25);
		assertThat(gameService.rentBonusPercentText(player, "test-city")).isEqualTo("0.25%");
		assertThat(gameService.buildingWaitReductionPercent(player, "test-city")).isEqualTo(0.5);
		assertThat(gameService.buildingWaitReductionPercentText(player, "test-city")).isEqualTo("0.5%");
		assertThat(gameService.rentBonusPercent(player, "other-city")).isZero();
		assertThat(gameService.buildingWaitReductionPercent(player, "other-city")).isZero();
	}

	@Test
	@Transactional
	void buildingImagePathUsesCityAndSlot() {
		Player player = playerRepository.save(new Player("building-image-test", "hash"));
		BuildingOffer offer = new BuildingOffer(player, "\uC11C\uC6B8", 4, "landmark", "seoul landmark", 100_000_000L, 1_000_000L, 10, ValuationStatus.FAIR);
		OwnedBuilding building = new OwnedBuilding(player, "\uC778\uCC9C", 2, "tower", "incheon tower", 100_000_000L, 0L, 1_000_000L, 10);

		assertThat(gameService.buildingImagePath(offer)).isEqualTo("/assets/buildings/seoul-4.jpg");
		assertThat(gameService.buildingImagePath(building)).isEqualTo("/assets/buildings/incheon-2.jpg");
	}

	@Test
	@Transactional
	void secretaryTenantRentWaiverBlocksSaleAndShowsEventStatus() {
		Player player = playerRepository.save(new Player("secretary-rent-waiver-test", "hash"));
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
				player, "\uCCAD\uC8FC", 1, "room", "cheongju room", 30_000_000L, 0L, 200_000L, 4));
		building.moveInSecretaryTenant("secretary-1");
		SecretaryTenantEvent event = secretaryTenantEventRepository.save(new SecretaryTenantEvent(
				player, building, "secretary-1", "\uCCAD\uC8FC", player.getElapsedDays()));
		event.acceptRequest(player.getElapsedDays(), 60);

		assertThat(gameService.rentWaivedBySecretaryEvent(player, building)).isTrue();
		assertThat(gameService.secretaryTenantStatusText(building)).contains("\uC6D4\uC138 \uAC10\uBA74");
		assertThat(gameService.sellAvailabilityText(player, building)).isEqualTo("\uBE44\uC11C \uAC70\uC8FC\uC911");
		assertThat(gameService.canSell(player, building)).isFalse();
		assertThat(gameService.totalMonthlyRent(player)).isZero();
	}

	@Test
	@Transactional
	void missingDaejeonSecretaryIntroActivatesFromOwnedBuilding() {
		Player player = playerRepository.save(new Player("missing-daejeon-intro-test", "hash"));
		ownedBuildingRepository.save(new OwnedBuilding(
				player, "\uB300\uC804", 2, "\uC0C1\uAC00\uC8FC\uD0DD", "\uBD09\uBA85\uB3D9 \uC0C1\uAC00\uC8FC\uD0DD", 1_350_000_000L, 0L, 4_900_000L, 41));

		gameService.evaluateSecretaryTenantEvents(player);

		assertThat(gameEventRepository.findLatestByPlayerIdAndStatus(player.getId(), com.game.buildingstory.domain.GameEventStatus.ACTIVE, org.springframework.data.domain.PageRequest.of(0, 1)))
				.singleElement()
				.extracting(GameEvent::getEventKey)
				.isEqualTo("secretary_intro_secretary-3");
	}

	@Test
	@Transactional
	void auctionDisplayUsesCatalogNameBySlot() {
		Player player = playerRepository.save(new Player("auction-display-test", "hash"));
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
				player,
				"\uC778\uCC9C",
				2,
				"old-type",
				"old-name",
				75_000_000_000L,
				190_000_000L,
				101
		));

		assertThat(gameService.auctionDisplayTypeName(auction)).isEqualTo("\uC13C\uD2B8\uB7F4\uD30C\uD06C");
		assertThat(gameService.auctionDisplayName(auction)).isEqualTo("\uC1A1\uB3C4 \uC13C\uD2B8\uB7F4\uD30C\uD06C");
	}

	@Test
	@Transactional
	void secretaryRequestEventPaysCostAndUnlocksHire() {
		Player player = playerRepository.save(new Player("secretary-request-test", "hash"));
		player.addCash(500_000_000L);
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
				player, "\uC138\uC885", 2, "apt", "sejong apt", 300_000_000L, 0L, 1_600_000L, 21));
		building.moveInSecretaryTenant("secretary-2");
		secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, "secretary-2", "\uC138\uC885", player.getElapsedDays()));
		GameEvent request = gameEventRepository.save(new GameEvent(
				player,
				"secretary_request_secretary-2_test",
				"request",
				"body",
				"EMPTY_SECRETARY_EVENT_IMAGE",
				"SECRETARY_TENANT_REQUEST:secretary-2",
				"\uB300\uC2E0 \uAC1A\uC544\uC8FC\uAE30"
		));

		gameService.completeEvent(player.getId(), request.getId());

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(400_000_000L);
		assertThat(secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, "secretary-2").orElseThrow().getStatus())
				.isEqualTo(SecretaryTenantEventStatus.HIRE_AVAILABLE);
	}

	@Test
	@Transactional
	void seoulSecretaryHireMovesResidenceToFinalBuilding() {
		Player player = playerRepository.save(new Player("seoul-secretary-hire-test", "hash"));
		OwnedBuilding triplet = ownedBuildingRepository.save(new OwnedBuilding(
				player, "\uC11C\uC6B8", 1, "tower", "triplet", 700_000_000_000L, 0L, 1_400_000_000L, 150));
		OwnedBuilding finalResidence = ownedBuildingRepository.save(new OwnedBuilding(
				player, "\uC11C\uC6B8", 4, "residence", "ximeng li", 10_000_000_000_000L, 0L, 18_000_000_000L, 240));
		triplet.moveInSecretaryTenant("secretary-6");
		SecretaryTenantEvent tenantEvent = secretaryTenantEventRepository.save(new SecretaryTenantEvent(
				player, triplet, "secretary-6", "\uC11C\uC6B8", player.getElapsedDays()));
		tenantEvent.makeHireAvailable();
		GameEvent hire = gameEventRepository.save(new GameEvent(
				player,
				"secretary_hire_secretary-6_test",
				"hire",
				"body",
				"EMPTY_SECRETARY_EVENT_IMAGE",
				"SECRETARY_TENANT_HIRE:secretary-6",
				"\uACE0\uC6A9\uD558\uAE30"
		));

		gameService.completeEvent(player.getId(), hire.getId());

		assertThat(ownedBuildingRepository.findById(triplet.getId()).orElseThrow().isOccupied()).isFalse();
		assertThat(ownedBuildingRepository.findById(finalResidence.getId()).orElseThrow().isSecretaryResident()).isTrue();
		assertThat(ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-6")).isPresent();
		assertThat(secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, "secretary-6").orElseThrow().getStatus())
				.isEqualTo(SecretaryTenantEventStatus.COMPLETED);
	}

	@Test
	@Transactional
	void auctionBidRequiresCash() {
		Player player = playerRepository.save(new Player("auction-cash-test", "hash"));
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
				player,
				"청주",
				"원룸",
				"경매 원룸",
				30_000_000L,
				200_000L,
				4
		));

		assertThat(gameService.bidAuction(player.getId(), auction.getId(), 90)).isEqualTo("현금 부족");
		assertThat(auctionEventRepository.findById(auction.getId()).orElseThrow().getStatus()).isEqualTo(AuctionStatus.ACTIVE);
	}

	@Test
	@Transactional
	void auctionCanBeCanceled() {
		Player player = playerRepository.save(new Player("auction-cancel-test", "hash"));
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
				player,
				"청주",
				"원룸",
				"경매 원룸",
				30_000_000L,
				200_000L,
				4
		));

		assertThat(gameService.cancelAuction(player.getId(), auction.getId())).isEqualTo("경매 취소");
		assertThat(auctionEventRepository.findById(auction.getId()).orElseThrow().getStatus()).isEqualTo(AuctionStatus.COMPLETED);
	}

}
