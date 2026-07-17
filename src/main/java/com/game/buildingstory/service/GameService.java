package com.game.buildingstory.service;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventDefinition;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GameService {
    /*
     * GameService는 "게임 전체 진행 순서"를 조립하는 파사드 서비스다.
     *
     * 컨트롤러가 여러 세부 서비스를 직접 호출하면 화면 요청마다 규칙 순서가 달라질 수 있다.
     * 그래서 날짜 진행, 이벤트 처리, 구매/판매 같은 외부 진입점은 대부분 이 클래스에 모아두고,
     * 실제 세부 계산은 BuildingTradeService, SettlementService, StockService 같은 전용 서비스에 위임한다.
     */
    private static final long SIDE_JOB_REWARD = 10_000L;
    private static final int RECORD_RETENTION_DAYS = 62;
    private final PlayerRepository playerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final SecretaryTenantEventRepository secretaryTenantEventRepository;
    private final GameEventRepository gameEventRepository;
    private final GameEventCatalog gameEventCatalog;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryCatalog secretaryCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final ShopService shopService;
    private final AuctionService auctionService;
    private final BuildingTradeService buildingTradeService;
    private final LoanService loanService;
    private final SecretaryOperationsService secretaryOperationsService;
    private final SettlementService settlementService;
    private final EventFlowService eventFlowService;
    private final StockService stockService;
    private final CityMarketIndexService cityMarketIndexService;

    public GameService(
            PlayerRepository playerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            SecretaryTenantEventRepository secretaryTenantEventRepository,
            GameEventRepository gameEventRepository,
            GameEventCatalog gameEventCatalog,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            SecretaryCatalog secretaryCatalog,
            SecretaryTenantEventService secretaryTenantEventService,
            ShopService shopService,
            AuctionService auctionService,
            BuildingTradeService buildingTradeService,
            LoanService loanService,
            SecretaryOperationsService secretaryOperationsService,
            SettlementService settlementService,
            EventFlowService eventFlowService,
            StockService stockService,
            CityMarketIndexService cityMarketIndexService
    ) {
        this.playerRepository = playerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.secretaryTenantEventRepository = secretaryTenantEventRepository;
        this.gameEventRepository = gameEventRepository;
        this.gameEventCatalog = gameEventCatalog;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
        this.secretaryCatalog = secretaryCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.shopService = shopService;
        this.auctionService = auctionService;
        this.buildingTradeService = buildingTradeService;
        this.loanService = loanService;
        this.secretaryOperationsService = secretaryOperationsService;
        this.settlementService = settlementService;
        this.eventFlowService = eventFlowService;
        this.stockService = stockService;
        this.cityMarketIndexService = cityMarketIndexService;
    }

    @Transactional(readOnly = true)
    public Player player(long playerId) {
        return playerRepository.findById(playerId).orElseThrow();
    }

    @Transactional
    public void completeStory(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (!player.isStorySeen()) {
            // 스토리 완료는 메인 게임 최초 진입점이다.
            // 플레이어 상태 초기화, 첫 건물 지급, 첫 매물 생성이 한 트랜잭션에서 끝나야 중간 저장 오류가 없다.
            player.completeStory();
            playerRepository.save(player);
            BuildingSpec starter = buildingCatalog.firstCheongjuRoom().orElseThrow();
            ownedBuildingRepository.save(new OwnedBuilding(player, starter.city(), starter.slot(), starter.typeName(), starter.name(), starter.marketPrice(), 0L, starter.monthlyRent(), starter.tradeCooldownDays()));
            buildingTradeService.refreshOffers(player);
        }
    }

    @Transactional
    public String sideJob(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        if (!player.canDoSideJobToday()) {
            return "부업은 하루에 한 번만 가능";
        }
        player.addSideIncome(SIDE_JOB_REWARD);
        return "부업 수익 10,000원 획득";
    }

    @Transactional
    public String tick(long playerId) {
        return tick(playerId, false);
    }

    @Transactional
    public String tick(long playerId, boolean deferCityEvents) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return processTick(player, deferCityEvents);
    }

    @Transactional
    public String tick(long playerId, boolean deferCityEvents, int expectedElapsedDays) {
        // 화면이 렌더링될 때 본 elapsedDays와 DB의 최신 값을 비교한다.
        // 다른 탭이 이미 하루를 진행했다면 값이 달라지므로 같은 화면에서 온 오래된 요청은 아무 작업도 하지 않는다.
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        if (player.getElapsedDays() != expectedElapsedDays) {
            return "";
        }
        return processTick(player, deferCityEvents);
    }

    private String processTick(Player player, boolean deferCityEvents) {
        if (player.isPaused()) {
            return "";
        }
        /*
         * tick은 브라우저의 자동 시간 루프가 호출하는 "하루 진행" 함수다.
         *
         * deferCityEvents=true는 주식 화면에서 온 tick을 의미한다.
         * 주식 화면에서도 날짜, 월세, 주가 갱신은 진행되어야 하지만, 도시 이벤트 모달이
         * 주식 화면을 덮어버리면 사용자가 주식 흐름을 잃는다. 그래서 도시 이벤트는 생성만 해두고
         * 응답 신호는 보내지 않는다. 사용자가 도시 화면으로 돌아오면 이미 생성된 이벤트가 표시된다.
         */
        Optional<AuctionEvent> existingAuction = deferCityEvents ? Optional.empty() : activeAuction(player);
        if (existingAuction.isPresent()) {
            return "AUCTION:" + existingAuction.get().getId();
        }
        // 날짜는 반드시 한 번만 증가해야 한다. 프론트가 중복 tick을 막고, 서버도 이 메서드 한 곳에서만 advanceDay를 호출한다.
        player.advanceDay();
        // 공실 건물의 수리 요청처럼 매일 자연스럽게 정리되는 상태를 먼저 정리한다.
        settlementService.clearVacantRepairRequests(player);
        // 일일 정산은 월세, 월급, 대출, 월말 기록 같은 경제 흐름을 처리한다.
        String dailyNotice = settlementService.runDailySettlement(player);
        dailyNotice = appendNotice(dailyNotice, eventFlowService.processAutoResignation(player));
        if (player.getElapsedDays() >= player.getNextOfferRefreshDay()) {
            // 매물 갱신은 elapsedDays 기준이다. 월/일이 1월로 순환해도 쿨다운이 꼬이지 않는다.
            buildingTradeService.refreshOffers(player);
            player.scheduleNextOfferRefresh();
        }
        refreshTitle(player);
        Optional<AuctionEvent> activeAuction = activeAuction(player);
        if (activeAuction.isPresent()) {
            return "AUCTION:" + activeAuction.get().getId();
        }
        if (stockService.activateUnlockNoticeIfDue(player)) {
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
        }
        // 주식 가격은 주식 화면과 도시 화면 모두에서 같은 시간 축을 공유한다.
        stockService.processPriceUpdates(player);
        if (stockService.activateIndustryNewsIfDue(player)) {
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
        }
        if (deferCityEvents) {
            // 주식 화면에서는 도시 이벤트를 DB에만 준비하고, 클라이언트에는 EVENT 응답을 보내지 않는다.
            gameEventCatalog.findDueEvent(player.getMonth(), player.getDay())
                    .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(player, definition.key()))
                    .ifPresent(definition -> eventFlowService.activateEvent(player, definition, false));
            return dailyNotice;
        }
        Optional<GameEvent> activeEvent = activeEvent(player);
        if (activeEvent.isPresent()) {
            return "EVENT:" + activeEvent.get().getId();
        }
        secretaryTenantEventService.evaluate(player, activeAuction(player).isPresent());
        activeEvent = activeEvent(player);
        if (activeEvent.isPresent()) {
            return "EVENT:" + activeEvent.get().getId();
        }
        Optional<GameEventDefinition> dueEvent = gameEventCatalog.findDueEvent(player.getMonth(), player.getDay())
                .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(player, definition.key()))
                .map(definition -> {
                    eventFlowService.activateEvent(player, definition);
                    return definition;
                });
        if (dueEvent.isPresent()) {
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
        }
        return auctionService.tryActivate(player)
                .map(auction -> "AUCTION:" + auction.getId())
                .orElse(dailyNotice);
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(Player player) {
        return eventFlowService.activeEvent(player);
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(long playerId) {
        return eventFlowService.activeEvent(playerId);
    }

    @Transactional
    public void evaluateSecretaryTenantEvents(Player player) {
        secretaryTenantEventService.evaluate(player, activeAuction(player).isPresent());
    }

    @Transactional
    public Optional<AuctionEvent> activeAuction(Player player) {
        return auctionService.activeAuction(player);
    }

    @Transactional
    public void completeEvent(long playerId, long eventId) {
        eventFlowService.completeEvent(playerId, eventId);
    }

    @Transactional
    public void cancelEvent(long playerId, long eventId) {
        eventFlowService.cancelEvent(playerId, eventId);
    }

    @Transactional
    public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
        return buildingTradeService.buyOffer(playerId, offerId, loanPurchase);
    }

    @Transactional
    public String bidAuction(long playerId, long auctionId, int rate) {
        return auctionService.bid(playerId, auctionId, rate);
    }

    @Transactional
    public String cancelAuction(long playerId, long auctionId) {
        return auctionService.cancel(playerId, auctionId);
    }

    @Transactional
    public void completeAuctionResult(long playerId, long auctionId) {
        auctionService.completeResult(playerId, auctionId);
    }

    @Transactional
    public String resign(long playerId) {
        return eventFlowService.resign(playerId);
    }

    @Transactional
    public String sellBuilding(long playerId, long buildingId) {
        return buildingTradeService.sellBuilding(playerId, buildingId);
    }

    @Transactional
    public String repairBuilding(long playerId, long buildingId) {
        return buildingTradeService.repairBuilding(playerId, buildingId);
    }

    @Transactional
    public String hireFirstSecretary(long playerId) {
        return secretaryOperationsService.hireFirstSecretary(playerId);
    }

    @Transactional(readOnly = true)
    public List<BuildingOffer> offers(Player player) {
        return buildingTradeService.offers(player);
    }

    @Transactional(readOnly = true)
    public List<OwnedBuilding> ownedBuildings(Player player) {
        return buildingTradeService.ownedBuildings(player);
    }

    @Transactional(readOnly = true)
    public List<Loan> loans(Player player) {
        return loanService.loans(player);
    }

    @Transactional(readOnly = true)
    public long remainingLoanRepayment(Player player) {
        return loanService.remainingRepayment(player);
    }

    @Transactional(readOnly = true)
    public long remainingLoanPrincipal(Player player) {
        return loanService.remainingPrincipal(player);
    }

    @Transactional(readOnly = true)
    public long availableLoanLimit(Player player) {
        return loanService.availableLoanLimit(player);
    }

    @Transactional(readOnly = true)
    public int purchaseCooldownDaysLeft(Player player, BuildingOffer offer) {
        return buildingTradeService.purchaseCooldownDaysLeft(player, offer);
    }

    @Transactional(readOnly = true)
    public List<MonthlyRecord> recentRecords(Player player) {
        return monthlyRecordRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(
                player,
                Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1)
        );
    }

    @Transactional(readOnly = true)
    public long loanLimit(Player player) {
        return loanService.loanLimit(player);
    }

    @Transactional(readOnly = true)
    public long totalMonthlyRent(Player player) {
        return settlementService.totalMonthlyRent(player);
    }

    @Transactional(readOnly = true)
    public long effectiveMonthlyRent(Player player, OwnedBuilding building) {
        if (secretaryTenantEventService.isRentWaived(player, building)) {
            return 0;
        }
        return settlementService.effectiveMonthlyRent(player, building);
    }

    @Transactional(readOnly = true)
    public int daysUntilSellable(Player player, OwnedBuilding building) {
        return buildingTradeService.daysUntilSellable(player, building);
    }

    @Transactional(readOnly = true)
    public boolean canSell(Player player, OwnedBuilding building) {
        return buildingTradeService.canSell(player, building);
    }

    @Transactional(readOnly = true)
    public String sellAvailabilityText(Player player, OwnedBuilding building) {
        return buildingTradeService.sellAvailabilityText(player, building);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(BuildingOffer offer) {
        return buildingTradeService.buildingImagePath(offer);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(OwnedBuilding building) {
        return buildingTradeService.buildingImagePath(building);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(AuctionEvent auction) {
        return buildingTradeService.buildingImagePath(auction);
    }

    @Transactional(readOnly = true)
    public String auctionDisplayName(AuctionEvent auction) {
        return buildingTradeService.auctionDisplayName(auction);
    }

    @Transactional(readOnly = true)
    public String auctionDisplayTypeName(AuctionEvent auction) {
        return buildingTradeService.auctionDisplayTypeName(auction);
    }

    @Transactional(readOnly = true)
    public String secretaryTenantStatusText(OwnedBuilding building) {
        return secretaryTenantEventService.statusText(building);
    }

    @Transactional(readOnly = true)
    public boolean rentWaivedBySecretaryEvent(Player player, OwnedBuilding building) {
        return secretaryTenantEventService.isRentWaived(player, building);
    }

    @Transactional(readOnly = true)
    public String effectiveMoveInChancePercentText(Player player, String city) {
        return formatPercent(settlementService.moveInChancePercent(player, city));
    }

    @Transactional(readOnly = true)
    public String effectiveMoveOutChancePercentText(Player player, String city) {
        return formatPercent(settlementService.moveOutChancePercent(player, city));
    }

    @Transactional(readOnly = true)
    public String effectiveRepairRequestChancePercentText(Player player, String city) {
        return formatPercent(settlementService.repairRequestChancePercent(player, city));
    }

    @Transactional(readOnly = true)
    public int baseMoveInChancePercent(Player player) {
        return player.getMoveInChancePercent();
    }

    @Transactional(readOnly = true)
    public int baseMoveOutChancePercent(Player player) {
        return player.getMoveOutChancePercent();
    }

    @Transactional(readOnly = true)
    public int baseRepairRequestChancePercent(Player player) {
        return player.getRepairRequestChancePercent();
    }

    @Transactional(readOnly = true)
    public double rentBonusPercent(Player player, String city) {
        return secretaryOperationsService.rentBonusPercent(player, city);
    }

    @Transactional(readOnly = true)
    public String rentBonusPercentText(Player player, String city) {
        return formatPercent(rentBonusPercent(player, city));
    }

    @Transactional(readOnly = true)
    public double buildingWaitReductionPercent(Player player, String city) {
        return buildingTradeService.buildingWaitReductionPercent(player, city);
    }

    @Transactional(readOnly = true)
    public String buildingWaitReductionPercentText(Player player, String city) {
        return formatPercent(buildingWaitReductionPercent(player, city));
    }

    @Transactional(readOnly = true)
    public String marketNewsStatusText(Player player, String city) {
        if (!player.hasActiveMarketNewsForCity(city)) {
            return "";
        }
        String trendLabel = SettlementService.MARKET_NEWS_RISE.equals(player.getActiveMarketNewsTrend()) ? "폭등" : "폭락";
        return "부동산 " + trendLabel + " · 매물갱신 " + player.getActiveMarketNewsRefreshesLeft() + "회";
    }

    @Transactional(readOnly = true)
    public long currentBuildingValue(Player player, OwnedBuilding building) {
        return cityMarketIndexService.marketValue(player, building);
    }

    @Transactional(readOnly = true)
    public long buildingValuationProfit(Player player, OwnedBuilding building) {
        return cityMarketIndexService.valuationProfit(player, building);
    }

    @Transactional(readOnly = true)
    public String cityMarketIndexText(Player player, String city) {
        return cityMarketIndexService.indexText(player, city);
    }

    @Transactional(readOnly = true)
    public String appliedSecretarySpecialEffectSummary(OwnedSecretary secretary) {
        return secretaryOperationsService.appliedSpecialEffectSummary(secretary);
    }

    @Transactional(readOnly = true)
    public List<String> activeSecretaryAbilitySummaries(OwnedSecretary secretary) {
        return secretaryOperationsService.activeAbilitySummaries(secretary);
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return buildingCatalog.cities();
    }

    @Transactional(readOnly = true)
    public String cityBackgroundClass(String city) {
        return switch (city) {
            case "청주" -> "cheongju";
            case "세종" -> "sejong";
            case "대전" -> "daejeon";
            case "부산" -> "busan";
            case "인천" -> "incheon";
            case "서울" -> "seoul";
            default -> "default";
        };
    }

    @Transactional(readOnly = true)
    public List<BuildingSpec> buildingSpecs() {
        return buildingCatalog.all();
    }

    @Transactional(readOnly = true)
    public List<ReputationTier> reputationTiers() {
        return reputationCatalog.all();
    }

    public String reputationText(long amount) {
        return GameTextFormatter.reputationText(amount);
    }

    public String secretaryMoveInConditionText(SecretarySpec secretary) {
        return SecretaryTenantScenarioCatalog.moveInConditionText(secretary.key());
    }

    public String secretaryHireConditionText(SecretarySpec secretary) {
        return SecretaryTenantScenarioCatalog.hireConditionText(secretary, reputationText(secretary.requiredReputation()));
    }

    @Transactional(readOnly = true)
    public List<SecretarySpec> secretarySpecs() {
        return secretaryCatalog.all();
    }

    @Transactional(readOnly = true)
    public List<LuxuryItemSpec> luxuryItems() {
        return shopService.luxuryItems();
    }

    @Transactional(readOnly = true)
    public List<GiftItemSpec> giftItems() {
        return shopService.giftItems();
    }

    @Transactional(readOnly = true)
    public List<StockSpec> stockSpecs() {
        return stockService.stocks();
    }

    @Transactional
    public void ensureStockMarketInitialized(Player player) {
        if (stockService.isUnlocked(player)) {
            stockService.ensureMarketInitialized(player);
        }
    }

    @Transactional(readOnly = true)
    public List<StockQuoteView> stockQuotes(Player player) {
        return stockService.stockQuotes(player);
    }

    @Transactional(readOnly = true)
    public StockMarketStatusView stockMarketStatus(Player player) {
        return stockService.marketStatus(player);
    }

    @Transactional(readOnly = true)
    public List<StockTradeHistory> stockTradeHistories(Player player) {
        return stockService.tradeHistories(player);
    }

    @Transactional(readOnly = true)
    public StockHoldingSummaryView stockHoldingSummary(Player player) {
        return stockService.holdingSummary(player);
    }

    @Transactional
    public String depositSecuritiesCash(long playerId, long amount) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.deposit(player, amount);
    }

    @Transactional
    public String withdrawSecuritiesCash(long playerId, long amount) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.withdraw(player, amount);
    }

    @Transactional
    public String buyStock(long playerId, String stockKey, long quantity) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.buyStock(player, stockKey, quantity);
    }

    @Transactional
    public String buyMaxStock(long playerId, String stockKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.buyMaxStock(player, stockKey);
    }

    @Transactional
    public String sellStock(long playerId, String stockKey, long quantity) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.sellStock(player, stockKey, quantity);
    }

    @Transactional
    public String sellAllStock(long playerId, String stockKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return stockService.sellAllStock(player, stockKey);
    }

    public String stockMoneyText(long amount) {
        return stockService.moneyText(amount);
    }

    @Transactional
    public void ensureStockUnlockSchedule(Player player) {
        stockService.ensureUnlockSchedule(player);
    }

    @Transactional(readOnly = true)
    public boolean stockContentUnlocked(Player player) {
        return stockService.isUnlocked(player);
    }

    @Transactional(readOnly = true)
    public String stockContentStatusText(Player player) {
        return stockService.statusText(player);
    }

    @Transactional(readOnly = true)
    public int ownedGiftQuantity(Player player, GiftItemSpec gift) {
        return shopService.ownedGiftQuantity(player, gift);
    }

    @Transactional(readOnly = true)
    public int maxGiftQuantityForSecretary(Player player, OwnedSecretary secretary, GiftItemSpec gift) {
        return shopService.maxGiftQuantityForSecretary(player, secretary, gift);
    }

    @Transactional(readOnly = true)
    public boolean isLuxuryItemOwned(Player player, LuxuryItemSpec item) {
        return shopService.isLuxuryItemOwned(player, item);
    }

    @Transactional(readOnly = true)
    public SecretarySpec secretarySpec(String key) {
        return secretaryCatalog.find(key).orElseThrow();
    }

    @Transactional(readOnly = true)
    public boolean canHireSecretary(Player player, SecretarySpec spec) {
        return secretaryOperationsService.canHireSecretary(player, spec);
    }

    @Transactional(readOnly = true)
    public boolean isSecretaryOwned(Player player, SecretarySpec spec) {
        return secretaryOperationsService.isSecretaryOwned(player, spec);
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> ownedSecretary(Player player, SecretarySpec spec) {
        return secretaryOperationsService.ownedSecretary(player, spec);
    }

    @Transactional(readOnly = true)
    public Optional<SecretarySpec> availableSecretaryOffer(Player player) {
        return secretaryOperationsService.availableSecretaryOffer(player);
    }

    @Transactional(readOnly = true)
    public List<OwnedSecretary> ownedSecretaries(Player player) {
        return secretaryOperationsService.ownedSecretaries(player);
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> assignedSecretary(Player player, String city) {
        return secretaryOperationsService.assignedSecretary(player, city);
    }

    @Transactional(readOnly = true)
    public boolean canAssignSecretaryToCity(Player player, OwnedSecretary targetSecretary, String city) {
        return secretaryOperationsService.canAssignSecretaryToCity(player, targetSecretary, city);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> cityUnlocks(Player player) {
        return buildingCatalog.cities().stream()
                .collect(Collectors.toMap(
                        city -> city,
                        city -> reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> repairRequestCountsByCity(Player player) {
        return ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .filter(OwnedBuilding::isRepairRequested)
                .collect(Collectors.groupingBy(OwnedBuilding::getCity, Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public List<SecretaryTenantEvent> secretaryTenantEvents(Player player) {
        return secretaryTenantEventRepository.findByPlayerAndStatusNot(player, SecretaryTenantEventStatus.COMPLETED);
    }

    @Transactional
    public void dismissSecretaryOffer(long playerId, String secretaryKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return;
        }
        player.dismissSecretaryOffer(secretaryKey);
    }

    @Transactional
    public String donate(long playerId, int multiplier) {
        return shopService.donate(playerId, multiplier);
    }

    @Transactional
    public String buyLuxuryItem(long playerId, String itemKey) {
        return shopService.buyLuxuryItem(playerId, itemKey);
    }

    @Transactional
    public String buyGiftItem(long playerId, String giftKey, int quantity) {
        return shopService.buyGiftItem(playerId, giftKey, quantity);
    }

    @Transactional
    public String giveGiftToSecretary(long playerId, long ownedSecretaryId, String giftKey, int quantity) {
        return shopService.giveGiftToSecretary(playerId, ownedSecretaryId, giftKey, quantity);
    }

    @Transactional
    public String togglePause(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.togglePause();
        return player.isPaused() ? "시간 일시정지" : "시간 진행";
    }

    @Transactional
    public String hireSecretary(long playerId, String secretaryKey) {
        return secretaryOperationsService.hireSecretary(playerId, secretaryKey);
    }

    @Transactional
    public String assignSecretary(long playerId, long ownedSecretaryId, String city) {
        return secretaryOperationsService.assignSecretary(playerId, ownedSecretaryId, city);
    }

    @Transactional
    public String repayLoan(long playerId, long loanId) {
        return loanService.repayLoan(playerId, loanId);
    }

    @Transactional
    public String unassignSecretary(long playerId, long ownedSecretaryId) {
        return secretaryOperationsService.unassignSecretary(playerId, ownedSecretaryId);
    }

    @Transactional(readOnly = true)
    public boolean isOfferUnlocked(Player player, BuildingOffer offer) {
        return buildingTradeService.isOfferUnlocked(player, offer);
    }

    @Transactional
    public String changeCity(long playerId, String city) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (!buildingCatalog.cities().contains(city)) {
            return "존재하지 않는 도시";
        }
        if (!reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())) {
            return "아직 해금되지 않은 도시";
        }
        player.changeCity(city);
        buildingTradeService.ensureOffers(player);
        return city + " 이동 완료";
    }

    @Transactional
    public void ensureOffers(Player player) {
        buildingTradeService.ensureOffers(player);
    }

    private String formatPercent(double value) {
        return GameTextFormatter.percent(value);
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " · " + addition;
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }

}
