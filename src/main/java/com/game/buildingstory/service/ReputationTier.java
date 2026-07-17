package com.game.buildingstory.service;

/**
 * 평판 단계 하나를 표현한다.
 *
 * <p>requiredReputation 이상이면 title을 얻고, unlockCity/unlockSlot 조건을 만족하면 새 도시나 건물이 열린다.</p>
 */
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
