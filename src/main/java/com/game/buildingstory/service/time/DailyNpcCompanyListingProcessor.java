package com.game.buildingstory.service.time;

import com.game.buildingstory.service.NpcCompanyListingService;
import org.springframework.stereotype.Component;

/** NPC 신규상장 일정을 가격 갱신보다 먼저 하루에 한 단계씩 진행한다. */
@Component
public class DailyNpcCompanyListingProcessor implements DailyGameProcessor {
    private static final int ORDER = 275;
    private final NpcCompanyListingService listingService;

    public DailyNpcCompanyListingProcessor(NpcCompanyListingService listingService) {
        this.listingService = listingService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        listingService.process(context.player());
        return DailyProcessResult.continueWithoutNotice();
    }
}
