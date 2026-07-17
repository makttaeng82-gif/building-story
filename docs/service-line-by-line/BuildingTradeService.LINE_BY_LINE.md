# BuildingTradeService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/BuildingTradeService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
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
// 해설: 부동산 매물 조회, 구매, 판매, 수리, 매물 갱신 규칙을 담당하는 서비스다.
    /*
     * 부동산 매물과 보유 건물의 거래 규칙을 담당한다.
     *
     * Offer는 아직 사지 않은 시장 매물이고, OwnedBuilding은 구매 후 실제 보유 건물이다.
     * 구매하면 Offer의 가격/월세/쿨다운 정보가 OwnedBuilding으로 복사되고,
     * 대출 구매라면 Loan도 함께 생성된다.
     */
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
    // 해설: 거래 로직에 필요한 Repository, 카탈로그, 평판, 대출, 비서 이벤트 서비스를 생성자 주입으로 받는다.
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
    // 해설: 현재 도시에서 화면에 보여줄 매물 목록을 조회한다.
        return offerRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
        // 해설: 플레이어와 현재 도시 기준으로 매물을 id 순서로 가져온다.
    }

    @Transactional(readOnly = true)
    public List<OwnedBuilding> ownedBuildings(Player player) {
    // 해설: 현재 도시에서 플레이어가 보유한 건물 목록을 조회한다.
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
        // 해설: 플레이어와 현재 도시 기준으로 보유 건물을 id 순서로 가져온다.
    }

    public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
    // 해설: 매물 구매 요청을 처리한다. 현금 구매와 대출 구매를 같은 흐름 안에서 분기한다.
        // 구매 요청은 현금구매와 대출구매를 같은 흐름으로 처리한다. 차이는 cashCost와 Loan 생성 여부다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다. 이후 현금 차감과 보유 건물 생성은 이 엔티티 기준으로 처리된다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다. 이벤트 처리 중 구매가 끼어드는 것을 방지한다.
            return pausedActionMessage();
            // 해설: 일시정지 상태에서 경제 행동을 시도했을 때 공통 실패 메시지를 반환한다.
        }
        BuildingOffer offer = offerRepository.findById(offerId).orElseThrow();
        // 해설: 구매 대상 매물을 조회한다. 없는 id면 정상 요청이 아니므로 예외가 난다.
        if (!offer.getPlayer().getId().equals(player.getId())) {
        // 해설: 다른 플레이어의 매물을 구매하지 못하게 소유자를 검증한다.
            throw new IllegalArgumentException("잘못된 매물");
            // 해설: 매물 소유자가 맞지 않으면 잘못된 요청으로 중단한다.
        }
        long ownedCount = ownedBuildingRepository.countByPlayerAndCity(player, offer.getCity());
        // 해설: 구매하려는 도시에서 이미 몇 채를 보유 중인지 센다.
        if (ownedCount >= CITY_BUILDING_LIMIT) {
        // 해설: 도시별 보유 제한에 도달하면 추가 구매를 막는다.
            return offer.getCity() + " 보유 제한 8채 도달";
            // 해설: 보유 제한 때문에 구매할 수 없다는 메시지를 반환한다.
        }
        if (!isOfferUnlocked(player, offer)) {
        // 해설: 평판이나 퇴사 조건 기준으로 아직 해금되지 않은 매물인지 확인한다.
            return "아직 해금되지 않은 매물";
            // 해설: 해금 조건을 만족하지 못하면 구매를 거부한다.
        }
        int purchaseCooldownDaysLeft = purchaseCooldownDaysLeft(player, offer);
        // 해설: 같은 도시/슬롯 매물을 다시 살 수 있기까지 남은 날짜를 계산한다.
        if (purchaseCooldownDaysLeft > 0) {
        // 해설: 구매 쿨타임이 남아 있으면 구매를 막는다.
            return "구매 쿨타임 D-" + purchaseCooldownDaysLeft;
            // 해설: 남은 쿨타임을 D-day 형식으로 반환한다.
        }

        long cashCost = loanPurchase ? offer.cashForLoanPurchase() : offer.getOfferPrice();
        // 해설: 대출 구매면 일부 현금만 필요하고, 현금 구매면 매물가 전체가 필요하다.
        if (loanPurchase && loanService.remainingPrincipal(player) + offer.loanAmount() > loanService.loanLimit(player)) {
        // 해설: 새 대출을 더했을 때 플레이어 대출 한도를 넘는지 검사한다.
            return "대출 한도 초과";
            // 해설: 대출 한도를 넘으면 구매를 거부한다.
        }
        if (!player.spendCash(cashCost)) {
        // 해설: 필요 현금을 차감한다. 잔액이 부족하면 false가 반환된다.
            return "현금 부족";
            // 해설: 현금 차감에 실패했으므로 구매를 중단한다.
        }
        OwnedBuilding purchasedBuilding = ownedBuildingRepository.save(new OwnedBuilding(player, offer));
        // 해설: 매물 정보를 복사해 실제 보유 건물로 저장한다.
        if (loanPurchase) {
        // 해설: 대출 구매인 경우에만 Loan 엔티티를 추가 생성한다.
            loanRepository.save(new Loan(player, offer.loanAmount()));
            // 해설: 매물에 정의된 대출 금액만큼 플레이어 대출을 저장한다.
        }
        saveRecord(
        // 해설: 구매 내역을 월간 기록에 남긴다. 현금 구매와 대출 구매 모두 이 기록을 사용한다.
                player,
                RecordType.BUILDING_BUY,
                loanPurchase ? "대출구매" : "현금구매",
                -cashCost,
                0,
                offer.getName(),
                null
        );
        startPurchaseCooldown(player, offer);
        // 해설: 구매한 도시/슬롯에 재구매 쿨타임을 건다.
        secretaryTenantEventService.tryActivateIntro(player, purchasedBuilding);
        // 해설: 구매한 건물이 특정 비서 세입자 인트로 조건이면 이벤트를 열 수 있는지 시도한다.
        return loanPurchase ? "대출구매 완료" : "현금구매 완료";
        // 해설: 구매 방식에 맞는 성공 메시지를 반환한다.
    }

    public String sellBuilding(long playerId, long buildingId) {
    // 해설: 보유 건물 판매 요청을 처리한다.
        // 판매가는 매각 시점에 무작위 평가 상태를 뽑아 결정한다. 그래서 같은 건물도 매각 타이밍마다 가격이 달라질 수 있다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다. 이후 판매 대금 지급과 건물 삭제는 이 플레이어 기준으로 처리된다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다. 이벤트 처리 중 구매가 끼어드는 것을 방지한다.
            return pausedActionMessage();
            // 해설: 일시정지 상태에서 경제 행동을 시도했을 때 공통 실패 메시지를 반환한다.
        }
        Optional<OwnedBuilding> buildingOptional = ownedBuildingRepository.findById(buildingId);
        // 해설: 판매 대상 건물을 Optional로 조회한다. 없는 건물일 수 있으므로 바로 get하지 않는다.
        if (buildingOptional.isEmpty() || !buildingOptional.get().getPlayer().getId().equals(player.getId())) {
        // 해설: 건물이 없거나 다른 플레이어 소유면 판매할 수 없다.
            return "잘못된 건물";
            // 해설: 판매 대상 검증 실패 메시지를 반환한다.
        }
        OwnedBuilding building = buildingOptional.get();
        // 해설: 검증이 끝난 Optional에서 실제 건물 엔티티를 꺼낸다.
        if (building.isProtectedTenant()) {
        // 해설: 비서 외 보호 세입자 이벤트가 있으면 판매불가로 표시한다.
            return building.isSecretaryResident() ? "비서 거주중 건물은 판매 불가" : "거주 이벤트 진행 중인 건물은 판매 불가";
            // 해설: 판매 불가 사유를 비서 거주와 일반 이벤트 거주로 나눠 반환한다.
        }
        if (!canSell(player, building)) {
        // 해설: 구매 후 판매 가능 쿨타임이 끝났는지 확인한다.
            return "판매 쿨타임 D-" + daysUntilSellable(player, building);
            // 해설: 판매 가능일까지 남은 일수를 안내한다.
        }
        ValuationStatus valuationStatus = randomValuation();
        // 해설: 판매 시점의 평가 상태를 무작위로 뽑는다. 저평가/정가/고평가에 따라 판매가가 달라진다.
        long sellPrice = building.getMarketPrice() * valuationStatus.rate() / 100;
        // 해설: 기준 시세에 평가 상태 비율을 곱해 최종 판매가를 계산한다.
        player.addCash(sellPrice);
        // 해설: 판매 대금을 플레이어 현금에 더한다.
        saveRecord(player, RecordType.BUILDING_SELL, "건물 판매", sellPrice, 0, building.getName(), valuationStatus.label());
        // 해설: 판매 기록을 월간 기록에 남기고, 평가 상태 라벨을 메모로 저장한다.
        ownedBuildingRepository.delete(building);
        // 해설: 판매가 끝난 건물을 보유 목록에서 삭제한다.
        return "건물 판매 완료 · " + valuationStatus.label() + " " + sellPrice + "원";
        // 해설: 판매 완료 메시지에 평가 상태와 판매가를 포함해 반환한다.
    }

    public String repairBuilding(long playerId, long buildingId) {
    // 해설: 수리 요청이 걸린 보유 건물을 수리한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다. 이후 수리비 차감과 평판 보상은 이 플레이어 기준으로 처리된다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다. 이벤트 처리 중 구매가 끼어드는 것을 방지한다.
            return pausedActionMessage();
            // 해설: 일시정지 상태에서 경제 행동을 시도했을 때 공통 실패 메시지를 반환한다.
        }
        Optional<OwnedBuilding> buildingOptional = ownedBuildingRepository.findById(buildingId);
        // 해설: 판매 대상 건물을 Optional로 조회한다. 없는 건물일 수 있으므로 바로 get하지 않는다.
        if (buildingOptional.isEmpty() || !buildingOptional.get().getPlayer().getId().equals(player.getId())) {
        // 해설: 건물이 없거나 다른 플레이어 소유면 판매할 수 없다.
            return "잘못된 건물";
            // 해설: 판매 대상 검증 실패 메시지를 반환한다.
        }
        OwnedBuilding building = buildingOptional.get();
        // 해설: 검증이 끝난 Optional에서 실제 건물 엔티티를 꺼낸다.
        if (!building.isRepairRequested()) {
        // 해설: 수리 요청 상태가 아닌 건물은 수리할 수 없다.
            return "수리요청 없음";
            // 해설: 수리할 대상이 없다는 메시지를 반환한다.
        }
        long repairCost = building.repairCost();
        // 해설: 건물 상태 기준으로 수리비를 계산한다.
        if (!player.spendCash(repairCost)) {
        // 해설: 수리비를 차감한다. 현금이 부족하면 실패한다.
            return "수리비 부족 · 필요 금액 " + repairCost + "원";
            // 해설: 부족한 수리비 정보를 포함한 메시지를 반환한다.
        }
        boolean repairedWithinOneMonth = building.repair();
        // 해설: 건물의 수리 상태를 완료로 바꾸고, 요청 후 한 달 내 수리인지 결과를 받는다.
        int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
        // 해설: 한 달 내 수리했을 때만 평판 보상을 준다.
        if (repairedWithinOneMonth) {
        // 해설: 평판 보상 조건을 만족하면 평판과 칭호를 갱신한다.
            player.addReputation(REPAIR_REPUTATION_REWARD);
            // 해설: 수리 보상 평판을 플레이어에게 더한다.
            refreshTitle(player);
            // 해설: 평판 변화 후 현재 칭호를 다시 계산한다.
        }
        saveRecord(player, RecordType.REPAIR_COMPLETE, "수리 완료", -repairCost, reputationChange, building.getName(), null);
        // 해설: 수리비 지출과 평판 변화를 월간 기록에 저장한다.
        return repairedWithinOneMonth ? "수리 완료 · 평판 +" + REPAIR_REPUTATION_REWARD : "수리 완료";
        // 해설: 평판 보상이 있었는지에 따라 다른 완료 메시지를 반환한다.
    }

    public void ensureOffers(Player player) {
    // 해설: 현재 도시 매물이 없을 때 필요한 경우 새 매물을 보장한다.
        if (!offerRepository.existsByPlayerAndCity(player, player.getCurrentCity())
        // 해설: 현재 도시에 매물이 없는지 확인한다.
                && !purchaseCooldownRepository.existsByPlayerAndCity(player, player.getCurrentCity())) {
                // 해설: 구매 쿨타임이 남아 있는 도시라면 바로 매물을 재생성하지 않는다.
            refreshOffers(player);
            // 해설: 매물도 없고 쿨타임도 없으면 현재 도시 매물을 새로 만든다.
        }
    }

    public void refreshOffers(Player player) {
    // 해설: 현재 도시의 기존 매물을 삭제하고 카탈로그 기준으로 새 매물을 생성한다.
        String city = player.getCurrentCity();
        // 해설: 매물 갱신 대상 도시를 변수에 담는다. 호황/불황 효과에도 같은 도시값을 사용한다.
        offerRepository.deleteByPlayerAndCity(player, player.getCurrentCity());
        // 해설: 현재 도시의 기존 매물을 모두 지운다. 갱신은 기존 목록을 교체하는 방식이다.
        buildingCatalog.byCity(player.getCurrentCity()).stream()
        // 해설: 현재 도시의 건물 스펙 목록을 카탈로그에서 가져와 Stream으로 처리한다.
                .map(spec -> new BuildingOffer(
                // 해설: 각 건물 스펙을 화면에 노출될 매물 엔티티로 변환한다.
                        player,
                        spec.city(),
                        spec.slot(),
                        spec.typeName(),
                        spec.name(),
                        spec.marketPrice(),
                        spec.monthlyRent(),
                        spec.tradeCooldownDays(),
                        randomOfferValuation(player, city)
                        // 해설: 호황/불황 뉴스가 있으면 그 영향을 반영하고, 없으면 일반 확률로 매물 평가 상태를 정한다.
                ))
                .forEach(offerRepository::save);
                // 해설: 생성된 매물을 하나씩 저장한다.
        player.consumeMarketNewsRefresh(city);
        // 해설: 호황/불황 뉴스가 적용되는 매물 갱신 횟수를 1회 차감한다.
    }

    @Transactional(readOnly = true)
    public boolean isOfferUnlocked(Player player, BuildingOffer offer) {
    // 해설: 해당 매물이 현재 플레이어에게 해금됐는지 확인한다.
        return reputationCatalog.isBuildingUnlocked(offer.getCity(), offer.getBuildingSlot(), player.getReputation(), !player.isEmployed());
        // 해설: 도시, 건물 슬롯, 평판, 퇴사 여부를 기준으로 건물 해금 여부를 계산한다.
    }

    @Transactional(readOnly = true)
    public int purchaseCooldownDaysLeft(Player player, BuildingOffer offer) {
    // 해설: 같은 도시/슬롯 건물 구매 쿨타임이 얼마나 남았는지 계산한다.
        return purchaseCooldownRepository.findByPlayerAndCityAndBuildingSlot(player, offer.getCity(), offer.getBuildingSlot())
        // 해설: 플레이어, 도시, 건물 슬롯 기준으로 구매 쿨타임 기록을 찾는다.
                .map(cooldown -> cooldown.daysLeft(player.getElapsedDays()))
                // 해설: 쿨타임 기록이 있으면 현재 경과일 기준 남은 날짜로 변환한다.
                .orElse(0);
                // 해설: 쿨타임 기록이 없으면 남은 날짜는 0일이다.
    }

    @Transactional(readOnly = true)
    public int daysUntilSellable(Player player, OwnedBuilding building) {
    // 해설: 보유 건물이 판매 가능해지기까지 남은 날짜를 계산한다.
        int purchaseDay = building.getPurchaseDayCountForCalculation(player.getElapsedDays());
        // 해설: 건물 구매일을 계산용 값으로 가져온다. 과거 데이터 보정이 있을 수 있다.
        int effectiveCooldown = effectiveBuildingWaitDays(player, building.getCity(), building.getTradeCooldownDays());
        // 해설: 비서 효과를 반영한 실제 거래 대기일을 계산한다.
        return Math.max(0, purchaseDay + effectiveCooldown - player.getElapsedDays());
        // 해설: 판매 가능일까지 남은 날짜를 계산하고, 이미 지났으면 0으로 보정한다.
    }

    @Transactional(readOnly = true)
    public boolean canSell(Player player, OwnedBuilding building) {
    // 해설: 판매 쿨타임과 보호 세입자 조건을 합쳐 실제 판매 가능 여부를 반환한다.
        return daysUntilSellable(player, building) == 0 && !building.isProtectedTenant();
        // 해설: 대기일이 0이고 보호 세입자가 없을 때만 판매 가능하다.
    }

    @Transactional(readOnly = true)
    public String sellAvailabilityText(Player player, OwnedBuilding building) {
    // 해설: 건물 카드에 표시할 판매 가능 상태 문구를 만든다.
        if (building.isSecretaryResident()) {
        // 해설: 비서가 거주 중이면 일반 판매 가능 여부보다 이 상태를 먼저 보여준다.
            return "비서 거주중";
        }
        if (building.isProtectedTenant()) {
        // 해설: 비서 외 보호 세입자 이벤트가 있으면 판매불가로 표시한다.
            return "판매불가";
        }
        int daysLeft = daysUntilSellable(player, building);
        // 해설: 판매 가능일까지 남은 날짜를 계산한다.
        return daysLeft == 0 ? "가능" : player.ddayText(daysLeft);
        // 해설: 판매 가능하면 '가능', 아직이면 D-day 문구를 반환한다.
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(BuildingOffer offer) {
    // 해설: 매물 카드에 쓸 건물 이미지 경로를 만든다.
        return buildingImagePath(offer.getCity(), offer.getBuildingSlot());
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(OwnedBuilding building) {
    // 해설: 보유 건물 카드에 쓸 이미지 경로를 만든다.
        int slot = building.getBuildingSlot() == null ? catalogSlot(building.getCity(), building.getTypeName(), building.getName()) : building.getBuildingSlot();
        // 해설: 저장된 슬롯이 없으면 카탈로그에서 도시/타입/이름으로 슬롯을 찾아 보정한다.
        return buildingImagePath(building.getCity(), slot);
    }

    @Transactional(readOnly = true)
    public String buildingImagePath(AuctionEvent auction) {
    // 해설: 경매 건물에 쓸 이미지 경로를 만든다.
        int slot = auction.getBuildingSlot() == null ? catalogSlot(auction.getCity(), auction.getTypeName(), auction.getName()) : auction.getBuildingSlot();
        // 해설: 경매 데이터에 슬롯이 없을 수 있어 카탈로그로 보정한다.
        return buildingImagePath(auction.getCity(), slot);
    }

    @Transactional(readOnly = true)
    public String auctionDisplayName(AuctionEvent auction) {
    // 해설: 경매 화면에 보여줄 건물 이름을 카탈로그 기준으로 보정한다.
        return catalogSpec(auction.getCity(), auction.getBuildingSlot())
                .map(BuildingSpec::name)
                .orElse(auction.getName());
    }

    @Transactional(readOnly = true)
    public String auctionDisplayTypeName(AuctionEvent auction) {
    // 해설: 경매 화면에 보여줄 건물 타입명을 카탈로그 기준으로 보정한다.
        return catalogSpec(auction.getCity(), auction.getBuildingSlot())
                .map(BuildingSpec::typeName)
                .orElse(auction.getTypeName());
    }

    public int effectiveBuildingWaitDays(Player player, String city, int baseDays) {
    // 해설: 비서 배치 효과를 반영한 실제 거래 대기일을 계산한다.
        double reduction = buildingWaitReductionPercent(player, city);
        // 해설: 해당 도시에 배치된 비서가 줄여주는 대기 시간 감소율을 가져온다.
        return Math.max(1, (int) Math.round(baseDays * (100.0 - reduction) / 100.0));
        // 해설: 기본 대기일에 감소율을 적용하되 최소 1일은 유지한다.
    }

    private void startPurchaseCooldown(Player player, BuildingOffer offer) {
    // 해설: 구매 완료 후 같은 도시/슬롯의 재구매 가능일을 기록한다.
        int availableDayCount = player.getElapsedDays() + effectiveBuildingWaitDays(player, offer.getCity(), offer.getTradeCooldownDays());
        // 해설: 현재 경과일에 실제 대기일을 더해 다시 구매 가능한 날짜를 계산한다.
        PurchaseCooldown cooldown = purchaseCooldownRepository
        // 해설: 기존 쿨타임 기록을 찾거나 새로 만들기 위한 Repository 호출을 시작한다.
                .findByPlayerAndCityAndBuildingSlot(player, offer.getCity(), offer.getBuildingSlot())
                .orElseGet(() -> purchaseCooldownRepository.save(new PurchaseCooldown(player, offer.getCity(), offer.getBuildingSlot(), availableDayCount)));
                // 해설: 쿨타임 기록이 없으면 새 PurchaseCooldown을 저장한다.
        cooldown.reset(availableDayCount);
        // 해설: 기존 기록이든 새 기록이든 재구매 가능일을 최신 값으로 갱신한다.
    }

    private ValuationStatus randomValuation() {
    // 해설: 일반 평가 상태를 무작위로 결정한다.
        int roll = random.nextInt(100);
        // 해설: 0부터 99 사이 난수를 뽑아 확률 분기에 사용한다.
        if (roll < 25) {
        // 해설: 25% 확률로 저평가 상태를 반환한다.
            return ValuationStatus.UNDER;
            // 해설: 저평가 상태다. 판매가나 매물가 계산에서 낮은 비율을 쓴다.
        }
        if (roll < 75) {
        // 해설: 앞의 25%를 제외한 다음 50% 구간은 정가 상태다.
            return ValuationStatus.FAIR;
            // 해설: 정가 상태다.
        }
        return ValuationStatus.OVER;
        // 해설: 호황의 남은 70% 구간은 고평가로 처리한다.
    }

    private ValuationStatus randomOfferValuation(Player player, String city) {
    // 해설: 매물 갱신 시 평가 상태를 정한다. 호황/불황 뉴스가 있으면 확률이 달라진다.
        if (!player.hasActiveMarketNewsForCity(city)) {
        // 해설: 해당 도시의 부동산 뉴스가 없으면 일반 평가 확률을 사용한다.
            return randomValuation();
            // 해설: 일반 평가 확률로 저평가/정가/고평가를 뽑는다.
        }
        int roll = random.nextInt(100);
        // 해설: 0부터 99 사이 난수를 뽑아 확률 분기에 사용한다.
        if (SettlementService.MARKET_NEWS_RISE.equals(player.getActiveMarketNewsTrend())) {
        // 해설: 호황 뉴스라면 고평가 매물이 더 잘 나오게 확률을 바꾼다.
            if (roll < 10) {
            // 해설: 호황 중 저평가 확률은 10%로 낮춘다.
                return ValuationStatus.UNDER;
                // 해설: 저평가 상태다. 판매가나 매물가 계산에서 낮은 비율을 쓴다.
            }
            if (roll < 30) {
            // 해설: 호황 중 정가 확률은 20% 구간이다.
                return ValuationStatus.FAIR;
                // 해설: 정가 상태다.
            }
            return ValuationStatus.OVER;
            // 해설: 호황의 남은 70% 구간은 고평가로 처리한다.
        }
        if (roll < 70) {
        // 해설: 불황 중 저평가 확률은 70%로 높인다.
            return ValuationStatus.UNDER;
            // 해설: 저평가 상태다. 판매가나 매물가 계산에서 낮은 비율을 쓴다.
        }
        if (roll < 90) {
        // 해설: 불황 중 정가 확률은 20% 구간이다.
            return ValuationStatus.FAIR;
            // 해설: 정가 상태다.
        }
        return ValuationStatus.OVER;
        // 해설: 호황의 남은 70% 구간은 고평가로 처리한다.
    }

    public double buildingWaitReductionPercent(Player player, String city) {
    // 해설: 해당 도시에 배치된 비서가 줄여주는 건물 거래 대기 시간 감소율을 계산한다.
        return ownedSecretaryRepository.findByPlayerOrderById(player).stream()
        // 해설: 플레이어가 보유한 비서 목록을 순회한다.
                .filter(secretary -> secretary.isAssignedTo(city))
                // 해설: 해당 도시에 배치된 비서만 후보로 남긴다.
                .findFirst()
                // 해설: 도시에 배치된 첫 번째 비서만 효과 적용 대상으로 사용한다.
                .map(secretary -> switch (secretary.getSecretaryKey()) {
                // 해설: 비서 키에 따라 대기 시간 감소 공식을 다르게 적용한다.
                    case "secretary-5" -> 0.5 * secretary.getAffinity();
                    // 해설: 5번 비서는 친밀도 1당 0.5% 감소 효과를 준다.
                    case "secretary-6" -> 1.0 * secretary.getAffinity();
                    // 해설: 6번 비서는 친밀도 1당 1% 감소 효과를 준다.
                    default -> 0.0;
                    // 해설: 다른 비서는 거래 대기 시간 감소 효과가 없다.
                })
                .orElse(0.0);
    }

    private Optional<BuildingSpec> catalogSpec(String city, Integer slot) {
    // 해설: 도시와 슬롯으로 건물 카탈로그 스펙을 찾는다.
        if (slot == null) {
        // 해설: 슬롯 정보가 없으면 카탈로그 매칭을 할 수 없다.
            return Optional.empty();
            // 해설: 찾을 수 없음을 Optional.empty로 표현한다.
        }
        return buildingCatalog.byCity(city).stream()
        // 해설: 해당 도시의 건물 스펙 목록에서 검색한다.
                .filter(candidate -> candidate.slot() == slot)
                // 해설: 슬롯 번호가 같은 스펙만 남긴다.
                .findFirst();
    }

    private String buildingImagePath(String city, int slot) {
    // 해설: 도시와 슬롯 번호로 실제 이미지 파일 경로를 만든다.
        return "/assets/buildings/" + citySlug(city) + "-" + Math.max(1, Math.min(4, slot)) + ".jpg";
        // 해설: 슬롯 번호를 1~4 범위로 보정한 뒤 도시 slug와 조합해 이미지 경로를 만든다.
    }

    private int catalogSlot(String city, String typeName, String name) {
    // 해설: 과거 데이터처럼 슬롯이 비어 있는 건물을 카탈로그 기준으로 보정한다.
        return buildingCatalog.all().stream()
        // 해설: 전체 건물 스펙을 순회한다.
                .filter(spec -> spec.city().equals(city))
                // 해설: 도시가 같은 스펙만 남긴다.
                .filter(spec -> spec.name().equals(name) || spec.typeName().equals(typeName))
                // 해설: 이름이나 타입명이 일치하는 스펙을 찾는다.
                .map(BuildingSpec::slot)
                // 해설: 찾은 스펙에서 슬롯 번호만 꺼낸다.
                .findFirst()
                // 해설: 도시에 배치된 첫 번째 비서만 효과 적용 대상으로 사용한다.
                .orElse(1);
    }

    private String citySlug(String city) {
    // 해설: 한글 도시명을 이미지 파일명에 쓰는 영문 slug로 바꾼다.
        return switch (city) {
        // 해설: 도시 이름별로 고정된 영문 문자열을 반환한다.
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
    // 해설: 일시정지 중 경제 행동을 막을 때 쓰는 공통 메시지다.
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private void refreshTitle(Player player) {
    // 해설: 평판 변화 후 플레이어 칭호를 현재 티어에 맞게 갱신한다.
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        // 해설: 평판과 퇴사 여부로 현재 티어를 찾고 그 칭호를 플레이어에 저장한다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 거래/수리 기록을 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 월간 기록 엔티티를 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 기준일은 최소 1일로 보정한다.
    }
}
```
