package com.game.buildingstory.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReputationCatalogTests {
    private final ReputationCatalog reputationCatalog = new ReputationCatalog();

    @Test
    void usesReducedReputationThresholdsForCityProgression() {
        assertThat(reputationCatalog.isBuildingUnlocked("청주", 4, 189, true)).isFalse();
        assertThat(reputationCatalog.isBuildingUnlocked("청주", 4, 190, true)).isTrue();
        assertThat(reputationCatalog.isBuildingUnlocked("대전", 4, 3_599, true)).isFalse();
        assertThat(reputationCatalog.isBuildingUnlocked("대전", 4, 3_600, true)).isTrue();
        assertThat(reputationCatalog.isBuildingUnlocked("부산", 4, 8_249, true)).isFalse();
        assertThat(reputationCatalog.isBuildingUnlocked("부산", 4, 8_250, true)).isTrue();
        assertThat(reputationCatalog.isBuildingUnlocked("서울", 4, 31_499, true)).isFalse();
        assertThat(reputationCatalog.isBuildingUnlocked("서울", 4, 31_500, true)).isTrue();
    }
}
