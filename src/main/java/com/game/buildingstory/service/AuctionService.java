package com.game.buildingstory.service;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
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
    private static final int CITY_BUILDING_LIMIT = 8;
    private static final int RECORD_RETENTION_DAYS = 62;
    private static final int AUCTION_CHANCE_PERCENT = 3;
    private static final int AUCTION_DURATION_SECONDS = 20;

    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final AuctionEventRepository auctionEventRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final BuildingCatalog buildingCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;

    public AuctionService(
            PlayerRepository playerRepository,
            AuctionEventRepository auctionEventRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            BuildingCatalog buildingCatalog,
            SecretaryTenantEventService secretaryTenantEventService
    ) {
        this.playerRepository = playerRepository;
        this.auctionEventRepository = auctionEventRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.buildingCatalog = buildingCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
    }

    public Optional<AuctionEvent> activeAuction(Player player) {
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
                spec.marketPrice(),
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
        long price = auction.bidPrice(rate);
        if (!player.spendCash(price)) {
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
                    auction.getTradeCooldownDays()
            ));
            secretaryTenantEventService.tryActivateIntro(player, purchasedBuilding);
            saveRecord(player, RecordType.BUILDING_BUY, "경매 낙찰", -price, 0, auction.getName(), "시장가 " + rate + "% 입찰");
            auction.resolve(rate, successChance, true, "경매 낙찰 성공");
        } else {
            player.addCash(price);
            saveRecord(player, RecordType.BUILDING_BUY, "경매 패찰", null, 0, auction.getName(), "시장가 " + rate + "% 입찰 실패");
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
            case 90 -> 80;
            case 70 -> 60;
            case 50 -> 40;
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

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
