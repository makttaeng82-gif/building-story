# SecretaryOperationsService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/SecretaryOperationsService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
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
// 해설: 고용된 비서의 채용, 도시 배치, 월급, 평판 보너스, 자동수리, 퇴거방어 효과를 담당하는 서비스다.
    /*
     * 비서 고용, 배치, 숙련도/호감도 성장, 자동 수리 같은 운영 규칙을 담당한다.
     *
     * 비서는 assignedCity가 있을 때만 해당 도시의 건물 운영에 영향을 준다.
     * 숙련도는 관리 가능한 건물 수와 자동 수리 성능을, 호감도는 월세/쿨다운 보너스 같은 효과를 강화한다.
     */
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
    // 해설: 비서 운영에 필요한 플레이어, 건물, 비서, 기록 Repository와 카탈로그를 생성자 주입으로 받는다.
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
    // 해설: 첫 번째 비서인 후배를 고용하는 전용 흐름이다.
        // 첫 비서는 청주 보호 임차인 이벤트와 연결된다. 후배가 거주 중이어야 고용 가능하다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 이벤트 처리 등으로 일시정지 중이면 경제 행동을 막는다.
            return pausedActionMessage();
            // 해설: 일시정지 중 행동 제한 메시지를 반환한다.
        }
        if (player.isFirstSecretaryHired()) {
        // 해설: 첫 비서를 이미 고용했다면 중복 고용을 막는다.
            return "이미 고용한 비서";
        }
        if (player.getReputation() < 1000) {
        // 해설: 첫 비서 고용에는 평판 1000 이상이 필요하다.
            return "평판 1000 이상 필요";
        }
        Optional<OwnedBuilding> tenantBuilding = ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
        // 해설: 청주 보유 건물 중 후배가 보호 임차인으로 거주 중인 건물을 찾는다.
                .filter(OwnedBuilding::isProtectedTenant)
                // 해설: 보호 세입자 상태인 건물만 후보로 남긴다.
                .findFirst();
                // 해설: 조건을 만족하는 첫 번째 청주 보호 세입자 건물을 가져온다.
        if (tenantBuilding.isEmpty()) {
        // 해설: 후배가 거주 중인 건물이 없으면 첫 비서를 고용할 수 없다.
            return "후배가 거주 중이어야 고용 가능";
        }
        tenantBuilding.get().moveOut();
        // 해설: 고용되면 세입자 역할은 끝나므로 건물에서 퇴거 처리한다.
        player.hireFirstSecretary();
        // 해설: 플레이어 상태에 첫 비서를 고용했다는 표시를 남긴다.
        ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, "secretary-1")
        // 해설: 이미 secretary-1 소유 정보가 있는지 확인한다.
                .orElseGet(() -> ownedSecretaryRepository.save(new OwnedSecretary(player, "secretary-1", 1)));
                // 해설: 없으면 숙련도 1로 첫 비서 소유 정보를 새로 저장한다.
        SecretarySpec spec = secretaryCatalog.find("secretary-1").orElseThrow();
        // 해설: 첫 비서의 이름, 월급, 효과 정보를 카탈로그에서 가져온다.
        return "비서 고용 완료 · 월급 " + spec.monthlySalaryForProficiency(1) + "원";
    }

    @Transactional(readOnly = true)
    public boolean canHireSecretary(Player player, SecretarySpec spec) {
    // 해설: 특정 비서를 현재 플레이어가 고용할 수 있는지 조건만 검사한다.
        if (player.getReputation() < spec.requiredReputation()) {
        // 해설: 비서별 요구 평판보다 낮으면 고용 불가다.
            return false;
        }
        if ("secretary-1".equals(spec.key())) {
        // 해설: 첫 비서는 일반 평판 조건 외에 후배 세입자 조건이 있다.
            return ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
            // 해설: 청주 건물 중 보호 세입자가 있는지 확인한다.
                    .anyMatch(OwnedBuilding::isProtectedTenant);
                    // 해설: 하나라도 보호 세입자 건물이 있으면 첫 비서 고용 조건을 만족한다.
        }
        return true;
        // 해설: 퇴거방어에 성공했음을 SettlementService에 알린다.
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
    // 해설: 화면에 제안할 수 있는 다음 비서 후보를 찾는다.
        return secretaryCatalog.all().stream()
        // 해설: 카탈로그의 모든 비서를 순회한다.
                .filter(spec -> !isSecretaryOwned(player, spec))
                // 해설: 이미 보유한 비서는 제안 대상에서 제외한다.
                .filter(spec -> !player.isSecretaryOfferDismissed(spec.key()))
                // 해설: 사용자가 닫은 제안은 다시 보여주지 않는다.
                .filter(spec -> canHireSecretary(player, spec))
                // 해설: 현재 고용 조건을 만족하는 비서만 남긴다.
                .findFirst();
                // 해설: 조건을 만족하는 첫 번째 비서를 제안 후보로 반환한다.
    }

    @Transactional(readOnly = true)
    public List<OwnedSecretary> ownedSecretaries(Player player) {
        return ownedSecretaryRepository.findByPlayerOrderById(player);
    }

    @Transactional(readOnly = true)
    public Optional<OwnedSecretary> assignedSecretary(Player player, String city) {
    // 해설: 특정 도시에 배치된 비서를 찾는다.
        return ownedSecretaryRepository.findByPlayerOrderById(player).stream()
                .filter(secretary -> secretary.isAssignedTo(city))
                // 해설: assignedCity가 요청 도시와 같은 비서만 남긴다.
                .findFirst();
                // 해설: 해당 도시에 배치된 첫 번째 비서를 반환한다.
    }

    @Transactional(readOnly = true)
    public boolean canAssignSecretaryToCity(Player player, OwnedSecretary targetSecretary, String city) {
    // 해설: 해당 도시에 targetSecretary를 배치할 수 있는지 검사한다.
        if (player == null || targetSecretary == null || city == null) {
        // 해설: 필수 값이 없으면 배치 가능 여부를 판단할 수 없으므로 false다.
            return false;
        }
        return ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city).stream()
        // 해설: 이미 해당 도시에 배치된 비서 목록을 조회한다.
                .allMatch(secretary -> secretary.getId().equals(targetSecretary.getId()));
                // 해설: 도시에 비서가 없거나 같은 비서만 있으면 배치 가능하다.
    }

    public String hireSecretary(long playerId, String secretaryKey) {
    // 해설: 일반 비서 고용 요청을 처리한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 이벤트 처리 등으로 일시정지 중이면 경제 행동을 막는다.
            return pausedActionMessage();
            // 해설: 일시정지 중 행동 제한 메시지를 반환한다.
        }
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        // 해설: 고용할 비서 스펙을 카탈로그에서 찾는다.
        if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isPresent()) {
        // 해설: 이미 보유 중인 비서면 중복 고용을 막는다.
            return "이미 보유중인 비서";
        }
        if (!canHireSecretary(player, spec)) {
        // 해설: 평판이나 특수 조건을 만족하지 못하면 고용을 막는다.
            return "고용 조건 미달";
        }
        if ("secretary-1".equals(secretaryKey)) {
        // 해설: 첫 비서 고용이면 후배 세입자 퇴거와 플레이어 상태 표시가 필요하다.
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
                    .filter(OwnedBuilding::isProtectedTenant)
                    // 해설: 보호 세입자 상태인 건물만 후보로 남긴다.
                    .findFirst()
                    .ifPresent(OwnedBuilding::moveOut);
                    // 해설: 후배가 거주 중인 건물을 찾으면 퇴거 처리한다.
            player.hireFirstSecretary();
            // 해설: 플레이어 상태에 첫 비서를 고용했다는 표시를 남긴다.
        }
        ownedSecretaryRepository.save(new OwnedSecretary(player, secretaryKey, spec.baseProficiency()));
        // 해설: 비서 소유 정보를 기본 숙련도로 저장한다.
        return spec.name() + " 고용 완료";
    }

    public String assignSecretary(long playerId, long ownedSecretaryId, String city) {
    // 해설: 보유 비서를 특정 도시에 배치한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 이벤트 처리 등으로 일시정지 중이면 경제 행동을 막는다.
            return pausedActionMessage();
            // 해설: 일시정지 중 행동 제한 메시지를 반환한다.
        }
        if (!buildingCatalog.cities().contains(city)) {
        // 해설: 카탈로그에 없는 도시는 배치할 수 없다.
            return "존재하지 않는 도시";
        }
        if (!reputationCatalog.isCityUnlocked(city, player.getReputation(), !player.isEmployed())) {
        // 해설: 해금되지 않은 도시는 비서를 배치할 수 없다.
            return "해금되지 않은 도시";
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        // 해설: 배치할 보유 비서를 조회한다.
        if (!secretary.getPlayer().getId().equals(player.getId())) {
        // 해설: 다른 플레이어의 비서는 조작할 수 없다.
            return "잘못된 비서";
        }
        boolean occupiedByOtherSecretary = ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city).stream()
        // 해설: 해당 도시에 이미 다른 비서가 배치되어 있는지 확인한다.
                .anyMatch(assigned -> !assigned.getId().equals(secretary.getId()));
                // 해설: 같은 비서가 아닌 다른 비서가 있으면 도시는 이미 점유된 상태다.
        if (occupiedByOtherSecretary) {
        // 해설: 도시에 다른 비서가 있으면 배치를 거부한다.
            return "이미 다른 비서가 배치된 도시";
        }
        secretary.assignTo(city);
        // 해설: 비서의 assignedCity를 요청 도시로 변경한다.
        return "비서 배치 완료";
    }

    public String unassignSecretary(long playerId, long ownedSecretaryId) {
    // 해설: 비서를 현재 배치 도시에서 제외한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 이벤트 처리 등으로 일시정지 중이면 경제 행동을 막는다.
            return pausedActionMessage();
            // 해설: 일시정지 중 행동 제한 메시지를 반환한다.
        }
        OwnedSecretary secretary = ownedSecretaryRepository.findById(ownedSecretaryId).orElseThrow();
        // 해설: 배치할 보유 비서를 조회한다.
        if (!secretary.getPlayer().getId().equals(player.getId())) {
        // 해설: 다른 플레이어의 비서는 조작할 수 없다.
            return "잘못된 비서";
        }
        secretary.assignTo(null);
        // 해설: assignedCity를 null로 만들어 미배치 상태로 바꾼다.
        return "비서 배치 제외 완료";
    }

    public String processAutoRepairs(Player player) {
    // 해설: 배치된 비서들이 자동 수리를 수행할 수 있는지 검사하고 실행한다.
        String notice = "";
        // 해설: 자동수리 결과 안내 문구를 누적한다.
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
        // 해설: 플레이어가 보유한 모든 비서를 순회한다.
            if (secretary.getAssignedCity() == null) {
            // 해설: 도시에 배치되지 않은 비서는 건물 운영 효과를 발동하지 않는다.
                continue;
            }
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            // 해설: 비서 키로 스펙을 찾는다. 데이터가 없으면 해당 비서는 건너뛴다.
            int maxRepairs = maxAutoRepairsPerCooldown(secretary);
            // 해설: 숙련도에 따라 이번 자동수리 쿨타임 동안 가능한 최대 수리 횟수를 계산한다.
            if (spec == null || !secretary.canAutoRepair(player.getElapsedDays(), maxRepairs)) {
            // 해설: 스펙이 없거나 아직 자동수리 쿨타임이면 처리하지 않는다.
                continue;
            }
            for (int repaired = 0; repaired < maxRepairs && secretary.canAutoRepair(player.getElapsedDays(), maxRepairs); repaired++) {
            // 해설: 최대 수리 횟수 안에서 수리 가능한 동안 반복한다.
                String repairNotice = repairOneBuilding(player, secretary, spec);
                // 해설: 비서가 관리 도시의 수리 요청 건물 하나를 수리하도록 시도한다.
                if (repairNotice.isBlank()) {
                // 해설: 수리할 건물이 없거나 비용 부족이면 반복을 중단한다.
                    break;
                }
                notice = appendNotice(notice, repairNotice);
                // 해설: 수리 성공 안내를 기존 안내에 이어 붙인다.
            }
        }
        return notice;
    }

    public String processMonthlyReputation(Player player) {
    // 해설: 매월 1일 배치된 비서가 주는 평판 보너스를 처리한다.
        String notice = "";
        // 해설: 비서 관리 평판 보너스 안내 문구를 누적한다.
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
        // 해설: 플레이어가 보유한 모든 비서를 순회한다.
            if (secretary.getAssignedCity() == null) {
            // 해설: 도시에 배치되지 않은 비서는 월간 관리 평판을 주지 않는다.
                continue;
            }
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            // 해설: 비서 키로 스펙을 찾는다. 데이터가 없으면 해당 비서는 건너뛴다.
            if (spec == null) {
                continue;
            }
            int reputationGain = random.nextInt(3) + 1;
            // 해설: 배치된 비서마다 평판 1~3을 무작위로 지급한다.
            player.addReputation(reputationGain);
            // 해설: 비서 관리 효과로 플레이어 평판을 올린다.
            saveRecord(player, RecordType.SECRETARY_SALARY, "비서 관리", null, reputationGain, null, spec.name() + " · " + secretary.getAssignedCity());
            // 해설: 비서 관리 평판 증가를 월간 기록에 저장한다.
            notice = appendNotice(notice, spec.name() + " 비서 관리 평판 +" + reputationGain);
        }
        if (!notice.isBlank()) {
        // 해설: 평판 안내가 하나라도 있으면 칭호 갱신이 필요하다.
            refreshTitle(player);
            // 해설: 평판 변화에 따라 현재 칭호를 다시 계산한다.
        }
        return notice;
    }

    public void processSalaries(Player player) {
    // 해설: 매월 15일 비서 월급을 처리한다.
        for (OwnedSecretary secretary : ownedSecretaryRepository.findByPlayerOrderById(player)) {
        // 해설: 플레이어가 보유한 모든 비서를 순회한다.
            SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
            // 해설: 비서 키로 스펙을 찾는다. 데이터가 없으면 해당 비서는 건너뛴다.
            if (spec == null) {
                continue;
            }
            long salary = spec.monthlySalaryForProficiency(secretary.getProficiency());
            // 해설: 비서 숙련도 기준 월급을 계산한다.
            player.addSecretarySalaryCost(salary);
            // 해설: 비서 월급을 플레이어 비용으로 반영한다.
            saveRecord(player, RecordType.SECRETARY_SALARY, "비서 월급", -salary, 0, null, spec.name() + " · 숙련도 " + secretary.getProficiency());
            // 해설: 비서 월급 지출 기록을 저장한다.
        }
    }

    @Transactional(readOnly = true)
    public String appliedSpecialEffectSummary(OwnedSecretary secretary) {
    // 해설: 비서별 특수 효과가 현재 친밀도 기준으로 얼마나 적용되는지 문구로 만든다.
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        // 해설: 비서 스펙을 가져와 효과 이름과 기본 설명을 사용한다.
        int affinity = secretary.getAffinity();
        // 해설: 친밀도를 가져온다. 여러 비서 효과가 친밀도에 비례한다.
        return switch (secretary.getSecretaryKey()) {
        // 해설: 비서 키마다 다른 특수 효과 계산식을 적용한다.
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
    // 해설: 비서 카드에 표시할 현재 적용 능력 목록을 만든다.
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElseThrow();
        // 해설: 비서 스펙을 가져와 효과 이름과 기본 설명을 사용한다.
        return List.of(
        // 해설: 여러 능력 설명 후보를 리스트로 만든 뒤 빈 문구를 제거한다.
                appliedSpecialEffectSummary(secretary),
                "관리 가능 건물 " + managedBuildingLimit(secretary) + "채",
                // 해설: 숙련도 기준으로 관리 가능한 건물 수를 표시한다.
                "자동수리 주기 " + spec.autoCheckDays(secretary.getProficiency()) + "일",
                // 해설: 숙련도 기준 자동수리 체크 주기를 표시한다.
                "쿨타임 내 자동수리 최대 " + maxAutoRepairsPerCooldown(secretary) + "건",
                // 해설: 쿨타임 동안 가능한 자동수리 횟수를 표시한다.
                secretary.getProficiency() >= 16 ? "자동수리 시 평판 +1 추가 증가" : "",
                // 해설: 숙련도 16 이상이면 자동수리 평판 추가 보너스를 표시한다.
                moveOutDefenseChance(secretary) > 0 ? "입주자 퇴거방어 " + moveOutDefenseChance(secretary) + "%" : "",
                // 해설: 퇴거방어 확률이 있으면 표시한다.
                secretary.getAssignedCity() == null ? "" : "매월 1일 배치 평판 +1~+3"
        ).stream()
                .filter(summary -> summary != null && !summary.isBlank())
                // 해설: 빈 문자열 능력 설명은 화면에 표시하지 않도록 제거한다.
                .toList();
    }

    public double repairRequestReductionPercent(Player player, String city) {
    // 해설: 수리 요청 발생 확률 감소 효과를 반환한다. 현재 구현은 효과 없음이다.
        return 0.0;
        // 해설: 현재 해당 효과를 주는 비서가 없으므로 0%를 반환한다.
    }

    public double moveOutReductionPercent(Player player, String city) {
    // 해설: 2번 비서가 해당 도시에 배치됐을 때 퇴거 확률 감소율을 계산한다.
        return assignedSecretary(player, city)
                .filter(secretary -> "secretary-2".equals(secretary.getSecretaryKey()))
                // 해설: 2번 비서만 퇴거 확률 감소 효과를 가진다.
                .map(secretary -> 0.3 * secretary.getAffinity())
                // 해설: 친밀도 1당 0.3% 감소로 계산한다.
                .orElse(0.0);
    }

    public double moveInBonusPercent(Player player, String city) {
    // 해설: 3번 비서가 해당 도시에 배치됐을 때 입주 확률 증가율을 계산한다.
        return assignedSecretary(player, city)
                .filter(secretary -> "secretary-3".equals(secretary.getSecretaryKey()))
                // 해설: 3번 비서만 입주 확률 증가 효과를 가진다.
                .map(secretary -> 0.5 * secretary.getAffinity())
                // 해설: 친밀도 1당 0.5% 증가로 계산한다.
                .orElse(0.0);
    }

    public double rentBonusPercent(Player player, String city) {
    // 해설: 해당 도시에 배치된 비서의 월세 보너스를 계산한다.
        return assignedSecretary(player, city)
                .map(secretary -> switch (secretary.getSecretaryKey()) {
                // 해설: 비서 키에 따라 월세 보너스 공식을 다르게 적용한다.
                    case "secretary-4" -> 0.5 * secretary.getAffinity();
                    // 해설: 4번 비서는 친밀도 1당 월세 0.5% 보너스를 준다.
                    case "secretary-5" -> 0.25 * secretary.getAffinity();
                    // 해설: 5번 비서는 친밀도 1당 월세 0.25% 보너스를 준다.
                    default -> 0.0;
                })
                .orElse(0.0);
    }

    public boolean defendMoveOut(Player player, OwnedBuilding building) {
    // 해설: 세입자 퇴거 이벤트가 발생했을 때 비서가 방어하는지 판정한다.
        Optional<OwnedSecretary> secretaryOptional = assignedSecretary(player, building.getCity())
        // 해설: 건물 도시와 같은 도시에 배치된 비서를 찾는다.
                .filter(secretary -> secretary.getProficiency() >= 21);
                // 해설: 퇴거방어는 숙련도 21 이상 비서만 가능하다.
        if (secretaryOptional.isEmpty()) {
        // 해설: 조건을 만족하는 비서가 없으면 방어 실패다.
            return false;
        }
        OwnedSecretary secretary = secretaryOptional.get();
        int chance = moveOutDefenseChance(secretary);
        // 해설: 숙련도 기준 퇴거방어 확률을 계산한다.
        if (random.nextInt(100) >= chance) {
        // 해설: 난수 판정이 확률 밖이면 방어에 실패한다.
            return false;
        }
        SecretarySpec spec = secretaryCatalog.find(secretary.getSecretaryKey()).orElse(null);
        // 해설: 비서 키로 스펙을 찾는다. 데이터가 없으면 해당 비서는 건너뛴다.
        String secretaryName = spec == null ? "비서" : spec.name();
        saveRecord(player, RecordType.MOVE_OUT, "퇴거방어", null, 0, building.getName(), secretaryName + " · 방어확률 " + chance + "%");
        // 해설: 퇴거방어 성공 기록을 남긴다.
        return true;
        // 해설: 퇴거방어에 성공했음을 SettlementService에 알린다.
    }

    private String repairOneBuilding(Player player, OwnedSecretary secretary, SecretarySpec spec) {
    // 해설: 비서 한 명이 관리 도시에서 수리 요청 건물 하나를 자동 수리한다.
        Optional<OwnedBuilding> repairTarget = ownedBuildingRepository.findByPlayerAndCityOrderById(player, secretary.getAssignedCity()).stream()
        // 해설: 비서가 배치된 도시의 보유 건물 중 수리 대상을 찾는다.
                .limit(managedBuildingLimit(secretary))
                // 해설: 비서 숙련도에 따라 관리 가능한 앞쪽 건물 수까지만 검사한다.
                .filter(OwnedBuilding::isRepairRequested)
                // 해설: 수리 요청이 있는 건물만 수리 후보로 남긴다.
                .findFirst();
                // 해설: 관리 범위 안에서 가장 앞에 있는 수리 요청 건물 하나를 선택한다.
        if (repairTarget.isEmpty()) {
        // 해설: 수리 요청 건물이 없으면 자동수리 결과 문구 없이 끝낸다.
            return "";
        }
        OwnedBuilding building = repairTarget.get();
        long repairCost = automaticRepairCost(secretary, building);
        // 해설: 비서 효과를 반영한 자동수리 비용을 계산한다.
        if (!player.spendCash(repairCost)) {
        // 해설: 수리비를 차감한다. 현금 부족이면 수리하지 않는다.
            return "";
        }
        boolean repairedWithinOneMonth = building.repair();
        // 해설: 건물을 수리 완료 상태로 바꾸고 한 달 내 수리였는지 결과를 받는다.
        int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
        // 해설: 한 달 내 수리면 기본 평판 보상을 준다.
        if (repairedWithinOneMonth && secretary.getProficiency() >= 16) {
        // 해설: 숙련도 16 이상이면 자동수리 평판 보상이 1 추가된다.
            reputationChange += 1;
        }
        if (repairedWithinOneMonth) {
            player.addReputation(reputationChange);
            // 해설: 자동수리로 얻은 평판을 플레이어에게 반영한다.
            refreshTitle(player);
            // 해설: 평판 변화에 따라 현재 칭호를 다시 계산한다.
        }
        int experienceGain = random.nextInt(100) < 70 ? 1 : 2;
        // 해설: 자동수리 경험치는 70% 확률로 1, 30% 확률로 2를 준다.
        secretary.addProficiencyExperience(experienceGain);
        // 해설: 비서 숙련도 경험치를 증가시킨다.
        secretary.recordAutoRepair(player.getElapsedDays(), spec.autoCheckDays(secretary.getProficiency()));
        // 해설: 자동수리 사용일과 다음 체크 주기를 기록해 쿨타임을 관리한다.
        saveRecord(player, RecordType.REPAIR_COMPLETE, "비서수리", -repairCost, reputationChange, building.getName(), spec.name() + " · 숙련도 경험치 +" + experienceGain);
        // 해설: 자동수리 비용, 평판 변화, 경험치 정보를 월간 기록에 저장한다.
        return spec.name() + " 비서수리 1건";
    }

    private int managedBuildingLimit(OwnedSecretary secretary) {
    // 해설: 숙련도에 따라 비서가 관리 가능한 건물 수를 계산한다.
        int proficiency = secretary.getProficiency();
        if (proficiency >= 16) {
        // 해설: 숙련도 16 이상이면 8채까지 관리한다.
            return 8;
        }
        if (proficiency >= 11) {
        // 해설: 숙련도 11 이상이면 6채까지 관리한다.
            return 6;
        }
        if (proficiency >= 6) {
        // 해설: 숙련도 6 이상이면 4채까지 관리한다.
            return 4;
        }
        return 2;
        // 해설: 기본 관리 가능 건물 수는 2채다.
    }

    private int maxAutoRepairsPerCooldown(OwnedSecretary secretary) {
    // 해설: 자동수리 쿨타임 한 번에 가능한 최대 수리 횟수를 계산한다.
        return secretary.getProficiency() >= 11 ? 2 : 1;
        // 해설: 숙련도 11 이상이면 2건, 그 전에는 1건이다.
    }

    private int moveOutDefenseChance(OwnedSecretary secretary) {
    // 해설: 숙련도 기준 퇴거방어 확률을 계산한다.
        if (secretary.getProficiency() >= 26) {
        // 해설: 숙련도 26 이상이면 퇴거방어 확률 50%다.
            return 50;
        }
        if (secretary.getProficiency() >= 21) {
        // 해설: 숙련도 21 이상이면 퇴거방어 확률 30%다.
            return 30;
        }
        return 0;
    }

    private long automaticRepairCost(OwnedSecretary secretary, OwnedBuilding building) {
    // 해설: 비서 특수 효과를 반영한 자동수리 비용을 계산한다.
        double reduction = "secretary-1".equals(secretary.getSecretaryKey()) ? repairCostReductionPercent(secretary) : 0.0;
        // 해설: 1번 비서만 친밀도만큼 수리비 감소 효과를 가진다.
        return Math.max(0L, (long) Math.floor(building.repairCost() * (100.0 - reduction) / 100.0));
        // 해설: 수리비에 감소율을 적용하고 음수가 되지 않게 보정한다.
    }

    private double repairCostReductionPercent(OwnedSecretary secretary) {
    // 해설: 1번 비서의 수리비 감소율을 계산한다.
        return secretary.getAffinity();
        // 해설: 친밀도 1당 수리비 1% 감소로 사용한다.
    }

    private String pausedActionMessage() {
    // 해설: 일시정지 중 경제 행동 제한 메시지를 반환한다.
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
    // 해설: 여러 안내 문구를 하나로 합친다.
        if (addition == null || addition.isBlank()) {
        // 해설: 추가 안내가 없으면 기존 안내만 반환한다.
            return base;
        }
        if (base == null || base.isBlank()) {
        // 해설: 기존 안내가 없으면 추가 안내만 반환한다.
            return addition;
        }
        return base + " · " + addition;
        // 해설: 두 안내가 모두 있으면 가운데 구분자를 넣어 합친다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 비서 운영 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 MonthlyRecord를 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 기준일은 최소 1일로 보정한다.
    }

    private void refreshTitle(Player player) {
    // 해설: 평판 변화 후 플레이어 칭호를 현재 티어에 맞게 갱신한다.
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        // 해설: 평판과 퇴사 여부로 티어를 찾고 해당 칭호를 플레이어에 저장한다.
    }
}
```
