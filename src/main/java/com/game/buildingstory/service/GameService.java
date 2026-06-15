package com.game.buildingstory.service;

import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventDefinition;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.ValuationStatus;
import com.game.buildingstory.repo.BuildingOfferRepository;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {
    private static final long SIDE_JOB_REWARD = 10_000L;
    private static final int CITY_BUILDING_LIMIT = 8;

    private final PlayerRepository playerRepository;
    private final BuildingOfferRepository offerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final LoanRepository loanRepository;
    private final GameEventRepository gameEventRepository;
    private final GameEventCatalog gameEventCatalog;

    public GameService(
            PlayerRepository playerRepository,
            BuildingOfferRepository offerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            LoanRepository loanRepository,
            GameEventRepository gameEventRepository,
            GameEventCatalog gameEventCatalog
    ) {
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.loanRepository = loanRepository;
        this.gameEventRepository = gameEventRepository;
        this.gameEventCatalog = gameEventCatalog;
    }

    @Transactional(readOnly = true)
    public Player player(long playerId) {
        return playerRepository.findById(playerId).orElseThrow();
    }

    @Transactional
    public void completeStory(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (!player.isStorySeen()) {
            player.completeStory();
            playerRepository.save(player);
            ownedBuildingRepository.save(new OwnedBuilding(player, "청주", "원룸", "복대동 12평 원룸", 50_000_000L, 0L, 300_000L));
            seedCheongjuOffers(player);
        }
    }

    @Transactional
    public String sideJob(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.addSideIncome(SIDE_JOB_REWARD);
        return "부업 수익 10,000원 획득";
    }

    @Transactional
    public String tick(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return "";
        }
        player.advanceDay();
        return gameEventCatalog.findDueEvent(player.getMonth(), player.getDay())
                .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(player, definition.key()))
                .map(definition -> activateEvent(player, definition))
                .orElse("");
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(Player player) {
        return gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE);
    }

    @Transactional
    public void completeEvent(long playerId, long eventId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        GameEvent event = gameEventRepository.findById(eventId).orElseThrow();
        if (!event.getPlayer().getId().equals(player.getId()) || event.getStatus() != GameEventStatus.ACTIVE) {
            throw new IllegalArgumentException("잘못된 이벤트");
        }
        applyEventEffect(player, event);
        event.complete();
        player.resume();
    }

    @Transactional
    public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        BuildingOffer offer = offerRepository.findById(offerId).orElseThrow();
        if (!offer.getPlayer().getId().equals(player.getId())) {
            throw new IllegalArgumentException("잘못된 매물");
        }
        long ownedCount = ownedBuildingRepository.countByPlayerAndCity(player, offer.getCity());
        if (ownedCount >= CITY_BUILDING_LIMIT) {
            return offer.getCity() + " 보유 제한 8채 도달";
        }

        long cashCost = loanPurchase ? offer.cashForLoanPurchase() : offer.getOfferPrice();
        if (!player.spendCash(cashCost)) {
            return "현금 부족";
        }
        ownedBuildingRepository.save(new OwnedBuilding(player, offer));
        if (loanPurchase) {
            loanRepository.save(new Loan(player, offer.loanAmount()));
        }
        offerRepository.delete(offer);
        return loanPurchase ? "대출구매 완료" : "현금구매 완료";
    }

    @Transactional(readOnly = true)
    public List<BuildingOffer> offers(Player player) {
        return offerRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
    }

    @Transactional(readOnly = true)
    public List<OwnedBuilding> ownedBuildings(Player player) {
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
    }

    @Transactional(readOnly = true)
    public List<Loan> loans(Player player) {
        return loanRepository.findByPlayer(player);
    }

    @Transactional
    public void ensureOffers(Player player) {
        if (!offerRepository.existsByPlayerAndCity(player, player.getCurrentCity())) {
            seedCheongjuOffers(player);
        }
    }

    private void seedCheongjuOffers(Player player) {
        offerRepository.save(new BuildingOffer(player, "청주", "원룸", "사창동 12평 원룸", 50_000_000L, 300_000L, ValuationStatus.UNDER));
        offerRepository.save(new BuildingOffer(player, "청주", "주택", "율량동 투룸 주택", 95_000_000L, 520_000L, ValuationStatus.FAIR));
        offerRepository.save(new BuildingOffer(player, "청주", "오피스텔", "가경동 역세권 오피스텔", 180_000_000L, 950_000L, ValuationStatus.OVER));
        offerRepository.save(new BuildingOffer(player, "청주", "빌딩", "복대동 상가 빌딩", 420_000_000L, 2_400_000L, ValuationStatus.FAIR));
    }

    private String activateEvent(Player player, GameEventDefinition definition) {
        GameEvent event = gameEventRepository.save(new GameEvent(player, definition));
        player.pause();
        return "EVENT:" + event.getId();
    }

    private void applyEventEffect(Player player, GameEvent event) {
        if (GameEventCatalog.EFFECT_FIRST_TENANT_MOVE_IN.equals(event.getEffectKey())) {
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                    .filter(building -> !building.isOccupied())
                    .findFirst()
                    .ifPresent(OwnedBuilding::moveIn);
            player.markFirstTenantEventDone();
        }
    }
}
