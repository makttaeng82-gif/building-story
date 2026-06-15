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
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GameService {
    private static final long SIDE_JOB_REWARD = 10_000L;
    private static final long MONTHLY_JOB_SALARY = 3_000_000L;
    private static final long FIRST_SECRETARY_SALARY = 3_000_000L;
    private static final int CITY_BUILDING_LIMIT = 8;

    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final BuildingOfferRepository offerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final LoanRepository loanRepository;
    private final GameEventRepository gameEventRepository;
    private final GameEventCatalog gameEventCatalog;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryCatalog secretaryCatalog;

    public GameService(
            PlayerRepository playerRepository,
            BuildingOfferRepository offerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            LoanRepository loanRepository,
            GameEventRepository gameEventRepository,
            GameEventCatalog gameEventCatalog,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            SecretaryCatalog secretaryCatalog
    ) {
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.loanRepository = loanRepository;
        this.gameEventRepository = gameEventRepository;
        this.gameEventCatalog = gameEventCatalog;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
        this.secretaryCatalog = secretaryCatalog;
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
            BuildingSpec starter = buildingCatalog.firstCheongjuRoom().orElseThrow();
            ownedBuildingRepository.save(new OwnedBuilding(player, starter.city(), starter.typeName(), starter.name(), starter.marketPrice(), 0L, starter.monthlyRent(), starter.tradeCooldownDays()));
            refreshOffers(player);
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
        runDailySettlement(player);
        if (player.getElapsedDays() >= player.getNextOfferRefreshDay()) {
            refreshOffers(player);
            player.scheduleNextOfferRefresh();
        }
        refreshTitle(player);
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

    @Transactional
    public String resign(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (!player.isEmployed()) {
            return "이미 퇴사 상태";
        }
        player.resign();
        refreshTitle(player);
        return "퇴사 완료 · 퇴직금 30,000,000원 지급";
    }

    @Transactional
    public String sellBuilding(long playerId, long buildingId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        Optional<OwnedBuilding> buildingOptional = ownedBuildingRepository.findById(buildingId);
        if (buildingOptional.isEmpty() || !buildingOptional.get().getPlayer().getId().equals(player.getId())) {
            return "잘못된 건물";
        }
        OwnedBuilding building = buildingOptional.get();
        if (building.isProtectedTenant()) {
            return "후배가 거주 중인 건물은 비서 고용 전 판매 불가";
        }
        if (!building.isSellable(player.getElapsedDays())) {
            return "판매 쿨타임 D-" + building.daysUntilSellable(player.getElapsedDays());
        }
        ValuationStatus valuationStatus = randomValuation();
        long sellPrice = building.getMarketPrice() * valuationStatus.rate() / 100;
        player.addCash(sellPrice);
        ownedBuildingRepository.delete(building);
        return "건물 판매 완료 · " + valuationStatus.label() + " " + sellPrice + "원";
    }

    @Transactional
    public String hireFirstSecretary(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isFirstSecretaryHired()) {
            return "이미 고용한 비서";
        }
        if (player.getReputation() < 1000) {
            return "평판 1000 이상 필요";
        }
        Optional<OwnedBuilding> tenantBuilding = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                .filter(OwnedBuilding::isProtectedTenant)
                .findFirst();
        if (tenantBuilding.isEmpty()) {
            return "후배가 거주 중이어야 고용 가능";
        }
        tenantBuilding.get().moveOut();
        player.hireFirstSecretary();
        return "비서 고용 완료 · 월급 3,000,000원";
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

    @Transactional(readOnly = true)
    public long loanLimit(Player player) {
        return Math.max(10_000_000L, (long) (player.getReputation() + 1) * 100_000L);
    }

    @Transactional(readOnly = true)
    public List<String> cities() {
        return buildingCatalog.cities();
    }

    @Transactional(readOnly = true)
    public List<BuildingSpec> buildingSpecs() {
        return buildingCatalog.all();
    }

    @Transactional(readOnly = true)
    public List<ReputationTier> reputationTiers() {
        return reputationCatalog.all();
    }

    @Transactional(readOnly = true)
    public List<SecretarySpec> secretarySpecs() {
        return secretaryCatalog.all();
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> cityUnlocks(Player player) {
        return buildingCatalog.cities().stream()
                .collect(Collectors.toMap(
                        city -> city,
                        city -> reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())
                ));
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
        ensureOffers(player);
        return city + " 이동 완료";
    }

    @Transactional
    public void ensureOffers(Player player) {
        if (!offerRepository.existsByPlayerAndCity(player, player.getCurrentCity())) {
            refreshOffers(player);
        }
    }

    private void refreshOffers(Player player) {
        offerRepository.deleteByPlayerAndCity(player, player.getCurrentCity());
        buildingCatalog.byCity(player.getCurrentCity()).stream()
                .filter(spec -> reputationCatalog.isBuildingUnlocked(spec.city(), spec.slot(), player.getReputation(), !player.isEmployed()))
                .map(spec -> new BuildingOffer(
                        player,
                        spec.city(),
                        spec.slot(),
                        spec.typeName(),
                        spec.name(),
                        spec.marketPrice(),
                        spec.monthlyRent(),
                        spec.tradeCooldownDays(),
                        randomValuation()
                ))
                .forEach(offerRepository::save);
    }

    private ValuationStatus randomValuation() {
        int roll = random.nextInt(100);
        if (roll < 30) {
            return ValuationStatus.UNDER;
        }
        if (roll < 70) {
            return ValuationStatus.FAIR;
        }
        return ValuationStatus.OVER;
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
                    .ifPresent(OwnedBuilding::moveInProtectedTenant);
            player.markFirstTenantEventDone();
        }
    }

    private void runDailySettlement(Player player) {
        if (player.getDay() == 1) {
            if (player.isEmployed()) {
                player.addSalaryIncome(MONTHLY_JOB_SALARY);
            }
            int oldReputation = player.getReputation();
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity()).stream()
                    .filter(OwnedBuilding::isOccupied)
                    .forEach(building -> {
                        player.addMonthlyRentIncome(building.getMonthlyRent());
                        player.addReputation(random.nextInt(5) + 1);
                    });
            reputationCatalog.newlyUnlocked(oldReputation, player.getReputation(), !player.isEmployed()).stream()
                    .findFirst()
                    .ifPresent(tier -> activateUnlockEvent(player, tier));
        }
        if (player.getDay() == 15 && player.isFirstSecretaryHired()) {
            player.addSecretarySalaryCost(FIRST_SECRETARY_SALARY);
        }
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }

    private void activateUnlockEvent(Player player, ReputationTier tier) {
        if (activeEvent(player).isPresent()) {
            return;
        }
        GameEventDefinition definition = new GameEventDefinition(
                "unlock_" + tier.unlockCity() + "_" + tier.unlockBuildingSlot() + "_" + player.getReputation(),
                player.getMonth(),
                player.getDay(),
                "새 건물이 해금되었습니다",
                tier.unlockLabel() + " 매물을 거래할 수 있습니다.",
                "AI 해금 이미지",
                "NONE"
        );
        activateEvent(player, definition);
    }
}
