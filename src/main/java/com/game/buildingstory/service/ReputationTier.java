package com.game.buildingstory.service;

public record ReputationTier(
        String title,
        int requiredReputation,
        boolean requiresResignation,
        String unlockCity,
        int unlockBuildingSlot
) {
    public String unlockLabel() {
        if (unlockCity == null || unlockBuildingSlot == 0) {
            return "";
        }
        return unlockCity + " " + unlockBuildingSlot + "번 건물";
    }
}
