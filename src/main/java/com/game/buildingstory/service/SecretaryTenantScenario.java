package com.game.buildingstory.service;

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
