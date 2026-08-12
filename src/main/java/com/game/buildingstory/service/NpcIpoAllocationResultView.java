package com.game.buildingstory.service;

/** 상장 후 한 번 표시하는 공모주 배정 결과 팝업 값이다. */
public record NpcIpoAllocationResultView(
        String stockKey,
        String companyName,
        String listingDateText,
        String requestedQuantityText,
        String allocatedQuantityText,
        String offerPriceText,
        String purchaseAmountText,
        String refundAmountText
) {
}
