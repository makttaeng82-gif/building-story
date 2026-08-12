package com.game.buildingstory.service;

import java.util.List;

/** 주식 화면의 공모주 청약 팝업에 필요한 값만 전달한다. */
public record NpcIpoSubscriptionView(
        String stockKey,
        String companyName,
        String industry,
        String description,
        String riskText,
        String listingDateText,
        long offerPrice,
        String offerPriceText,
        String marketCapText,
        String fairValueRangeText,
        String demandText,
        String competitionText,
        int expectedAllocationPercent,
        long maximumRequestQuantity,
        String maximumRequestQuantityText,
        long subscriptionFee,
        String subscriptionFeeText,
        String revenueText,
        String operatingProfitText,
        String netIncomeText,
        String revenueGrowthText,
        String operatingMarginText,
        String debtRatioText,
        String offerDiscountText,
        String tradableSharesText,
        List<NpcIpoQuarterView> recentQuarters,
        boolean subscribed,
        long requestedQuantity,
        long expectedAllocationQuantity,
        String reservedAmountText,
        String expectedRefundText
) {
    public NpcIpoSubscriptionView {
        recentQuarters = List.copyOf(recentQuarters);
    }
}
