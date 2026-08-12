package com.game.buildingstory.service.time;

import com.game.buildingstory.service.CompanyAccessService;
import com.game.buildingstory.service.EventFlowService;
import org.springframework.stereotype.Component;

/** 기업 설립 제안일을 확인하되 도시 밖에서는 이벤트 화면 전환을 미룬다. */
@Component
public class DailyCompanyAccessProcessor implements DailyGameProcessor {
    private static final int ORDER = 350;

    private final CompanyAccessService companyAccessService;
    private final EventFlowService eventFlowService;

    public DailyCompanyAccessProcessor(
            CompanyAccessService companyAccessService,
            EventFlowService eventFlowService
    ) {
        this.companyAccessService = companyAccessService;
        this.eventFlowService = eventFlowService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        companyAccessService.ensureUnlockSchedule(context.player());
        if (context.deferCityEvents() || !companyAccessService.activateProposalIfDue(context.player())) {
            return DailyProcessResult.continueWithoutNotice();
        }
        long eventId = eventFlowService.activeEvent(context.player()).orElseThrow().getId();
        return DailyProcessResult.stop("EVENT:" + eventId);
    }
}
