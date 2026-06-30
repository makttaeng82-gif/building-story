package com.game.buildingstory.service;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class SecretaryOperationsService {
    private static final int REPAIR_REPUTATION_REWARD = 3;
    private static final int RECORD_RETENTION_DAYS = 62;

    private final Random random = new Random();
    private final PlayerRepository playerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryCatalog secretaryCatalog;

    public SecretaryOperationsService(
            PlayerRepository playerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog,
            SecretaryCatalog secretaryCatalog
    ) {
        this.playerRepository = playerRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
        this.secretaryCatalog = secretaryCatalog;
    }

    public String hireFirstSecretary(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
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
        ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-1")
                .orElseGet(() -> ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1)));
        SecretarySpec spec = secretaryCatalog.find("secretary-1").orElseThrow();
        return "비서 고용 완료 · 월급 " + spec.monthlySalaryForProficiency(1) + "원";
    }

    @Transactional(readOnly = true)
    public boolean canHireSecretary(Player player, SecretarySpec spec) {
        if (player.getReputation() < spec.requiredReputation()) {
            return false;
        }
        if ("secretary-1".equals(spec.key())) {
            return ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                    .anyMatch(OwnedBuilding::isProtectedTenant);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isSecretaryOwned(Player player, SecretarySpec spec) {
        return ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, spec.key()).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> ownedSecretary(Player player, SecretarySpec spec) {
        return ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, spec.key());
    }

    @Transactional(readOnly = true)
    public Optional<SecretarySpec> availableSecretaryOffer(Player player) {
        return secretaryCatalog.all().stream()
                .filter(spec -> !isSecretaryOwned(player, spec))
                .filter(spec -> !player.isSecretaryOfferDismissed(spec.key()))
                .filter(spec -> canHireSecretary(player, spec))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<OwnedSecretary> ownedSecretaries(Player player) {
        return ownedSecretaryRepository.findByPlayerOrderById(player);
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> assignedSecretary(Player player, String city) {
        return ownedSecretaryRepository.findByPlayerOrderById(player).stream()
                .filter(secretary -> secretary.isAssignedTo(city))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean canAssignSecretaryToCity(Player player, OwnedSecretary targetSecretary, String city) {
        if (player == null || targetSecretary == null || city == null) {
            return false;
        }
        return ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city).stream()
                .allMatch(secretary -> secretary.getId().equals(targetSecretary.getId()));
    }

    public String hireSecretary(long playerId, String secretaryKey) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isPresent()) {
            return "이미 보유중인 비서";
        }
        if (!canHireSecretary(player, spec)) {
            return "고용 조건 미달";
        }
        if ("secretary-1".equals(secretaryKey)) {
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                    .filter(OwnedBuilding::isProtectedTenant)
                    .findFirst()
                    .ifPresent(OwnedBuilding::moveOut);
            player.hireFirstSecretary();
        }
        ownedSecretaryRepository.save(new OwnedSecretary(player, secretaryKey, spec.baseProficiency()));
        return spec.name() + " 고용 완료";
    }

    public String assignSecretary(long playerId, long ownedSecretaryId, String city) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        if (!buildingCatalog.cities().contains(city)) {
            return "존재하지 않는 도시";
        }
        if (!reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())) {
            return "해금되지 않은 도시";
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        if (!secretary.getPlayer().getId().equals(player.getId())) {
            return "잘못된 비서";
        }
        boolean occupiedByOtherSecretary = ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city).stream()
                .anyMatch(assigned -> !assigned.getId().equals(secretary.getId()));
        if (occupiedByOtherSecretary) {
            return "이미 다른 비서가 배치된 도시";
        }
        secretary.assignTo(city);
        return "비서 배치 완료";
    }

    public String unassignSecretary(long playerId, long ownedSecretaryId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        if (!secretary.getPlayer().getId().equals(player.getId())) {
            return "잘못된 비서";
        }
        secretary.assignTo(null);
        return "비서 배치 제외 완료";
    }

    public String processAutoRepairs(Player player) {
        String notice = "";
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
            if (secretary.getAssignedCity() == null) {
                continue;
            }
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            int maxRepairs = maxAutoRepairsPerCooldown(secretary);
            if (spec == null || !secretary.canAutoRepair(player.getElapsedDays(), maxRepairs)) {
                continue;
            }
            for (int repaired = 0; repaired < maxRepairs && secretary.canAutoRepair(player.getElapsedDays(), maxRepairs); repaired++) {
                String repairNotice = repairOneBuilding(player, secretary, spec);
                if (repairNotice.isBlank()) {
                    break;
                }
                notice = appendNotice(notice, repairNotice);
            }
        }
        return notice;
    }

    public String processMonthlyReputation(Player player) {
        String notice = "";
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
            if (secretary.getAssignedCity() == null) {
                continue;
            }
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            if (spec == null) {
                continue;
            }
            int reputationGain = random.nextInt(3) + 1;
            player.addReputation(reputationGain);
            saveRecord(player, RecordType.SECRETARY_SALARY, "비서 관리", null, reputationGain, null, spec.name() + " · " + secretary.getAssignedCity());
            notice = appendNotice(notice, spec.name() + " 비서 관리 평판 +" + reputationGain);
        }
        if (!notice.isBlank()) {
            refreshTitle(player);
        }
        return notice;
    }

    public void processSalaries(Player player) {
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            if (spec == null) {
                continue;
            }
            long salary = spec.monthlySalaryForProficiency(secretary.getProficiency());
            player.addSecretarySalaryCost(salary);
            saveRecord(player, RecordType.SECRETARY_SALARY, "비서 월급", -salary, 0, null, spec.name() + " · 숙련도 " + secretary.getProficiency());
        }
    }

    @Transactional(readOnly = true)
    public String appliedSpecialEffectSummary(OwnedSecretary secretary) {
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        int affinity = secretary.getAffinity();
        return switch (secretary.getSecretaryKey()) {
            case "secretary-1" -> spec.specialEffect() + " " + GameTextFormatter.percent(repairCostReductionPercent(secretary));
            case "secretary-2" -> spec.specialEffect() + " " + GameTextFormatter.percent(0.3 * affinity);
            case "secretary-3" -> spec.specialEffect() + " " + GameTextFormatter.percent(0.5 * affinity);
            case "secretary-4" -> spec.specialEffect() + " " + GameTextFormatter.percent(0.5 * affinity);
            case "secretary-5" -> spec.specialEffect() + " " + GameTextFormatter.percent(0.25 * affinity) + " · " + GameTextFormatter.percent(0.5 * affinity);
            case "secretary-6" -> spec.specialEffect() + " " + GameTextFormatter.percent(1.0 * affinity);
            default -> spec.specialEffectSummary();
        };
    }

    @Transactional(readOnly = true)
    public List<String> activeAbilitySummaries(OwnedSecretary secretary) {
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        return List.of(
                appliedSpecialEffectSummary(secretary),
                "관리 가능 건물 " + managedBuildingLimit(secretary) + "채",
                "자동수리 주기 " + spec.autoCheckDays(secretary.getProficiency()) + "일",
                "쿨타임 내 자동수리 최대 " + maxAutoRepairsPerCooldown(secretary) + "건",
                secretary.getProficiency() >= 16 ? "자동수리 시 평판 +1 추가 증가" : "",
                moveOutDefenseChance(secretary) > 0 ? "입주자 퇴거방어 " + moveOutDefenseChance(secretary) + "%" : "",
                secretary.getAssignedCity() == null ? "" : "매월 1일 배치 평판 +1~+3"
        ).stream()
                .filter(summary -> summary != null && !summary.isBlank())
                .toList();
    }

    public double repairRequestReductionPercent(Player player, String city) {
        return 0.0;
    }

    public double moveOutReductionPercent(Player player, String city) {
        return assignedSecretary(player, city)
                .filter(secretary -> "secretary-2".equals(secretary.getSecretaryKey()))
                .map(secretary -> 0.3 * secretary.getAffinity())
                .orElse(0.0);
    }

    public double moveInBonusPercent(Player player, String city) {
        return assignedSecretary(player, city)
                .filter(secretary -> "secretary-3".equals(secretary.getSecretaryKey()))
                .map(secretary -> 0.5 * secretary.getAffinity())
                .orElse(0.0);
    }

    public double rentBonusPercent(Player player, String city) {
        return assignedSecretary(player, city)
                .map(secretary -> switch (secretary.getSecretaryKey()) {
                    case "secretary-4" -> 0.5 * secretary.getAffinity();
                    case "secretary-5" -> 0.25 * secretary.getAffinity();
                    default -> 0.0;
                })
                .orElse(0.0);
    }

    public boolean defendMoveOut(Player player, OwnedBuilding building) {
        Optional<OwnedSecretary> secretaryOptional = assignedSecretary(player, building.getCity())
                .filter(secretary -> secretary.getProficiency() >= 21);
        if (secretaryOptional.isEmpty()) {
            return false;
        }
        OwnedSecretary secretary = secretaryOptional.get();
        int chance = moveOutDefenseChance(secretary);
        if (random.nextInt(100) >= chance) {
            return false;
        }
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
        String secretaryName = spec == null ? "비서" : spec.name();
        saveRecord(player, RecordType.MOVE_OUT, "퇴거방어", null, 0, building.getName(), secretaryName + " · 방어확률 " + chance + "%");
        return true;
    }

    private String repairOneBuilding(Player player, OwnedSecretary secretary, SecretarySpec spec) {
        Optional<OwnedBuilding> repairTarget = ownedBuildingRepository.findByPlayerAndCityOrderById(player, secretary.getAssignedCity()).stream()
                .limit(managedBuildingLimit(secretary))
                .filter(OwnedBuilding::isRepairRequested)
                .findFirst();
        if (repairTarget.isEmpty()) {
            return "";
        }
        OwnedBuilding building = repairTarget.get();
        long repairCost = automaticRepairCost(secretary, building);
        if (!player.spendCash(repairCost)) {
            return "";
        }
        boolean repairedWithinOneMonth = building.repair();
        int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
        if (repairedWithinOneMonth && secretary.getProficiency() >= 16) {
            reputationChange += 1;
        }
        if (repairedWithinOneMonth) {
            player.addReputation(reputationChange);
            refreshTitle(player);
        }
        int experienceGain = random.nextInt(100) < 70 ? 1 : 2;
        secretary.addProficiencyExperience(experienceGain);
        secretary.recordAutoRepair(player.getElapsedDays(), spec.autoCheckDays(secretary.getProficiency()));
        saveRecord(player, RecordType.REPAIR_COMPLETE, "비서수리", -repairCost, reputationChange, building.getName(), spec.name() + " · 숙련도 경험치 +" + experienceGain);
        return spec.name() + " 비서수리 1건";
    }

    private int managedBuildingLimit(OwnedSecretary secretary) {
        int proficiency = secretary.getProficiency();
        if (proficiency >= 16) {
            return 8;
        }
        if (proficiency >= 11) {
            return 6;
        }
        if (proficiency >= 6) {
            return 4;
        }
        return 2;
    }

    private int maxAutoRepairsPerCooldown(OwnedSecretary secretary) {
        return secretary.getProficiency() >= 11 ? 2 : 1;
    }

    private int moveOutDefenseChance(OwnedSecretary secretary) {
        if (secretary.getProficiency() >= 26) {
            return 50;
        }
        if (secretary.getProficiency() >= 21) {
            return 30;
        }
        return 0;
    }

    private long automaticRepairCost(OwnedSecretary secretary, OwnedBuilding building) {
        double reduction = "secretary-1".equals(secretary.getSecretaryKey()) ? repairCostReductionPercent(secretary) : 0.0;
        return Math.max(0L, (long) Math.floor(building.repairCost() * (100.0 - reduction) / 100.0));
    }

    private double repairCostReductionPercent(OwnedSecretary secretary) {
        return secretary.getAffinity();
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " · " + addition;
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }
}
