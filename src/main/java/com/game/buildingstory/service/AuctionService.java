package com.game.buildingstory.service;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
import com.game.buildingstory.domain.EconomyBalanceRules;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.AuctionEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AuctionService {
    /*
     * 경매 이벤트의 생성, 입찰, 결과 처리를 담당한다.
     *
     * 경매는 일반 매물과 달리 제한 시간 안에 입찰해야 한다.
     * ACTIVE 상태로 생성되고, 시간이 지나면 결과 상태로 바뀐다.
     */
    private static final int CITY_BUILDING_LIMIT = 8;
    private static final int RECORD_RETENTION_DAYS = 62;
    private static final int AUCTION_CHANCE_PERCENT = 2;
    private static final int AUCTION_DURATION_SECONDS = 20;

    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final AuctionEventRepository auctionEventRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final BuildingCatalog buildingCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final ReputationCatalog reputationCatalog;
    private final CityMarketIndexService cityMarketIndexService;

    public AuctionService(
            PlayerRepository playerRepository,
            AuctionEventRepository auctionEventRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            BuildingCatalog buildingCatalog,
            SecretaryTenantEventService secretaryTenantEventService,
            ReputationCatalog reputationCatalog,
            CityMarketIndexService cityMarketIndexService
    ) {
        this.playerRepository = playerRepository;
        this.auctionEventRepository = auctionEventRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.buildingCatalog = buildingCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.reputationCatalog = reputationCatalog;
        this.cityMarketIndexService = cityMarketIndexService;
    }

    public Optional<AuctionEvent> activeAuction(Player player) {
        // 조회 시점에 만료 여부도 함께 정리한다. 별도 스케줄러 없이 화면 진입만으로 상태가 최신화된다.
        Optional<AuctionEvent> auction = auctionEventRepository.findFirstByPlayerAndStatusInOrderByIdDesc(
                player,
                List.of(AuctionStatus.ACTIVE, AuctionStatus.RESULT)
        );
        auction.filter(this::isExpired)
                .filter(active -> active.getStatus() == AuctionStatus.ACTIVE)
                .ifPresent(AuctionEvent::complete);
        return auction.filter(active -> active.getStatus() != AuctionStatus.COMPLETED);
    }

    public Optional<AuctionEvent> tryActivate(Player player) {
        // 하루 진행 마지막에 낮은 확률로 경매를 연다. 현재 도시 보유 한도에 도달하면 생성하지 않는다.
        if (!rollPercent(AUCTION_CHANCE_PERCENT)) {
            return Optional.empty();
        }
        if (ownedBuildingRepository.countByPlayerAndCity(player, player.getCurrentCity()) >= CITY_BUILDING_LIMIT) {
            return Optional.empty();
        }
        List<BuildingSpec> citySpecs = buildingCatalog.byCity(player.getCurrentCity());
        if (citySpecs.isEmpty()) {
            return Optional.empty();
        }
        BuildingSpec spec = citySpecs.get(random.nextInt(citySpecs.size()));
        return Optional.of(auctionEventRepository.save(new AuctionEvent(
                player,
                spec.city(),
                spec.slot(),
                spec.typeName(),
                spec.name(),
                cityMarketIndexService.marketValue(player, spec.city(), spec.slot(), spec.marketPrice()),
                spec.monthlyRent(),
                spec.tradeCooldownDays()
        )));
    }

    public String bid(long playerId, long auctionId, int rate) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalArgumentException("잘못된 경매");
        }
        if (isExpired(auction)) {
            auction.complete();
            return "경매 시간이 종료되었습니다";
        }
        if (ownedBuildingRepository.countByPlayerAndCity(player, auction.getCity()) >= CITY_BUILDING_LIMIT) {
            auction.complete();
            return auction.getCity() + " 보유 제한 8채 도달";
        }
        int successChance = successChance(rate);
        if (successChance == 0) {
            throw new IllegalArgumentException("잘못된 입찰가");
        }
        long originalBidPrice = auction.bidPrice(rate);
        boolean governmentSupported = auction.isGovernmentSupportEligible();
        long price = auction.effectiveBidPrice(rate);
        long purchaseFee = EconomyBalanceRules.purchaseFee(price);
        long totalPurchasePrice = price + purchaseFee;
        if (!player.spendCash(totalPurchasePrice)) {
            return "현금 부족";
        }
        boolean successful = random.nextInt(100) < successChance;
        if (successful) {
            BuildingSpec auctionSpec = catalogSpec(auction.getCity(), auction.getBuildingSlot()).orElse(null);
            String typeName = auctionSpec == null ? auction.getTypeName() : auctionSpec.typeName();
            String buildingName = auctionSpec == null ? auction.getName() : auctionSpec.name();
            OwnedBuilding purchasedBuilding = ownedBuildingRepository.save(new OwnedBuilding(
                    player,
                    auction.getCity(),
                    auction.getBuildingSlot(),
                    typeName,
                    buildingName,
                    auction.getMarketPrice(),
                    price,
                    auction.getMonthlyRent(),
                    auction.getTradeCooldownDays(),
                    governmentSupported ? originalBidPrice - price : 0L
            ));
            if (governmentSupported) {
                player.claimGovernmentPurchaseSupport(auction.getCity());
            }
            awardBuildingMilestone(player, purchasedBuilding);
            secretaryTenantEventService.tryActivateIntro(player, purchasedBuilding);
            saveRecord(player, RecordType.BUILDING_BUY, "경매 낙찰", -totalPurchasePrice, 0, auction.getName(), "시장가 " + rate + "% 입찰 · " + (governmentSupported ? "정부지원 " + (originalBidPrice - price) + "원 · " : "") + "부대비용 " + purchaseFee + "원");
            auction.resolve(rate, successChance, true, "경매 낙찰 성공");
        } else {
            long deposit = EconomyBalanceRules.auctionDeposit(originalBidPrice);
            player.addCash(totalPurchasePrice - deposit);
            saveRecord(player, RecordType.BUILDING_BUY, "경매 패찰", -deposit, 0, auction.getName(), "시장가 " + rate + "% 입찰 실패 · 보증비 " + deposit + "원");
            auction.resolve(rate, successChance, false, "경매 낙찰 실패");
        }
        return auction.getResultMessage();
    }

    public String cancel(long playerId, long auctionId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() == AuctionStatus.COMPLETED) {
            throw new IllegalArgumentException("잘못된 경매");
        }
        auction.complete();
        return "경매 취소";
    }

    public void completeResult(long playerId, long auctionId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() != AuctionStatus.RESULT) {
            throw new IllegalArgumentException("잘못된 경매");
        }
        auction.complete();
    }

    private boolean isExpired(AuctionEvent auction) {
        return auction.getCreatedAt() != null
                && auction.getCreatedAt().plusSeconds(AUCTION_DURATION_SECONDS).isBefore(LocalDateTime.now());
    }

    private int successChance(int rate) {
        return switch (rate) {
            case 95 -> 70;
            case 88 -> 35;
            case 80 -> 10;
            default -> 0;
        };
    }

    private Optional<BuildingSpec> catalogSpec(String city, Integer slot) {
        if (slot == null) {
            return Optional.empty();
        }
        return buildingCatalog.byCity(city).stream()
                .filter(candidate -> candidate.slot() == slot)
                .findFirst();
    }

    private boolean rollPercent(double percent) {
        return random.nextInt(100) < percent;
    }

    private void awardBuildingMilestone(Player player, OwnedBuilding building) {
        int slot = building.getBuildingSlot() == null ? 1 : building.getBuildingSlot();
        int reputationReward = EconomyBalanceRules.buildingMilestoneReputation(building.getCity(), slot);
        if (reputationReward <= 0 || !player.claimBuildingMilestone(building.getCity(), slot)) {
            return;
        }
        player.addReputation(reputationReward);
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        saveRecord(player, RecordType.BUILDING_BUY, "최초 건물 단계 달성", null, reputationReward, building.getName(), null);
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
