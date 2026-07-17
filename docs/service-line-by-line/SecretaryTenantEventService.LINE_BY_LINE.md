# SecretaryTenantEventService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/SecretaryTenantEventService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드는 설명하지 않는다.

```java
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
// 해설: 비서가 세입자로 등장하고, 부탁을 주고, 고용 가능 상태로 넘어가는 전체 흐름을 담당하는 서비스다.
    /*
     * 비서 임차인 이벤트의 등장 조건, 요청 처리, 임대료 감면 상태를 담당한다.
     *
     * 비서별 시나리오는 SecretaryTenantScenarioCatalog에 있고, 이 서비스는 실제 플레이어 상태와
     * 보유 건물을 비교해 이벤트를 만들거나 완료 상태로 전환한다.
     */
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
    // 해설: 외부에서 주기적으로 호출하는 진입점이다. 현재 플레이어 상태를 보고 비서 이벤트를 새로 띄울지 판단한다.
        if (activeEvent(player).isPresent() || hasActiveAuction) {
        // 해설: 이미 처리해야 할 이벤트가 있거나 경매가 진행 중이면 비서 이벤트를 새로 만들지 않는다. 이벤트가 겹쳐서 UI 흐름이 꼬이는 것을 막는다.
            return;
            // 해설: 이 메서드의 남은 처리를 하지 않고 즉시 종료한다.
        }
        activateMissingIntro(player);
        // 해설: 보유 건물 중 아직 인트로를 보지 않은 비서 이벤트가 있는지 먼저 찾는다.
        if (activeEvent(player).isPresent()) {
        // 해설: 방금 인트로 이벤트가 생성됐을 수 있으므로, 부탁/고용 이벤트를 이어서 띄우지 않게 다시 확인한다.
            return;
            // 해설: 이 메서드의 남은 처리를 하지 않고 즉시 종료한다.
        }
        updateProgress(player);
        // 해설: 이미 수락한 부탁의 기간이 끝났는지 확인해서 고용 가능 상태로 넘긴다.
        activateNextEvent(player);
        // 해설: 부탁 가능 또는 고용 가능 상태인 비서 이벤트를 실제 화면 이벤트로 띄운다.
    }

    public void tryActivateIntro(Player player, OwnedBuilding building) {
    // 해설: 특정 건물을 기준으로 연결된 비서 인트로 이벤트를 열 수 있는지 검사한다.
        Optional<SecretaryTenantScenario> scenario = SecretaryTenantScenarioCatalog.findByCityAndSlot(building.getCity(), buildingSlot(building));
        // 해설: 건물의 도시와 슬롯 번호로 어떤 비서 시나리오가 연결되는지 카탈로그에서 찾는다.
        if (scenario.isEmpty() || "secretary-1".equals(scenario.get().secretaryKey())) {
        // 해설: 해당 건물에 연결된 시나리오가 없거나 첫 비서라면 여기서 인트로 이벤트를 만들지 않는다. 첫 비서는 별도 초기 입주 로직을 탄다.
            return;
            // 해설: 이 메서드의 남은 처리를 하지 않고 즉시 종료한다.
        }
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.get().secretaryKey()).isPresent()) {
        // 해설: 같은 비서 이벤트가 이미 만들어졌으면 중복 생성하지 않는다.
            return;
            // 해설: 이 메서드의 남은 처리를 하지 않고 즉시 종료한다.
        }
        if (activeEvent(player).isPresent()) {
        // 해설: 이미 다른 활성 이벤트가 있으면 새 인트로 이벤트를 띄우지 않는다.
            return;
            // 해설: 현재 화면 이벤트 처리를 우선해야 하므로 여기서 끝낸다.
        }
        activateSecretaryEvent(
        // 해설: 화면에 표시될 GameEvent를 생성한다. 아래 인자들이 이벤트 키, 제목, 본문, 효과 키, 버튼 문구다.
                player,
                "secretary_intro_" + scenario.get().secretaryKey(),
                scenario.get().introTitle(),
                scenario.get().introBody(),
                INTRO_EFFECT_PREFIX + scenario.get().secretaryKey(),
                "확인"
        );
    }

    public void applyFirstTenantMoveIn(Player player) {
    // 해설: 첫 비서 세입자를 청주 건물에 입주시키는 초기 이벤트 처리다.
        ownedBuildingRepository.findByPlayerAndCityOrderById(player, "청주").stream()
        // 해설: 플레이어가 가진 청주 건물을 id 순서로 가져와 Stream으로 순회한다.
                .filter(building -> !building.isOccupied())
                // 해설: 이미 입주자가 있는 건물은 제외하고, 빈 건물만 후보로 남긴다.
                .findFirst()
                // 해설: 조건에 맞는 첫 번째 건물만 사용한다. Optional로 반환되므로 없을 수도 있다.
                .ifPresent(building -> {
                // 해설: 빈 건물이 있을 때만 내부 입주 로직을 실행한다.
                    building.moveInSecretaryTenant("secretary-1");
                    // 해설: 찾은 건물에 첫 비서 세입자를 입주시킨다.
                    createTenantEvent(player, building, SecretaryTenantScenarioCatalog.find("secretary-1").orElseThrow());
                    // 해설: 첫 비서의 세입자 이벤트 엔티티를 생성한다. 시나리오가 없으면 데이터 오류로 보고 예외가 난다.
                });
        player.markFirstTenantEventDone();
        // 해설: 첫 세입자 이벤트가 다시 실행되지 않도록 플레이어 상태에 완료 표시를 남긴다.
    }

    public void applyIntroEvent(Player player, String secretaryKey) {
    // 해설: 인트로 이벤트 확인 후 실제 건물에 비서 세입자를 입주시키는 처리다.
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 시나리오 정의를 찾는다. 이 시점에는 반드시 존재해야 하므로 없으면 예외를 던진다.
        OwnedBuilding building = ownedBuildingRepository.findByPlayerAndCityOrderById(player, scenario.city()).stream()
        // 해설: 시나리오에 지정된 도시의 보유 건물 목록에서 실제 입주할 건물을 찾기 시작한다.
                .filter(candidate -> buildingSlot(candidate) == scenario.buildingSlot())
                // 해설: 시나리오가 요구하는 슬롯 번호와 같은 건물만 남긴다.
                .findFirst()
                // 해설: 조건에 맞는 첫 번째 건물만 사용한다. Optional로 반환되므로 없을 수도 있다.
                .orElseThrow();
                // 해설: 조건에 맞는 건물이 없으면 이벤트 데이터와 보유 건물 상태가 맞지 않는 것이므로 예외를 낸다.
        building.moveInSecretaryTenant(secretaryKey);
        // 해설: 찾은 건물에 해당 비서 세입자를 입주시킨다.
        createTenantEvent(player, building, scenario);
        // 해설: 입주 상태를 추적할 SecretaryTenantEvent를 만든다.
        saveRecord(player, RecordType.MOVE_IN, "비서 세입자 입주", null, 0, building.getName(), secretaryCatalog.find(secretaryKey).map(SecretarySpec::name).orElse("비서"));
        // 해설: 월간 기록에 비서 세입자 입주 사실을 남긴다. 비서 이름을 찾지 못하면 기본값으로 '비서'를 쓴다.
    }

    public void applyRequestEvent(Player player, String secretaryKey) {
    // 해설: 비서의 부탁을 수락했을 때 실행되는 처리다.
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(secretaryKey).orElseThrow();
        // 해설: 비서 키로 시나리오 정의를 찾는다. 이 시점에는 반드시 존재해야 하므로 없으면 예외를 던진다.
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElseThrow();
        // 해설: 현재 플레이어와 비서 키에 해당하는 진행 중 이벤트를 가져온다. 없으면 잘못된 요청이다.
        if (scenario.requestCost() > 0 && !player.spendCash(scenario.requestCost())) {
        // 해설: 부탁 비용이 필요한데 현금이 부족하면 수락 처리를 중단한다.
            return;
            // 해설: 현금 지불에 실패했으므로 상태 변경이나 기록 저장 없이 끝낸다.
        }
        event.acceptRequest(player.getElapsedDays(), scenario.durationDays());
        // 해설: 이벤트 상태를 부탁 수락으로 바꾸고, 현재 날짜와 소요 기간으로 완료 예정일을 계산한다.
        if (scenario.durationDays() == 0) {
        // 해설: 소요 기간이 0일이면 기다릴 필요가 없으므로 바로 고용 가능 상태로 넘긴다.
            event.makeHireAvailable();
            // 해설: 이벤트 상태를 고용 가능으로 바꾼다.
        }
        if (scenario.requestCost() > 0) {
        // 해설: 실제로 비용을 냈을 때만 지출 기록을 남긴다.
            saveRecord(player, RecordType.BUILDING_BUY, "비서 부탁", -scenario.requestCost(), 0, event.getBuilding().getName(), scenario.requestButton());
            // 해설: 부탁 비용을 음수 금액으로 월간 기록에 저장한다.
        }
    }

    public void applyHireEvent(Player player, String secretaryKey) {
    // 해설: 비서를 고용하기 버튼을 눌렀을 때 실행되는 처리다.
        SecretaryTenantEvent event = secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, secretaryKey).orElseThrow();
        // 해설: 현재 플레이어와 비서 키에 해당하는 진행 중 이벤트를 가져온다. 없으면 잘못된 요청이다.
        SecretarySpec spec = secretaryCatalog.find(secretaryKey).orElseThrow();
        // 해설: 고용할 비서의 이름과 기본 숙련도 정보를 카탈로그에서 가져온다.
        if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isEmpty()) {
        // 해설: 이미 고용한 비서가 아니라면 새 OwnedSecretary를 만든다. 중복 고용을 막는다.
            ownedSecretaryRepository.save(new OwnedSecretary(player, secretaryKey, spec.baseProficiency()));
            // 해설: 새 비서 소유 정보를 저장한다. 기본 숙련도는 비서 스펙에서 가져온다.
        }
        if ("secretary-6".equals(secretaryKey)) {
        // 해설: 6번 비서는 고용 후 서울 4번 슬롯 건물로 이동하는 특수 규칙이 있어 따로 처리한다.
            event.getBuilding().moveOut();
            // 해설: 6번 비서가 기존 건물에서 나간 뒤 서울 4번 슬롯으로 이동할 수 있게 먼저 퇴거 처리한다.
            ownedBuildingRepository.findByPlayerAndCityOrderById(player, "서울").stream()
            // 해설: 서울 보유 건물 중 비서가 이동할 대상 건물을 찾는다.
                    .filter(building -> buildingSlot(building) == 4)
                    // 해설: 서울 건물 중 4번 슬롯 건물만 후보로 남긴다.
                    .findFirst()
                    // 해설: 조건에 맞는 첫 번째 건물만 사용한다. Optional로 반환되므로 없을 수도 있다.
                    .ifPresent(building -> building.moveInSecretaryTenant(secretaryKey));
                    // 해설: 대상 건물이 있으면 해당 건물에 다시 비서 세입자 상태를 표시한다.
        } else {
            event.getBuilding().moveOut();
            // 해설: 일반 비서는 고용되면 세입자 역할이 끝나므로 기존 건물에서 퇴거 처리한다.
        }
        event.complete();
        // 해설: 비서 세입자 이벤트를 완료 상태로 바꾼다. 이후 진행 중 이벤트 조회에서 제외된다.
        saveRecord(player, RecordType.BUILDING_BUY, "비서 고용", null, 0, event.getBuilding().getName(), spec.name());
        // 해설: 월간 기록에 비서 고용 사실을 남긴다.
    }

    public void createTenantEvent(Player player, OwnedBuilding building, SecretaryTenantScenario scenario) {
    // 해설: 비서 세입자 이벤트 엔티티를 처음 생성하는 공통 메서드다.
        if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.secretaryKey()).isPresent()) {
            return;
            // 해설: 이미 같은 비서 이벤트가 있으므로 중복 생성 없이 끝낸다.
        }
        secretaryTenantEventRepository.save(new SecretaryTenantEvent(player, building, scenario.secretaryKey(), scenario.city(), player.getElapsedDays()));
        // 해설: 새 이벤트에 플레이어, 건물, 비서 키, 도시, 생성 시점 날짜를 저장한다.
    }

    public void activateNextEvent(Player player) {
    // 해설: 진행 중인 비서 이벤트 중 지금 화면에 띄울 수 있는 다음 이벤트를 찾는다.
        if (activeEvent(player).isPresent()) {
        // 해설: 이미 활성 이벤트가 있으면 다음 비서 이벤트를 새로 띄우지 않는다.
            return;
            // 해설: 한 번에 하나의 이벤트만 처리하기 위해 여기서 끝낸다.
        }
        for (SecretaryTenantEvent event : secretaryTenantEventRepository.findByPlayerAndStatusNot(player, SecretaryTenantEventStatus.COMPLETED)) {
        // 해설: 완료되지 않은 비서 이벤트를 하나씩 확인한다.
            SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(event.getSecretaryKey()).orElseThrow();
            if (event.getStatus() == SecretaryTenantEventStatus.TENANT && requestConditionMet(player, scenario)) {
            // 해설: 아직 세입자 상태이고 부탁 조건을 만족하면 부탁 가능 상태로 올린다.
                event.makeRequestAvailable();
                // 해설: 상태를 REQUEST_AVAILABLE로 바꾼다. 다음 분기에서 실제 부탁 이벤트를 화면에 띄울 수 있다.
            }
            if (event.getStatus() == SecretaryTenantEventStatus.REQUEST_AVAILABLE) {
            // 해설: 부탁 가능 상태라면 부탁 GameEvent를 생성한다.
                activateSecretaryEvent(
                // 해설: 화면에 표시될 GameEvent를 생성한다. 아래 인자들이 이벤트 키, 제목, 본문, 효과 키, 버튼 문구다.
                        player,
                        "secretary_request_" + scenario.secretaryKey(),
                        scenario.requestTitle(),
                        scenario.requestBody(),
                        REQUEST_EFFECT_PREFIX + scenario.secretaryKey(),
                        scenario.requestButton()
                );
                return;
                // 해설: 부탁 이벤트 하나를 띄웠으므로 나머지 이벤트 탐색을 멈춘다.
            }
            if (event.getStatus() == SecretaryTenantEventStatus.HIRE_AVAILABLE && hireConditionMet(player, scenario)) {
            // 해설: 고용 가능 상태이고 추가 고용 조건도 만족하면 고용 GameEvent를 생성한다.
                activateSecretaryEvent(
                // 해설: 화면에 표시될 GameEvent를 생성한다. 아래 인자들이 이벤트 키, 제목, 본문, 효과 키, 버튼 문구다.
                        player,
                        "secretary_hire_" + scenario.secretaryKey(),
                        secretaryCatalog.find(scenario.secretaryKey()).map(SecretarySpec::name).orElse("비서") + " 고용 가능",
                        "부탁을 해결했다. 이제 비서로 고용할 수 있다.",
                        HIRE_EFFECT_PREFIX + scenario.secretaryKey(),
                        "고용하기"
                );
                return;
                // 해설: 고용 이벤트 하나를 띄웠으므로 나머지 이벤트 탐색을 멈춘다.
            }
        }
    }

    public String statusText(OwnedBuilding building) {
    // 해설: 건물 카드에 표시할 비서 세입자 상태 문구를 만든다.
        Optional<SecretaryTenantEvent> event = secretaryTenantEventRepository.findByBuildingAndStatusNot(building, SecretaryTenantEventStatus.COMPLETED);
        // 해설: 해당 건물에 완료되지 않은 비서 세입자 이벤트가 있는지 찾는다.
        if (event.isEmpty() && building.isSecretaryResident()) {
        // 해설: 이벤트 엔티티는 없지만 건물에 비서 거주 표시가 남아 있으면 기본 거주 문구를 보여준다.
            return "비서 거주중";
            // 해설: 특별한 진행 문구가 없으면 기본 거주 상태를 반환한다.
        }
        if (event.isEmpty()) {
        // 해설: 이벤트도 없고 비서 거주 표시도 없으면 빈 문자열을 반환한다.
            return "";
        }
        SecretaryTenantScenario scenario = SecretaryTenantScenarioCatalog.find(event.get().getSecretaryKey()).orElse(null);
        // 해설: 이벤트의 비서 키로 시나리오를 찾는다. 상태 문구를 만들 때 진행 메모가 필요하다.
        if (event.get().getStatus() == SecretaryTenantEventStatus.REQUEST_ACCEPTED && scenario != null && scenario.progressMemo() != null) {
        // 해설: 부탁 진행 중이고 시나리오에 진행 메모가 있으면 D-day와 함께 표시한다.
            return scenario.progressMemo() + progressDday(event.get());
            // 해설: 진행 메모 뒤에 남은 날짜를 붙여 UI 문구로 반환한다.
        }
        return "비서 거주중";
        // 해설: 특별한 진행 문구가 없으면 기본 거주 상태를 반환한다.
    }

    public boolean isRentWaived(Player player, OwnedBuilding building) {
    // 해설: 특정 건물의 임대료가 면제되는지 계산한다.
        Optional<SecretaryTenantEvent> event = secretaryTenantEventRepository.findByBuildingAndStatusNot(building, SecretaryTenantEventStatus.COMPLETED);
        // 해설: 해당 건물에 완료되지 않은 비서 세입자 이벤트가 있는지 찾는다.
        if (event.isEmpty()) {
        // 해설: 이벤트도 없고 비서 거주 표시도 없으면 빈 문자열을 반환한다.
            return false;
        }
        if ("secretary-5".equals(event.get().getSecretaryKey()) && event.get().getStatus() != SecretaryTenantEventStatus.COMPLETED) {
        // 해설: 5번 비서는 이벤트가 완료되기 전까지 임대료 면제 효과를 준다.
            return true;
        }
        return "secretary-1".equals(event.get().getSecretaryKey())
        // 해설: 1번 비서는 부탁 수락 후 정해진 기간 안에서만 임대료 면제를 준다.
                && event.get().getStatus() == SecretaryTenantEventStatus.REQUEST_ACCEPTED
                && player.getElapsedDays() < event.get().getDueDayCount();
                // 해설: 현재 날짜가 마감일 전이어야 면제 효과가 유지된다.
    }

    private void activateMissingIntro(Player player) {
    // 해설: 플레이어의 모든 보유 건물을 검사해서 아직 뜨지 않은 비서 인트로를 찾는다.
        for (OwnedBuilding building : ownedBuildingRepository.findByPlayerOrderById(player)) {
        // 해설: 보유 건물을 id 순서로 하나씩 순회한다.
            Optional<SecretaryTenantScenario> scenario = SecretaryTenantScenarioCatalog.findByCityAndSlot(building.getCity(), buildingSlot(building));
            // 해설: 건물의 도시와 슬롯 번호로 어떤 비서 시나리오가 연결되는지 카탈로그에서 찾는다.
            if (scenario.isEmpty() || "secretary-1".equals(scenario.get().secretaryKey())) {
            // 해설: 해당 건물에 연결된 시나리오가 없거나 첫 비서라면 여기서 인트로 이벤트를 만들지 않는다. 첫 비서는 별도 초기 입주 로직을 탄다.
                continue;
                // 해설: 현재 반복 대상은 처리하지 않고 다음 건물 또는 이벤트로 넘어간다.
            }
            if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(player, scenario.get().secretaryKey()).isPresent()) {
            // 해설: 같은 비서 이벤트가 이미 만들어졌으면 중복 생성하지 않는다.
                continue;
                // 해설: 현재 반복 대상은 처리하지 않고 다음 건물 또는 이벤트로 넘어간다.
            }
            tryActivateIntro(player, building);
            // 해설: 현재 건물로 인트로 이벤트를 만들 수 있는지 시도한다.
            if (activeEvent(player).isPresent()) {
            // 해설: 현재 건물에서 인트로 이벤트가 생성됐는지 확인한다.
                return;
                // 해설: 인트로 이벤트 하나를 만들었으므로 더 이상 다른 건물을 검사하지 않는다.
            }
        }
    }

    private void updateProgress(Player player) {
    // 해설: 수락된 부탁 이벤트들의 완료 시점을 확인한다.
        for (SecretaryTenantEvent event : secretaryTenantEventRepository.findByPlayerAndStatus(player, SecretaryTenantEventStatus.REQUEST_ACCEPTED)) {
        // 해설: 부탁 수락 상태인 이벤트만 가져와 순회한다.
            if (event.getDueDayCount() > 0 && player.getElapsedDays() >= event.getDueDayCount()) {
            // 해설: 마감일이 설정되어 있고 현재 날짜가 마감일 이상이면 부탁 기간이 끝난 것이다.
                event.makeHireAvailable();
                // 해설: 이벤트 상태를 고용 가능으로 바꾼다.
            }
        }
    }

    private void activateSecretaryEvent(Player player, String eventKey, String title, String body, String effectKey, String actionLabel) {
    // 해설: 비서 관련 GameEvent를 실제로 저장하는 공통 메서드다.
        if (gameEventRepository.existsByPlayerIdAndEventKey(player.getId(), eventKey)) {
        // 해설: 같은 이벤트 키가 이미 저장되어 있으면 중복 생성하지 않는다.
            return;
            // 해설: 이미 생성된 이벤트라면 저장하지 않고 끝낸다.
        }
        gameEventRepository.save(new GameEvent(player, eventKey, title, body, secretaryEventImage(eventKey), effectKey, actionLabel));
        // 해설: GameEvent를 저장한다. 이미지 경로는 eventKey 규칙으로 계산한다.
        player.pause();
        // 해설: 이벤트가 표시되면 플레이어 시간을 멈춘다. 사용자가 이벤트를 처리하기 전까지 자동 진행을 막는다.
    }

    private String secretaryEventImage(String eventKey) {
    // 해설: 이벤트 키를 보고 사용할 이미지 경로를 만든다.
        if (eventKey.startsWith("secretary_intro_")) {
        // 해설: 인트로 이벤트 키면 intro 이미지 파일을 사용한다.
            return "/assets/events/" + eventKey.replace("secretary_intro_", "") + "-intro.png";
            // 해설: 이벤트 키에서 비서 키만 남기고 intro 이미지 파일명으로 조립한다.
        }
        if (eventKey.startsWith("secretary_request_")) {
        // 해설: 부탁 이벤트 키면 request 이미지 파일을 사용한다.
            return "/assets/events/" + eventKey.replace("secretary_request_", "") + "-request.png";
            // 해설: 이벤트 키에서 비서 키만 남기고 request 이미지 파일명으로 조립한다.
        }
        if (eventKey.startsWith("secretary_hire_")) {
        // 해설: 고용 이벤트 키면 hire 이미지 파일을 사용한다.
            return "/assets/events/" + eventKey.replace("secretary_hire_", "") + "-hire.png";
            // 해설: 이벤트 키에서 비서 키만 남기고 hire 이미지 파일명으로 조립한다.
        }
        return IMAGE_PLACEHOLDER;
        // 해설: 규칙에 맞지 않는 이벤트 키는 대체 이미지 키를 반환한다.
    }

    private boolean requestConditionMet(Player player, SecretaryTenantScenario scenario) {
    // 해설: 비서 부탁 이벤트가 열릴 조건을 모두 검사한다.
        if (player.getCash() < scenario.requiredCash()) {
        // 해설: 요구 현금보다 보유 현금이 적으면 조건 실패다.
            return false;
        }
        if (player.getReputation() < scenario.requiredReputation()) {
        // 해설: 요구 평판보다 현재 평판이 낮으면 조건 실패다.
            return false;
        }
        if (scenario.requiredLuxuryKey() != null && ownedLuxuryItemRepository.findByPlayerAndItemKey(player, scenario.requiredLuxuryKey()).isEmpty()) {
        // 해설: 특정 명품 아이템이 필요한데 보유하지 않았으면 조건 실패다.
            return false;
        }
        if (scenario.requiresNoLoan() && remainingLoanPrincipal(player) > 0) {
        // 해설: 대출이 없어야 하는 시나리오인데 남은 원금이 있으면 조건 실패다.
            return false;
        }
        return !scenario.requiresAllLuxuryItems() || hasAllLuxuryItems(player);
        // 해설: 모든 명품을 요구하지 않으면 통과한다. 요구한다면 실제로 전부 보유했는지 확인한다.
    }

    private boolean hireConditionMet(Player player, SecretaryTenantScenario scenario) {
    // 해설: 고용 이벤트가 열릴 추가 조건을 검사한다.
        if (!"secretary-6".equals(scenario.secretaryKey())) {
        // 해설: 6번 비서가 아니면 별도 조건 없이 고용 가능하다.
            return true;
        }
        return ownedBuildingRepository.findByPlayerAndCityOrderById(player, "서울").stream()
        // 해설: 6번 비서는 서울 건물 조건을 만족해야 하므로 서울 보유 건물을 조회한다.
                .anyMatch(building -> buildingSlot(building) == 4);
                // 해설: 서울 4번 슬롯 건물이 하나라도 있으면 고용 조건을 만족한다.
    }

    private boolean hasAllLuxuryItems(Player player) {
    // 해설: 플레이어가 모든 명품 아이템을 보유했는지 검사한다.
        return luxuryItemCatalog.all().stream()
        // 해설: 카탈로그에 등록된 전체 명품 목록을 순회한다.
                .allMatch(item -> ownedLuxuryItemRepository.findByPlayerAndItemKey(player, item.key()).isPresent());
                // 해설: 각 명품 키마다 보유 기록이 있어야 true가 된다.
    }

    private long remainingLoanPrincipal(Player player) {
    // 해설: 플레이어의 남은 대출 원금 총합을 계산한다.
        return loanRepository.findByPlayer(player).stream()
        // 해설: 플레이어의 모든 대출을 가져와 합산 준비를 한다.
                .mapToLong(Loan::getPrincipal)
                // 해설: 각 Loan 객체에서 principal 값만 long으로 꺼낸다.
                .sum();
                // 해설: 꺼낸 원금 값들을 모두 더한다.
    }

    private Optional<GameEvent> activeEvent(Player player) {
    // 해설: 현재 플레이어에게 활성 이벤트가 있는지 조회한다.
        return gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE);
        // 해설: ACTIVE 상태 이벤트 하나를 Optional로 반환한다. 없으면 빈 Optional이다.
    }

    private int buildingSlot(OwnedBuilding building) {
    // 해설: 건물의 슬롯 번호를 얻는다.
        return building.getBuildingSlot() == null ? catalogSlot(building.getCity(), building.getTypeName(), building.getName()) : building.getBuildingSlot();
        // 해설: 저장된 슬롯이 있으면 그대로 쓰고, 없으면 카탈로그에서 도시/타입/이름으로 찾아 보정한다.
    }

    private int catalogSlot(String city, String typeName, String name) {
    // 해설: 카탈로그에서 건물 슬롯 번호를 찾는다.
        return buildingCatalog.all().stream()
        // 해설: 전체 건물 스펙을 Stream으로 순회한다.
                .filter(spec -> spec.city().equals(city))
                // 해설: 도시가 같은 건물 스펙만 남긴다.
                .filter(spec -> spec.name().equals(name) || spec.typeName().equals(typeName))
                // 해설: 이름이 같거나 타입명이 같은 스펙만 남긴다.
                .map(BuildingSpec::slot)
                // 해설: 찾은 건물 스펙에서 슬롯 번호만 꺼낸다.
                .findFirst()
                // 해설: 조건에 맞는 첫 번째 건물만 사용한다. Optional로 반환되므로 없을 수도 있다.
                .orElse(1);
                // 해설: 카탈로그에서도 못 찾으면 기본 슬롯 1을 사용한다.
    }

    private String progressDday(SecretaryTenantEvent event) {
    // 해설: 부탁 진행 상태에 붙일 D-day 문자열을 만든다.
        if (event.getDueDayCount() <= 0) {
        // 해설: 마감일이 없으면 D-day를 표시하지 않는다.
            return "";
        }
        return " D-" + Math.max(0, event.getDueDayCount() - event.getPlayer().getElapsedDays());
        // 해설: 남은 날짜를 계산한다. 이미 지났더라도 음수가 나오지 않게 0으로 보정한다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 월간 기록을 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 MonthlyRecord를 만들어 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 최소 기준일은 1일로 보정한다.
    }
}
```
