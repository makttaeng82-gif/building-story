package com.game.buildingstory.service;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PurchaseCooldown;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.domain.ValuationStatus;
import com.game.buildingstory.repo.BuildingOfferRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.PurchaseCooldownRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class BuildingTradeService {
    private static final int CITY_BUILDING_LIMIT = 8;
    private static final int REPAIR_REPUTATION_REWARD = 3;
    private static final int RECORD_RETENTION_DAYS = 62;

    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final BuildingOfferRepository offerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final LoanRepository loanRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final PurchaseCooldownRepository purchaseCooldownRepository;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final LoanService loanService;

    public BuildingTradeService(
            PlayerRepository playerRepository,
            BuildingOfferRepository offerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            LoanRepository loanRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            PurchaseCooldownRepository purchaseCooldownRepository,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            SecretaryTenantEventService secretaryTenantEventService,
            LoanService loanService
    ) {
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.loanRepository = loanRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.purchaseCooldownRepository = purchaseCooldownRepository;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.loanService = loanService;
    }

    @Transactional(readOnly = true)
    public List<BuildingOffer> offers(Player player) {
        return offerRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
    }

    @Transactional(readOnly = true)
    public List<OwnedBuilding> ownedBuildings(Player player) {
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
    }

    public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        BuildingOffer offer = offerRepository.findById(offerId).orElseThrow();
        if (!offer.getPlayer().getId().equals(player.getId())) {
            throw new IllegalArgumentException("잘못된 매물");
        }
        long ownedCount = ownedBuildingRepository.countByPlayerAndCity(player, offer.getCity());
        if (ownedCount >= CITY_BUILDING_LIMIT) {
            return offer.getCity() + " 보유 제한 8채 도달";
        }
        if (!isOfferUnlocked(player, offer)) {
            return "아직 해금되지 않은 매물";
        }
        int purchaseCooldownDaysLeft = purchaseCooldownDaysLeft(player, offer);
        if (purchaseCooldownDaysLeft > 0) {
            return "구매 쿨타임 D-" + purchaseCooldownDaysLeft;
        }

        long cashCost = loanPurchase ? offer.cashForLoanPurchase() : offer.getOfferPrice();
        if (loanPurchase && loanService.remainingPrincipal(player) + offer.loanAmount() > loanService.loanLimit(player)) {
            return "대출 한도 초과";
        }
        if (!player.spendCash(cashCost)) {
            return "현금 부족";
        }
        OwnedBuilding purchasedBuilding = ownedBuildingRepository.save(new OwnedBuilding(player, offer));
        if (loanPurchase) {
            loanRepository.save(new Loan(player, offer.loanAmount()));
        }
        saveRecord(
                player,
                RecordType.BUILDING_BUY,
                loanPurchase ? "대출구매" : "현금구매",
                -cashCost,
                0,
                offer.getName(),
                null
        );
        startPurchaseCooldown(player, offer);
        secretaryTenantEventService.tryActivateIntro(player, purchasedBuilding);
        return loanPurchase ? "대출구매 완료" : "현금구매 완료";
    }

    public String sellBuilding(long playerId, long buildingId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        Optional<OwnedBuilding> buildingOptional = ownedBuildingRepository.findById(buildingId);
        if (buildingOptional.isEmpty() || !buildingOptional.get().getPlayer().getId().equals(player.getId())) {
            return "잘못된 건물";
        }
        OwnedBuilding building = buildingOptional.get();
        if (building.isProtectedTenant()) {
            return building.isSecretaryResident() ? "비서 거주중 건물은 판매 불가" : "거주 이벤트 진행 중인 건물은 판매 불가";
        }
        if (!canSell(player, building)) {
            return "판매 쿨타임 D-" + daysUntilSellable(player, building);
        }
        ValuationStatus valuationStatus = randomValuation();
        long sellPrice = building.getMarketPrice() * valuationStatus.rate() / 100;
        player.addCash(sellPrice);
        saveRecord(player, RecordType.BUILDING_SELL, "건물 판매", sellPrice, 0, building.getName(), valuationStatus.label());
        ownedBuildingRepository.delete(building);
        return "건물 판매 완료 · " + valuationStatus.label() + " " + sellPrice + "원";
    }

    public String repairBuilding(long playerId, long buildingId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        Optional<OwnedBuilding> buildingOptional = ownedBuildingRepository.findById(buildingId);
        if (buildingOptional.isEmpty() || !buildingOptional.get().getPlayer().getId().equals(player.getId())) {
            return "잘못된 건물";
        }
        OwnedBuilding building = buildingOptional.get();
        if (!building.isRepairRequested()) {
            return "수리요청 없음";
        }
        long repairCost = building.repairCost();
        if (!player.spendCash(repairCost)) {
            return "수리비 부족 · 필요 금액 " + repairCost + "원";
        }
        boolean repairedWithinOneMonth = building.repair();
        int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
        if (repairedWithinOneMonth) {
            player.addReputation(REPAIR_REPUTATION_REWARD);
            refreshTitle(player);
        }
        saveRecord(player, RecordType.REPAIR_COMPLETE, "수리 완료", -repairCost, reputationChange, building.getName(), null);
        return repairedWithinOneMonth ? "수리 완료 · 평판 +" + REPAIR_REPUTATION_REWARD : "수리 완료";
    }

    public void ensureOffers(Player player) {
        if (!offerRepository.existsByPlayerAndCity(player, player.getCurrentCity())
                && !purchaseCooldownRepository.existsByPlayerAndCity(player, player.getCurrentCity())) {
            refreshOffers(player);
        }
    }

    public void refreshOffers(Player player) {
        offerRepository.deleteByPlayerAndCity(player, player.getCurrentCity());
        buildingCatalog.byCity(player.getCurrentCity()).stream()
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

    @Transactional(readOnly = true)
    public boolean isOfferUnlocked(Player player, BuildingOffer offer) {
        return reputationCatalog.isBuildingUnlocked(offer.getCity(), offer.getBuildingSlot(), player.getReputation(), !player.isEmployed());
    }

    @Transactional(readOnly = true)
    public int purchaseCooldownDaysLeft(Player player, BuildingOffer offer) {
        return purchaseCooldownRepository.findByPlayerAndCityAndBuildingSlot(player, offer.getCity(), offer.getBuildingSlot())
                .map(cooldown -> cooldown.daysLeft(player.getElapsedDays()))
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int daysUntilSellable(Player player, OwnedBuilding building) {
        int purchaseDay = building.getPurchaseDayCountForCalculation(player.getElapsedDays());
        int effectiveCooldown = effectiveBuildingWaitDays(player, building.getCity(), building.getTradeCooldownDays());
        return Math.max(0, purchaseDay + effectiveCooldown - player.getElapsedDays());
    }

    @Transactional(readOnly = true)
    public boolean canSell(Player player, OwnedBuilding building) {
        return daysUntilSellable(player, building) == 0 && !building.isProtectedTenant();
    }

    @Transactional(readOnly = true)
    public String sellAvailabilityText(Player player, OwnedBuilding building) {
        if (building.isSecretaryResident()) {
            return "비서 거주중";
        }
        if (building.isProtectedTenant()) {
            return "판매불가";
        }
        int daysLeft = daysUntilSellable(player, building);
        return daysLeft == 0 ? "가능" : player.ddayText(daysLeft);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(BuildingOffer offer) {
        return buildingImagePath(offer.getCity(), offer.getBuildingSlot());
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(OwnedBuilding building) {
        int slot = building.getBuildingSlot() == null ? catalogSlot(building.getCity(), building.getTypeName(), building.getName()) : building.getBuildingSlot();
        return buildingImagePath(building.getCity(), slot);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(AuctionEvent auction) {
        int slot = auction.getBuildingSlot() == null ? catalogSlot(auction.getCity(), auction.getTypeName(), auction.getName()) : auction.getBuildingSlot();
        return buildingImagePath(auction.getCity(), slot);
    }

    @Transactional(readOnly = true)
    public String auctionDisplayName(AuctionEvent auction) {
        return catalogSpec(auction.getCity(), auction.getBuildingSlot())
                .map(BuildingSpec::name)
                .orElse(auction.getName());
    }

    @Transactional(readOnly = true)
    public String auctionDisplayTypeName(AuctionEvent auction) {
        return catalogSpec(auction.getCity(), auction.getBuildingSlot())
                .map(BuildingSpec::typeName)
                .orElse(auction.getTypeName());
    }

    public int effectiveBuildingWaitDays(Player player, String city, int baseDays) {
        double reduction = buildingWaitReductionPercent(player, city);
        return Math.max(1, (int) Math.round(baseDays * (100.0 - reduction) / 100.0));
    }

    private void startPurchaseCooldown(Player player, BuildingOffer offer) {
        int availableDayCount = player.getElapsedDays() + effectiveBuildingWaitDays(player, offer.getCity(), offer.getTradeCooldownDays());
        PurchaseCooldown cooldown = purchaseCooldownRepository
                .findByPlayerAndCityAndBuildingSlot(player, offer.getCity(), offer.getBuildingSlot())
                .orElseGet(() -> purchaseCooldownRepository.save(new PurchaseCooldown(player, offer.getCity(), offer.getBuildingSlot(), availableDayCount)));
        cooldown.reset(availableDayCount);
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

    private double buildingWaitReductionPercent(Player player, String city) {
        return ownedSecretaryRepository.findByPlayerOrderById(player).stream()
                .filter(secretary -> secretary.isAssignedTo(city))
                .findFirst()
                .map(secretary -> switch (secretary.getSecretaryKey()) {
                    case "secretary-5" -> 0.5 * secretary.getAffinity();
                    case "secretary-6" -> 1.0 * secretary.getAffinity();
                    default -> 0.0;
                })
                .orElse(0.0);
    }

    private Optional<BuildingSpec> catalogSpec(String city, Integer slot) {
        if (slot == null) {
            return Optional.empty();
        }
        return buildingCatalog.byCity(city).stream()
                .filter(candidate -> candidate.slot() == slot)
                .findFirst();
    }

    private String buildingImagePath(String city, int slot) {
        return "/assets/buildings/" + citySlug(city) + "-" + Math.max(1, Math.min(4, slot)) + ".jpg";
    }

    private int catalogSlot(String city, String typeName, String name) {
        return buildingCatalog.all().stream()
                .filter(spec -> spec.city().equals(city))
                .filter(spec -> spec.name().equals(name) || spec.typeName().equals(typeName))
                .map(BuildingSpec::slot)
                .findFirst()
                .orElse(1);
    }

    private String citySlug(String city) {
        return switch (city) {
            case "청주" -> "cheongju";
            case "세종" -> "sejong";
            case "대전" -> "daejeon";
            case "부산" -> "busan";
            case "인천" -> "incheon";
            case "서울" -> "seoul";
            default -> "cheongju";
        };
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
