package com.game.buildingstory.service.time;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.BuildingTradeService;
import com.game.buildingstory.service.EventFlowService;
import com.game.buildingstory.service.ReputationCatalog;
import com.game.buildingstory.service.SettlementService;
import org.springframework.stereotype.Component;

/** 하루의 수입, 지출, 매물 갱신과 칭호 변경을 처리한다. */
@Component
public class DailySettlementProcessor implements DailyGameProcessor {
    private static final int ORDER = 100;
    private final SettlementService settlementService;
    private final EventFlowService eventFlowService;
    private final BuildingTradeService buildingTradeService;
    private final ReputationCatalog reputationCatalog;

    public DailySettlementProcessor(SettlementService settlementService, EventFlowService eventFlowService,
                                    BuildingTradeService buildingTradeService, ReputationCatalog reputationCatalog) {
        this.settlementService = settlementService;
        this.eventFlowService = eventFlowService;
        this.buildingTradeService = buildingTradeService;
        this.reputationCatalog = reputationCatalog;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        Player player = context.player();
        settlementService.clearVacantRepairRequests(player);
        String notice = settlementService.runDailySettlement(player);
        notice = appendNotice(notice, eventFlowService.processAutoResignation(player));

        if (player.getElapsedDays() >= player.getNextOfferRefreshDay()) {
            buildingTradeService.refreshOffers(player);
            player.scheduleNextOfferRefresh();
        }

        player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        return DailyProcessResult.continueWith(notice);
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " \u00b7 " + addition;
    }
}
