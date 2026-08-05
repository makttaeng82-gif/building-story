package com.game.buildingstory.service;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.OwnedPropertyManager;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedPropertyManagerRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 도시별 부동산 관리직원의 채용, 급여와 자동수리를 한 곳에서 처리한다.
 *
 * <p>관리직원이 존재하는 도시는 비서 자동수리에서 제외된다. 급여 체불로 관리직원이 중단되어도
 * 비서가 다시 수리하면 체불 패널티와 역할 인계가 무효가 되므로, 고용 여부와 실제 수리 가능 상태를
 * 구분해서 제공한다.</p>
 */
@Service
@Transactional
public class PropertyManagementService {
    public static final long MONTHLY_SALARY = 5_000_000L;
    private static final int REPAIR_REPUTATION_REWARD = 3;
    private static final int RECORD_RETENTION_DAYS = 62;
    private static final int RECENT_REPAIR_WINDOW_DAYS = 30;

    private final PlayerRepository playerRepository;
    private final OwnedPropertyManagerRepository propertyManagerRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;

    public PropertyManagementService(
            PlayerRepository playerRepository,
            OwnedPropertyManagerRepository propertyManagerRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog
    ) {
        this.playerRepository = playerRepository;
        this.propertyManagerRepository = propertyManagerRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.buildingCatalog = buildingCatalog;
        this.reputationCatalog = reputationCatalog;
    }

    public String hire(long playerId, String city) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return "일시정지 중에는 관리직원을 채용할 수 없음";
        }
        if (!isFeatureVisible(player)) {
            return "서울 해금 후 관리직원을 채용할 수 있음";
        }
        if (!isHandoffReady(player)) {
            return "비서 6명 고용 후 부동산 관리를 인계할 수 있음";
        }
        if (!buildingCatalog.cities().contains(city)) {
            return "존재하지 않는 도시";
        }
        if (propertyManagerRepository.findByPlayerAndCity(player, city).isPresent()) {
            return city + " 관리직원은 이미 채용됨";
        }

        var assignedSecretaries = ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city);
        int unassignedSecretaryCount = assignedSecretaries.size();
        assignedSecretaries.forEach(secretary -> secretary.assignTo(null));
        propertyManagerRepository.save(new OwnedPropertyManager(player, city));
        String memo = unassignedSecretaryCount == 0
                ? "자동수리 인계 완료"
                : "자동수리 인계 완료 · 비서 배치 해제 " + unassignedSecretaryCount + "명";
        saveRecord(player, "부동산 관리직원 채용", null, 0, city, memo);
        return city + " 부동산 관리직원 채용 완료"
                + (unassignedSecretaryCount == 0 ? "" : " · 비서 배치 해제");
    }

    @Transactional(readOnly = true)
    public boolean isFeatureVisible(Player player) {
        return reputationCatalog.isCityUnlocked("서울", player.getReputation(), !player.isEmployed());
    }

    @Transactional(readOnly = true)
    public boolean isHandoffReady(Player player) {
        return isFeatureVisible(player)
                && ownedSecretaryRepository.findByPlayerOrderById(player).size() >= 6;
    }

    @Transactional(readOnly = true)
    public Optional<OwnedPropertyManager> manager(Player player, String city) {
        return propertyManagerRepository.findByPlayerAndCity(player, city);
    }

    @Transactional(readOnly = true)
    public List<OwnedPropertyManager> managers(Player player) {
        return propertyManagerRepository.findByPlayerOrderById(player);
    }

    /** 비서 자동수리에서 제외해야 하는, 이미 역할을 인계받은 도시 목록이다. */
    @Transactional(readOnly = true)
    public Set<String> managedCities(Player player) {
        Set<String> cities = new LinkedHashSet<>();
        propertyManagerRepository.findByPlayerOrderById(player)
                .forEach(manager -> cities.add(manager.getCity()));
        return Set.copyOf(cities);
    }

    @Transactional(readOnly = true)
    public long salaryDue(OwnedPropertyManager manager) {
        return Math.multiplyExact(MONTHLY_SALARY, manager.getUnpaidSalaryMonths() + 1L);
    }

    /**
     * 고용된 관리직원 전원의 당월 급여와 체불액을 한 번에 지급한다.
     * 일부 도시만 지급되는 순서 의존성을 막기 위해 합계가 부족하면 누구에게도 지급하지 않는다.
     */
    public String processSalaries(Player player) {
        List<OwnedPropertyManager> managers = propertyManagerRepository.findByPlayerOrderById(player);
        if (managers.isEmpty()) {
            return "";
        }
        long totalDue = managers.stream().mapToLong(this::salaryDue).sum();
        if (player.spendCash(totalDue)) {
            managers.forEach(OwnedPropertyManager::recordSalaryPaid);
            saveRecord(player, "관리직원 월급", -totalDue, 0, null,
                    managers.size() + "명 · 자동수리 정상");
            return "관리직원 월급 지급 " + managers.size() + "명";
        }

        managers.forEach(OwnedPropertyManager::recordUnpaidSalary);
        saveRecord(player, "관리직원 월급 미지급", null, 0, null,
                managers.size() + "명 · 자동수리 중단");
        return "관리직원 월급 미지급 · 자동수리 중단";
    }

    /**
     * 활성 관리직원이 맡은 도시의 모든 수리요청을 즉시 처리한다.
     * 오래 방치된 건물을 먼저 처리하고, 방치기간이 같으면 낮은 건물 ID부터 처리한다.
     */
    public String processAutoRepairs(Player player) {
        String notice = "";
        for (OwnedPropertyManager manager : propertyManagerRepository.findByPlayerOrderById(player)) {
            if (!manager.isActive()) {
                continue;
            }
            List<OwnedBuilding> targets = ownedBuildingRepository.findByPlayerAndCityOrderById(player, manager.getCity()).stream()
                    .filter(OwnedBuilding::isRepairRequested)
                    .sorted(Comparator.comparingInt(OwnedBuilding::getRepairNeglectedMonths).reversed()
                            .thenComparing(OwnedBuilding::getId))
                    .toList();
            int repairedCount = 0;
            boolean cashShortage = false;
            for (OwnedBuilding building : targets) {
                long repairCost = building.repairCost();
                if (!player.spendCash(repairCost)) {
                    cashShortage = true;
                    break;
                }
                boolean repairedWithinOneMonth = building.repair();
                int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
                if (reputationChange > 0) {
                    player.addReputation(reputationChange);
                }
                saveRecord(player, "관리직원 수리", -repairCost, reputationChange,
                        building.getName(), "부동산 관리직원");
                repairedCount++;
            }
            if (repairedCount > 0) {
                notice = appendNotice(notice, manager.getCity() + " 관리직원 자동수리 " + repairedCount + "건");
            }
            if (cashShortage) {
                notice = appendNotice(notice, manager.getCity() + " 관리직원 수리비 부족");
            }
        }
        if (!notice.isBlank()) {
            player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        }
        return notice;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> recentRepairCountsByCity(Player player) {
        Map<String, String> cityByBuildingName = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .collect(Collectors.toMap(
                        OwnedBuilding::getName,
                        OwnedBuilding::getCity,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        int firstElapsedDay = Math.max(0, player.getElapsedDays() - RECENT_REPAIR_WINDOW_DAYS + 1);
        return monthlyRecordRepository
                .findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(player, firstElapsedDay)
                .stream()
                .filter(record -> record.getType() == RecordType.PROPERTY_MANAGER)
                .map(MonthlyRecord::getBuildingName)
                .filter(cityByBuildingName::containsKey)
                .collect(Collectors.groupingBy(
                        cityByBuildingName::get,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private void saveRecord(Player player, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(
                player, RecordType.PROPERTY_MANAGER, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(
                player, Math.max(0, player.getElapsedDays() - RECORD_RETENTION_DAYS));
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        return base == null || base.isBlank() ? addition : base + " · " + addition;
    }
}
