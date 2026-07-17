# BuildingStoryApplicationTests.java 코드 주석형 해설

원본 파일: `src/test/java/com/game/buildingstory/BuildingStoryApplicationTests.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
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
import com.game.buildingstory.domain.StockPriceHistory;
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
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.BuildingTradeService;
import com.game.buildingstory.service.QaService;
import com.game.buildingstory.service.SecretaryCatalog;
import com.game.buildingstory.service.SecretaryOperationsService;
import com.game.buildingstory.service.SettlementService;
import com.game.buildingstory.service.StockService;
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
	/*
	 * 통합 테스트 모음이다.
	 *
	 * 실제 Spring Bean과 H2 인메모리 DB를 띄워 서비스, 엔티티, Repository가 함께 동작하는지 검증한다.
	 * 이 프로젝트는 게임 규칙이 여러 서비스와 엔티티에 걸쳐 있으므로 단위 테스트만으로는
	 * "하루 진행 후 DB 상태가 맞는지"를 확인하기 어렵다. 그래서 주요 회귀는 여기서 실제 흐름으로 검증한다.
	 */

	@Autowired
	private GameService gameService;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private BuildingTradeService buildingTradeService;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private QaService qaService;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private SecretaryOperationsService secretaryOperationsService;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private StockService stockService;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private PlayerRepository playerRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private OwnedSecretaryRepository ownedSecretaryRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private OwnedBuildingRepository ownedBuildingRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private MonthlyRecordRepository monthlyRecordRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private OwnedLuxuryItemRepository ownedLuxuryItemRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private OwnedGiftItemRepository ownedGiftItemRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private AuctionEventRepository auctionEventRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private GameEventRepository gameEventRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private StockPriceHistoryRepository stockPriceHistoryRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private BuildingOfferRepository buildingOfferRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private SecretaryTenantEventRepository secretaryTenantEventRepository;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@Autowired
	private SecretaryCatalog secretaryCatalog;
	// 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

	@BeforeEach
	void cleanDatabase() {
		// 테스트는 실행 순서에 의존하면 안 된다. 각 테스트 전에 모든 테이블을 비워 독립성을 보장한다.
		// 자식 테이블을 먼저 지우는 이유는 JPA 외래키 제약 때문에 부모 Player를 먼저 삭제할 수 없기 때문이다.
		monthlyRecordRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		auctionEventRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		gameEventRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		buildingOfferRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		secretaryTenantEventRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		ownedGiftItemRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		ownedLuxuryItemRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		ownedBuildingRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		ownedSecretaryRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
		playerRepository.deleteAll();
		// 해설: Repository를 통해 DB 데이터를 삭제한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void contextLoads() {
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void cannotAssignTwoSecretariesToSameCity() {
		Player player = playerRepository.save(new Player("assignment-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary first = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary second = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 5));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		assertThat(gameService.assignSecretary(player.getId(), first.getId(), "청주")).isEqualTo("비서 배치 완료");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.assignSecretary(player.getId(), second.getId(), "청주")).isEqualTo("이미 다른 비서가 배치된 도시");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedSecretaryRepository.findById(second.getId()).orElseThrow().getAssignedCity()).isNull();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void updatesSecretaryProficiencyForTestBySecretary() {
		Player player = playerRepository.save(new Player("proficiency-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 5));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		assertThat(qaService.updateTestSecretaryProficiency(player.getId(), "secretary-2", 17)).isEqualTo("설하은 숙련도 변경 완료");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getProficiency()).isEqualTo(17);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		qaService.updateTestSecretaryProficiency(player.getId(), "secretary-2", 99);
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getProficiency()).isEqualTo(30);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void qaCashChangePersistsWithoutCallerTransaction() {
		Player player = playerRepository.save(new Player("qa-cash-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		assertThat(qaService.addTestCash(player.getId())).isEqualTo("테스트 현금 30,000,000원 지급");
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(30_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryAutoRepairAddsProficiencyExperience() {
		Player player = playerRepository.save(new Player("auto-repair-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		player.updateTestChances(0, 0, 0);
		OwnedBuilding building = new OwnedBuilding(player, "청주", "원룸", "테스트 원룸", 30_000_000L, 0L, 200_000L, 4);
		building.moveIn();
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		building.requestRepair();
		ownedBuildingRepository.save(building);
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("청주");

		gameService.tick(player.getId());

		OwnedBuilding repairedBuilding = ownedBuildingRepository.findById(building.getId()).orElseThrow();
		OwnedSecretary experiencedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(repairedBuilding.isRepairRequested()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(experiencedSecretary.getProficiencyExperience()).isBetween(1, 2);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(experiencedSecretary.getRequiredProficiencyExperience()).isEqualTo(3);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(experiencedSecretary.canAutoRepair(player.getElapsedDays())).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(monthlyRecordRepository.findAll())
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서수리");
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
					assertThat(record.getMemo()).contains("숙련도 경험치 +");
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				});
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void lowProficiencySecretaryAutoRepairsOnlyFirstTwoManagedBuildings() {
		Player player = playerRepository.save(new Player("managed-repair-limit-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		for (int i = 1; i <= 3; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			OwnedBuilding building = new OwnedBuilding(player, "청주", i, "원룸", "테스트 원룸 " + i, 30_000_000L, 0L, 200_000L, 4);
			building.moveIn();
			// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
			if (i == 3) {
			// 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
				building.requestRepair();
			}
			ownedBuildingRepository.save(building);
			// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		}
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("청주");

		assertThat(secretaryOperationsService.processAutoRepairs(player)).isBlank();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		var buildings = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주");
		assertThat(buildings.get(0).isRepairRequested()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildings.get(1).isRepairRequested()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildings.get(2).isRepairRequested()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void proficiencyElevenSecretaryCanAutoRepairTwoBuildingsPerCooldown() {
		Player player = playerRepository.save(new Player("double-auto-repair-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		for (int i = 1; i <= 3; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			OwnedBuilding building = new OwnedBuilding(player, "청주", i, "원룸", "테스트 원룸 " + i, 30_000_000L, 0L, 200_000L, 4);
			building.moveIn();
			// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
			building.requestRepair();
			ownedBuildingRepository.save(building);
			// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		}
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-2", 11));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("청주");

		assertThat(secretaryOperationsService.processAutoRepairs(player)).contains("비서수리 1건");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		var buildings = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주");
		assertThat(buildings.get(0).isRepairRequested()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildings.get(1).isRepairRequested()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildings.get(2).isRepairRequested()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryOneAutoRepairCostUsesAffinityDiscount() {
		Player player = playerRepository.save(new Player("auto-repair-discount-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedBuilding building = new OwnedBuilding(player, "청주", 1, "원룸", "테스트 원룸", 30_000_000L, 0L, 200_000L, 4);
		building.moveIn();
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		building.requestRepair();
		ownedBuildingRepository.save(building);
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.addAffinityExperience(63);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		assertThat(secretary.getAffinity()).isEqualTo(10);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		secretary.assignTo("청주");

		secretaryOperationsService.processAutoRepairs(player);

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(973_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(monthlyRecordRepository.findAll())
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서수리");
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
					assertThat(record.getAmount()).isEqualTo(-27_000L);
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				});
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryAbilitySummariesShowCurrentUnlockedAbilities() {
		Player player = playerRepository.save(new Player("secretary-ability-summary-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 16));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.addAffinityExperience(168);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		assertThat(secretary.getAffinity()).isEqualTo(17);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		secretary.assignTo("청주");

		assertThat(gameService.activeSecretaryAbilitySummaries(secretary))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.contains(
						"자동수리 수리비 감소 17%",
						"관리 가능 건물 8채",
						"쿨타임 내 자동수리 최대 2건",
						"자동수리 시 평판 +1 추가 증가",
						"매월 1일 배치 평판 +1~+3"
				);
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void secretarySalaryIncreasesByProficiencyTier() {
		var secretary = secretaryCatalog.find("secretary-1").orElseThrow();

		assertThat(secretary.monthlySalaryForProficiency(1)).isEqualTo(500_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretary.monthlySalaryForProficiency(2)).isEqualTo(2_160_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretary.monthlySalaryForProficiency(11)).isEqualTo(4_469_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretaryCatalog.find("secretary-6").orElseThrow().monthlySalaryForProficiency(1)).isEqualTo(40_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretaryCatalog.find("secretary-6").orElseThrow().monthlySalaryForProficiency(25)).isEqualTo(36_729_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void secretarySpecialEffectAndDefaultMoveOutChanceAreUpdated() {
		Player player = new Player("default-chance-test", "hash");

		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getMoveOutChancePercent()).isEqualTo(25);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		player.updateTestChances(40, 20, 30);
		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getMoveOutChancePercent()).isEqualTo(25);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.baseMoveInChancePercent(player)).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.baseMoveOutChancePercent(player)).isEqualTo(25);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.baseRepairRequestChancePercent(player)).isEqualTo(35);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretaryCatalog.find("secretary-2").orElseThrow().specialEffectSummary()).isEqualTo("퇴거확률 감소 0.3%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void randomBuildingEventsAllowTwoRepairDaysPerMonth() {
		Player player = new Player("repair-schedule-test", "hash");

		player.scheduleMonthlyRandomEvents(2, 3, 4, 5, 6, 7);

		assertThat(player.hasEventScheduleForCurrentMonth()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.isRepairEventDay()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		for (int i = 0; i < 5; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			player.advanceDay();
		}
		assertThat(player.getDay()).isEqualTo(6);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.isRepairEventDay()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		player.advanceDay();
		assertThat(player.getDay()).isEqualTo(7);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.isRepairEventDay()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void donationAddsOneReputationPerFiftyThousandWon() {
		Player player = playerRepository.save(new Player("donation-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(5_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.

		assertThat(gameService.donate(player.getId(), 100)).isEqualTo("기부 완료 · 평판 +100");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(updatedPlayer.getReputation()).isEqualTo(100);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void luxuryItemCanBeBoughtOnceAndHasDonationEfficiencyBonus() {
		Player player = playerRepository.save(new Player("luxury-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.

		assertThat(gameService.buyLuxuryItem(player.getId(), "bicycle")).isEqualTo("자전거 구매 완료 · 평판 +9");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isEqualTo(700_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(updatedPlayer.getReputation()).isEqualTo(9);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buyLuxuryItem(player.getId(), "bicycle")).isEqualTo("이미 구매한 아이템");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void giftItemCanBeBoughtInQuantityAndGivenByAffinityRange() {
		Player player = playerRepository.save(new Player("gift-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		assertThat(gameService.buyGiftItem(player.getId(), "coffee-beans", 2)).isEqualTo("고급 원두세트 2개 구매 완료");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(2);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 2))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("설아름에게 고급 원두세트 2개 선물 완료");
		OwnedSecretary updatedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(updatedSecretary.getAffinity()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(updatedSecretary.getAffinityExperience()).isEqualTo(2);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void giftRequiresMatchingAffinityRange() {
		Player player = playerRepository.save(new Player("gift-range-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(2_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		gameService.buyGiftItem(player.getId(), "fountain-pen", 1);

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "fountain-pen", 1))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("호감도 구간에 맞지 않는 선물");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "fountain-pen").orElseThrow().getQuantity()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void giftBalancePricesAreUpdated() {
		assertThat(gameService.giftItems().stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(gift -> "jewelry".equals(gift.key()))
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow()
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
				.price()).isEqualTo(80_000_000L);
		assertThat(gameService.giftItems().stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(gift -> "incentive".equals(gift.key()))
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow()
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
				.price()).isEqualTo(500_000_000L);
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void giftQuantityCannotExceedOwnedQuantity() {
		Player player = playerRepository.save(new Player("gift-owned-quantity-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(1_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		gameService.buyGiftItem(player.getId(), "coffee-beans", 1);

		assertThat(gameService.maxGiftQuantityForSecretary(player, secretary, gameService.giftItems().getFirst())).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 2))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("선물 수량 부족");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getAffinityExperience()).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void giftQuantityCannotContinuePastGiftAffinityRange() {
		Player player = playerRepository.save(new Player("gift-affinity-boundary-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(5_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.addAffinityExperience(63);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		assertThat(secretary.getAffinity()).isEqualTo(10);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		gameService.buyGiftItem(player.getId(), "coffee-beans", 20);
		var coffeeBeans = gameService.giftItems().stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(gift -> "coffee-beans".equals(gift.key()))
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow();
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.

		assertThat(gameService.maxGiftQuantityForSecretary(player, secretary, coffeeBeans)).isEqualTo(12);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 13))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("현재 호감도 구간에서 선물 가능한 수량 초과");
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(20);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		assertThat(gameService.giveGiftToSecretary(player.getId(), secretary.getId(), "coffee-beans", 12))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("설아름에게 고급 원두세트 12개 선물 완료");
		OwnedSecretary updatedSecretary = ownedSecretaryRepository.findById(secretary.getId()).orElseThrow();
		assertThat(updatedSecretary.getAffinity()).isEqualTo(11);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedGiftItemRepository.findByPlayerAndGiftKey(player, "coffee-beans").orElseThrow().getQuantity()).isEqualTo(8);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void loanPurchaseUsesSixtyPercentLoanAndFortyPercentCash() {
		Player player = playerRepository.save(new Player("loan-ratio-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(20_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		player.setReputationForTest(1_000);
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
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
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(offer.cashForLoanPurchase()).isEqualTo(12_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buyOffer(player.getId(), offer.getId(), true)).isEqualTo("대출구매 완료");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(8_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void loanPurchaseRejectsLoanLimitOverflow() {
		Player player = playerRepository.save(new Player("loan-limit-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(20_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
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
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(20_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void buyingOfferDoesNotRefreshMarketBeforeRefreshDay() {
		Player player = playerRepository.save(new Player("offer-refresh-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(100_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		BuildingOffer offer = buildingOfferRepository.save(new BuildingOffer(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
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
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		gameService.ensureOffers(player);

		BuildingOffer remainingOffer = buildingOfferRepository.findById(offer.getId()).orElseThrow();
		assertThat(remainingOffer.getName()).isEqualTo("갱신 테스트 원룸");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.purchaseCooldownDaysLeft(player, remainingOffer)).isPositive();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildingOfferRepository.findByPlayerAndCityOrderById(player, "청주")).hasSize(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void marketNewsAppliesForTwoOfferRefreshes() {
		Player player = playerRepository.save(new Player("market-news-refresh-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.scheduleMonthlyMarketNews(1, "청주", SettlementService.MARKET_NEWS_RISE);
		player.activateMarketNews();

		assertThat(gameService.marketNewsStatusText(player, "청주")).isEqualTo("부동산 폭등 · 매물갱신 2회");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		buildingTradeService.refreshOffers(player);

		assertThat(player.getActiveMarketNewsRefreshesLeft()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.marketNewsStatusText(player, "청주")).isEqualTo("부동산 폭등 · 매물갱신 1회");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(buildingOfferRepository.findByPlayerAndCityOrderById(player, "청주")).hasSize(4);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		buildingTradeService.refreshOffers(player);

		assertThat(player.hasActiveMarketNewsForCity("청주")).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.marketNewsStatusText(player, "청주")).isBlank();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void qaCanActivateCurrentCityMarketNews() {
		Player player = playerRepository.save(new Player("qa-market-news-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.changeCity("세종");

		assertThat(qaService.activateMarketNewsEvent(player.getId(), SettlementService.MARKET_NEWS_FALL))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo("세종 부동산 폭락 뉴스");

		assertThat(playerRepository.findById(player.getId()).orElseThrow().hasActiveMarketNewsForCity("세종")).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(player, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isPresent()
				.get()
				.extracting(GameEvent::getTitle)
				.isEqualTo("세종 부동산 폭락 뉴스");
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockContentUnlocksTwoDaysAfterSeoulUnlockSchedule() {
		Player player = playerRepository.save(new Player("stock-unlock-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.scheduleStockUnlock(player.getElapsedDays() + 2);

		player.advanceDay();

		assertThat(gameService.stockContentUnlocked(player)).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		player.advanceDay();
		assertThat(stockService.activateUnlockNoticeIfDue(player)).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.isStockContentUnlocked()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(updatedPlayer, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isPresent()
				.get()
				.extracting(GameEvent::getTitle)
				.isEqualTo("주식 투자 개방");
		assertThat(stockPriceHistoryRepository.countByPlayer(updatedPlayer)).isEqualTo(stockService.stocks().size());
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockPricesUpdateEveryFiveElapsedDays() {
		Player player = playerRepository.save(new Player("stock-price-update-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		assertThat(stockPriceHistoryRepository.countByPlayer(player)).isEqualTo(stockService.stocks().size());
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		for (int i = 0; i < 4; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);
		assertThat(stockPriceHistoryRepository.countByPlayer(player)).isEqualTo(stockService.stocks().size());
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		player.advanceDay();
		stockService.processPriceUpdates(player);

		assertThat(stockPriceHistoryRepository.countByPlayer(player)).isEqualTo(stockService.stocks().size() * 2L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(stockService.stockQuotes(player))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.hasSize(stockService.stocks().size())
				.allSatisfy(quote -> {
					assertThat(quote.currentPrice()).isPositive();
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
					assertThat(quote.previousPrice()).isPositive();
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
					assertThat(quote.candles()).isNotEmpty();
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				});
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockIndustryNewsActivatesAndAppliesForTwoPriceUpdates() {
		Player player = playerRepository.save(new Player("stock-news-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		player.scheduleMonthlyStockNews(player.getDay(), "IT", StockService.STOCK_NEWS_BOOM);

		assertThat(stockService.activateIndustryNewsIfDue(player)).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.hasActiveStockNewsForIndustry("IT")).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getActiveStockNewsRefreshesLeft()).isEqualTo(2);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(stockService.marketStatus(player).activeNewsText()).isEqualTo("IT 호황 적용중 · 2회 남음");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(stockService.marketStatus(player).activeNewsDirection()).isEqualTo("up");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(player, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isPresent()
				.get()
				.extracting(GameEvent::getTitle)
				.isEqualTo("IT 업종 호황 뉴스");

		for (int i = 0; i < 5; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);
		assertThat(player.getActiveStockNewsRefreshesLeft()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		for (int i = 0; i < 5; i++) {
		// 해설: 반복문이다. 여러 대상에 같은 처리를 순서대로 적용한다.
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);
		assertThat(player.hasActiveStockNewsForIndustry("IT")).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(stockService.marketStatus(player).hasActiveNews()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockExchangeAndImmediateTradeUseCoinWithFee() {
		Player player = playerRepository.save(new Player("stock-trade-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(10_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L)).isEqualTo("코인 부족 · 필요 8만2410코인 / 보유 0코인");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.sellStock(player.getId(), "bytecore", 5L)).isEqualTo("보유 수량 부족 · 보유 0주 / 매도 요청 5주");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		assertThat(gameService.exchangeCashToCoin(player.getId(), 100_000L)).isEqualTo("10만코인 교환");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCash()).isEqualTo(0L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCoin()).isEqualTo(100_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L)).isEqualTo("바이트코어 1주 매수");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCoin()).isEqualTo(17_590L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(stockService.stockQuotes(player).stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(quote -> quote.stock().key().equals("bytecore"))
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow()
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
				.quantity()).isEqualTo(1L);

		assertThat(gameService.sellStock(player.getId(), "bytecore", 1L)).isEqualTo("바이트코어 1주 매도");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCoin()).isEqualTo(99_180L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		assertThat(gameService.buyMaxStock(player.getId(), "bytecore")).isEqualTo("바이트코어 1주 매수");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCoin()).isEqualTo(16_770L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.sellAllStock(player.getId(), "bytecore")).isEqualTo("바이트코어 1주 매도");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(player.getCoin()).isEqualTo(98_360L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.stockTradeHistories(player)).hasSize(4);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockProfitTextShowsPercentAndCoinAmount() {
		Player player = playerRepository.save(new Player("stock-profit-text-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(10_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		gameService.exchangeCashToCoin(player.getId(), 100_000L);
		gameService.buyStock(player.getId(), "bytecore", 1L);
		stockPriceHistoryRepository.save(new StockPriceHistory(player, "bytecore", 82_000L, 92_660L, 82_000L, 92_660L, 100L));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.

		assertThat(stockService.stockQuotes(player).stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(quote -> quote.stock().key().equals("bytecore"))
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow()
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
				.valuationProfitText()).isEqualTo("(+13%) +1만660코인");
		assertThat(stockService.holdingSummary(player).totalProfitText()).isEqualTo("(+13%) +1만660코인");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void stockViewTickDefersDueCityEventWithoutPausing() {
		Player player = playerRepository.save(new Player("stock-view-defer-event-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.completeStory();
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		player.unlockStockContent();
		player.scheduleNoMonthlyStockNews();

		assertThat(gameService.tick(player.getId(), true)).isBlank();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.tick(player.getId(), true)).isBlank();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.

		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getMonth()).isEqualTo(1);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(updatedPlayer.getDay()).isEqualTo(3);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(updatedPlayer.isPaused()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.activeEvent(updatedPlayer)).isPresent();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	void buildingCatalogRentAndCooldownBalanceAreUpdated() {
		var cheongjuRoom = gameService.buildingSpecs().stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(spec -> spec.city().equals("청주") && spec.slot() == 1)
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow();
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
		var seoulFinal = gameService.buildingSpecs().stream()
		// 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
				.filter(spec -> spec.city().equals("서울") && spec.slot() == 4)
				// 해설: Stream에서 조건을 만족하는 값만 다음 단계로 통과시킨다.
				.findFirst()
				// 해설: 조건을 만족하는 첫 번째 값을 Optional로 가져온다. 없으면 빈 Optional이다.
				.orElseThrow();
				// 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.

		assertThat(cheongjuRoom.monthlyRent()).isEqualTo(300_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(cheongjuRoom.tradeCooldownDays()).isEqualTo(5);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(seoulFinal.monthlyRent()).isEqualTo(27_000_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(seoulFinal.tradeCooldownDays()).isEqualTo(264);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryRentBonusAppliesOnlyToAssignedCity() {
		Player player = playerRepository.save(new Player("secretary-rent-bonus-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedBuilding assignedCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "test-city", "office", "assigned city office", 100_000_000L, 0L, 1_000_000L, 10));
		OwnedBuilding otherCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "other-city", "office", "other city office", 100_000_000L, 0L, 1_000_000L, 10));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-4", 15));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("test-city");

		assertThat(gameService.effectiveMonthlyRent(player, assignedCityBuilding)).isEqualTo(1_005_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.effectiveMonthlyRent(player, otherCityBuilding)).isEqualTo(1_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void cityPanelChanceTextUsesSecretaryEffectForAssignedCityOnly() {
		Player player = playerRepository.save(new Player("secretary-chance-text-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-3", 20));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("test-city");

		assertThat(gameService.effectiveMoveInChancePercentText(player, "test-city")).isEqualTo("35.5%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.effectiveMoveInChancePercentText(player, "other-city")).isEqualTo("35%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.effectiveMoveOutChancePercentText(player, "test-city")).isEqualTo("25%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.effectiveRepairRequestChancePercentText(player, "test-city")).isEqualTo("35%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void assignedSecretaryAddsMonthlyReputationRecordOnFirstDay() {
		Player player = playerRepository.save(new Player("secretary-monthly-reputation-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("청주");

		secretaryOperationsService.processMonthlyReputation(player);

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getReputation()).isBetween(1, 3);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(monthlyRecordRepository.findAll())
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.anySatisfy(record -> {
					assertThat(record.getTitle()).isEqualTo("비서 관리");
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
					assertThat(record.getReputationChange()).isBetween(1, 3);
					// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				});
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryBuildingWaitReductionAppliesToPurchaseAndSaleCooldownCity() {
		Player player = playerRepository.save(new Player("secretary-wait-reduction-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedBuilding assignedCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "test-city", "office", "assigned city office", 100_000_000L, 0L, 1_000_000L, 100));
		OwnedBuilding otherCityBuilding = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "other-city", "office", "other city office", 100_000_000L, 0L, 1_000_000L, 100));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-6", 25));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("test-city");

		assertThat(gameService.daysUntilSellable(player, assignedCityBuilding)).isEqualTo(99);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.daysUntilSellable(player, otherCityBuilding)).isEqualTo(100);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void cityPanelCanDisplayRentBonusAndBuildingWaitReductionText() {
		Player player = playerRepository.save(new Player("secretary-city-effect-text-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-5", 20));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		secretary.assignTo("test-city");

		assertThat(gameService.rentBonusPercent(player, "test-city")).isEqualTo(0.25);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.rentBonusPercentText(player, "test-city")).isEqualTo("0.25%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buildingWaitReductionPercent(player, "test-city")).isEqualTo(0.5);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buildingWaitReductionPercentText(player, "test-city")).isEqualTo("0.5%");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.rentBonusPercent(player, "other-city")).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buildingWaitReductionPercent(player, "other-city")).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void buildingImagePathUsesCityAndSlot() {
		Player player = playerRepository.save(new Player("building-image-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		BuildingOffer offer = new BuildingOffer(player, "\uC11C\uC6B8", 4, "landmark", "seoul landmark", 100_000_000L, 1_000_000L, 10, ValuationStatus.FAIR);
		OwnedBuilding building = new OwnedBuilding(player, "\uC778\uCC9C", 2, "tower", "incheon tower", 100_000_000L, 0L, 1_000_000L, 10);

		assertThat(gameService.buildingImagePath(offer)).isEqualTo("/assets/buildings/seoul-4.jpg");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.buildingImagePath(building)).isEqualTo("/assets/buildings/incheon-2.jpg");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryTenantRentWaiverBlocksSaleAndShowsEventStatus() {
		Player player = playerRepository.save(new Player("secretary-rent-waiver-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "\uCCAD\uC8FC", 1, "room", "cheongju room", 30_000_000L, 0L, 200_000L, 4));
		building.moveInSecretaryTenant("secretary-1");
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		SecretaryTenantEvent event = secretaryTenantEventRepository.save(new SecretaryTenantEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, building, "secretary-1", "\uCCAD\uC8FC", player.getElapsedDays()));
		event.acceptRequest(player.getElapsedDays(), 60);

		assertThat(gameService.rentWaivedBySecretaryEvent(player, building)).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.secretaryTenantStatusText(building)).contains("\uC6D4\uC138 \uAC10\uBA74");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.sellAvailabilityText(player, building)).isEqualTo("\uBE44\uC11C \uAC70\uC8FC\uC911");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.canSell(player, building)).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.totalMonthlyRent(player)).isZero();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void missingDaejeonSecretaryIntroActivatesFromOwnedBuilding() {
		Player player = playerRepository.save(new Player("missing-daejeon-intro-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "\uB300\uC804", 2, "\uC0C1\uAC00\uC8FC\uD0DD", "\uBD09\uBA85\uB3D9 \uC0C1\uAC00\uC8FC\uD0DD", 1_350_000_000L, 0L, 4_900_000L, 41));

		gameService.evaluateSecretaryTenantEvents(player);

		assertThat(gameEventRepository.findLatestByPlayerIdAndStatus(player.getId(), com.game.buildingstory.domain.GameEventStatus.ACTIVE, org.springframework.data.domain.PageRequest.of(0, 1)))
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.singleElement()
				.extracting(GameEvent::getEventKey)
				.isEqualTo("secretary_intro_secretary-3");
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void auctionDisplayUsesCatalogNameBySlot() {
		Player player = playerRepository.save(new Player("auction-display-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
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
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(gameService.auctionDisplayName(auction)).isEqualTo("\uC1A1\uB3C4 \uC13C\uD2B8\uB7F4\uD30C\uD06C");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void secretaryRequestEventPaysCostAndUnlocksHire() {
		Player player = playerRepository.save(new Player("secretary-request-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		player.addCash(500_000_000L);
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "\uC138\uC885", 2, "apt", "sejong apt", 300_000_000L, 0L, 1_600_000L, 21));
		building.moveInSecretaryTenant("secretary-2");
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, "secretary-2", "\uC138\uC885", player.getElapsedDays()));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		GameEvent request = gameEventRepository.save(new GameEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player,
				"secretary_request_secretary-2_test",
				"request",
				"body",
				"EMPTY_SECRETARY_EVENT_IMAGE",
				"SECRETARY_TENANT_REQUEST:secretary-2",
				"\uB300\uC2E0 \uAC1A\uC544\uC8FC\uAE30"
		));

		gameService.completeEvent(player.getId(), request.getId());
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.

		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(400_000_000L);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, "secretary-2").orElseThrow().getStatus())
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo(SecretaryTenantEventStatus.HIRE_AVAILABLE);
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void seoulSecretaryHireMovesResidenceToFinalBuilding() {
		Player player = playerRepository.save(new Player("seoul-secretary-hire-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		OwnedBuilding triplet = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "\uC11C\uC6B8", 1, "tower", "triplet", 700_000_000_000L, 0L, 1_400_000_000L, 150));
		OwnedBuilding finalResidence = ownedBuildingRepository.save(new OwnedBuilding(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, "\uC11C\uC6B8", 4, "residence", "ximeng li", 10_000_000_000_000L, 0L, 18_000_000_000L, 240));
		triplet.moveInSecretaryTenant("secretary-6");
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
		SecretaryTenantEvent tenantEvent = secretaryTenantEventRepository.save(new SecretaryTenantEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player, triplet, "secretary-6", "\uC11C\uC6B8", player.getElapsedDays()));
		tenantEvent.makeHireAvailable();
		GameEvent hire = gameEventRepository.save(new GameEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player,
				"secretary_hire_secretary-6_test",
				"hire",
				"body",
				"EMPTY_SECRETARY_EVENT_IMAGE",
				"SECRETARY_TENANT_HIRE:secretary-6",
				"\uACE0\uC6A9\uD558\uAE30"
		));

		gameService.completeEvent(player.getId(), hire.getId());
		// 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.

		assertThat(ownedBuildingRepository.findById(triplet.getId()).orElseThrow().isOccupied()).isFalse();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedBuildingRepository.findById(finalResidence.getId()).orElseThrow().isSecretaryResident()).isTrue();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-6")).isPresent();
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, "secretary-6").orElseThrow().getStatus())
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
				.isEqualTo(SecretaryTenantEventStatus.COMPLETED);
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void auctionBidRequiresCash() {
		Player player = playerRepository.save(new Player("auction-cash-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player,
				"청주",
				"원룸",
				"경매 원룸",
				30_000_000L,
				200_000L,
				4
		));

		assertThat(gameService.bidAuction(player.getId(), auction.getId(), 90)).isEqualTo("현금 부족");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(auctionEventRepository.findById(auction.getId()).orElseThrow().getStatus()).isEqualTo(AuctionStatus.ACTIVE);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

	@Test
	// 해설: JUnit 테스트 메서드다. 독립 검증 단위로 실행된다.
	@Transactional
	void auctionCanBeCanceled() {
		Player player = playerRepository.save(new Player("auction-cancel-test", "hash"));
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
		// 해설: Repository에 엔티티 저장을 요청한다. 새 객체 저장이나 변경 감지 대상 등록에 사용된다.
				player,
				"청주",
				"원룸",
				"경매 원룸",
				30_000_000L,
				200_000L,
				4
		));

		assertThat(gameService.cancelAuction(player.getId(), auction.getId())).isEqualTo("경매 취소");
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
		assertThat(auctionEventRepository.findById(auction.getId()).orElseThrow().getStatus()).isEqualTo(AuctionStatus.COMPLETED);
		// 해설: 테스트 검증문이다. 실제 결과가 기대값과 다르면 테스트가 실패한다.
	}

}
```
