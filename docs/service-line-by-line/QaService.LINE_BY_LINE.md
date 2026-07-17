# QaService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/QaService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedLuxuryItem;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedLuxuryItemRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QaService {
// 해설: 개발과 테스트를 위해 게임 상태를 빠르게 조작하는 QA 전용 서비스다.
    /*
     * QAController가 사용하는 테스트 보조 로직이다.
     *
     * 플레이 중 특정 조건을 빠르게 만들기 위해 현금, 평판, 이벤트, 비서 상태를 직접 조정한다.
     * 운영용 게임 규칙이 아니라 개발 검증 시간을 줄이기 위한 우회 도구다.
     */
    private static final long TEST_CASH_AMOUNT = 30_000_000L;

    private final PlayerRepository playerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final OwnedLuxuryItemRepository ownedLuxuryItemRepository;
    private final LoanRepository loanRepository;
    private final SecretaryTenantEventRepository secretaryTenantEventRepository;
    private final GameEventRepository gameEventRepository;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryCatalog secretaryCatalog;
    private final LuxuryItemCatalog luxuryItemCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final SettlementService settlementService;

    public QaService(
    // 해설: QA 조작에 필요한 Repository, 카탈로그, 비서 이벤트, 정산 서비스를 생성자 주입으로 받는다.
            PlayerRepository playerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            OwnedLuxuryItemRepository ownedLuxuryItemRepository,
            LoanRepository loanRepository,
            SecretaryTenantEventRepository secretaryTenantEventRepository,
            GameEventRepository gameEventRepository,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            SecretaryCatalog secretaryCatalog,
            LuxuryItemCatalog luxuryItemCatalog,
            SecretaryTenantEventService secretaryTenantEventService,
            SettlementService settlementService
    ) {
        this.playerRepository = playerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.ownedLuxuryItemRepository = ownedLuxuryItemRepository;
        this.loanRepository = loanRepository;
        this.secretaryTenantEventRepository = secretaryTenantEventRepository;
        this.gameEventRepository = gameEventRepository;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
        this.secretaryCatalog = secretaryCatalog;
        this.luxuryItemCatalog = luxuryItemCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.settlementService = settlementService;
    }

    public String addTestCash(long playerId) {
    // 해설: 테스트용 현금을 플레이어에게 지급한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.addCash(TEST_CASH_AMOUNT);
        // 해설: 정해진 테스트 현금 금액을 플레이어 현금에 더한다.
        return "테스트 현금 30,000,000원 지급";
    }

    public String updateTestReputation(long playerId, int reputation) {
    // 해설: 테스트용으로 플레이어 평판을 직접 설정한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.setReputationForTest(reputation);
        // 해설: 일반 게임 규칙을 거치지 않고 평판 값을 바로 바꾼다.
        refreshTitle(player);
        // 해설: 평판 변경 후 칭호를 현재 티어에 맞게 갱신한다.
        return "테스트 평판 변경 완료";
    }

    public String updateTestChances(long playerId, int moveInChance, int moveOutChance, int repairChance) {
    // 해설: 입주/퇴거/수리요청 확률을 테스트용으로 직접 바꾼다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.updateTestChances(moveInChance, moveOutChance, repairChance);
        // 해설: Player의 테스트 확률 값을 갱신한다.
        return "테스트 확률 변경 완료";
    }

    public String updateTestSecretaryProficiency(long playerId, String secretaryKey, int proficiency) {
    // 해설: 보유 비서의 숙련도를 테스트용으로 직접 변경한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 이름 표시용 스펙을 찾는다.
        OwnedSecretary secretary = ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElse(null);
        // 해설: 플레이어가 해당 비서를 보유 중인지 조회한다.
        if (secretary == null) {
        // 해설: 비서를 보유하지 않았다면 숙련도를 바꿀 수 없다.
            return "보유하지 않은 비서";
        }
        secretary.setProficiencyForTest(proficiency);
        // 해설: 일반 성장 규칙을 거치지 않고 숙련도를 직접 설정한다.
        return spec.name() + " 숙련도 변경 완료";
    }

    public String activateMarketNewsEvent(long playerId, String trend) {
    // 해설: 부동산 호황/불황 뉴스를 테스트용으로 즉시 활성화한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).ifPresent(GameEvent::complete);
        // 해설: 기존 활성 이벤트를 닫아 단계 변경 후 이벤트 활성화가 막히지 않게 한다.
        player.resume();
        // 해설: 기존 이벤트 때문에 멈춰 있을 수 있는 시간을 다시 진행 상태로 바꾼다.
        String safeTrend = SettlementService.MARKET_NEWS_FALL.equals(trend)
        // 해설: 입력값이 FALL이면 불황, 그 외에는 호황으로 보정한다.
                ? SettlementService.MARKET_NEWS_FALL
                : SettlementService.MARKET_NEWS_RISE;
        return settlementService.activateMarketNewsForTest(player, safeTrend);
        // 해설: SettlementService의 테스트용 뉴스 활성화 로직을 호출한다.
    }

    public String prepareSecretaryEventTestConditions(long playerId, String secretaryKey) {
    // 해설: 특정 비서 세입자 이벤트 조건을 테스트하기 좋게 강제로 맞춘다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 필요한 현금/평판/아이템/대출 조건을 가져온다.
        if (player.getCash() < scenario.requiredCash()) {
        // 해설: 필요 현금보다 적으면 부족분을 지급한다.
            player.addCash(scenario.requiredCash() - player.getCash());
        }
        if (player.getReputation() < scenario.requiredReputation()) {
        // 해설: 필요 평판보다 낮으면 테스트용으로 평판을 올린다.
            player.setReputationForTest(scenario.requiredReputation());
            refreshTitle(player);
            // 해설: 평판 변경 후 칭호를 현재 티어에 맞게 갱신한다.
        }
        if (scenario.requiredLuxuryKey() != null) {
        // 해설: 특정 명품이 필요한 시나리오면 그 명품을 지급한다.
            grantLuxuryItem(player, scenario.requiredLuxuryKey());
        }
        if (scenario.requiresAllLuxuryItems()) {
        // 해설: 모든 명품이 필요한 시나리오면 전체 명품을 지급한다.
            luxuryItemCatalog.all().forEach(item -> grantLuxuryItem(player, item.key()));
        }
        if (scenario.requiresNoLoan()) {
        // 해설: 대출이 없어야 하는 시나리오면 기존 대출을 삭제한다.
            loanRepository.deleteAll(loanRepository.findByPlayer(player));
        }
        if ("secretary-6".equals(secretaryKey)) {
        // 해설: 6번 비서 테스트에는 서울 4번 슬롯 건물이 필요하다.
            findOrCreateBuilding(player, "서울", 4);
        }
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 이벤트 조건 세팅 완료";
    }

    public String grantSecretaryEventBuilding(long playerId, String secretaryKey) {
    // 해설: 비서 세입자 이벤트에 필요한 건물을 만들고 입주 상태를 세팅한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 필요한 현금/평판/아이템/대출 조건을 가져온다.
        OwnedBuilding building = findOrCreateBuilding(player, scenario.city(), scenario.buildingSlot());
        // 해설: 시나리오 도시/슬롯에 맞는 건물을 찾거나 새로 만든다.
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isEmpty()) {
        // 해설: 같은 비서 이벤트가 없을 때만 입주와 이벤트 생성을 수행한다.
            building.moveInSecretaryTenant(secretaryKey);
            // 해설: 건물에 비서 세입자 입주 상태를 표시한다.
            secretaryTenantEventService.createTenantEvent(player, building, scenario);
            // 해설: 비서 세입자 이벤트 엔티티를 생성한다.
        }
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 입주 세팅 완료";
    }

    public String setSecretaryEventTestStage(long playerId, String secretaryKey, String stage) {
    // 해설: 비서 세입자 이벤트의 진행 단계를 테스트용으로 직접 바꾼다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 필요한 현금/평판/아이템/대출 조건을 가져온다.
        OwnedBuilding building = findOrCreateBuilding(player, scenario.city(), scenario.buildingSlot());
        // 해설: 시나리오 도시/슬롯에 맞는 건물을 찾거나 새로 만든다.
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey)
        // 해설: 기존 이벤트를 찾고, 없으면 새로 만든다.
                .orElseGet(() -> {
                    building.moveInSecretaryTenant(secretaryKey);
                    // 해설: 건물에 비서 세입자 입주 상태를 표시한다.
                    return secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, secretaryKey, scenario.city(), player.getElapsedDays()));
                });
        gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).ifPresent(GameEvent::complete);
        // 해설: 기존 활성 이벤트를 닫아 단계 변경 후 이벤트 활성화가 막히지 않게 한다.
        resetSecretaryQaGameEvents(player, secretaryKey);
        // 해설: 해당 비서의 기존 부탁/고용 GameEvent를 지워 중복 상태를 정리한다.
        player.resume();
        // 해설: 기존 이벤트 때문에 멈춰 있을 수 있는 시간을 다시 진행 상태로 바꾼다.
        switch (stage) {
        // 해설: 요청한 stage 문자열에 따라 이벤트 상태를 바꾼다.
            case "TENANT" -> event.makeTenant();
            // 해설: 세입자 상태로 되돌린다.
            case "REQUEST" -> event.makeRequestAvailable();
            // 해설: 부탁 가능 상태로 바꾼다.
            case "ACCEPTED" -> event.acceptRequest(player.getElapsedDays(), Math.max(1, scenario.durationDays()));
            // 해설: 부탁 수락 상태로 바꾸고 최소 1일 기간을 설정한다.
            case "HIRE" -> {
            // 해설: 고용 가능 상태로 바꾸기 위한 특수 처리 블록이다.
                if ("secretary-6".equals(secretaryKey)) {
                // 해설: 6번 비서 테스트에는 서울 4번 슬롯 건물이 필요하다.
                    findOrCreateBuilding(player, "서울", 4);
                }
                event.makeHireAvailable();
                // 해설: 비서 이벤트를 고용 가능 상태로 바꾼다.
            }
            case "COMPLETED" -> event.complete();
            // 해설: 비서 이벤트를 완료 상태로 바꾼다.
            default -> throw new IllegalArgumentException("잘못된 비서 이벤트 단계");
        }
        secretaryTenantEventService.activateNextEvent(player);
        // 해설: 변경된 상태에 맞는 GameEvent를 다시 활성화한다.
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 이벤트 단계 변경 완료";
    }

    private void resetSecretaryQaGameEvents(Player player, String secretaryKey) {
    // 해설: QA 단계 변경 전에 해당 비서의 기존 요청/고용 이벤트를 삭제한다.
        gameEventRepository.deleteByPlayerIdAndEventKey(player.getId(), "secretary_request_" + secretaryKey);
        gameEventRepository.deleteByPlayerIdAndEventKey(player.getId(), "secretary_hire_" + secretaryKey);
    }

    private void grantLuxuryItem(Player player, String itemKey) {
    // 해설: 테스트 조건 충족을 위해 명품 아이템을 지급한다.
        if (ownedLuxuryItemRepository.findByPlayerAndItemKey(player, itemKey).isEmpty()) {
        // 해설: 이미 보유한 명품은 중복 저장하지 않는다.
            ownedLuxuryItemRepository.save(new OwnedLuxuryItem(player, itemKey));
        }
    }

    private OwnedBuilding findOrCreateBuilding(Player player, String city, int slot) {
    // 해설: 요청 도시/슬롯 건물을 찾고 없으면 새로 만든다.
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, city).stream()
                .filter(building -> buildingSlot(building) == slot)
                // 해설: 저장 슬롯 또는 카탈로그 보정 슬롯이 요청 슬롯과 같은 건물만 남긴다.
                .findFirst()
                .orElseGet(() -> createBuilding(player, city, slot));
                // 해설: 조건에 맞는 건물이 없으면 새 건물을 생성한다.
    }

    private OwnedBuilding createBuilding(Player player, String city, int slot) {
    // 해설: 카탈로그 스펙 기준으로 테스트용 보유 건물을 생성한다.
        BuildingSpec spec = buildingCatalog.byCity(city).stream()
        // 해설: 도시 카탈로그에서 요청 슬롯의 건물 스펙을 찾는다.
                .filter(candidate -> candidate.slot() == slot)
                .findFirst()
                .orElseThrow();
        return ownedBuildingRepository.save(new OwnedBuilding(
                player,
                spec.city(),
                spec.slot(),
                spec.typeName(),
                spec.name(),
                spec.marketPrice(),
                spec.marketPrice(),
                spec.monthlyRent(),
                spec.tradeCooldownDays()
        ));
    }

    private int buildingSlot(OwnedBuilding building) {
    // 해설: 건물 슬롯을 읽고, 없으면 카탈로그 기준으로 보정한다.
        return building.getBuildingSlot() == null ? catalogSlot(building.getCity(), building.getTypeName(), building.getName()) : building.getBuildingSlot();
    }

    private int catalogSlot(String city, String typeName, String name) {
    // 해설: 도시/타입/이름으로 카탈로그에서 슬롯 번호를 찾는다.
        return buildingCatalog.all().stream()
                .filter(spec -> spec.city().equals(city))
                .filter(spec -> spec.name().equals(name) || spec.typeName().equals(typeName))
                .map(BuildingSpec::slot)
                .findFirst()
                .orElse(1);
    }

    private void refreshTitle(Player player) {
    // 해설: 평판 변경 후 플레이어 칭호를 갱신한다.
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }
}
```