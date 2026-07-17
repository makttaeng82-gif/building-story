package com.game.buildingstory.service.time;

import com.game.buildingstory.service.AuctionService;
import org.springframework.stereotype.Component;

/** 정산 결과로 활성화된 경매가 있으면 다음 처리보다 경매 화면을 우선한다. */
@Component
public class DailyAuctionGateProcessor implements DailyGameProcessor {
    private static final int ORDER = 200;
    private final AuctionService auctionService;

    public DailyAuctionGateProcessor(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public DailyProcessResult process(DailyProcessContext context) {
        return auctionService.activeAuction(context.player())
                .map(auction -> DailyProcessResult.stop("AUCTION:" + auction.getId()))
                .orElseGet(DailyProcessResult::continueWithoutNotice);
    }
}
