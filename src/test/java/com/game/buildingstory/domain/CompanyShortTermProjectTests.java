package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyShortTermProjectTests {

    @Test
    void refundDecisionPausesRevenueAndAppliesSelectedReduction() {
        CompanyShortTermProject project = completedProject();
        project.applyPostCompletionRisk(CompanyShortTermProjectRisk.REFUND_REQUEST);

        assertThat(project.isRiskDecisionRequired()).isTrue();
        assertThat(project.advanceRevenueMonth()).isFalse();

        project.resolveRisk(CompanyShortTermProjectRiskResolution.PARTIAL_REFUND);

        assertThat(project.isRiskDecisionRequired()).isFalse();
        assertThat(project.getRevenueMultiplierPercent()).isEqualTo(70);
        assertThat(project.currentRevenue()).isEqualTo(700_000_000L);
    }

    @Test
    void afterServiceCostIsChargedOnceWithoutReducingRevenue() {
        CompanyShortTermProject project = completedProject();
        project.applyPostCompletionRisk(CompanyShortTermProjectRisk.AFTER_SERVICE);

        assertThat(project.currentMonthlyCost()).isEqualTo(100_000_000L);
        assertThat(project.currentRevenue()).isEqualTo(1_000_000_000L);

        project.clearPendingRiskCost();

        assertThat(project.currentMonthlyCost()).isZero();
    }

    private CompanyShortTermProject completedProject() {
        Player player = new Player("short-term-domain", "hash");
        PlayerCompany company = new PlayerCompany(
                player, "테스트AI", "AI플랫폼",
                1_000_000_000L, 1_000_000_000L, 10_000_000L, 1);
        CompanyShortTermProject project = new CompanyShortTermProject(
                company, "test", "테스트 사업", 1,
                100, 500_000_000L, 1_000_000_000L);
        project.accept(100);
        project.advanceWorkMonth(100);
        return project;
    }
}
