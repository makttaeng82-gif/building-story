package com.game.buildingstory.service.time;

import com.game.buildingstory.service.ListedCompanyFinancialService;
import org.springframework.stereotype.Component;

/** 분기 첫날에 상장기업 실적을 확정하며, 중단 가능한 이벤트 처리보다 먼저 실행된다. */
@Component
public class DailyListedCompanyProcessor implements DailyGameProcessor {
    private static final int ORDER = 150;
    private final ListedCompanyFinancialService financialService;

    public DailyListedCompanyProcessor(ListedCompanyFinancialService financialService) {
        this.financialService = financialService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        financialService.settlePreviousQuarterIfDue(context.player());
        return DailyProcessResult.continueWithoutNotice();
    }
}
