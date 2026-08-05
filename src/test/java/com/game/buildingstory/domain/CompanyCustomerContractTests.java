package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyCustomerContractTests {

    @Test
    void contractTypesPreserveDesignedScaleAndRiskRules() {
        assertThat(CompanyCustomerContractType.LARGE.getMinimumFeePercent()).isEqualTo(12);
        assertThat(CompanyCustomerContractType.LARGE.getMaximumFeePercent()).isEqualTo(18);
        assertThat(CompanyCustomerContractType.LARGE.getConstructionFeePercent()).isEqualTo(250);
        assertThat(CompanyCustomerContractType.LARGE.getDurationMonths()).isEqualTo(18);
        assertThat(CompanyCustomerContractType.LARGE.getTotalWork()).isEqualTo(900);
        assertThat(CompanyCustomerContractType.LARGE.getBuildDeadlineMonths()).isEqualTo(9);

        assertThat(CompanyCustomerContractType.STRATEGIC.getMinimumFeePercent()).isEqualTo(20);
        assertThat(CompanyCustomerContractType.STRATEGIC.getMaximumFeePercent()).isEqualTo(30);
        assertThat(CompanyCustomerContractType.STRATEGIC.getConstructionFeePercent()).isEqualTo(200);
        assertThat(CompanyCustomerContractType.STRATEGIC.getDurationMonths()).isEqualTo(24);
        assertThat(CompanyCustomerContractType.STRATEGIC.getTotalWork()).isEqualTo(2_000);
        assertThat(CompanyCustomerContractType.STRATEGIC.getBuildDeadlineMonths()).isEqualTo(12);
        assertThat(CompanyCustomerContractType.TRIAL.hasRiskRules()).isFalse();
        assertThat(CompanyCustomerContractType.STRATEGIC.hasRiskRules()).isTrue();
    }

    @Test
    void threeMonthDelayFailsNormalContractAndSchedulesRefundAndPenalty() {
        CompanyCustomerContract contract = normalContract(3);
        contract.accept(40, 20);

        for (int month = 0; month < 6; month++) {
            contract.advanceBuildMonth(false);
        }

        assertThat(contract.getDelayMonths()).isEqualTo(3);
        assertThat(contract.getStatus()).isEqualTo(
                CompanyCustomerContractStatus.CONTRACT_FAILURE_PAYMENT);
        assertThat(contract.currentCost()).isEqualTo(contract.getConstructionFee() * 50 / 100);

        contract.settleTerminalPayment();
        assertThat(contract.getStatus()).isEqualTo(CompanyCustomerContractStatus.FAILED);
    }

    @Test
    void secondSlaViolationTerminatesNormalContractWithTwoMonthPenalty() {
        CompanyCustomerContract contract = normalContract(6);
        launch(contract);

        contract.advanceActiveMonth(true);
        assertThat(contract.getSlaViolations()).isEqualTo(1);
        assertThat(contract.currentRevenue()).isZero();

        contract.advanceActiveMonth(true);
        assertThat(contract.getStatus()).isEqualTo(
                CompanyCustomerContractStatus.TERMINATION_PAYMENT);
        assertThat(contract.currentCost()).isEqualTo(contract.getMonthlyFee() * 2);

        contract.settleTerminalPayment();
        assertThat(contract.getStatus()).isEqualTo(CompanyCustomerContractStatus.TERMINATED);
    }

    @Test
    void oneSlaViolationOffersRenewalAtTenPercentLowerFee() {
        CompanyCustomerContract contract = normalContract(6);
        launch(contract);
        long originalMonthlyFee = contract.getMonthlyFee();

        contract.advanceActiveMonth(true);
        for (int month = 1; month < contract.getContractDurationMonths(); month++) {
            contract.advanceActiveMonth(false);
        }

        assertThat(contract.getStatus()).isEqualTo(
                CompanyCustomerContractStatus.RENEWAL_OFFERED);
        contract.renew();
        assertThat(contract.getStatus()).isEqualTo(CompanyCustomerContractStatus.ACTIVE);
        assertThat(contract.getMonthlyFee()).isEqualTo(originalMonthlyFee * 90 / 100);
        assertThat(contract.getSlaViolations()).isZero();
    }

    private CompanyCustomerContract normalContract(int deadlineMonths) {
        Player player = new Player("contract-domain", "hash");
        PlayerCompany company = new PlayerCompany(
                player, "테스트AI", "AI플랫폼", 1_000_000_000L,
                1_000_000_000L, 10_000_000L, 1);
        return new CompanyCustomerContract(
                company,
                "normal-contract",
                "테스트고객",
                "업무자동화 구축",
                CompanyCustomerContractType.NORMAL,
                1,
                200,
                60,
                55,
                280,
                120,
                3_000_000_000L,
                1_800_000_000L,
                1_000_000_000L,
                12,
                deadlineMonths
        );
    }

    private void launch(CompanyCustomerContract contract) {
        contract.accept(280, 120);
        assertThat(contract.advanceBuildMonth(true)).isTrue();
        contract.activate();
    }
}
