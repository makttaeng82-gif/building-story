package com.game.buildingstory;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.domain.StockMarketRegimeState;
import com.game.buildingstory.domain.ValuationStatus;
import com.game.buildingstory.repo.AuctionEventRepository;
import com.game.buildingstory.repo.BuildingOfferRepository;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.StockMarketRegimeStateRepository;
import com.game.buildingstory.repo.StockMarketIndexHistoryRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedGiftItemRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.LoanService;
import com.game.buildingstory.service.ListedCompanyFinancialService;
import com.game.buildingstory.service.ListedCompanyValuationService;
import com.game.buildingstory.service.BuildingTradeService;
import com.game.buildingstory.service.QaService;
import com.game.buildingstory.service.SecretaryCatalog;
import com.game.buildingstory.service.SecretaryOperationsService;
import com.game.buildingstory.service.SettlementService;
import com.game.buildingstory.service.StockService;
import com.game.buildingstory.service.StockCompanyNewsService;
import com.game.buildingstory.service.StockIndustryNewsService;
import com.game.buildingstory.service.StockLiquidityService;
import com.game.buildingstory.service.StockMarketNewsService;
import com.game.buildingstory.service.StockMarketRegime;
import com.game.buildingstory.service.StockMarketRegimeService;
import com.game.buildingstory.repo.OwnedLuxuryItemRepository;
import com.game.buildingstory.web.SessionKeys;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:building-story-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
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

	@Autowired
	private BuildingTradeService buildingTradeService;

	@Autowired
	private QaService qaService;

	@Autowired
	private SecretaryOperationsService secretaryOperationsService;

	@Autowired
	private StockService stockService;

	@Autowired
	private StockLiquidityService stockLiquidityService;

	@Autowired
	private LoanService loanService;

	@Autowired
	private ListedCompanyFinancialService listedCompanyFinancialService;

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
	private StockPriceHistoryRepository stockPriceHistoryRepository;

	@Autowired
	private OwnedStockRepository ownedStockRepository;

	@Autowired
	private StockTradeHistoryRepository stockTradeHistoryRepository;

	@Autowired
	private BuildingOfferRepository buildingOfferRepository;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private ListedCompanyRepository listedCompanyRepository;

	@Autowired
	private ListedCompanyQuarterlyReportRepository listedCompanyQuarterlyReportRepository;

	@Autowired
	private ListedCompanyValuationSnapshotRepository listedCompanyValuationSnapshotRepository;

	@Autowired
	private StockMarketRegimeStateRepository stockMarketRegimeStateRepository;

	@Autowired
	private StockMarketIndexHistoryRepository stockMarketIndexHistoryRepository;

	@Autowired
	private StockNewsArticleRepository stockNewsArticleRepository;

	@Autowired
	private StockIndustryNewsService stockIndustryNewsService;

	@Autowired
	private StockCompanyNewsService stockCompanyNewsService;

	@Autowired
	private StockMarketNewsService stockMarketNewsService;

	@Autowired
	private StockMarketRegimeService stockMarketRegimeService;

	@Autowired
	private ListedCompanyValuationService listedCompanyValuationService;

	@Autowired
	private SecretaryTenantEventRepository secretaryTenantEventRepository;

	@Autowired
	private SecretaryCatalog secretaryCatalog;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void cleanDatabase() {
		// 테스트는 실행 순서에 의존하면 안 된다. 각 테스트 전에 모든 테이블을 비워 독립성을 보장한다.
		// 자식 테이블을 먼저 지우는 이유는 JPA 외래키 제약 때문에 부모 Player를 먼저 삭제할 수 없기 때문이다.
		monthlyRecordRepository.deleteAll();
		auctionEventRepository.deleteAll();
		gameEventRepository.deleteAll();
		buildingOfferRepository.deleteAll();
		secretaryTenantEventRepository.deleteAll();
		ownedGiftItemRepository.deleteAll();
		ownedLuxuryItemRepository.deleteAll();
		loanRepository.deleteAll();
		ownedBuildingRepository.deleteAll();
		ownedSecretaryRepository.deleteAll();
		stockTradeHistoryRepository.deleteAll();
		stockPriceHistoryRepository.deleteAll();
		stockMarketIndexHistoryRepository.deleteAll();
		stockNewsArticleRepository.deleteAll();
		ownedStockRepository.deleteAll();
		listedCompanyValuationSnapshotRepository.deleteAll();
		listedCompanyQuarterlyReportRepository.deleteAll();
		listedCompanyRepository.deleteAll();
		stockMarketRegimeStateRepository.deleteAll();
		playerRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void mainCityAndStockViewsRenderWithWonAccountAndMarketIndex() throws Exception {
		Player player = playerRepository.save(new Player("main-render-test", "hash"));
		gameService.completeStory(player.getId());
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		stockIndustryNewsService.publish(player, "it-public-cloud", 0);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

		mockMvc.perform(get("/main").param("view", "city").session(session))
				.andExpect(status().isOk());
		String stockHtml = mockMvc.perform(get("/main").param("view", "stocks").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(stockHtml).contains(
				"data-stock-market-overview", "상승 / 하락 / 보합", "시장이슈",
				"stock-summary-more", "더보기", "data-market-buy-limit",
				"기준 실적", "분기 매출", "3조원", "실적 발표 D-", "적정가", "예상배당금", "중립",
				"기업 개요", "한도윤", "최근 4분기", "재무·가치평가", "PER", "PBR",
				"배당성향", "최근 주당배당", "예상 주당배당", "내 예상배당금", "stock-news-new", "NEW",
				"주가 갱신", "stock-update-status", "공공 클라우드 전환 예산 확정",
				"정부가 공공 정보시스템의 클라우드 전환 예산을 확정했다.",
				"실적 전망 반영", "수수료 0.25%",
				"data-stock-chart", "data-chart-period=\"18\"", "data-chart-period=\"all\"",
				"data-chart-toggle=\"events\"", "data-chart-toggle=\"fair\"",
				"data-chart-toggle=\"index\"", "data-chart-toggle=\"average\"",
				"data-chart-inspector", "data-factor-text");
		assertThat(stockHtml).contains("data-stock-theme-option=\"light\"", "data-stock-theme-option=\"dark\"");
		assertThat(stockHtml).containsOnlyOnce("class=\"stock-candle-chart\"");
		assertThat(stockHtml).containsOnlyOnce("data-stock-order-context");
		var selectedQuote = gameService.selectedStockQuote(player, "bytecore");
		var selectedCompany = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		int surprise = listedCompanyQuarterlyReportRepository
				.findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(selectedCompany).orElseThrow()
				.getEarningsSurpriseBasisPoints();
		assertThat(selectedQuote.earningsSurpriseDirection())
				.isEqualTo(surprise > 0 ? "up" : surprise < 0 ? "down" : "flat");

		String selectedStockHtml = mockMvc.perform(get("/main")
					.param("view", "stocks")
					.param("stockKey", "neonsoft")
					.session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(selectedStockHtml).contains(
				"data-selected-stock-key=\"neonsoft\"",
				"data-stock-detail=\"neonsoft\"",
				"action=\"/stocks/neonsoft/buy\"");
		assertThat(selectedStockHtml).doesNotContain("data-stock-detail=\"bytecore\"");
	}

	@Test
	void cityViewRendersSectionsBelowMarketAfterServiceTransactionCloses() throws Exception {
		Player player = playerRepository.save(new Player("detached-city-render-test", "hash"));
		gameService.completeStory(player.getId());
		OwnedBuilding collateral = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 1, "원룸", "대출 담보 원룸", 30_000_000L, 30_000_000L, 375_000L, 4));
		OwnedBuilding secondCollateral = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 2, "오피스텔", "두 번째 대출 담보", 70_000_000L, 70_000_000L, 850_000L, 7));
		OwnedBuilding thirdCollateral = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 3, "아파트", "세 번째 대출 담보", 140_000_000L, 140_000_000L, 1_690_000L, 10));
		loanRepository.save(new Loan(player, collateral, 18_000_000L));
		loanRepository.save(new Loan(player, secondCollateral, 42_000_000L));
		loanRepository.save(new Loan(player, thirdCollateral, 84_000_000L));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

		String html = mockMvc.perform(get("/main").param("view", "city").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("정부지원 40%", "(부대비용 ", "누적 기부액", "30만원당 평판 1", "순자산 30억원 · 평판 8,250 필요", "대출현황", "월 이자 합계", "대출 원금은 매월 줄지 않으며", "대출 담보 원룸", "두 번째 대출 담보", "세 번째 대출 담보", "원금 전액 상환", "사치품", "30만원당 평판 1.5", "구입 후 보유 비서에게 선물 가능");
	}

	@Test
	void infoViewShowsCurrentGameGuides() throws Exception {
		Player player = playerRepository.save(new Player("loan-info-render-test", "hash"));
		gameService.completeStory(player.getId());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

		String html = mockMvc.perform(get("/info").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains(
				"대출 기본정보", "원금의 월 0.4%", "별도 조건 없이 24개월 갱신", "대출별로 독립 계산", "시세의 90%로 담보 건물 강제매각",
				"부동산 기본정보", "시장가의 90%", "월세의 10%", "기본 입주확률 35%", "도시별 최초 1회 구매가격 40% 지원",
				"비서 기본정보와 목록", "영입 경로", "필요 평판", "초기 월급", "최대 월급", "숙련 효과", "호감도 효과",
				"주식 기본정보와 용어", "종합지수", "경기국면", "β(베타)", "적정가치", "예상배당금", "평가손익",
				"data-collapsible-key=\"info-loan\"", "data-collapsible-key=\"info-real-estate\"",
				"data-collapsible-key=\"info-secretary\"", "data-collapsible-key=\"info-stock-guide\""
		);
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
	void secretarySalaryUsesCommonProgressiveCurveAndEqualMaximum() {
		var secretary = secretaryCatalog.find("secretary-1").orElseThrow();

		assertThat(secretary.monthlySalaryForProficiency(1)).isEqualTo(1_500_000L);
		assertThat(secretary.monthlySalaryForProficiency(2)).isEqualTo(1_519_024L);
		assertThat(secretary.monthlySalaryForProficiency(11)).isEqualTo(3_402_497L);
		assertThat(secretaryCatalog.find("secretary-6").orElseThrow().monthlySalaryForProficiency(27)).isEqualTo(14_360_879L);
		assertThat(secretaryCatalog.all()).allSatisfy(spec ->
				assertThat(spec.monthlySalaryForProficiency(30)).isEqualTo(17_500_000L));
	}

	@Test
	void secretarySpecialEffectAndDefaultMoveOutChanceAreUpdated() {
		Player player = new Player("default-chance-test", "hash");

		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		assertThat(player.getMoveOutChancePercent()).isEqualTo(18);
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(10);
		player.updateTestChances(40, 20, 30);
		assertThat(player.getMoveInChancePercent()).isEqualTo(35);
		assertThat(player.getMoveOutChancePercent()).isEqualTo(18);
		assertThat(player.getRepairRequestChancePercent()).isEqualTo(10);
		assertThat(gameService.baseMoveInChancePercent(player)).isEqualTo(35);
		assertThat(gameService.baseMoveOutChancePercent(player)).isEqualTo(18);
		assertThat(gameService.baseRepairRequestChancePercent(player)).isEqualTo(10);
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
	void donationAwardsOneReputationPerThreeHundredThousandWon() {
		Player player = playerRepository.save(new Player("donation-test", "hash"));
		player.addCash(5_000_000L);

		assertThat(gameService.donate(player.getId(), 10)).isEqualTo("기부 완료 · 평판 +10");
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isEqualTo(2_000_000L);
		assertThat(updatedPlayer.getReputation()).isEqualTo(10);
		assertThat(updatedPlayer.getCumulativeDonation()).isEqualTo(3_000_000L);
		assertThat(monthlyRecordRepository
				.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(updatedPlayer, 0))
				.singleElement()
				.extracting(record -> record.getMemo())
				.isNull();
	}

	@Test
	void donationPanelShowsCurrentCumulativeDonation() throws Exception {
		Player player = playerRepository.save(new Player("donation-panel-test", "hash"));
		gameService.completeStory(player.getId());
		Player fundedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		fundedPlayer.addCash(5_000_000L);
		playerRepository.save(fundedPlayer);
		gameService.donate(player.getId(), 10);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

		String html = mockMvc.perform(get("/main").param("view", "city").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("누적 기부액", "300만원");
		assertThat(html).doesNotContain("누적 기부 3000000원");
	}

	@Test
	@Transactional
	void luxuryItemCanBeBoughtOnceAndHasFixedReputationReward() {
		Player player = playerRepository.save(new Player("luxury-test", "hash"));
		player.addCash(1_000_000L);

		assertThat(gameService.buyLuxuryItem(player.getId(), "bicycle")).isEqualTo("자전거 구매 완료 · 평판 +1");
		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getCash()).isEqualTo(700_000L);
		assertThat(updatedPlayer.getReputation()).isEqualTo(1);
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
				.price()).isEqualTo(12_500_000L);
		assertThat(gameService.giftItems().stream()
				.filter(gift -> "incentive".equals(gift.key()))
				.findFirst()
				.orElseThrow()
				.price()).isEqualTo(50_000_000L);
	}

	@Test
	void luxuryItemReputationUsesOnePointFivePerThreeHundredThousandWonWithFlooring() {
		assertThat(gameService.luxuryItems()).allSatisfy(item ->
				assertThat(item.reputationReward()).isEqualTo((int) Math.floor(item.price() / 300_000.0 * 1.5)));
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
		player.claimGovernmentPurchaseSupport("청주");
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

		assertThat(offer.loanAmount()).isEqualTo(24_000_000L);
		assertThat(offer.cashForLoanPurchase()).isEqualTo(6_450_000L);
		assertThat(gameService.buyOffer(player.getId(), offer.getId(), true)).isEqualTo("대출구매 완료");
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(13_550_000L);
	}

	@Test
	@Transactional
	void firstPaidPurchaseInSupportedCityReceivesGovernmentDiscountOnce() {
		Player player = playerRepository.save(new Player("government-support-test", "hash"));
		player.addCash(20_000_000L);
		BuildingOffer firstOffer = buildingOfferRepository.save(new BuildingOffer(
				player,
				"청주",
				1,
				"원룸",
				"정부지원 테스트 원룸",
				30_000_000L,
				200_000L,
				4,
				ValuationStatus.FAIR
		));

		assertThat(firstOffer.isGovernmentSupportEligible()).isTrue();
		assertThat(firstOffer.effectivePurchasePrice()).isEqualTo(18_000_000L);
		assertThat(firstOffer.loanAmount()).isEqualTo(14_400_000L);
		assertThat(firstOffer.cashForLoanPurchase()).isEqualTo(3_870_000L);
		assertThat(gameService.buyOffer(player.getId(), firstOffer.getId(), true))
				.isEqualTo("대출구매 완료 · 정부지원 40%");
		assertThat(loanRepository.findByPlayer(player))
				.singleElement()
				.extracting(Loan::getPrincipal)
				.isEqualTo(14_400_000L);
		assertThat(ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주"))
				.singleElement()
				.extracting(OwnedBuilding::getGovernmentSupportAmount)
				.isEqualTo(12_000_000L);

		BuildingOffer laterOffer = new BuildingOffer(
				player, "청주", 1, "원룸", "두 번째 원룸", 30_000_000L, 200_000L, 4, ValuationStatus.FAIR);
		assertThat(laterOffer.isGovernmentSupportEligible()).isFalse();
		assertThat(laterOffer.effectivePurchasePrice()).isEqualTo(30_000_000L);
	}

	@Test
	void governmentSupportRatesExtendThroughIncheon() {
		Player player = new Player("government-support-city-test", "hash");

		assertThat(player.canUseGovernmentPurchaseSupport("청주")).isTrue();
		assertThat(player.canUseGovernmentPurchaseSupport("세종")).isTrue();
		assertThat(player.canUseGovernmentPurchaseSupport("대전")).isTrue();
		assertThat(player.canUseGovernmentPurchaseSupport("부산")).isTrue();
		assertThat(player.canUseGovernmentPurchaseSupport("인천")).isTrue();
		assertThat(player.canUseGovernmentPurchaseSupport("서울")).isTrue();
		assertThat(com.game.buildingstory.domain.EconomyBalanceRules.governmentPurchaseSupportPercent("청주")).isEqualTo(40);
		assertThat(com.game.buildingstory.domain.EconomyBalanceRules.governmentPurchaseSupportPercent("부산")).isEqualTo(30);
		assertThat(com.game.buildingstory.domain.EconomyBalanceRules.governmentPurchaseSupportPercent("인천")).isEqualTo(20);
		assertThat(com.game.buildingstory.domain.EconomyBalanceRules.governmentPurchaseSupportPercent("서울")).isEqualTo(10);
		assertThat(player.claimGovernmentPurchaseSupport("세종")).isTrue();
		assertThat(player.claimGovernmentPurchaseSupport("세종")).isFalse();
	}

	@Test
	void incheonFirstPurchaseReceivesTwentyPercentGovernmentSupport() {
		Player player = new Player("incheon-support-test", "hash");
		BuildingOffer offer = new BuildingOffer(
				player, "인천", 1, "메디컬 빌딩", "지원 테스트 빌딩", 100_000_000L, 500_000L, 10, ValuationStatus.FAIR);

		assertThat(offer.governmentSupportPercent()).isEqualTo(20);
		assertThat(offer.effectivePurchasePrice()).isEqualTo(80_000_000L);
		assertThat(offer.governmentSupportAmount()).isEqualTo(20_000_000L);
		assertThat(offer.purchaseFee()).isEqualTo(1_200_000L);
		assertThat(offer.loanAmount()).isEqualTo(64_000_000L);
		assertThat(offer.cashForLoanPurchase()).isEqualTo(17_200_000L);
	}

	@Test
	void governmentSupportIsRepaidWhenSupportedBuildingIsSoldWithinOneYear() {
		Player player = new Player("support-clawback-test", "hash");
		BuildingOffer offer = new BuildingOffer(
				player, "청주", 1, "원룸", "환수 테스트 원룸", 30_000_000L, 200_000L, 4, ValuationStatus.FAIR);
		OwnedBuilding building = new OwnedBuilding(player, offer);

		assertThat(building.getGovernmentSupportAmount()).isEqualTo(12_000_000L);
		assertThat(building.governmentSupportClawback(player.getElapsedDays())).isEqualTo(12_000_000L);
		assertThat(building.governmentSupportClawbackDaysLeft(player.getElapsedDays())).isEqualTo(365);
		assertThat(building.governmentSupportClawback(player.getElapsedDays() + 365)).isZero();
	}

	@Test
	@Transactional
	void loanPurchaseUsesPropertyCashFlowInsteadOfReputationLimit() {
		Player player = playerRepository.save(new Player("loan-limit-test", "hash"));
		player.claimGovernmentPurchaseSupport("청주");
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

		assertThat(gameService.buyOffer(player.getId(), offer.getId(), true)).isEqualTo("대출구매 완료");
		assertThat(playerRepository.findById(player.getId()).orElseThrow().getCash()).isEqualTo(13_550_000L);
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

		assertThat(gameService.buyOffer(player.getId(), offer.getId(), false)).isEqualTo("현금구매 완료 · 정부지원 40%");
		gameService.ensureOffers(player);

		BuildingOffer remainingOffer = buildingOfferRepository.findById(offer.getId()).orElseThrow();
		assertThat(remainingOffer.getName()).isEqualTo("갱신 테스트 원룸");
		assertThat(gameService.purchaseCooldownDaysLeft(player, remainingOffer)).isPositive();
		assertThat(buildingOfferRepository.findByPlayerAndCityOrderById(player, "청주")).hasSize(1);
	}

	@Test
	@Transactional
	void marketNewsAppliesForTwoOfferRefreshes() {
		Player player = playerRepository.save(new Player("market-news-refresh-test", "hash"));
		player.scheduleMonthlyMarketNews(1, "청주", SettlementService.MARKET_NEWS_RISE);
		player.activateMarketNews();

		assertThat(gameService.marketNewsStatusText(player, "청주")).isEqualTo("부동산 폭등 · 매물갱신 2회");

		buildingTradeService.refreshOffers(player);

		assertThat(player.getActiveMarketNewsRefreshesLeft()).isEqualTo(1);
		assertThat(gameService.marketNewsStatusText(player, "청주")).isEqualTo("부동산 폭등 · 매물갱신 1회");
		assertThat(buildingOfferRepository.findByPlayerAndCityOrderById(player, "청주")).hasSize(4);

		buildingTradeService.refreshOffers(player);

		assertThat(player.hasActiveMarketNewsForCity("청주")).isFalse();
		assertThat(gameService.marketNewsStatusText(player, "청주")).isBlank();
	}

	@Test
	@Transactional
	void qaCanActivateCurrentCityMarketNews() {
		Player player = playerRepository.save(new Player("qa-market-news-test", "hash"));
		player.changeCity("세종");

		assertThat(qaService.activateMarketNewsEvent(player.getId(), SettlementService.MARKET_NEWS_FALL))
				.isEqualTo("세종 부동산 폭락 뉴스");

		assertThat(playerRepository.findById(player.getId()).orElseThrow().hasActiveMarketNewsForCity("세종")).isTrue();
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(player, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
				.isPresent()
				.get()
				.extracting(GameEvent::getTitle)
				.isEqualTo("세종 부동산 폭락 뉴스");
	}

	@Test
	@Transactional
	void stockContentUnlocksTwoDaysAfterSeoulUnlockSchedule() {
		Player player = playerRepository.save(new Player("stock-unlock-test", "hash"));
		player.scheduleStockUnlock(player.getElapsedDays() + 2);

		player.advanceDay();

		assertThat(gameService.stockContentUnlocked(player)).isFalse();

		player.advanceDay();
		assertThat(stockService.activateUnlockNoticeIfDue(player)).isTrue();

		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.isStockContentUnlocked()).isTrue();
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(updatedPlayer, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
				.isPresent()
				.get()
				.extracting(GameEvent::getTitle)
				.isEqualTo("주식 투자 개방");
		assertThat(stockPriceHistoryRepository.countByPlayer(updatedPlayer))
				.isEqualTo(stockService.stocks().size() * (long) com.game.buildingstory.service.StockChartDataService.INITIAL_HISTORY_CANDLES);
	}

	@Test
	@Transactional
	void stockMarketStartsWithTwoYearsOfValidFiveDayCandles() {
		Player player = playerRepository.save(new Player("stock-history-backfill-test", "hash"));
		player.unlockStockContent();

		stockService.ensureMarketInitialized(player);

		var stock = stockService.stocks().stream()
				.filter(item -> item.key().equals("bytecore"))
				.findFirst()
				.orElseThrow();
		var history = stockPriceHistoryRepository
				.findByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(player, stock.key());
		assertThat(history).hasSize(com.game.buildingstory.service.StockChartDataService.INITIAL_HISTORY_CANDLES);
		assertThat(history.getFirst().getElapsedDays()).isEqualTo(player.getElapsedDays() - 725);
		assertThat(history.getLast().getClosePrice()).isEqualTo(stock.basePrice());
		assertThat(history.stream().mapToLong(com.game.buildingstory.domain.StockPriceHistory::getLowPrice).min().orElseThrow())
				.isGreaterThan(stock.basePrice() / 2);
		assertThat(history.stream().mapToLong(com.game.buildingstory.domain.StockPriceHistory::getHighPrice).max().orElseThrow())
				.isLessThan(stock.basePrice() * 2);
		assertThat(history).allSatisfy(candle -> {
			assertThat(candle.getLowPrice()).isPositive();
			assertThat(candle.getHighPrice()).isGreaterThanOrEqualTo(Math.max(candle.getOpenPrice(), candle.getClosePrice()));
			assertThat(candle.getLowPrice()).isLessThanOrEqualTo(Math.min(candle.getOpenPrice(), candle.getClosePrice()));
		});
		for (int index = 1; index < history.size(); index++) {
			assertThat(history.get(index).getElapsedDays() - history.get(index - 1).getElapsedDays()).isEqualTo(5);
		}

		stockService.ensureMarketInitialized(player);
		assertThat(stockPriceHistoryRepository.countByPlayerAndStockKey(player, stock.key()))
				.isEqualTo(com.game.buildingstory.service.StockChartDataService.INITIAL_HISTORY_CANDLES);
		var quote = stockService.selectedStockQuote(player, stock.key());
		assertThat(quote.currentPrice()).isEqualTo(stock.basePrice());
		assertThat(quote.candles()).hasSize(com.game.buildingstory.service.StockChartDataService.INITIAL_HISTORY_CANDLES);
		assertThat(quote.candles()).filteredOn(com.game.buildingstory.service.StockCandleView::visible).hasSize(73);
	}

	@Test
	@Transactional
	void stockUnlockScheduleUsesLateBusanReputationAndNetWorth() {
		Player player = playerRepository.save(new Player("stock-condition-test", "hash"));
		player.addCash(3_000_000_000L);
		player.addReputation(8_250);

		stockService.ensureUnlockSchedule(player);

		assertThat(player.hasStockUnlockSchedule()).isTrue();
	}

	@Test
	@Transactional
	void stockUnlockScheduleRequiresBothLateBusanConditions() {
		Player lowReputation = playerRepository.save(new Player("stock-low-reputation-test", "hash"));
		lowReputation.addCash(3_000_000_000L);
		lowReputation.addReputation(8_249);
		Player lowNetWorth = playerRepository.save(new Player("stock-low-net-worth-test", "hash"));
		lowNetWorth.addCash(2_999_999_999L);
		lowNetWorth.addReputation(8_250);

		stockService.ensureUnlockSchedule(lowReputation);
		stockService.ensureUnlockSchedule(lowNetWorth);

		assertThat(lowReputation.hasStockUnlockSchedule()).isFalse();
		assertThat(lowNetWorth.hasStockUnlockSchedule()).isFalse();
	}

	@Test
	@Transactional
	void stockPricesUpdateEveryFiveElapsedDays() {
		Player player = playerRepository.save(new Player("stock-price-update-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		long initialHistoryCount = stockService.stocks().size()
				* (long) com.game.buildingstory.service.StockChartDataService.INITIAL_HISTORY_CANDLES;
		assertThat(stockPriceHistoryRepository.countByPlayer(player)).isEqualTo(initialHistoryCount);
		assertThat(stockMarketIndexHistoryRepository.count()).isEqualTo(1L);
		assertThat(stockService.marketStatus(player).indexValueText()).isEqualTo("1,000.00");

		for (int i = 0; i < 4; i++) {
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);
		assertThat(stockPriceHistoryRepository.countByPlayer(player)).isEqualTo(initialHistoryCount);
		assertThat(stockMarketIndexHistoryRepository.count()).isEqualTo(1L);

		player.advanceDay();
		stockService.processPriceUpdates(player);

		assertThat(stockPriceHistoryRepository.countByPlayer(player))
				.isEqualTo(initialHistoryCount + stockService.stocks().size());
		assertThat(stockMarketIndexHistoryRepository.count()).isEqualTo(2L);
		assertThat(stockService.stockQuotes(player))
				.hasSize(stockService.stocks().size())
				.allSatisfy(quote -> {
					assertThat(quote.currentPrice()).isPositive();
					assertThat(quote.previousPrice()).isPositive();
					assertThat(quote.candles()).isNotEmpty();
				});
	}

	@Test
	@Transactional
	void confirmedStockIndustryNewsIsStoredAndAffectsForecastAndNextPriceUpdate() {
		Player player = playerRepository.save(new Player("stock-news-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		var bytecore = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		long previousExpectedRevenue = bytecore.getExpectedRevenue();

		stockIndustryNewsService.publish(player, "it-public-cloud", 0);

		assertThat(stockNewsArticleRepository.count()).isEqualTo(1L);
		assertThat(stockService.newsArticles(player)).singleElement()
				.satisfies(article -> {
					assertThat(article.title()).isEqualTo("공공 클라우드 전환 예산 확정");
					assertThat(article.unread()).isTrue();
				});
		long articleId = stockService.newsArticles(player).getFirst().id();
		assertThat(stockService.markNewsRead(player, articleId)).isTrue();
		assertThat(stockService.newsArticles(player).getFirst().unread()).isFalse();
		assertThat(bytecore.getExpectedRevenue()).isGreaterThan(previousExpectedRevenue);
		assertThat(bytecore.getPendingIndustryRevenueImpactBasisPoints()).isEqualTo(180);
		long confirmedExpectedRevenue = bytecore.getExpectedRevenue();
		stockIndustryNewsService.publish(player, "it-export-order", 1);
		assertThat(bytecore.getExpectedRevenue()).isEqualTo(confirmedExpectedRevenue);
		assertThat(bytecore.getPendingIndustryRevenueImpactBasisPoints()).isEqualTo(180);
		assertThat(stockService.marketStatus(player).activeNewsText()).isEqualTo("IT 호재 외 1건 적용중");
		assertThat(stockService.marketStatus(player).activeNewsDirection()).isEqualTo("up");
		assertThat(gameEventRepository.findFirstByPlayerAndStatus(player, com.game.buildingstory.domain.GameEventStatus.ACTIVE))
				.isEmpty();

		for (int i = 0; i < 5; i++) {
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);

		assertThat(stockNewsArticleRepository.findTop20ByPlayerOrderByPublishedElapsedDaysDescIdDesc(player))
				.filteredOn(article -> article.getEventKey().equals("it-public-cloud"))
				.singleElement()
				.extracting(article -> article.getRemainingPriceRefreshes())
				.isEqualTo(2);
		assertThat(stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "bytecore").orElseThrow()
				.getIndustryImpactBasisPoints()).isPositive();
	}

	@Test
	@Transactional
	void confirmedCompanyNewsAffectsOnlyTargetCompanyFinanceAndPrice() {
		Player player = playerRepository.save(new Player("stock-company-news-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		var bytecore = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var neonsoft = listedCompanyRepository.findByPlayerAndStockKey(player, "neonsoft").orElseThrow();
		long bytecoreExpectedNetIncome = bytecore.getExpectedNetIncome();
		long neonsoftExpectedNetIncome = neonsoft.getExpectedNetIncome();

		stockCompanyNewsService.publish(player, "bytecore-efficiency", 0);

		assertThat(bytecore.getExpectedNetIncome()).isGreaterThan(bytecoreExpectedNetIncome);
		assertThat(bytecore.getPendingCompanyOperatingExpenseImpactBasisPoints()).isEqualTo(-80);
		assertThat(bytecore.hasBalancedFinancialPosition()).isTrue();
		assertThat(neonsoft.getExpectedNetIncome()).isEqualTo(neonsoftExpectedNetIncome);
		assertThat(neonsoft.getPendingCompanyOperatingExpenseImpactBasisPoints()).isZero();
		assertThat(stockService.newsArticles(player)).singleElement().satisfies(article -> {
			assertThat(article.category()).isEqualTo("company");
			assertThat(article.scopeText()).isEqualTo(stockService.stocks().stream()
					.filter(stock -> stock.key().equals("bytecore"))
					.findFirst().orElseThrow().name());
		});

		for (int i = 0; i < 5; i++) {
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);

		assertThat(stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "bytecore").orElseThrow()
				.getCompanyImpactBasisPoints()).isPositive();
		assertThat(stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "neonsoft").orElseThrow()
				.getCompanyImpactBasisPoints()).isZero();
	}

	@Test
	@Transactional
	void marketNewsIsShownInUnifiedFeedAndConsumedAfterPriceUpdate() {
		Player player = playerRepository.save(new Player("stock-market-news-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		stockMarketNewsService.publish(player, "market-fiscal-stimulus", 0);

		assertThat(stockMarketNewsService.activePriceEffectPercent(player)).isEqualTo(0.70);
		assertThat(stockService.newsArticles(player)).singleElement().satisfies(article -> {
			assertThat(article.category()).isEqualTo("market");
			assertThat(article.scopeText()).isEqualTo("종합시장");
			assertThat(article.title()).isEqualTo("경기 보강 재정안 확정");
		});
		assertThat(stockService.marketStatus(player).activeNewsText()).isEqualTo("시장 호재 적용중");

		for (int i = 0; i < 5; i++) {
			player.advanceDay();
		}
		stockService.processPriceUpdates(player);

		assertThat(stockNewsArticleRepository.findTop20ByPlayerOrderByPublishedElapsedDaysDescIdDesc(player))
				.filteredOn(article -> article.getEventKey().equals("market-fiscal-stimulus"))
				.singleElement()
				.extracting(article -> article.getRemainingPriceRefreshes())
				.isEqualTo(2);
		assertThat(stockMarketIndexHistoryRepository.count()).isEqualTo(2L);
	}

	@Test
	@Transactional
	void accumulatedExpansionRegimeRaisesActualQuarterlyRevenue() {
		Player player = playerRepository.save(new Player("stock-regime-finance-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		stockMarketRegimeStateRepository.deleteAll();
		stockMarketRegimeStateRepository.flush();
		stockMarketRegimeStateRepository.save(
				new StockMarketRegimeState(player, StockMarketRegime.EXPANSION.name(), 24)
		);
		for (int update = 0; update < 18; update++) {
			stockMarketRegimeService.recordQuarterExposure(player);
		}
		for (int day = 0; day < 90; day++) {
			player.advanceDay();
		}

		assertThat(listedCompanyFinancialService.settlePreviousQuarterIfDue(player)).isEqualTo(15);

		var company = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var report = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company).getFirst();
		assertThat(report.getRevenueGrowthBasisPoints()).isGreaterThanOrEqualTo(145);
		var state = stockMarketRegimeStateRepository.findByPlayer(player).orElseThrow();
		assertThat(state.getQuarterExpansionUpdates()).isZero();
		assertThat(report.getEndingCash() + report.getEndingNonCashAssets())
				.isEqualTo(report.getEndingDebt() + report.getEndingOtherLiabilities() + report.getEndingNetAssets());
	}

	@Test
	@Transactional
	void securitiesAccountAndImmediateTradeUseWonWithFee() {
		Player player = playerRepository.save(new Player("stock-trade-test", "hash"));
		player.addCash(10_000_000L);
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L)).isEqualTo("예수금 부족 · 필요 8만2205원 / 보유 0원");
		assertThat(gameService.sellStock(player.getId(), "bytecore", 5L)).isEqualTo("보유 수량 부족 · 보유 0주 / 매도 요청 5주");

		assertThat(gameService.depositSecuritiesCash(player.getId(), 100_000L)).isEqualTo("10만원 입금");
		assertThat(player.getCash()).isEqualTo(9_900_000L);
		assertThat(player.getSecuritiesCash()).isEqualTo(100_000L);

		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L)).isEqualTo("바이트코어 1주 매수");
		assertThat(player.getSecuritiesCash()).isEqualTo(17_795L);
		assertThat(stockService.stockQuotes(player).stream()
				.filter(quote -> quote.stock().key().equals("bytecore"))
				.findFirst()
				.orElseThrow()
				.quantity()).isEqualTo(1L);

		assertThat(gameService.sellStock(player.getId(), "bytecore", 1L)).isEqualTo("바이트코어 1주 매도");
		assertThat(player.getSecuritiesCash()).isEqualTo(99_590L);

		assertThat(gameService.buyMaxStock(player.getId(), "bytecore")).isEqualTo("바이트코어 1주 매수");
		assertThat(player.getSecuritiesCash()).isEqualTo(17_385L);
		assertThat(gameService.sellAllStock(player.getId(), "bytecore")).isEqualTo("바이트코어 1주 매도");
		assertThat(player.getSecuritiesCash()).isEqualTo(99_180L);
		assertThat(gameService.stockTradeHistories(player)).hasSize(4);
	}

	@Test
	@Transactional
	void pausedPlayerCannotBuyOrSellStock() {
		Player player = playerRepository.save(new Player("paused-stock-trade-test", "hash"));
		player.unlockStockContent();
		player.addSecuritiesCash(1_000_000L);
		stockService.ensureMarketInitialized(player);
		player.pause();

		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L))
				.isEqualTo("일시정지 중에는 주식을 거래할 수 없습니다.");
		assertThat(gameService.sellStock(player.getId(), "bytecore", 1L))
				.isEqualTo("일시정지 중에는 주식을 거래할 수 없습니다.");
		assertThat(ownedStockRepository.findByPlayerAndStockKey(player, "bytecore")).isEmpty();
		assertThat(player.getSecuritiesCash()).isEqualTo(1_000_000L);
	}

	@Test
	@Transactional
	void stockMoneyOperationsRejectOverflowWithoutChangingBalances() {
		Player player = playerRepository.save(new Player("stock-overflow-test", "hash"));
		player.addCash(1_000_000L);
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		player.addSecuritiesCash(Long.MAX_VALUE);
		assertThat(gameService.depositSecuritiesCash(player.getId(), 1L)).isEqualTo("입금액 오류");
		assertThat(player.getCash()).isEqualTo(1_000_000L);
		assertThat(player.getSecuritiesCash()).isEqualTo(Long.MAX_VALUE);
		assertThat(player.spendCash(-1L)).isFalse();
		assertThat(player.getCash()).isEqualTo(1_000_000L);

		assertThat(gameService.buyStock(player.getId(), "bytecore", Long.MAX_VALUE)).isEqualTo("매수 수량 오류");
		assertThat(player.getSecuritiesCash()).isEqualTo(Long.MAX_VALUE);
	}

	@Test
	@Transactional
	void staleTickRequestDoesNotAdvancePlayerTwice() {
		Player player = playerRepository.save(new Player("stale-tick-test", "hash"));
		player.completeStory();
		player.scheduleNoMonthlyStockNews();
		int renderedElapsedDays = player.getElapsedDays();

		gameService.tick(player.getId(), false, renderedElapsedDays);
		assertThat(player.getElapsedDays()).isEqualTo(renderedElapsedDays + 1);

		assertThat(gameService.tick(player.getId(), false, renderedElapsedDays)).isBlank();
		assertThat(player.getElapsedDays()).isEqualTo(renderedElapsedDays + 1);
	}

	@Test
	@Transactional
	void sharedTickAdvancesDateOnceAndUpdatesDueStockPrices() {
		Player player = playerRepository.save(new Player("shared-stock-tick-test", "hash"));
		player.unlockStockContent();
		player.scheduleNoMonthlyStockNews();
		stockService.ensureMarketInitialized(player);
		for (int i = 0; i < 4; i++) {
			player.advanceDay();
		}
		int elapsedDaysBeforeTick = player.getElapsedDays();
		long historyCountBeforeTick = stockPriceHistoryRepository.countByPlayer(player);

		gameService.tick(player.getId(), true);

		assertThat(player.getElapsedDays()).isEqualTo(elapsedDaysBeforeTick + 1);
		assertThat(stockPriceHistoryRepository.countByPlayer(player))
				.isEqualTo(historyCountBeforeTick + stockService.stocks().size());
	}

	@Test
	@Transactional
	void activeAuctionStopsTickBeforeDateAdvances() {
		Player player = playerRepository.save(new Player("auction-tick-gate-test", "hash"));
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
				player, "청주", 1, "원룸", "경매 원룸", 100_000_000L, 300_000L, 10));
		int elapsedDaysBeforeTick = player.getElapsedDays();

		assertThat(gameService.tick(player.getId(), false)).isEqualTo("AUCTION:" + auction.getId());
		assertThat(player.getElapsedDays()).isEqualTo(elapsedDaysBeforeTick);
	}

	@Test
	void naturalKeyUniqueConstraintsAreCreated() {
		Integer constraintCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
				WHERE CONSTRAINT_TYPE = 'UNIQUE'
				  AND CONSTRAINT_NAME IN (
				      'UK_OWNED_STOCK_PLAYER_KEY',
				      'UK_OWNED_SECRETARY_PLAYER_KEY',
				      'UK_OWNED_GIFT_PLAYER_KEY',
				      'UK_OWNED_LUXURY_PLAYER_KEY',
				      'UK_SECRETARY_TENANT_PLAYER_KEY',
				      'UK_GAME_EVENT_PLAYER_KEY',
				      'UK_PURCHASE_COOLDOWN_PLAYER_SLOT'
				  )
				""", Integer.class);

		assertThat(constraintCount).isEqualTo(7);
	}

	@Test
	@Transactional
	void stockProfitTextShowsPercentAndWonAmount() {
		Player player = playerRepository.save(new Player("stock-profit-text-test", "hash"));
		player.addCash(10_000_000L);
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		gameService.depositSecuritiesCash(player.getId(), 100_000L);
		gameService.buyStock(player.getId(), "bytecore", 1L);
		stockPriceHistoryRepository.save(new StockPriceHistory(player, "bytecore", 82_000L, 92_660L, 82_000L, 92_660L, 100L));

		assertThat(stockService.stockQuotes(player).stream()
				.filter(quote -> quote.stock().key().equals("bytecore"))
				.findFirst()
				.orElseThrow()
				.valuationProfitText()).isEqualTo("(+12.7%) +1만455원");
		assertThat(stockService.holdingSummary(player).totalProfitText()).isEqualTo("(+12.7%) +1만455원");
	}

	@Test
	@Transactional
	void stockAccountingIncludesFeesAndAllocatesCostBasisOnPartialSale() {
		Player player = playerRepository.save(new Player("stock-accounting-test", "hash"));
		player.addCash(1_000_000L);
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		gameService.depositSecuritiesCash(player.getId(), 500_000L);
		gameService.buyStock(player.getId(), "bytecore", 2L);
		gameService.sellStock(player.getId(), "bytecore", 1L);

		var holding = ownedStockRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		assertThat(holding.getQuantity()).isEqualTo(1L);
		assertThat(holding.getTotalCostBasis()).isEqualTo(82_205L);
		assertThat(holding.getAveragePrice()).isEqualTo(82_205L);

		var histories = stockTradeHistoryRepository
				.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(player, 1);
		assertThat(histories).hasSize(2);
		assertThat(histories.get(0).getTradeType()).isEqualTo("매도");
		assertThat(histories.get(0).getCostBasis()).isEqualTo(82_205L);
		assertThat(histories.get(0).getRealizedProfit()).isEqualTo(-410L);
		assertThat(histories.get(1).getCostBasis()).isEqualTo(164_410L);

		var summary = stockService.holdingSummary(player);
		assertThat(summary.totalProfit()).isEqualTo(-205L);
		assertThat(summary.totalRealizedProfit()).isEqualTo(-410L);
		assertThat(summary.totalFees()).isEqualTo(615L);
	}

	@Test
	@Transactional
	void listedCompaniesInitializeWithConservedShareComposition() {
		Player player = playerRepository.save(new Player("listed-company-init-test", "hash"));
		player.unlockStockContent();

		stockService.ensureMarketInitialized(player);

		assertThat(listedCompanyRepository.findByPlayer(player)).hasSize(15);
		var safeCompany = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		assertThat(safeCompany.getIssuedShares()).isEqualTo(300_000_000L);
		assertThat(safeCompany.getFounderShares()).isEqualTo(120_000_000L);
		assertThat(safeCompany.getInstitutionalShares()).isEqualTo(60_000_000L);
		assertThat(safeCompany.getMarketParticipantShares()).isEqualTo(120_000_000L);
		assertThat(safeCompany.getCorporatePlayerShares()).isZero();
		assertThat(safeCompany.hasConservedShares(0)).isTrue();
		assertThat(safeCompany.hasBalancedFinancialPosition()).isTrue();

		var aggressiveCompany = listedCompanyRepository.findByPlayerAndStockKey(player, "cloudnine").orElseThrow();
		assertThat(aggressiveCompany.getFounderShares()).isEqualTo(6_000_000L);
		assertThat(aggressiveCompany.getInstitutionalShares()).isEqualTo(4_500_000L);
		assertThat(aggressiveCompany.getMarketParticipantShares()).isEqualTo(19_500_000L);
		assertThat(aggressiveCompany.hasConservedShares(0)).isTrue();
		assertThat(aggressiveCompany.hasBalancedFinancialPosition()).isTrue();
	}

	@Test
	@Transactional
	void stockTradesMoveSharesWithoutChangingIssuedShareTotal() {
		Player player = playerRepository.save(new Player("listed-company-trade-test", "hash"));
		player.unlockStockContent();
		player.addSecuritiesCash(1_000_000L);
		stockService.ensureMarketInitialized(player);

		assertThat(stockService.buyStock(player, "bytecore", 3L)).isEqualTo("바이트코어 3주 매수");
		var companyAfterBuy = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var holdingAfterBuy = ownedStockRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		assertThat(companyAfterBuy.getMarketParticipantShares()).isEqualTo(119_999_997L);
		assertThat(companyAfterBuy.hasConservedShares(holdingAfterBuy.getQuantity())).isTrue();

		assertThat(stockService.sellStock(player, "bytecore", 2L)).isEqualTo("바이트코어 2주 매도");
		var companyAfterSell = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var holdingAfterSell = ownedStockRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		assertThat(companyAfterSell.getMarketParticipantShares()).isEqualTo(119_999_999L);
		assertThat(companyAfterSell.hasConservedShares(holdingAfterSell.getQuantity())).isTrue();
	}

	@Test
	@Transactional
	void stockOrderCannotExceedCurrentLiquidity() {
		Player player = playerRepository.save(new Player("stock-liquidity-limit-test", "hash"));
		player.unlockStockContent();
		player.addSecuritiesCash(1_000_000_000_000L);
		stockService.ensureMarketInitialized(player);

		assertThat(stockService.buyStock(player, "bytecore", 1_500_001L))
				.isEqualTo("매수 유동성 부족 · 현재 체결 가능 1500000주");
		assertThat(ownedStockRepository.findByPlayerAndStockKey(player, "bytecore")).isEmpty();
	}

	@Test
	@Transactional
	void actualQuarterSettlementPaysDividendAndAppliesExDividendPrice() {
		Player player = playerRepository.save(new Player("stock-dividend-test", "hash"));
		player.unlockStockContent();
		player.addSecuritiesCash(100_000_000L);
		stockService.ensureMarketInitialized(player);
		assertThat(stockService.buyStock(player, "bytecore", 100L)).isEqualTo("바이트코어 100주 매수");
		long priceBeforeSettlement = stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "bytecore")
				.orElseThrow().getClosePrice();
		long cashBeforeSettlement = player.getSecuritiesCash();
		for (int day = 0; day < 90; day++) {
			player.advanceDay();
		}

		listedCompanyFinancialService.settlePreviousQuarterIfDue(player);

		var company = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var report = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company).getFirst();
		assertThat(report.getDividendPerShare()).isPositive();
		assertThat(player.getSecuritiesCash())
				.isEqualTo(cashBeforeSettlement + report.getDividendPerShare() * 100L);
		assertThat(stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "bytecore")
				.orElseThrow().getClosePrice())
				.isEqualTo(priceBeforeSettlement - report.getDividendPerShare());
		assertThat(stockTradeHistoryRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(player, 1))
				.anyMatch(history -> history.getTradeType().equals("배당"));
	}

	@Test
	@Transactional
	void listedCompanyMarketCapsStayAboveLateSeoulPersonalCapitalScale() {
		Player player = playerRepository.save(new Player("listed-company-market-cap-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		var quotes = stockService.stockQuotes(player);
		long minimumMarketCap = quotes.stream()
				.mapToLong(quote -> quote.currentPrice() * quote.stock().issuedShares())
				.min()
				.orElseThrow();
		long maximumMarketCap = quotes.stream()
				.mapToLong(quote -> quote.currentPrice() * quote.stock().issuedShares())
				.max()
				.orElseThrow();

		assertThat(minimumMarketCap).isEqualTo(1_520_000_000_000L);
		assertThat(maximumMarketCap).isEqualTo(24_600_000_000_000L);
	}

	@Test
	@Transactional
	void stockQuotesIncludeCompanyOverviewRecentEarningsAndValuation() {
		Player player = playerRepository.save(new Player("stock-company-detail-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);

		var quotes = stockService.stockQuotes(player);
		assertThat(quotes).hasSize(15);
		assertThat(quotes).allSatisfy(quote -> {
			assertThat(quote.companyDetail().chiefExecutive()).isNotBlank();
			assertThat(quote.companyDetail().mainRevenueSource()).isNotBlank();
			assertThat(quote.companyDetail().recentQuarters()).hasSize(4);
			assertThat(quote.companyDetail().cashText()).isNotBlank();
			assertThat(quote.companyDetail().earningsPerShareText()).isNotBlank();
			assertThat(quote.companyDetail().priceEarningsRatioText()).isNotBlank();
		});
	}

	@Test
	@Transactional
	void quarterlySettlementIsStoredOnceAndKeepsBalanceSheetBalanced() {
		Player player = playerRepository.save(new Player("listed-company-quarter-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		for (int day = 0; day < 90; day++) {
			player.advanceDay();
		}

		assertThat(player.getMonth()).isEqualTo(4);
		assertThat(player.getDay()).isEqualTo(1);
		assertThat(listedCompanyFinancialService.settlePreviousQuarterIfDue(player)).isEqualTo(15);
		assertThat(listedCompanyFinancialService.settlePreviousQuarterIfDue(player)).isZero();

		var company = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var report = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company)
				.getFirst();
		assertThat(report.getFiscalYear()).isEqualTo(1);
		assertThat(report.getFiscalQuarter()).isEqualTo(1);
		assertThat(report.getRevenue())
				.isEqualTo(report.getCostOfRevenue() + report.getOperatingExpenses() + report.getOperatingProfit());
		assertThat(report.getEndingCash() + report.getEndingNonCashAssets())
				.isEqualTo(report.getEndingDebt() + report.getEndingOtherLiabilities() + report.getEndingNetAssets());
		assertThat(company.getLatestSettledFiscalPeriod()).isZero();
		assertThat(company.hasBalancedFinancialPosition()).isTrue();
		assertThat(listedCompanyQuarterlyReportRepository.findByListedCompanyOrderByFiscalPeriodIndexDesc(company)).hasSize(5);
	}

	@Test
	@Transactional
	void baselineFinancialHistoryProvidesTtmValuationWithoutChangingPlayerMoney() {
		Player player = playerRepository.save(new Player("stock-baseline-financial-test", "hash"));
		player.unlockStockContent();
		long cashBefore = player.getCash();
		long securitiesCashBefore = player.getSecuritiesCash();

		stockService.ensureMarketInitialized(player);

		var company = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var reports = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company);
		assertThat(reports).hasSize(4).allMatch(report -> report.isBaselineHistory());
		assertThat(reports).allSatisfy(report -> {
			assertThat(report.getRevenue())
					.isEqualTo(report.getCostOfRevenue() + report.getOperatingExpenses() + report.getOperatingProfit());
			assertThat(report.getEndingCash() + report.getEndingNonCashAssets())
					.isEqualTo(report.getEndingDebt() + report.getEndingOtherLiabilities() + report.getEndingNetAssets());
		});

		var valuation = listedCompanyValuationService.latest(company).orElseThrow();
		long trailingNetIncome = reports.stream().mapToLong(report -> report.getNetIncome()).sum();
		assertThat(valuation.getEarningsPerShare()).isEqualTo(trailingNetIncome / company.getIssuedShares());
		assertThat(valuation.getFairValueLower()).isPositive().isLessThan(valuation.getFairValueBase());
		assertThat(valuation.getFairValueUpper()).isGreaterThan(valuation.getFairValueBase());
		assertThat(player.getCash()).isEqualTo(cashBefore);
		assertThat(player.getSecuritiesCash()).isEqualTo(securitiesCashBefore);
	}

	@Test
	@Transactional
	void existingPartialFinancialHistoryIsBackfilledToFourQuarters() {
		Player player = playerRepository.save(new Player("stock-partial-history-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		for (int day = 0; day < 90; day++) {
			player.advanceDay();
		}
		listedCompanyFinancialService.settlePreviousQuarterIfDue(player);

		var company = listedCompanyRepository.findByPlayerAndStockKey(player, "bytecore").orElseThrow();
		var reports = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company);
		listedCompanyValuationSnapshotRepository.deleteAll();
		listedCompanyQuarterlyReportRepository.deleteAll(
				reports.stream().filter(report -> report.isBaselineHistory()).toList());

		listedCompanyFinancialService.ensureBaselineHistory(player);

		var backfilled = listedCompanyQuarterlyReportRepository
				.findByListedCompanyOrderByFiscalPeriodIndexDesc(company);
		assertThat(backfilled).hasSize(4);
		assertThat(backfilled).filteredOn(report -> !report.isBaselineHistory()).hasSize(1);
		assertThat(listedCompanyValuationService.latest(company)).isPresent();
	}

	@Test
	@Transactional
	void priceV2StoresConsistentOhlcAndFactorAttribution() {
		Player player = playerRepository.save(new Player("stock-price-v2-test", "hash"));
		player.unlockStockContent();
		stockService.ensureMarketInitialized(player);
		for (int day = 0; day < 5; day++) {
			player.advanceDay();
		}

		stockService.processPriceUpdates(player);

		StockPriceHistory candle = stockPriceHistoryRepository
				.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, "bytecore").orElseThrow();
		assertThat(candle.getHighPrice()).isGreaterThanOrEqualTo(Math.max(candle.getOpenPrice(), candle.getClosePrice()));
		assertThat(candle.getLowPrice()).isLessThanOrEqualTo(Math.min(candle.getOpenPrice(), candle.getClosePrice()));
		assertThat(candle.getLowPrice()).isPositive();
		assertThat(Math.abs(candle.getMarketImpactBasisPoints())
				+ Math.abs(candle.getIndustryImpactBasisPoints())
				+ Math.abs(candle.getEarningsImpactBasisPoints())
				+ Math.abs(candle.getValuationImpactBasisPoints())
				+ Math.abs(candle.getNoiseImpactBasisPoints())).isPositive();
		assertThat(candle.getMarketImpactBasisPoints()
				+ candle.getIndustryImpactBasisPoints()
				+ candle.getValuationImpactBasisPoints()
				+ candle.getNoiseImpactBasisPoints()).isEqualTo(candle.getPathImpactBasisPoints());
	}

	@Test
	@Transactional
	void generalMarketPurchaseCannotExceedTwentyPercentOwnership() {
		Player player = playerRepository.save(new Player("stock-market-limit-test", "hash"));
		player.unlockStockContent();
		player.addSecuritiesCash(1_000_000_000_000_000_000L);
		stockService.ensureMarketInitialized(player);

		for (int refresh = 0; refresh < 40; refresh++) {
			assertThat(gameService.buyStock(player.getId(), "bytecore", 1_500_000L))
					.isEqualTo("바이트코어 1500000주 매수");
			if (refresh < 39) {
				stockLiquidityService.refreshAll(player);
			}
		}
		assertThat(gameService.buyStock(player.getId(), "bytecore", 1L))
				.isEqualTo("일반시장 매집 한도 초과 · 최대 지분 20%");
		assertThat(gameService.buyMaxStock(player.getId(), "bytecore"))
				.isEqualTo("일반시장 매집 한도 도달 · 최대 지분 20%");

		var quote = stockService.stockQuotes(player).stream()
				.filter(item -> item.stock().key().equals("bytecore"))
				.findFirst()
				.orElseThrow();
		assertThat(quote.quantity()).isEqualTo(60_000_000L);
		assertThat(quote.ownershipPercentText()).isEqualTo("20.00%");
		assertThat(quote.remainingMarketBuyQuantity()).isZero();
	}

	@Test
	@Transactional
	void stockViewTickDefersDueCityEventWithoutPausing() {
		Player player = playerRepository.save(new Player("stock-view-defer-event-test", "hash"));
		player.completeStory();
		player.unlockStockContent();
		player.scheduleNoMonthlyStockNews();

		assertThat(gameService.tick(player.getId(), true)).isBlank();
		assertThat(gameService.tick(player.getId(), true)).isBlank();

		Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
		assertThat(updatedPlayer.getMonth()).isEqualTo(1);
		assertThat(updatedPlayer.getDay()).isEqualTo(3);
		assertThat(updatedPlayer.isPaused()).isFalse();
		assertThat(gameService.activeEvent(updatedPlayer)).isPresent();
	}

	@Test
	@Transactional
	void pausedStockViewStaysPausedWhileCityEventIsDeferred() throws Exception {
		Player player = playerRepository.save(new Player("paused-stock-view-test", "hash"));
		player.completeStory();
		player.unlockStockContent();
		player.scheduleNoMonthlyStockNews();
		gameService.tick(player.getId(), true);
		gameService.tick(player.getId(), true);
		gameService.togglePause(player.getId());
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKeys.PLAYER_ID, player.getId());

		String html = mockMvc.perform(get("/main").param("view", "stocks").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(html).contains("<body class=\"game-paused view-stocks\"");
		assertThat(html).contains("data-player-paused=\"true\"");
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

		assertThat(cheongjuRoom.monthlyRent()).isEqualTo(375_000L);
		assertThat(cheongjuRoom.tradeCooldownDays()).isEqualTo(4);
		assertThat(seoulFinal.marketPrice()).isEqualTo(220_000_000_000L);
		assertThat(seoulFinal.monthlyRent()).isEqualTo(1_833_333_300L);
		assertThat(seoulFinal.tradeCooldownDays()).isEqualTo(185);
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
		assertThat(gameService.effectiveMoveOutChancePercentText(player, "test-city")).isEqualTo("18%");
		assertThat(gameService.effectiveRepairRequestChancePercentText(player, "test-city")).isEqualTo("10%");
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

		assertThat(gameService.auctionDisplayTypeName(auction)).isEqualTo("물류센터");
		assertThat(gameService.auctionDisplayName(auction)).isEqualTo("송도 물류센터");
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

		assertThat(gameService.bidAuction(player.getId(), auction.getId(), 95)).isEqualTo("현금 부족");
		assertThat(auctionEventRepository.findById(auction.getId()).orElseThrow().getStatus()).isEqualTo(AuctionStatus.ACTIVE);
	}

	@Test
	void auctionPriceAppliesGovernmentSupportBeforePurchaseFee() {
		Player player = new Player("auction-support-price-test", "hash");
		AuctionEvent auction = new AuctionEvent(
				player, "대전", 1, "다가구주택", "경매 다가구주택", 100_000_000L, 500_000L, 10);

		assertThat(auction.effectiveBidPrice(88)).isEqualTo(52_800_000L);
		assertThat(auction.totalPurchasePrice(88)).isEqualTo(53_592_000L);

		player.claimGovernmentPurchaseSupport("대전");
		assertThat(auction.effectiveBidPrice(88)).isEqualTo(88_000_000L);
		assertThat(auction.totalPurchasePrice(88)).isEqualTo(89_320_000L);
	}

	@Test
	void incheonAuctionUsesTwentyPercentGovernmentSupport() {
		Player player = new Player("incheon-auction-support-test", "hash");
		AuctionEvent auction = new AuctionEvent(
				player, "인천", 1, "메디컬 빌딩", "경매 메디컬 빌딩", 100_000_000L, 500_000L, 10);

		assertThat(auction.governmentSupportPercent()).isEqualTo(20);
		assertThat(auction.effectiveBidPrice(88)).isEqualTo(70_400_000L);
		assertThat(auction.totalPurchasePrice(88)).isEqualTo(71_456_000L);
	}

	@Test
	@Transactional
	void auctionUsesRebalancedChanceAndOnePercentFailureDeposit() {
		Player player = playerRepository.save(new Player("auction-balance-test", "hash"));
		player.addCash(100_000_000L);
		player.claimGovernmentPurchaseSupport("청주");
		AuctionEvent auction = auctionEventRepository.save(new AuctionEvent(
				player, "청주", 1, "원룸", "경매 균형 테스트", 30_000_000L, 200_000L, 4));

		gameService.bidAuction(player.getId(), auction.getId(), 95);

		AuctionEvent resolved = auctionEventRepository.findById(auction.getId()).orElseThrow();
		assertThat(resolved.getSuccessChance()).isEqualTo(70);
		assertThat(com.game.buildingstory.domain.EconomyBalanceRules.auctionDeposit(30_000_000L * 95 / 100))
				.isEqualTo(285_000L);
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

	@Test
	@Transactional
	void secretarySalaryCannotMakeCashNegativeAndTwoMissedMonthsEndContract() {
		Player player = playerRepository.save(new Player("secretary-arrears-test", "hash"));
		OwnedSecretary secretary = ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1));
		secretary.assignTo("청주");

		secretaryOperationsService.processSalaries(player);

		assertThat(player.getCash()).isZero();
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getUnpaidSalaryMonths()).isEqualTo(1);
		assertThat(ownedSecretaryRepository.findById(secretary.getId()).orElseThrow().getAssignedCity()).isNull();

		secretaryOperationsService.processSalaries(player);

		assertThat(player.getCash()).isZero();
		assertThat(ownedSecretaryRepository.findById(secretary.getId())).isEmpty();
	}

	@Test
	@Transactional
	void securedLoanChargesMonthlyInterestAndResetsDelinquency() {
		Player player = playerRepository.save(new Player("loan-interest-test", "hash"));
		player.addCash(100_000L);
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 1, "원룸", "담보 원룸", 30_000_000L, 30_000_000L, 250_000L, 5));
		Loan loan = loanRepository.save(new Loan(player, building, 18_000_000L));

		loanService.processMaturity(player);

		assertThat(player.getCash()).isEqualTo(28_000L);
		assertThat(loanRepository.findById(loan.getId()).orElseThrow().getRemainingMonths()).isEqualTo(23);
		assertThat(loanRepository.findById(loan.getId()).orElseThrow().getDelinquentMonths()).isZero();
	}

	@Test
	@Transactional
	void securedLoanForeclosesAfterTwoMissedInterestPayments() {
		Player player = playerRepository.save(new Player("loan-foreclosure-test", "hash"));
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 1, "원룸", "연체 원룸", 30_000_000L, 30_000_000L, 250_000L, 5));
		loanRepository.save(new Loan(player, building, 18_000_000L));

		loanService.processMaturity(player);
		loanService.processMaturity(player);

		assertThat(loanRepository.findByPlayer(player)).isEmpty();
		assertThat(ownedBuildingRepository.findById(building.getId())).isEmpty();
		assertThat(player.getCash()).isEqualTo(9_000_000L);
	}

	@Test
	@Transactional
	void securedLoanDefersForeclosureWhenSecretaryEventReferencesBuilding() {
		Player player = playerRepository.save(new Player("loan-event-reference-test", "hash"));
		OwnedBuilding building = ownedBuildingRepository.save(new OwnedBuilding(
				player, "청주", 1, "원룸", "이벤트 원룸", 30_000_000L, 30_000_000L, 250_000L, 5));
		Loan loan = loanRepository.save(new Loan(player, building, 18_000_000L));
		secretaryTenantEventRepository.save(new SecretaryTenantEvent(
				player, building, "secretary-1", "청주", player.getElapsedDays()));

		loanService.processMaturity(player);
		String notice = loanService.processMaturity(player);

		assertThat(notice).contains("담보 처분 보류");
		assertThat(loanRepository.findById(loan.getId())).isPresent();
		assertThat(ownedBuildingRepository.findById(building.getId())).isPresent();
	}

}
