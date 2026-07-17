package com.game.buildingstory.service.time;

import com.game.buildingstory.domain.GameEventDefinition;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.service.AuctionService;
import com.game.buildingstory.service.EventFlowService;
import com.game.buildingstory.service.GameEventCatalog;
import com.game.buildingstory.service.SecretaryTenantEventService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 도시 고정 이벤트, 비서 임차인 이벤트, 신규 경매를 순서대로 검사한다. */
@Component
public class DailyCityEventProcessor implements DailyGameProcessor {
    private static final int ORDER = 400;
    private final GameEventCatalog gameEventCatalog;
    private final GameEventRepository gameEventRepository;
    private final EventFlowService eventFlowService;
    private final SecretaryTenantEventService secretaryTenantEventService;
    private final AuctionService auctionService;

    public DailyCityEventProcessor(GameEventCatalog gameEventCatalog, GameEventRepository gameEventRepository,
                                   EventFlowService eventFlowService,
                                   SecretaryTenantEventService secretaryTenantEventService,
                                   AuctionService auctionService) {
        this.gameEventCatalog = gameEventCatalog;
        this.gameEventRepository = gameEventRepository;
        this.eventFlowService = eventFlowService;
        this.secretaryTenantEventService = secretaryTenantEventService;
        this.auctionService = auctionService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        if (context.deferCityEvents()) {
            // 주식 화면에서는 이벤트를 저장만 하고 플레이어를 일시 정지시키지 않는다.
            activateDueEvent(context, false);
            return DailyProcessResult.continueWithoutNotice();
        }

        Optional<Long> activeEventId = activeEventId(context);
        if (activeEventId.isPresent()) {
            return eventSignal(activeEventId.get());
        }

        secretaryTenantEventService.evaluate(context.player(), auctionService.activeAuction(context.player()).isPresent());
        activeEventId = activeEventId(context);
        if (activeEventId.isPresent()) {
            return eventSignal(activeEventId.get());
        }

        if (activateDueEvent(context, true)) {
            return eventSignal(activeEventId(context).orElseThrow());
        }

        return auctionService.tryActivate(context.player())
                .map(auction -> DailyProcessResult.stop("AUCTION:" + auction.getId()))
                .orElseGet(DailyProcessResult::continueWithoutNotice);
    }

    private boolean activateDueEvent(DailyProcessContext context, boolean pausePlayer) {
        Optional<GameEventDefinition> dueEvent = gameEventCatalog
                .findDueEvent(context.player().getMonth(), context.player().getDay())
                .filter(definition -> !gameEventRepository.existsByPlayerAndEventKey(context.player(), definition.key()));
        dueEvent.ifPresent(definition -> eventFlowService.activateEvent(context.player(), definition, pausePlayer));
        return dueEvent.isPresent();
    }

    private Optional<Long> activeEventId(DailyProcessContext context) {
        return eventFlowService.activeEvent(context.player()).map(event -> event.getId());
    }

    private DailyProcessResult eventSignal(long eventId) {
        return DailyProcessResult.stop("EVENT:" + eventId);
    }
}
