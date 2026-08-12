package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListingStatus;

import java.util.List;

/** 기업 화면이 IPO 상태와 예상 공모 결과를 다시 계산하지 않도록 제공하는 읽기 전용 값이다. */
public record CompanyIpoOverview(
        CompanyListingStatus status,
        CompanyIpoQualification qualification,
        long latestValuation,
        int selectedOfferPercent,
        int preparationMonthsCompleted,
        long preparationCost,
        long confirmedOfferPrice,
        long confirmedNewShares,
        long confirmedProceeds,
        Integer listedElapsedDay,
        boolean workCapacityAvailable,
        List<OfferOption> offerOptions
) {
    public CompanyIpoOverview {
        offerOptions = List.copyOf(offerOptions);
    }

    public boolean hasApplication() {
        return status != null && status != CompanyListingStatus.CANCELLED;
    }

    public boolean isPreparing() {
        return status == CompanyListingStatus.PREPARING;
    }

    public boolean isReady() {
        return status == CompanyListingStatus.READY;
    }

    public boolean isListed() {
        return status == CompanyListingStatus.LISTED;
    }

    public record OfferOption(
            int percent,
            long newShares,
            long offerPrice,
            long proceeds,
            double playerOwnershipPercent,
            boolean preservesMinimumOwnership
    ) {
    }
}
