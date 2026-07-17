package com.game.buildingstory.service;

/**
 * 비서가 임차인으로 등장하는 이벤트 시나리오다.
 *
 * <p>어떤 비서가 어떤 도시/건물 조건에서 등장하고, 요청 비용과 이벤트 문구가 무엇인지 정의한다.</p>
 */
record SecretaryTenantScenario(
        String secretaryKey,
        String city,
        int buildingSlot,
        String introTitle,
        String introBody,
        String requestTitle,
        String requestBody,
        String requestButton,
        long requiredCash,
        int requiredReputation,
        String requiredLuxuryKey,
        boolean requiresNoLoan,
        boolean requiresAllLuxuryItems,
        long requestCost,
        int durationDays,
        String progressMemo
) {
}
