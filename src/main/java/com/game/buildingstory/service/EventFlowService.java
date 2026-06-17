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
        return activeEvent(player.getId());
    }

    @Transactional(readOnly = true)
    public Optional<GameEvent> activeEvent(long playerId) {
        return gameEventRepository.findLatestByPlayerIdAndStatus(playerId, GameEventStatus.ACTIVE, PageRequest.of(0, 1)).stream().findFirst();
    }

    public String activateEvent(Player player, GameEventDefinition definition) {
        GameEvent event = gameEventRepository.save(new GameEvent(player, definition));
        player.pause();
        return "EVENT:" + event.getId();
    }

    public void completeEvent(long playerId, long eventId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        GameEvent event = gameEventRepository.findById(eventId).orElseThrow();
        if (!event.getPlayer().getId().equals(player.getId()) || event.getStatus() != GameEventStatus.ACTIVE) {
            throw new IllegalArgumentException("잘못된 이벤트");
        }
        applyEventEffect(player, event);
        event.complete();
        if (activeEvent(player).isEmpty()) {
            player.resume();
        }
    }

    public void cancelEvent(long playerId, long eventId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        GameEvent event = gameEventRepository.findById(eventId).orElseThrow();
        if (!event.getPlayer().getId().equals(player.getId()) || event.getStatus() != GameEventStatus.ACTIVE) {
            throw new IllegalArgumentException("잘못된 이벤트");
        }
        if (!RESIGN_CONFIRM_EFFECT.equals(event.getEffectKey())) {
            throw new IllegalArgumentException("취소할 수 없는 이벤트");
        }
        event.complete();
        player.resume();
    }

    public String resign(long playerId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return "일시정지 중에는 경제 행동을 할 수 없음";
        }
        if (!player.isEmployed()) {
            return "이미 퇴사 상태";
        }
        if (!player.canResign()) {
            return "가입 후 한 달간 퇴사 불가 · D-" + player.daysUntilResignAvailable();
        }
        gameEventRepository.save(new GameEvent(
                player,
                "resign_confirm_" + player.getId() + "_" + player.getElapsedDays(),
                "퇴사 고민",
                "정말 회사를 그만두시겠습니까?",
                RESIGN_THINK_IMAGE,
                RESIGN_CONFIRM_EFFECT,
                "그래! 퇴사하자"
        ));
        player.pause();
        return "퇴사 여부를 선택하세요.";
    }

    public String processAutoResignation(Player player) {
        if (!player.isEmployed() || player.getElapsedDays() < AUTO_RESIGN_DAY) {
            return "";
        }
        long severance = completeResignation(player, "자동 퇴사");
        return "6개월 경과로 자동 퇴사 처리 · 퇴직금 " + String.format("%,d", severance) + "원 지급";
    }

    private void applyEventEffect(Player player, GameEvent event) {
        if (GameEventCatalog.EFFECT_FIRST_TENANT_MOVE_IN.equals(event.getEffectKey())) {
            secretaryTenantEventService.applyFirstTenantMoveIn(player);
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.INTRO_EFFECT_PREFIX)) {
            secretaryTenantEventService.applyIntroEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.INTRO_EFFECT_PREFIX.length()));
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.REQUEST_EFFECT_PREFIX)) {
            secretaryTenantEventService.applyRequestEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.REQUEST_EFFECT_PREFIX.length()));
            return;
        }
        if (event.getEffectKey().startsWith(SecretaryTenantEventService.HIRE_EFFECT_PREFIX)) {
            secretaryTenantEventService.applyHireEvent(player, event.getEffectKey().substring(SecretaryTenantEventService.HIRE_EFFECT_PREFIX.length()));
            return;
        }
        if (RESIGN_CONFIRM_EFFECT.equals(event.getEffectKey())) {
            activateResignPayoutEvent(player);
            return;
        }
        if (RESIGN_PAYOUT_EFFECT.equals(event.getEffectKey())) {
            completeResignation(player, "퇴직금 수령");
        }
    }

    private void activateResignPayoutEvent(Player player) {
        long severance = severanceAmount(player);
        gameEventRepository.save(new GameEvent(
                player,
                "resign_payout_" + player.getId() + "_" + player.getElapsedDays(),
                "퇴사 완료",
                "퇴직금 " + String.format("%,d", severance) + "원을 수령할 수 있습니다.",
                RESIGN_SUBMIT_IMAGE,
                RESIGN_PAYOUT_EFFECT,
                "퇴직금 수령하기"
        ));
        player.pause();
    }

    private long completeResignation(Player player, String memo) {
        if (!player.isEmployed()) {
            return 0L;
        }
        long severance = severanceAmount(player);
        player.leaveJob();
        player.addCash(severance);
        refreshTitle(player);
        saveRecord(player, RecordType.SALARY_INCOME, "퇴직금", severance, 0, null, memo);
        return severance;
    }

    private long severanceAmount(Player player) {
        return Math.max(0, player.getElapsedDays()) * SEVERANCE_PER_DAY;
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }

    private void refreshTitle(Player player) {
        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
    }
}
