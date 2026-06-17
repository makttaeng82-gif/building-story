package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedLuxuryItem;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedLuxuryItemRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SecretaryTenantEventService {
    static final String INTRO_EFFECT_PREFIX = "SECRETARY_TENANT_INTRO:";
    static final String REQUEST_EFFECT_PREFIX = "SECRETARY_TENANT_REQUEST:";
    static final String HIRE_EFFECT_PREFIX = "SECRETARY_TENANT_HIRE:";

    private static final int RECORD_RETENTION_DAYS = 62;
    private static final String IMAGE_PLACEHOLDER = "EMPTY_SECRETARY_EVENT_IMAGE";

    private final OwnedBuildingRepository ownedBuildingRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final OwnedLuxuryItemRepository ownedLuxuryItemRepository;
    private final LoanRepository loanRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final SecretaryTenantEventRepository secretaryTenantEventRepository;
    private final GameEventRepository gameEventRepository;
    private final BuildingCatalog buildingCatalog;
    private final SecretaryCatalog secretaryCatalog;
    private final LuxuryItemCatalog luxuryItemCatalog;

    public SecretaryTenantEventService(
            OwnedBuildingRepository ownedBuildingRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            OwnedLuxuryItemRepository ownedLuxuryItemRepository,
            LoanRepository loanRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            SecretaryTenantEventRepository secretaryTenantEventRepository,
            GameEventRepository gameEventRepository,
            BuildingCatalog buildingCatalog,
            SecretaryCatalog secretaryCatalog,
            LuxuryItemCatalog luxuryItemCatalog
    ) {
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.ownedLuxuryItemRepository = ownedLuxuryItemRepository;
        this.loanRepository = loanRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.secretaryTenantEventRepository = secretaryTenantEventRepository;
        this.gameEventRepository = gameEventRepository;
        this.buildingCatalog = buildingCatalog;
        this.secretaryCatalog = secretaryCatalog;
        this.luxuryItemCatalog = luxuryItemCatalog;
    }

    public void evaluate(Player player, boolean hasActiveAuction) {
        if (activeEvent(player).isPresent() || hasActiveAuction) {
            return;
        }
        activateMissingIntro(player);
        if (activeEvent(player).isPresent()) {
            return;
        }
        updateProgress(player);
        activateNextEvent(player);
    }

    public void tryActivateIntro(Player player, OwnedBuilding building) {
        Optional<SecretaryTenantScenario> scenario = SecretaryTenantScenarioCatalog.findByCityAndSlot(building.getCity(), buildingSlot(building));
        if (scenario.isEmpty() || "secretary-1".equals(scenario.get().secretaryKey())) {
            return;
        }
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.get().secretaryKey()).isPresent()) {
            return;
        }
        if (activeEvent(player).isPresent()) {
            return;
        }
        activateSecretaryEvent(
                player,
                "secretary_intro_" + scenario.get().secretaryKey(),
                scenario.get().introTitle(),
                scenario.get().introBody(),
                INTRO_EFFECT_PREFIX + scenario.get().secretaryKey(),
                "확인"
        );
    }

    public void applyFirstTenantMoveIn(Player player) {
        ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                .filter(building -> !building.isOccupied())
                .findFirst()
                .ifPresent(building -> {
                    building.moveInSecretaryTenant("secretary-1");
                    createTenantEvent(player, building, SecretaryTenantScenarioCatalog.find("secretary-1").orElseThrow());
                });
        player.markFirstTenantEventDone();
    }

    public void applyIntroEvent(Player player, String secretaryKey) {
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        OwnedBuilding building = ownedBuildingRepository.findByPlayerAndCityOrderById(player, scenario.city()).stream()
                .filter(candidate -> buildingSlot(candidate) == scenario.buildingSlot())
                .findFirst()
                .orElseThrow();
        building.moveInSecretaryTenant(secretaryKey);
        createTenantEvent(player, building, scenario);
        saveRecord(player, RecordType.MOVE_IN, "비서 세입자 입주", null, 0, building.getName(), secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서"));
    }

    public void applyRequestEvent(Player player, String secretaryKey) {
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElseThrow();
        if (scenario.requestCost() > 0 && !player.spendCash(scenario.requestCost())) {
            return;
        }
        event.acceptRequest(player.getElapsedDays(), scenario.durationDays());
        if (scenario.durationDays() == 0) {
            event.makeHireAvailable();
        }
        if (scenario.requestCost() > 0) {
            saveRecord(player, RecordType.BUILDING_BUY, "비서 부탁", -scenario.requestCost(), 0, event.getBuilding().getName(), scenario.requestButton());
        }
    }

    public void applyHireEvent(Player player, String secretaryKey) {
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElseThrow();
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isEmpty()) {
            ownedSecretaryRepository.save(new OwnedSecretary(player, secretaryKey, spec.baseProficiency()));
        }
        if ("secretary-6".equals(secretaryKey)) {
            event.getBuilding().moveOut();
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, "서울").stream()
                    .filter(building -> buildingSlot(building) == 4)
                    .findFirst()
                    .ifPresent(building -> building.moveInSecretaryTenant(secretaryKey));
        } else {
            event.getBuilding().moveOut();
        }
        event.complete();
        saveRecord(player, RecordType.BUILDING_BUY, "비서 고용", null, 0, event.getBuilding().getName(), spec.name());
    }

    public void createTenantEvent(Player player, OwnedBuilding building, SecretaryTenantScenario scenario) {
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.secretaryKey()).isPresent()) {
            return;
        }
        secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, scenario.secretaryKey(), scenario.city(), player.getElapsedDays()));
    }

    public void activateNextEvent(Player player) {
        if (activeEvent(player).isPresent()) {
            return;
        }
        for (SecretaryTenantEvent event : secretaryTenantEventRepository.findByPlayerAndStatusNot(player, SecretaryTenantEventStatus.COMPLETED)) {
            SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(event.getSecretaryKey()).orElseThrow();
            if (event.getStatus() == SecretaryTenantEventStatus.TENANT && requestConditionMet(player, scenario)) {
                event.makeRequestAvailable();
            }
            if (event.getStatus() == SecretaryTenantEventStatus.REQUEST_AVAILABLE) {
                activateSecretaryEvent(
                        player,
                        "secretary_request_" + scenario.secretaryKey(),
                        scenario.requestTitle(),
                        scenario.requestBody(),
                        REQUEST_EFFECT_PREFIX + scenario.secretaryKey(),
                        scenario.requestButton()
                );
                return;
            }
            if (event.getStatus() == SecretaryTenantEventStatus.HIRE_AVAILABLE && hireConditionMet(player, scenario)) {
                activateSecretaryEvent(
                        player,
                        "secretary_hire_" + scenario.secretaryKey(),
                        secretaryCatalog.find(scenario.secretaryKey()).map(SecretarySpec::name).orElse("비서") + " 고용 가능",
                        "부탁을 해결했다. 이제 비서로 고용할 수 있다.",
                        HIRE_EFFECT_PREFIX + scenario.secretaryKey(),
                        "고용하기"
                );
                return;
            }
        }
    }

    public String statusText(OwnedBuilding building) {
        Optional<SecretaryTenantEvent> event = secretaryTenantEventRepository.findByBuildingAndStatusNot(building, SecretaryTenantEventStatus.COMPLETED);
        if (event.isEmpty() && building.isSecretaryResident()) {
            return "비서 거주중";
        }
        if (event.isEmpty()) {
            return "";
        }
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(event.get().getSecretaryKey()).orElse(null);
        if (event.get().getStatus() == SecretaryTenantEventStatus.REQUEST_ACCEPTED && scenario != null && scenario.progressMemo() != null) {
            return scenario.progressMemo() + progressDday(event.get());
        }
        return "비서 거주중";
    }

    public boolean isRentWaived(Player player, OwnedBuilding building) {
        Optional<SecretaryTenantEvent> event = secretaryTenantEventRepository.findByBuildingAndStatusNot(building, SecretaryTenantEventStatus.COMPLETED);
        if (event.isEmpty()) {
            return false;
        }
        if ("secretary-5".equals(event.get().getSecretaryKey()) && event.get().getStatus() != SecretaryTenantEventStatus.COMPLETED) {
            return true;
        }
        return "secretary-1".equals(event.get().getSecretaryKey())
                && event.get().getStatus() == SecretaryTenantEventStatus.REQUEST_ACCEPTED
                && player.getElapsedDays() < event.get().getDueDayCount();
    }

    private void activateMissingIntro(Player player) {
        for (OwnedBuilding building : ownedBuildingRepository.findByPlayerOrderById(player)) {
            Optional<SecretaryTenantScenario> scenario = SecretaryTenantScenarioCatalog.findByCityAndSlot(building.getCity(), buildingSlot(building));
            if (scenario.isEmpty() || "secretary-1".equals(scenario.get().secretaryKey())) {
                continue;
            }
            if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.get().secretaryKey()).isPresent()) {
                continue;
            }
            tryActivateIntro(player, building);
            if (activeEvent(player).isPresent()) {
                return;
            }
        }
    }

    private void updateProgress(Player player) {
        for (SecretaryTenantEvent event : secretaryTenantEventRepository.findByPlayerAndStatus(player, SecretaryTenantEventStatus.REQUEST_ACCEPTED)) {
            if (event.getDueDayCount() > 0 && player.getElapsedDays() >= event.getDueDayCount()) {
                event.makeHireAvailable();
            }
        }
    }

    private void activateSecretaryEvent(Player player, String eventKey, String title, String body, String effectKey, String actionLabel) {
        if (gameEventRepository.existsByPlayerIdAndEventKey(player.getId(), eventKey)) {
            return;
        }
        gameEventRepository.save(new GameEvent(player, eventKey, title, body, secretaryEventImage(eventKey), effectKey, actionLabel));
        player.pause();
    }

    private String secretaryEventImage(String eventKey) {
        if (eventKey.startsWith("secretary_intro_")) {
            return "/assets/events/" + eventKey.replace("secretary_intro_", "") + "-intro.png";
        }
        if (eventKey.startsWith("secretary_request_")) {
            return "/assets/events/" + eventKey.replace("secretary_request_", "") + "-request.png";
        }
        if (eventKey.startsWith("secretary_hire_")) {
            return "/assets/events/" + eventKey.replace("secretary_hire_", "") + "-hire.png";
        }
        return IMAGE_PLACEHOLDER;
    }

    private boolean requestConditionMet(Player player, SecretaryTenantScenario scenario) {
        if (player.getCash() < scenario.requiredCash()) {
            return false;
        }
        if (player.getReputation() < scenario.requiredReputation()) {
            return false;
        }
        if (scenario.requiredLuxuryKey() != null && ownedLuxuryItemRepository.findByPlayerAndItemKey(player, scenario.requiredLuxuryKey()).isEmpty()) {
            return false;
        }
        if (scenario.requiresNoLoan() && remainingLoanPrincipal(player) > 0) {
            return false;
        }
        return !scenario.requiresAllLuxuryItems() || hasAllLuxuryItems(player);
    }

    private boolean hireConditionMet(Player player, SecretaryTenantScenario scenario) {
        if (!"secretary-6".equals(scenario.secretaryKey())) {
            return true;
        }
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, "서울").stream()
                .anyMatch(building -> buildingSlot(building) == 4);
    }

    private boolean hasAllLuxuryItems(Player player) {
        return luxuryItemCatalog.all().stream()
                .allMatch(item -> ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent());
    }

    private long remainingLoanPrincipal(Player player) {
        return loanRepository.findByPlayer(player).stream()
                .mapToLong(Loan::getPrincipal)
                .sum();
    }

    private Optional<GameEvent> activeEvent(Player player) {
        return gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE);
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

    private String progressDday(SecretaryTenantEvent event) {
        if (event.getDueDayCount() <= 0) {
            return "";
        }
        return " D-" + Math.max(0, event.getDueDayCount() - event.getPlayer().getElapsedDays());
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
