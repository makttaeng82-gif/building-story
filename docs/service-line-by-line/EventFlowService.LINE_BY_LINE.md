# EventFlowService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/EventFlowService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventDefinition;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class EventFlowService {
// 해설: 게임 이벤트 모달을 만들고, 사용자가 이벤트 버튼을 눌렀을 때 후속 효과를 적용하는 서비스다.
    /*
     * 게임 이벤트 모달의 생성과 완료 처리를 담당한다.
     *
     * 이벤트는 GameEvent 엔티티로 저장되고, 사용자가 버튼을 누르면 effect 문자열에 따라
     * 현금 지급, 퇴사, 비서 고용 가능 상태 변경 같은 후속 처리가 실행된다.
     */
    private static final int AUTO_RESIGN_DAY = 181;
    private static final int RECORD_RETENTION_DAYS = 62;
    private static final long SEVERANCE_PER_DAY = 400_000L;
    private static final String RESIGN_CONFIRM_EFFECT = "RESIGN_CONFIRM";
    private static final String RESIGN_PAYOUT_EFFECT = "RESIGN_PAYOUT";
    private static final String RESIGN_THINK_IMAGE = "/assets/events/resign-think.jpg";
    private static final String RESIGN_SUBMIT_IMAGE = "/assets/events/resign-submit.jpg";

    private final PlayerRepository playerRepository;
    private final GameEventRepository gameEventRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final ReputationCatalog reputationCatalog;
    private final SecretaryTenantEventService secretaryTenantEventService;

    public EventFlowService(
    // 해설: 이벤트 처리에 필요한 플레이어, 이벤트, 기록 Repository와 평판/비서 이벤트 서비스를 생성자 주입으로 받는다.
            PlayerRepository playerRepository,
            GameEventRepository gameEventRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            ReputationCatalog reputationCatalog,
            SecretaryTenantEventService secretaryTenantEventService
    ) {
        this.playerRepository = playerRepository;
        this.gameEventRepository = gameEventRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.reputationCatalog = reputationCatalog;
        this.secretaryTenantEventService = secretaryTenantEventService;
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(Player player) {
    // 해설: Player 객체를 받은 경우 현재 활성 이벤트 조회를 playerId 기반 메서드로 위임한다.
        return activeEvent(player.getId());
        // 해설: 실제 조회는 id만 받는 activeEvent 메서드에서 처리한다.
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(long playerId) {
    // 해설: 플레이어 id로 현재 활성 이벤트가 있는지 조회한다.
        return gameEventRepository.findLatestByPlayerIdAndStatus(playerId, GameEventStatus.ACTIVE, PageRequest.of(0, 1)).stream().findFirst();
        // 해설: ACTIVE 상태 이벤트를 최신순으로 1개만 가져와 Optional로 반환한다.
    }

    public String activateEvent(Player player, GameEventDefinition definition) {
    // 해설: 이벤트 정의를 실제 GameEvent로 활성화하는 기본 진입점이다.
        return activateEvent(player, definition, true);
        // 해설: 기본 동작은 이벤트를 만들면서 플레이어 시간을 일시정지하는 것이다.
    }

    public String activateEvent(Player player, GameEventDefinition definition, boolean pausePlayer) {
    // 해설: 이벤트 활성화 시 플레이어를 멈출지 선택할 수 있는 실제 구현 메서드다.
        GameEvent event = gameEventRepository.save(new GameEvent(player, definition));
        // 해설: 카탈로그 정의를 기반으로 새 GameEvent를 저장한다.
        if (pausePlayer) {
        // 해설: 호출자가 일시정지를 요청한 경우에만 플레이어 시간을 멈춘다.
            player.pause();
            // 해설: 이벤트 모달을 먼저 처리하게 하려고 시간 진행을 멈춘다.
        }
        return "EVENT:" + event.getId();
        // 해설: 프론트가 이벤트 모달을 열 수 있도록 EVENT 응답과 이벤트 id를 반환한다.
    }

    public void completeEvent(long playerId, long eventId) {
    // 해설: 사용자가 이벤트의 확인/선택 버튼을 눌렀을 때 호출되는 완료 처리다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        GameEvent event = gameEventRepository.findById(eventId).orElseThrow();
        // 해설: 완료하려는 이벤트를 DB에서 조회한다.
        if (!event.getPlayer().getId().equals(player.getId()) || event.getStatus() != GameEventStatus.ACTIVE) {
        // 해설: 다른 플레이어 이벤트이거나 이미 완료된 이벤트면 처리하지 않는다.
            throw new IllegalArgumentException("잘못된 이벤트");
            // 해설: 이벤트 검증 실패를 예외로 중단한다.
        }
        applyEventEffect(player, event);
        // 해설: 이벤트 effectKey에 맞는 실제 게임 상태 변경을 먼저 적용한다.
        event.complete();
        // 해설: 효과 적용이 끝난 이벤트를 완료 상태로 바꾼다.
        if (activeEvent(player).isEmpty()) {
        // 해설: 남아 있는 활성 이벤트가 없을 때만 시간 진행을 재개한다.
            player.resume();
            // 해설: 이벤트 처리가 모두 끝났으므로 플레이어 시간을 다시 진행 상태로 바꾼다.
        }
    }

    public void cancelEvent(long playerId, long eventId) {
    // 해설: 취소 가능한 이벤트를 사용자가 취소했을 때 호출된다. 현재는 퇴사 확인 이벤트만 취소 가능하다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        GameEvent event = gameEventRepository.findById(eventId).orElseThrow();
        // 해설: 완료하려는 이벤트를 DB에서 조회한다.
        if (!event.getPlayer().getId().equals(player.getId()) || event.getStatus() != GameEventStatus.ACTIVE) {
        // 해설: 다른 플레이어 이벤트이거나 이미 완료된 이벤트면 처리하지 않는다.
            throw new IllegalArgumentException("잘못된 이벤트");
            // 해설: 이벤트 검증 실패를 예외로 중단한다.
        }
        if (!RESIGN_CONFIRM_EFFECT.equals(event.getEffectKey())) {
        // 해설: 퇴사 확인 이벤트가 아니면 취소를 허용하지 않는다.
            throw new IllegalArgumentException("취소할 수 없는 이벤트");
            // 해설: 취소 불가능한 이벤트를 취소하려 하면 예외로 중단한다.
        }
        event.complete();
        // 해설: 취소도 이벤트 흐름상 처리가 끝난 것이므로 완료 상태로 바꾼다.
        player.resume();
        // 해설: 이벤트 처리가 모두 끝났으므로 플레이어 시간을 다시 진행 상태로 바꾼다.
    }

    public String resign(long playerId) {
    // 해설: 사용자가 퇴사 버튼을 눌렀을 때 퇴사 확인 이벤트를 생성한다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 이미 다른 이벤트 처리 중이면 퇴사 요청을 받지 않는다.
            return "일시정지 중에는 경제 행동을 할 수 없음";
            // 해설: 일시정지 중 행동 제한 메시지를 반환한다.
        }
        if (!player.isEmployed()) {
        // 해설: 이미 퇴사한 플레이어는 다시 퇴사할 수 없다.
            return "이미 퇴사 상태";
            // 해설: 이미 퇴사한 상태라는 메시지를 반환한다.
        }
        if (!player.canResign()) {
        // 해설: 가입 후 일정 기간이 지나지 않았으면 퇴사할 수 없다.
            return "가입 후 한 달간 퇴사 불가 · D-" + player.daysUntilResignAvailable();
            // 해설: 퇴사 가능일까지 남은 날짜를 안내한다.
        }
        gameEventRepository.save(new GameEvent(
        // 해설: 퇴사 여부를 묻는 확인 이벤트를 저장한다.
                player,
                "resign_confirm_" + player.getId() + "_" + player.getElapsedDays(),
                // 해설: 플레이어와 현재 날짜를 포함해 퇴사 확인 이벤트 키를 만든다.
                "퇴사 고민",
                "정말 회사를 그만두시겠습니까?",
                RESIGN_THINK_IMAGE,
                RESIGN_CONFIRM_EFFECT,
                // 해설: 이 이벤트를 완료하면 바로 퇴사하지 않고 퇴직금 수령 이벤트로 넘어가게 하는 effectKey다.
                "그래! 퇴사하자"
        ));
        player.pause();
        // 해설: 이벤트 모달을 먼저 처리하게 하려고 시간 진행을 멈춘다.
        return "퇴사 여부를 선택하세요.";
        // 해설: 퇴사 확인 이벤트가 생성됐음을 사용자에게 알린다.
    }

    public String processAutoResignation(Player player) {
    // 해설: 일정 기간이 지나면 자동 퇴사를 처리한다.
        if (!player.isEmployed() || player.getElapsedDays() < AUTO_RESIGN_DAY) {
        // 해설: 이미 퇴사했거나 자동 퇴사 날짜 전이면 아무 일도 하지 않는다.
            return "";
            // 해설: 발생한 안내가 없다는 뜻으로 빈 문자열을 반환한다.
        }
        long severance = completeResignation(player, "자동 퇴사");
        // 해설: 자동 퇴사 사유로 퇴사 처리하고 지급된 퇴직금을 받는다.
        return "6개월 경과로 자동 퇴사 처리 · 퇴직금 " + String.format("%,d", severance) + "원 지급";
        // 해설: 자동 퇴사와 퇴직금 지급 안내 문구를 반환한다.
    }

    private void applyEventEffect(Player player, GameEvent event) {
    // 해설: event.effectKey를 해석해 이벤트별 실제 효과를 적용한다.
        if (GameEventCatalog.EFFECT_FIRST_TENANT_MOVE_IN.equals(event.getEffectKey())) {
        // 해설: 첫 세입자 이벤트라면 첫 비서 세입자 입주 로직을 실행한다.
            secretaryTenantEventService.applyFirstTenantMoveIn(player);
            // 해설: 첫 비서 세입자를 실제 건물에 입주시키는 처리를 위임한다.
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.INTRO_EFFECT_PREFIX)) {
        // 해설: 비서 인트로 이벤트 effectKey인지 접두사로 판별한다.
            secretaryTenantEventService.applyIntroEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.INTRO_EFFECT_PREFIX.length()));
            // 해설: effectKey에서 비서 키만 잘라내 실제 입주 처리에 넘긴다.
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.REQUEST_EFFECT_PREFIX)) {
        // 해설: 비서 부탁 수락 이벤트 effectKey인지 판별한다.
            secretaryTenantEventService.applyRequestEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.REQUEST_EFFECT_PREFIX.length()));
            // 해설: 비서 키를 추출해 부탁 수락 처리를 실행한다.
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.HIRE_EFFECT_PREFIX)) {
        // 해설: 비서 고용 이벤트 effectKey인지 판별한다.
            secretaryTenantEventService.applyHireEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.HIRE_EFFECT_PREFIX.length()));
            // 해설: 비서 키를 추출해 고용 처리를 실행한다.
            return;
        }
        if (RESIGN_CONFIRM_EFFECT.equals(event.getEffectKey())) {
        // 해설: 퇴사 확인 이벤트를 완료한 경우에는 퇴직금 수령 이벤트를 새로 띄운다.
            activateResignPayoutEvent(player);
            // 해설: 퇴직금 수령 이벤트를 생성한다.
            return;
        }
        if (RESIGN_PAYOUT_EFFECT.equals(event.getEffectKey())) {
        // 해설: 퇴직금 수령 이벤트를 완료한 경우 실제 퇴사 처리를 한다.
            completeResignation(player, "퇴직금 수령");
            // 해설: 퇴사 상태 변경, 퇴직금 지급, 기록 저장을 실행한다.
        }
    }

    private void activateResignPayoutEvent(Player player) {
    // 해설: 퇴사 확인 후 실제 퇴직금을 받을 수 있는 두 번째 이벤트를 만든다.
        long severance = severanceAmount(player);
        // 해설: 퇴직금 금액을 계산한다.
        gameEventRepository.save(new GameEvent(
        // 해설: 퇴직금을 수령하는 두 번째 퇴사 이벤트를 저장한다.
                player,
                "resign_payout_" + player.getId() + "_" + player.getElapsedDays(),
                // 해설: 플레이어와 현재 날짜를 포함해 퇴직금 이벤트 키를 만든다.
                "퇴사 완료",
                "퇴직금 " + String.format("%,d", severance) + "원을 수령할 수 있습니다.",
                // 해설: 계산된 퇴직금을 천 단위 구분자로 표시해 이벤트 본문을 만든다.
                RESIGN_SUBMIT_IMAGE,
                RESIGN_PAYOUT_EFFECT,
                // 해설: 이 이벤트를 완료하면 completeResignation이 실행되게 하는 effectKey다.
                "퇴직금 수령하기"
        ));
        player.pause();
        // 해설: 이벤트 모달을 먼저 처리하게 하려고 시간 진행을 멈춘다.
    }

    private long completeResignation(Player player, String memo) {
    // 해설: 퇴사 상태 변경과 퇴직금 지급을 실제로 수행한다.
        if (!player.isEmployed()) {
        // 해설: 이미 퇴사 상태라면 중복 지급을 막는다.
            return 0L;
            // 해설: 중복 퇴사 처리에서는 지급액 0원을 반환한다.
        }
        long severance = severanceAmount(player);
        // 해설: 퇴직금 금액을 계산한다.
        player.leaveJob();
        // 해설: 플레이어를 퇴사 상태로 변경한다.
        player.addCash(severance);
        // 해설: 퇴직금을 현금에 더한다.
        refreshTitle(player);
        // 해설: 퇴사 여부가 칭호 조건에 영향을 주므로 칭호를 갱신한다.
        saveRecord(player, RecordType.SALARY_INCOME, "퇴직금", severance, 0, null, memo);
        // 해설: 퇴직금 지급 기록을 월간 기록에 저장한다.
        return severance;
        // 해설: 실제 지급된 퇴직금을 호출자에게 반환한다.
    }

    private long severanceAmount(Player player) {
    // 해설: 퇴직금 계산식을 한 곳에 모아둔다.
        return Math.max(0, player.getElapsedDays()) * SEVERANCE_PER_DAY;
        // 해설: 경과일이 음수가 되지 않게 보정한 뒤 일당 퇴직금 단가를 곱한다.
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 이벤트 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        // 해설: 새 월간 기록 엔티티를 저장한다.
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
        // 해설: 보관 기간보다 오래된 기록을 삭제한다. 기준일은 최소 1일로 보정한다.
    }

    private void refreshTitle(Player player) {
    // 해설: 플레이어 칭호를 현재 평판과 퇴사 상태에 맞게 갱신한다.
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        // 해설: 평판과 퇴사 여부로 현재 티어를 찾고 해당 칭호를 저장한다.
    }
}
```
