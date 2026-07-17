package com.game.buildingstory.service.time;

import com.game.buildingstory.service.EventFlowService;
import com.game.buildingstory.service.StockService;
import org.springframework.stereotype.Component;

/** 주식 해금, 가격 갱신, 업종 뉴스를 하루 흐름에 반영한다. */
@Component
public class DailyStockProcessor implements DailyGameProcessor {
    private static final int ORDER = 300;
    private final StockService stockService;
    private final EventFlowService eventFlowService;

    public DailyStockProcessor(StockService stockService, EventFlowService eventFlowService) {
        this.stockService = stockService;
        this.eventFlowService = eventFlowService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        if (stockService.activateUnlockNoticeIfDue(context.player())) {
            return activeEventSignal(context);
        }
        stockService.processPriceUpdates(context.player());
        if (stockService.activateIndustryNewsIfDue(context.player())) {
            return activeEventSignal(context);
        }
        return DailyProcessResult.continueWithoutNotice();
    }

    private DailyProcessResult activeEventSignal(DailyProcessContext context) {
        long eventId = eventFlowService.activeEvent(context.player()).orElseThrow().getId();
        return DailyProcessResult.stop("EVENT:" + eventId);
    }
}
