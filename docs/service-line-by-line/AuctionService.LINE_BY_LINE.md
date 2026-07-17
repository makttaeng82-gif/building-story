# AuctionService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/AuctionService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드는 설명하지 않는다.

```java
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
// 해설: 경매 이벤트의 생성, 현재 경매 조회, 입찰, 취소, 결과 완료를 담당하는 서비스다.
    /*
     * 경매 이벤트의 생성, 입찰, 결과 처리를 담당한다.
     *
     * 경매는 일반 매물과 달리 제한 시간 안에 입찰해야 한다.
     * ACTIVE 상태로 생성되고, 시간이 지나면 결과 상태로 바뀐다.
     */
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
    // 해설: 경매 처리에 필요한 플레이어, 경매, 건물, 기록 Repository와 건물 카탈로그, 비서 이벤트 서비스를 생성자 주입으로 받는다.
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
    // 해설: 플레이어에게 현재 처리해야 할 경매가 있는지 조회한다.
        // 조회 시점에 만료 여부도 함께 정리한다. 별도 스케줄러 없이 화면 진입만으로 상태가 최신화된다.
        Optional<AuctionEvent> auction = auctionEventRepository.findFirstByPlayerAndStatusInOrderByIdDesc(
        // 해설: ACTIVE 또는 RESULT 상태 경매 중 가장 최근 경매를 가져온다.
                player,
                List.of(AuctionStatus.ACTIVE, AuctionStatus.RESULT)
                // 해설: 진행 중인 경매와 결과 확인 대기 경매를 조회 대상으로 삼는다.
        );
        auction.filter(this::isExpired)
        // 해설: 조회된 경매가 제한 시간을 넘겼는지 검사한다.
                .filter(active -> active.getStatus() == AuctionStatus.ACTIVE)
                // 해설: 만료된 ACTIVE 경매만 결과 상태로 넘긴다. 이미 RESULT인 경매는 그대로 둔다.
                .ifPresent(AuctionEvent::complete);
                // 해설: 시간이 만료된 경매를 complete 도메인 메서드로 결과 상태 처리한다.
        return auction.filter(active -> active.getStatus() != AuctionStatus.COMPLETED);
        // 해설: 완료된 경매는 제외하고, 화면에서 처리해야 할 경매만 Optional로 반환한다.
    }

    public Optional<AuctionEvent> tryActivate(Player player) {
    // 해설: 하루 진행 마지막에 확률적으로 새 경매를 생성한다.
        // 하루 진행 마지막에 낮은 확률로 경매를 연다. 현재 도시 보유 한도에 도달하면 생성하지 않는다.
        if (!rollPercent(AUCTION_CHANCE_PERCENT)) {
        // 해설: 정해진 확률에 실패하면 오늘은 경매를 만들지 않는다.
            return Optional.empty();
            // 해설: 생성되거나 조회된 대상이 없음을 Optional.empty로 표현한다.
        }
        if (ownedBuildingRepository.countByPlayerAndCity(player, player.getCurrentCity()) >= CITY_BUILDING_LIMIT) {
        // 해설: 현재 도시 보유 건물 한도에 도달한 경우 경매를 생성하지 않는다.
            return Optional.empty();
            // 해설: 생성되거나 조회된 대상이 없음을 Optional.empty로 표현한다.
        }
        List<BuildingSpec> citySpecs = buildingCatalog.byCity(player.getCurrentCity());
        // 해설: 현재 도시에서 경매 후보가 될 건물 스펙 목록을 가져온다.
        if (citySpecs.isEmpty()) {
        // 해설: 도시에 등록된 건물 스펙이 없으면 경매를 만들 수 없다.
            return Optional.empty();
            // 해설: 생성되거나 조회된 대상이 없음을 Optional.empty로 표현한다.
        }
        BuildingSpec spec = citySpecs.get(random.nextInt(citySpecs.size()));
        // 해설: 현재 도시 건물 중 하나를 무작위로 경매 대상으로 고른다.
        return Optional.of(auctionEventRepository.save(new AuctionEvent(
        // 해설: 선택한 건물 스펙으로 새 경매 이벤트를 저장하고 Optional로 감싸 반환한다.
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
    // 해설: 사용자가 경매 입찰 버튼을 눌렀을 때 실행된다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        // 해설: 입찰 대상 경매를 DB에서 조회한다.
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() != AuctionStatus.ACTIVE) {
        // 해설: 다른 플레이어 경매이거나 ACTIVE 상태가 아니면 입찰할 수 없다.
            throw new IllegalArgumentException("잘못된 경매");
            // 해설: 경매 검증 실패를 예외로 중단한다.
        }
        if (isExpired(auction)) {
        // 해설: 입찰 시점에 제한 시간이 지났는지 다시 검사한다.
            auction.complete();
            // 해설: 만료된 경매를 결과 또는 완료 상태로 전환한다.
            return "경매 시간이 종료되었습니다";
            // 해설: 시간 만료로 입찰하지 못했다는 메시지를 반환한다.
        }
        if (ownedBuildingRepository.countByPlayerAndCity(player, auction.getCity()) >= CITY_BUILDING_LIMIT) {
        // 해설: 입찰하려는 경매 도시의 보유 한도를 다시 검사한다.
            auction.complete();
            // 해설: 만료된 경매를 결과 또는 완료 상태로 전환한다.
            return auction.getCity() + " 보유 제한 8채 도달";
            // 해설: 보유 제한 때문에 입찰할 수 없다는 메시지를 반환한다.
        }
        int successChance = successChance(rate);
        // 해설: 선택한 입찰가 비율에 따른 낙찰 확률을 계산한다.
        if (successChance == 0) {
        // 해설: 허용되지 않는 입찰 비율이면 잘못된 요청이다.
            throw new IllegalArgumentException("잘못된 입찰가");
        }
        long price = auction.bidPrice(rate);
        // 해설: 시장가 대비 입찰 비율로 실제 입찰 금액을 계산한다.
        if (!player.spendCash(price)) {
        // 해설: 입찰 금액을 현금에서 먼저 차감한다.
            return "현금 부족";
            // 해설: 입찰 금액을 낼 현금이 부족하면 입찰을 중단한다.
        }
        boolean successful = random.nextInt(100) < successChance;
        // 해설: 낙찰 확률을 난수로 판정한다.
        if (successful) {
        // 해설: 낙찰에 성공한 경우 보유 건물을 생성한다.
            BuildingSpec auctionSpec = catalogSpec(auction.getCity(), auction.getBuildingSlot()).orElse(null);
            // 해설: 경매 건물의 슬롯 기준 카탈로그 스펙을 찾아 이름과 타입을 보정한다.
            String typeName = auctionSpec == null ? auction.getTypeName() : auctionSpec.typeName();
            // 해설: 카탈로그 스펙이 있으면 스펙의 타입명을 우선 사용한다.
            String buildingName = auctionSpec == null ? auction.getName() : auctionSpec.name();
            // 해설: 카탈로그 스펙이 있으면 스펙의 건물명을 우선 사용한다.
            OwnedBuilding purchasedBuilding = ownedBuildingRepository.save(new OwnedBuilding(
            // 해설: 낙찰된 건물을 플레이어 보유 건물로 저장한다.
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
            // 해설: 낙찰 건물이 비서 세입자 인트로 조건이면 이벤트를 열 수 있는지 시도한다.
            saveRecord(player, RecordType.BUILDING_BUY, "경매 낙찰", -price, 0, auction.getName(), "시장가 " + rate + "% 입찰");
            // 해설: 낙찰 지출 기록을 월간 기록에 저장한다.
            auction.resolve(rate, successChance, true, "경매 낙찰 성공");
            // 해설: 경매를 결과 상태로 바꾸고 입찰률, 성공확률, 성공 여부, 결과 메시지를 저장한다.
        } else {
        // 해설: 낙찰에 실패한 경우 입찰금을 돌려주고 패찰 기록을 남긴다.
            player.addCash(price);
            // 해설: 입찰 전에 차감했던 현금을 다시 돌려준다.
            saveRecord(player, RecordType.BUILDING_BUY, "경매 패찰", null, 0, auction.getName(), "시장가 " + rate + "% 입찰 실패");
            // 해설: 패찰 기록을 월간 기록에 남긴다. 실제 지출은 없으므로 amount는 null이다.
            auction.resolve(rate, successChance, false, "경매 낙찰 실패");
            // 해설: 경매 결과에 실패 상태와 메시지를 저장한다.
        }
        return auction.getResultMessage();
        // 해설: 낙찰 성공/실패 결과 메시지를 반환한다.
    }

    public String cancel(long playerId, long auctionId) {
    // 해설: 경매를 사용자가 취소할 때 실행된다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        // 해설: 취소 대상 경매를 DB에서 조회한다.
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() == AuctionStatus.COMPLETED) {
        // 해설: 다른 플레이어 경매이거나 이미 완료된 경매면 취소할 수 없다.
            throw new IllegalArgumentException("잘못된 경매");
            // 해설: 경매 검증 실패를 예외로 중단한다.
        }
        auction.complete();
        // 해설: 만료된 경매를 결과 또는 완료 상태로 전환한다.
        return "경매 취소";
        // 해설: 취소 완료 메시지를 반환한다.
    }

    public void completeResult(long playerId, long auctionId) {
    // 해설: 경매 결과 화면을 확인한 뒤 결과 상태를 최종 완료로 바꾼다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        AuctionEvent auction = auctionEventRepository.findById(auctionId).orElseThrow();
        // 해설: 결과 확인을 완료할 경매를 DB에서 조회한다.
        if (!auction.getPlayer().getId().equals(player.getId()) || auction.getStatus() != AuctionStatus.RESULT) {
        // 해설: 해당 플레이어의 RESULT 상태 경매만 완료할 수 있다.
            throw new IllegalArgumentException("잘못된 경매");
            // 해설: 경매 검증 실패를 예외로 중단한다.
        }
        auction.complete();
        // 해설: 만료된 경매를 결과 또는 완료 상태로 전환한다.
    }

    private boolean isExpired(AuctionEvent auction) {
    // 해설: 경매 제한 시간이 지났는지 계산한다.
        return auction.getCreatedAt() != null
        // 해설: 생성 시간이 있는 경매만 만료 여부를 판단할 수 있다.
                && auction.getCreatedAt().plusSeconds(AUCTION_DURATION_SECONDS).isBefore(LocalDateTime.now());
                // 해설: 생성 시간에 제한 시간을 더한 시각이 현재보다 과거면 만료다.
    }

    private int successChance(int rate) {
    // 해설: 입찰가 비율에 따른 낙찰 확률을 반환한다.
        return switch (rate) {
        // 해설: 허용된 입찰 비율만 확률로 매핑한다.
            case 90 -> 80;
            // 해설: 시장가 90% 입찰은 80% 확률이다.
            case 70 -> 60;
            // 해설: 시장가 70% 입찰은 60% 확률이다.
            case 50 -> 40;
            // 해설: 시장가 50% 입찰은 40% 확률이다.
            default -> 0;
            // 해설: 그 외 비율은 허용하지 않는다.
        };
    }

    private Optional<BuildingSpec> catalogSpec(String city, Integer slot) {
    // 해설: 도시와 슬롯으로 건물 카탈로그 스펙을 찾는다.
        if (slot == null) {
        // 해설: 슬롯 정보가 없으면 카탈로그 매칭을 할 수 없다.
            return Optional.empty();
            // 해설: 생성되거나 조회된 대상이 없음을 Optional.empty로 표현한다.
        }
        return buildingCatalog.byCity(city).stream()
        // 해설: 해당 도시의 건물 스펙 목록을 순회한다.
                .filter(candidate -> candidate.slot() == slot)
                // 해설: 슬롯 번호가 같은 건물 스펙만 남긴다.
                .findFirst();
    }

    private boolean rollPercent(double percent) {
    // 해설: 퍼센트 확률 판정을 수행한다.
        return random.nextInt(100) < percent;
        // 해설: 0~99 난수가 percent보다 작으면 성공이다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 경매 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 월간 기록 엔티티를 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 기준일은 최소 1일로 보정한다.
    }
}
```
