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

    public QaService(
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
            SecretaryTenantEventService secretaryTenantEventService
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
    }

    public String addTestCash(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.addCash(TEST_CASH_AMOUNT);
        return "테스트 현금 30,000,000원 지급";
    }

    public String updateTestReputation(long playerId, int reputation) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.setReputationForTest(reputation);
        refreshTitle(player);
        return "테스트 평판 변경 완료";
    }

    public String updateTestChances(long playerId, int moveInChance, int moveOutChance, int repairChance) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        player.updateTestChances(moveInChance, moveOutChance, repairChance);
        return "테스트 확률 변경 완료";
    }

    public String updateTestSecretaryProficiency(long playerId, String secretaryKey, int proficiency) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        OwnedSecretary secretary = ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElse(null);
        if (secretary == null) {
            return "보유하지 않은 비서";
        }
        secretary.setProficiencyForTest(proficiency);
        return spec.name() + " 숙련도 변경 완료";
    }

    public String prepareSecretaryEventTestConditions(long playerId, String secretaryKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        if (player.getCash() < scenario.requiredCash()) {
            player.addCash(scenario.requiredCash() - player.getCash());
        }
        if (player.getReputation() < scenario.requiredReputation()) {
            player.setReputationForTest(scenario.requiredReputation());
            refreshTitle(player);
        }
        if (scenario.requiredLuxuryKey() != null) {
            grantLuxuryItem(player, scenario.requiredLuxuryKey());
        }
        if (scenario.requiresAllLuxuryItems()) {
            luxuryItemCatalog.all().forEach(item -> grantLuxuryItem(player, item.key()));
        }
        if (scenario.requiresNoLoan()) {
            loanRepository.deleteAll(loanRepository.findByPlayer(player));
        }
        if ("secretary-6".equals(secretaryKey)) {
            findOrCreateBuilding(player, "서울", 4);
        }
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 이벤트 조건 세팅 완료";
    }

    public String grantSecretaryEventBuilding(long playerId, String secretaryKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        OwnedBuilding building = findOrCreateBuilding(player, scenario.city(), scenario.buildingSlot());
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isEmpty()) {
            building.moveInSecretaryTenant(secretaryKey);
            secretaryTenantEventService.createTenantEvent(player, building, scenario);
        }
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 입주 세팅 완료";
    }

    public String setSecretaryEventTestStage(long playerId, String secretaryKey, String stage) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        OwnedBuilding building = findOrCreateBuilding(player, scenario.city(), scenario.buildingSlot());
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey)
                .orElseGet(() -> {
                    building.moveInSecretaryTenant(secretaryKey);
                    return secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, secretaryKey, scenario.city(), player.getElapsedDays()));
                });
        gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).ifPresent(GameEvent::complete);
        resetSecretaryQaGameEvents(player, secretaryKey);
        player.resume();
        switch (stage) {
            case "TENANT" -> event.makeTenant();
            case "REQUEST" -> event.makeRequestAvailable();
            case "ACCEPTED" -> event.acceptRequest(player.getElapsedDays(), Math.max(1, scenario.durationDays()));
            case "HIRE" -> {
                if ("secretary-6".equals(secretaryKey)) {
                    findOrCreateBuilding(player, "서울", 4);
                }
                event.makeHireAvailable();
            }
            case "COMPLETED" -> event.complete();
            default -> throw new IllegalArgumentException("잘못된 비서 이벤트 단계");
        }
        secretaryTenantEventService.activateNextEvent(player);
        return secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서") + " 이벤트 단계 변경 완료";
    }

    private void resetSecretaryQaGameEvents(Player player, String secretaryKey) {
        gameEventRepository.deleteByPlayerIdAndEventKey(player.getId(), "secretary_request_" + secretaryKey);
        gameEventRepository.deleteByPlayerIdAndEventKey(player.getId(), "secretary_hire_" + secretaryKey);
    }

    private void grantLuxuryItem(Player player, String itemKey) {
        if (ownedLuxuryItemRepository.findByPlayerAndItemKey(player, itemKey).isEmpty()) {
            ownedLuxuryItemRepository.save(new OwnedLuxuryItem(player, itemKey));
        }
    }

    private OwnedBuilding findOrCreateBuilding(Player player, String city, int slot) {
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, city).stream()
                .filter(building -> buildingSlot(building) == slot)
                .findFirst()
                .orElseGet(() -> createBuilding(player, city, slot));
    }

    private OwnedBuilding createBuilding(Player player, String city, int slot) {
        BuildingSpec spec = buildingCatalog.byCity(city).stream()
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
        return building.getBuildingSlot() == null ? catalogSlot(building.getCity(), building.getTypeName(), building.getName()) : building.getBuildingSlot();
    }

    private int catalogSlot(String city, String typeName, String name) {
        return buildingCatalog.all().stream()
                .filter(spec -> spec.city().equals(city))
                .filter(spec -> spec.name().equals(name) || spec.typeName().equals(typeName))
                .map(BuildingSpec::slot)
                .findFirst()
                .orElse(1);
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }
}
