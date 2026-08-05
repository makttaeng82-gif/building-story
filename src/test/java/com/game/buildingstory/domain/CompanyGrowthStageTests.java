package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyGrowthStageTests {

    @Test
    void centralizesStageLimitsCostsPlansAndContractGrades() {
        assertThat(CompanyGrowthStage.FOUNDED.getMajorWorkSlotLimit()).isEqualTo(2);
        assertThat(CompanyGrowthStage.GROWTH.getMajorWorkSlotLimit()).isEqualTo(3);
        assertThat(CompanyGrowthStage.LARGE.getMajorWorkSlotLimit()).isEqualTo(4);
        assertThat(CompanyGrowthStage.GLOBAL.getMajorWorkSlotLimit()).isEqualTo(6);

        assertThat(CompanyGrowthStage.GROWTH.isProPlanAvailable()).isTrue();
        assertThat(CompanyGrowthStage.GROWTH.isMaxPlanAvailable()).isFalse();
        assertThat(CompanyGrowthStage.LARGE.isMaxPlanAvailable()).isTrue();

        assertThat(CompanyGrowthStage.FOUNDED.getHighestContractType())
                .isEqualTo(CompanyCustomerContractType.TRIAL);
        assertThat(CompanyGrowthStage.GLOBAL.getHighestContractType())
                .isEqualTo(CompanyCustomerContractType.STRATEGIC);

        assertThat(CompanyGrowthStage.GROWTH.getDevelopmentCostPercent()).isEqualTo(22);
        assertThat(CompanyGrowthStage.GROWTH.getMarketingCostPercent()).isEqualTo(10);
        assertThat(CompanyGrowthStage.GROWTH.getPlatformCostPercent()).isEqualTo(12);
        assertThat(CompanyGrowthStage.GROWTH.getGeneralSalaryMultiplier()).isEqualTo(1.15);
    }
}
