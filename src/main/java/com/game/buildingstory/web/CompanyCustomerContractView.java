package com.game.buildingstory.web;

import java.util.List;

/** 기업 화면에 표시할 고객 계약의 구축·운영 상태와 신규 제안이다. */
public record CompanyCustomerContractView(
        String status,
        String activeName,
        int progressPercent,
        String progressText,
        String monthlyAmount,
        Long renewalContractId,
        String renewalTerms,
        List<Offer> offers,
        List<ActiveContract> contracts
) {
    public record ActiveContract(
            long id,
            String status,
            String name,
            String detail,
            String amount,
            int progressPercent,
            boolean renewalAvailable,
            String renewalTerms
    ) {
    }

    public record Offer(
            long id,
            String type,
            String clientName,
            String projectName,
            String requirements,
            String constructionFee,
            String monthlyFee,
            String duration,
            String deadline,
            String risk,
            String expiresText,
            boolean available,
            String availabilityReason
    ) {
    }
}
