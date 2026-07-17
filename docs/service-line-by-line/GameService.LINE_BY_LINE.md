# GameService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/GameService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/단순 위임은 과하게 설명하지 않는다.

```java
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
// 해설: 컨트롤러가 직접 여러 서비스를 만지지 않도록, 게임 진행의 외부 진입점을 한 곳에 모은 파사드 서비스다.
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

    public GameService(
    // 해설: Spring이 생성자를 통해 Repository와 세부 서비스를 주입한다. 이 클래스는 직접 계산하기보다 각 전용 서비스에 일을 나눠 맡긴다.
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
            StockService stockService
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
    }

    @Transactional(readOnly = true)
    public Player player(long playerId) {
    // 해설: 컨트롤러가 현재 플레이어 엔티티를 가져올 때 쓰는 조회 진입점이다.
        return playerRepository.findById(playerId).orElseThrow();
        // 해설: playerId에 해당하는 플레이어를 찾는다. 없으면 정상 진행이 불가능하므로 예외를 던진다.
    }

    @Transactional
    public void completeStory(long playerId) {
    // 해설: 스토리 화면을 끝내고 실제 게임 시작 상태를 만드는 메서드다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        if (!player.isStorySeen()) {
        // 해설: 스토리 완료 처리는 최초 1회만 실행되어야 하므로 이미 완료한 플레이어는 건드리지 않는다.
            // 스토리 완료는 메인 게임 최초 진입점이다.
            // 플레이어 상태 초기화, 첫 건물 지급, 첫 매물 생성이 한 트랜잭션에서 끝나야 중간 저장 오류가 없다.
            player.completeStory();
            // 해설: 플레이어를 스토리 완료 상태로 바꾸고, 메인 게임 진입이 가능하게 한다.
            playerRepository.save(player);
            BuildingSpec starter = buildingCatalog.firstCheongjuRoom().orElseThrow();
            // 해설: 처음 지급할 청주 원룸 스펙을 카탈로그에서 가져온다. 시작 건물이 없으면 게임을 시작할 수 없다.
            ownedBuildingRepository.save(new OwnedBuilding(player, starter.city(), starter.slot(), starter.typeName(), starter.name(), starter.marketPrice(), 0L, starter.monthlyRent(), starter.tradeCooldownDays()));
            // 해설: 시작 건물을 플레이어 보유 건물로 저장한다. 매입가는 0원으로 넣어 기본 지급 건물임을 표현한다.
            buildingTradeService.refreshOffers(player);
            // 해설: 게임 시작 직후 살 수 있는 매물을 생성한다.
        }
    }

    @Transactional
    public String sideJob(long playerId) {
    // 해설: 부업 버튼을 눌렀을 때 현금을 지급하는 경제 행동이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        if (player.isPaused()) {
        // 해설: 이벤트 모달 등으로 일시정지된 상태면 날짜를 진행하지 않는다.
            return pausedActionMessage();
            // 해설: 일시정지 때문에 행동할 수 없다는 사용자 메시지를 반환한다.
        }
        player.addSideIncome(SIDE_JOB_REWARD);
        // 해설: 부업 보상만큼 현금을 증가시킨다.
        return "부업 수익 10,000원 획득";
    }

    @Transactional
    public String tick(long playerId) {
    // 해설: 기본 하루 진행 API다. 도시 이벤트 지연 없이 일반 도시 화면 기준으로 tick을 실행한다.
        return tick(playerId, false);
        // 해설: 실제 구현은 오버로드된 tick에 위임하고, 도시 이벤트 지연 옵션만 false로 고정한다.
    }

    @Transactional
    public String tick(long playerId, boolean deferCityEvents) {
    // 해설: 게임 시간 하루를 진행하는 핵심 메서드다. 도시 화면과 주식 화면이 같은 시간 축을 공유한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        if (player.isPaused()) {
        // 해설: 이벤트 모달 등으로 일시정지된 상태면 날짜를 진행하지 않는다.
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
        // 해설: 주식 화면 tick이면 경매 응답을 우선하지 않고, 도시 화면 tick이면 기존 경매를 먼저 확인한다.
        if (existingAuction.isPresent()) {
        // 해설: 이미 진행 중인 경매가 있으면 날짜 진행보다 경매 화면 처리를 우선한다.
            return "AUCTION:" + existingAuction.get().getId();
            // 해설: 프론트가 경매 모달을 열 수 있도록 경매 id를 포함한 신호를 반환한다.
        }
        // 날짜는 반드시 한 번만 증가해야 한다. 프론트가 중복 tick을 막고, 서버도 이 메서드 한 곳에서만 advanceDay를 호출한다.
        player.advanceDay();
        // 해설: 서버 기준으로 날짜를 하루 증가시킨다. 시간 경과는 이 위치에서만 일어나야 중복 진행을 막을 수 있다.
        // 공실 건물의 수리 요청처럼 매일 자연스럽게 정리되는 상태를 먼저 정리한다.
        settlementService.clearVacantRepairRequests(player);
        // 해설: 공실 건물의 수리 요청처럼 하루가 지나며 정리되어야 할 상태를 먼저 정리한다.
        // 일일 정산은 월세, 월급, 대출, 월말 기록 같은 경제 흐름을 처리한다.
        String dailyNotice = settlementService.runDailySettlement(player);
        // 해설: 월세, 월급, 대출, 월말 처리 등 하루 경제 정산을 실행하고 안내 문구를 받는다.
        dailyNotice = appendNotice(dailyNotice, eventFlowService.processAutoResignation(player));
        // 해설: 자동 퇴사 같은 이벤트성 안내가 있으면 기존 일일 안내 문구에 이어 붙인다.
        if (player.getElapsedDays() >= player.getNextOfferRefreshDay()) {
        // 해설: 누적 경과일이 다음 매물 갱신일에 도달했는지 확인한다.
            // 매물 갱신은 elapsedDays 기준이다. 월/일이 1월로 순환해도 쿨다운이 꼬이지 않는다.
            buildingTradeService.refreshOffers(player);
            // 해설: 게임 시작 직후 살 수 있는 매물을 생성한다.
            player.scheduleNextOfferRefresh();
            // 해설: 이번 갱신이 끝났으므로 다음 매물 갱신일을 다시 예약한다.
        }
        refreshTitle(player);
        // 해설: 현재 평판과 고용 상태에 맞는 칭호를 갱신한다.
        Optional<AuctionEvent> activeAuction = activeAuction(player);
        // 해설: 정산이나 매물 갱신 이후 새로 활성화된 경매가 있는지 다시 확인한다.
        if (activeAuction.isPresent()) {
            return "AUCTION:" + activeAuction.get().getId();
        }
        if (stockService.activateUnlockNoticeIfDue(player)) {
        // 해설: 주식 컨텐츠 해금 시점이면 해금 안내 이벤트를 활성화한다.
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
            // 해설: 방금 활성화된 이벤트 id를 프론트에 알려 이벤트 모달을 열게 한다.
        }
        // 주식 가격은 주식 화면과 도시 화면 모두에서 같은 시간 축을 공유한다.
        stockService.processPriceUpdates(player);
        // 해설: 주식 가격을 현재 날짜 기준으로 갱신한다. 도시 화면과 주식 화면 모두 같은 호출을 사용한다.
        if (stockService.activateIndustryNewsIfDue(player)) {
        // 해설: 산업 뉴스 이벤트가 발생할 차례인지 확인하고 필요하면 이벤트를 활성화한다.
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
            // 해설: 방금 활성화된 이벤트 id를 프론트에 알려 이벤트 모달을 열게 한다.
        }
        if (deferCityEvents) {
        // 해설: 주식 화면에서 온 tick이면 도시 이벤트를 화면에 즉시 띄우지 않고 지연 처리한다.
            // 주식 화면에서는 도시 이벤트를 DB에만 준비하고, 클라이언트에는 EVENT 응답을 보내지 않는다.
            gameEventCatalog.findDueEvent(player.getMonth(), player.getDay())
            // 해설: 오늘 날짜에 발생해야 하는 도시 이벤트 정의를 카탈로그에서 찾는다.
                    .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(player, definition.key()))
                    // 해설: 이미 생성된 이벤트는 다시 만들지 않는다.
                    .ifPresent(definition -> eventFlowService.activateEvent(player, definition, false));
                    // 해설: 이벤트가 있으면 DB에는 생성하되, 주식 화면 흐름을 방해하지 않도록 즉시 표시 신호는 보내지 않는다.
            return dailyNotice;
            // 해설: 주식 화면 tick에서는 도시 이벤트 신호 대신 정산 안내만 반환한다.
        }
        Optional<GameEvent> activeEvent = activeEvent(player);
        // 해설: 이미 활성화된 일반 이벤트가 있는지 확인한다.
        if (activeEvent.isPresent()) {
            return "EVENT:" + activeEvent.get().getId();
        }
        secretaryTenantEventService.evaluate(player, activeAuction(player).isPresent());
        // 해설: 비서 세입자 이벤트가 새로 열릴 조건인지 검사한다. 경매가 있으면 비서 이벤트와 겹치지 않게 한다.
        activeEvent = activeEvent(player);
        // 해설: 비서 이벤트 평가 후 새로 생긴 활성 이벤트가 있는지 다시 조회한다.
        if (activeEvent.isPresent()) {
            return "EVENT:" + activeEvent.get().getId();
        }
        Optional<GameEventDefinition> dueEvent = gameEventCatalog.findDueEvent(player.getMonth(), player.getDay())
        // 해설: 오늘 날짜에 맞는 일반 도시 이벤트를 찾고, 중복이 아니면 활성화한다.
                .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(player, definition.key()))
                // 해설: 이미 생성된 이벤트는 다시 만들지 않는다.
                .map(definition -> {
                // 해설: 이벤트 정의가 있으면 실제 GameEvent를 만들고, Optional 흐름 안에서 같은 정의를 반환한다.
                    eventFlowService.activateEvent(player, definition);
                    // 해설: 도시 이벤트를 활성화하고 플레이어를 일시정지시키는 처리를 EventFlowService에 맡긴다.
                    return definition;
                });
        if (dueEvent.isPresent()) {
        // 해설: 새 도시 이벤트가 만들어졌으면 프론트에 EVENT 신호를 보낸다.
            return "EVENT:" + activeEvent(player).orElseThrow().getId();
            // 해설: 방금 활성화된 이벤트 id를 프론트에 알려 이벤트 모달을 열게 한다.
        }
        return auctionService.tryActivate(player)
        // 해설: 이벤트가 없을 때 마지막으로 경매 활성화 조건을 검사한다.
                .map(auction -> "AUCTION:" + auction.getId())
                // 해설: 경매가 활성화되면 프론트가 경매 모달을 열 수 있는 응답 문자열로 바꾼다.
                .orElse(dailyNotice);
                // 해설: 경매도 없으면 하루 정산 안내 문구를 최종 응답으로 돌려준다.
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(Player player) {
    // 해설: 현재 플레이어의 활성 이벤트 조회를 EventFlowService로 위임한다.
        return eventFlowService.activeEvent(player);
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(long playerId) {
    // 해설: playerId만 있을 때 활성 이벤트를 조회하는 편의 메서드다.
        return eventFlowService.activeEvent(playerId);
    }

    @Transactional
    public void evaluateSecretaryTenantEvents(Player player) {
    // 해설: 외부에서 비서 세입자 이벤트 평가만 따로 요청할 수 있는 진입점이다.
        secretaryTenantEventService.evaluate(player, activeAuction(player).isPresent());
        // 해설: 비서 세입자 이벤트가 새로 열릴 조건인지 검사한다. 경매가 있으면 비서 이벤트와 겹치지 않게 한다.
    }

    @Transactional
    public Optional<AuctionEvent> activeAuction(Player player) {
    // 해설: 현재 진행 중인 경매 조회를 AuctionService로 위임한다.
        return auctionService.activeAuction(player);
    }

    @Transactional
    public void completeEvent(long playerId, long eventId) {
    // 해설: 이벤트 완료 처리를 EventFlowService로 위임한다.
        eventFlowService.completeEvent(playerId, eventId);
    }

    @Transactional
    public void cancelEvent(long playerId, long eventId) {
    // 해설: 이벤트 취소 처리를 EventFlowService로 위임한다.
        eventFlowService.cancelEvent(playerId, eventId);
    }

    @Transactional
    public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
    // 해설: 매물 구매 요청을 BuildingTradeService로 위임한다.
        return buildingTradeService.buyOffer(playerId, offerId, loanPurchase);
    }

    @Transactional
    public String bidAuction(long playerId, long auctionId, int rate) {
    // 해설: 경매 입찰 요청을 AuctionService로 위임한다.
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
    // 해설: 보유 건물 판매 요청을 BuildingTradeService로 위임한다.
        return buildingTradeService.sellBuilding(playerId, buildingId);
    }

    @Transactional
    public String repairBuilding(long playerId, long buildingId) {
    // 해설: 건물 수리 요청을 BuildingTradeService로 위임한다.
        return buildingTradeService.repairBuilding(playerId, buildingId);
    }

    @Transactional
    public String hireFirstSecretary(long playerId) {
        return secretaryOperationsService.hireFirstSecretary(playerId);
    }

    @Transactional(readOnly = true)
    public List<BuildingOffer> offers(Player player) {
    // 해설: 화면에 보여줄 현재 매물 목록을 조회한다.
        return buildingTradeService.offers(player);
    }

    @Transactional(readOnly = true)
    public List<OwnedBuilding> ownedBuildings(Player player) {
    // 해설: 플레이어가 보유한 건물 목록을 조회한다.
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
    // 해설: 최근 월간 기록만 조회한다. 오래된 기록은 화면에 계속 보여주지 않는다.
        return monthlyRecordRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(
                player,
                Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1)
                // 해설: 조회 시작일이 1보다 작아지지 않게 보정한다.
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
    // 해설: 비서 이벤트 효과까지 반영한 실제 월세를 계산한다.
        if (secretaryTenantEventService.isRentWaived(player, building)) {
        // 해설: 비서 이벤트로 임대료 면제 상태라면 정산 서비스 계산 전에 0원을 반환한다.
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
    // 해설: 현재 도시의 부동산 호황/불황 뉴스 상태를 UI 문구로 만든다.
        if (!player.hasActiveMarketNewsForCity(city)) {
        // 해설: 해당 도시의 활성 부동산 뉴스가 없으면 아무 문구도 표시하지 않는다.
            return "";
        }
        String trendLabel = SettlementService.MARKET_NEWS_RISE.equals(player.getActiveMarketNewsTrend()) ? "폭등" : "폭락";
        // 해설: 저장된 뉴스 방향을 사람이 읽는 문구로 바꾼다.
        return "부동산 " + trendLabel + " · 매물갱신 " + player.getActiveMarketNewsRefreshesLeft() + "회";
        // 해설: 뉴스 방향과 남은 적용 횟수를 합쳐 화면 표시 문자열을 만든다.
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
    // 해설: 건물 카탈로그 기준으로 해금 대상 도시 목록을 반환한다.
        return buildingCatalog.cities();
    }

    @Transactional(readOnly = true)
    public String cityBackgroundClass(String city) {
    // 해설: 도시 이름을 CSS 배경 클래스명으로 변환한다.
        return switch (city) {
        // 해설: Java switch expression으로 도시별 반환값을 간결하게 선택한다.
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
    // 해설: 주식 컨텐츠가 열린 플레이어에게 주식 시장 초기 데이터가 있는지 보장한다.
        if (stockService.isUnlocked(player)) {
        // 해설: 주식 컨텐츠가 아직 잠겨 있으면 시장 데이터를 만들지 않는다.
            stockService.ensureMarketInitialized(player);
            // 해설: 해금된 플레이어의 주식 가격/보유 관련 초기 상태를 준비한다.
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
    public String exchangeCashToCoin(long playerId, long coinAmount) {
    // 해설: 현금을 주식용 코인으로 바꾸는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.exchangeCashToCoin(player, coinAmount);
        // 해설: 실제 환전 검증과 잔액 변경은 StockService에 맡긴다.
    }

    @Transactional
    public String exchangeCoinToCash(long playerId, long coinAmount) {
    // 해설: 주식용 코인을 현금으로 바꾸는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.exchangeCoinToCash(player, coinAmount);
    }

    @Transactional
    public String buyStock(long playerId, String stockKey, long quantity) {
    // 해설: 특정 종목을 지정 수량만큼 매수하는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.buyStock(player, stockKey, quantity);
    }

    @Transactional
    public String buyMaxStock(long playerId, String stockKey) {
    // 해설: 보유 코인으로 가능한 최대 수량을 매수하는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.buyMaxStock(player, stockKey);
    }

    @Transactional
    public String sellStock(long playerId, String stockKey, long quantity) {
    // 해설: 특정 종목을 지정 수량만큼 매도하는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.sellStock(player, stockKey, quantity);
    }

    @Transactional
    public String sellAllStock(long playerId, String stockKey) {
    // 해설: 특정 종목 보유분 전체를 매도하는 요청 진입점이다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        return stockService.sellAllStock(player, stockKey);
    }

    public String stockCoinText(long amount) {
        return stockService.coinText(amount);
    }

    @Transactional
    public void ensureStockUnlockSchedule(Player player) {
    // 해설: 주식 해금 이벤트 예약 상태가 없으면 준비한다.
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
    // 해설: 도시별 해금 여부를 Map으로 만들어 화면에서 바로 사용할 수 있게 한다.
        return buildingCatalog.cities().stream()
        // 해설: 카탈로그의 모든 도시를 순회해 해금 여부를 계산한다.
                .collect(Collectors.toMap(
                // 해설: 도시 이름을 key로, 해금 여부를 value로 하는 Map을 만든다.
                        city -> city,
                        city -> reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())
                        // 해설: 평판과 퇴사 여부를 기준으로 해당 도시가 열렸는지 계산한다.
                ));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> repairRequestCountsByCity(Player player) {
    // 해설: 도시별 수리 요청 건물 수를 계산한다.
        return ownedBuildingRepository.findByPlayerOrderById(player).stream()
        // 해설: 보유 건물 전체를 가져와 Stream으로 순회한다.
                .filter(OwnedBuilding::isRepairRequested)
                // 해설: 수리 요청 상태인 건물만 남긴다.
                .collect(Collectors.groupingBy(OwnedBuilding::getCity, Collectors.counting()));
                // 해설: 남은 건물을 도시별로 묶고 개수를 센다.
    }

    @Transactional(readOnly = true)
    public List<SecretaryTenantEvent> secretaryTenantEvents(Player player) {
    // 해설: 완료되지 않은 비서 세입자 이벤트 목록을 조회한다.
        return secretaryTenantEventRepository.findByPlayerAndStatusNot(player, SecretaryTenantEventStatus.COMPLETED);
    }

    @Transactional
    public void dismissSecretaryOffer(long playerId, String secretaryKey) {
    // 해설: 비서 고용 제안을 닫았을 때 다시 뜨지 않도록 플레이어 상태에 기록한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        if (player.isPaused()) {
        // 해설: 이벤트 모달 등으로 일시정지된 상태면 날짜를 진행하지 않는다.
            return;
        }
        player.dismissSecretaryOffer(secretaryKey);
        // 해설: 해당 비서 제안을 닫은 상태로 저장한다.
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
    // 해설: 사용자가 시간 정지/진행 버튼을 눌렀을 때 실행된다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        player.togglePause();
        // 해설: 플레이어의 일시정지 상태를 반대로 바꾼다.
        return player.isPaused() ? "시간 일시정지" : "시간 진행";
        // 해설: 변경된 상태에 맞는 안내 문구를 반환한다.
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
    // 해설: 도시 이동 요청을 처리한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청을 보낸 플레이어를 DB에서 다시 조회한다. 이후 변경은 이 엔티티에 반영된다.
        if (!buildingCatalog.cities().contains(city)) {
        // 해설: 카탈로그에 없는 도시는 잘못된 요청이므로 거부한다.
            return "존재하지 않는 도시";
            // 해설: 유효하지 않은 도시 요청에 대한 실패 메시지다.
        }
        if (!reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())) {
        // 해설: 평판과 퇴사 여부 기준으로 아직 해금되지 않은 도시는 이동할 수 없다.
            return "아직 해금되지 않은 도시";
            // 해설: 도시 해금 조건을 만족하지 못했을 때 반환하는 메시지다.
        }
        player.changeCity(city);
        // 해설: 플레이어의 현재 도시를 요청한 도시로 변경한다.
        buildingTradeService.ensureOffers(player);
        // 해설: 이동한 도시에서 보여줄 매물이 없으면 생성한다.
        return city + " 이동 완료";
        // 해설: 도시 이동 성공 메시지를 반환한다.
    }

    @Transactional
    public void ensureOffers(Player player) {
    // 해설: 현재 도시의 매물 목록이 비어 있지 않도록 보장한다.
        buildingTradeService.ensureOffers(player);
        // 해설: 이동한 도시에서 보여줄 매물이 없으면 생성한다.
    }

    private String formatPercent(double value) {
    // 해설: 퍼센트 표시 형식을 한 곳에서 통일한다.
        return GameTextFormatter.percent(value);
        // 해설: 실제 포맷팅 규칙은 GameTextFormatter에 위임한다.
    }

    private String pausedActionMessage() {
    // 해설: 일시정지 중 경제 행동을 막을 때 공통으로 쓰는 메시지를 반환한다.
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
    // 해설: 두 안내 문구를 자연스럽게 합치는 내부 유틸리티다.
        if (addition == null || addition.isBlank()) {
        // 해설: 추가 안내가 없으면 기존 안내만 유지한다.
            return base;
        }
        if (base == null || base.isBlank()) {
        // 해설: 기존 안내가 없으면 추가 안내만 반환한다.
            return addition;
        }
        return base + " · " + addition;
        // 해설: 두 안내가 모두 있으면 가운데 구분자를 넣어 하나의 문구로 만든다.
    }

    private void refreshTitle(Player player) {
    // 해설: 현재 평판 티어에 맞춰 플레이어 칭호를 갱신한다.
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        // 해설: 평판과 퇴사 여부로 현재 티어를 찾고, 그 티어의 칭호를 플레이어에 반영한다.
    }

}
```
