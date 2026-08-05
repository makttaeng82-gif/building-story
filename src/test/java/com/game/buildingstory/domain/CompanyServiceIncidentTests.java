package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyServiceIncidentTests {

    @Test
    void majorIncidentOffersTwoCostsAndResolvesSelectedResponse() {
        CompanyServiceIncident incident = incident(
                CompanyServiceIncidentSeverity.MAJOR, 10_000_000_000L, 0, 0, 0);

        assertThat(incident.getStatus()).isEqualTo(
                CompanyServiceIncidentStatus.AWAITING_DECISION);
        assertThat(incident.resolutionCost(
                CompanyServiceIncidentResolution.EMERGENCY_RECOVERY))
                .isEqualTo(10_000_000_000L);
        assertThat(incident.resolutionCost(
                CompanyServiceIncidentResolution.CUSTOMER_COMPENSATION))
                .isEqualTo(5_000_000_000L);
        assertThat(incident.hasCustomerImpact()).isTrue();

        incident.resolveMajor(CompanyServiceIncidentResolution.CUSTOMER_COMPENSATION);

        assertThat(incident.getStatus()).isEqualTo(CompanyServiceIncidentStatus.RESOLVED);
        assertThat(incident.hasCustomerImpact()).isFalse();
    }

    @Test
    void criticalIncidentUsesMonthlyCostUntilResponseWorkCompletes() {
        CompanyServiceIncident incident = incident(
                CompanyServiceIncidentSeverity.CRITICAL,
                10_000_000_000L, 180, 120, 3_000_000_000L);
        incident.startCriticalResponse(60, 40);

        assertThat(incident.currentMonthlyCost()).isEqualTo(3_000_000_000L);
        assertThat(incident.advanceResponseMonth()).isFalse();
        assertThat(incident.progressPercent()).isEqualTo(34);
        assertThat(incident.advanceResponseMonth()).isFalse();
        assertThat(incident.advanceResponseMonth()).isTrue();

        assertThat(incident.getStatus()).isEqualTo(CompanyServiceIncidentStatus.COMPLETED);
        assertThat(incident.progressPercent()).isEqualTo(100);
        assertThat(incident.currentMonthlyCost()).isZero();
        assertThat(incident.hasCustomerImpact()).isFalse();
    }

    @Test
    void computeIncidentRetainsItsInfrastructureSource() {
        Player player = new Player("compute-incident-domain", "hash");
        PlayerCompany company = new PlayerCompany(
                player, "테스트AI", "AI플랫폼",
                1_000_000_000L, 1_000_000_000L, 10_000_000L, 1);
        CompanyServiceIncident incident = new CompanyServiceIncident(
                company, CompanyServiceIncidentSeverity.MAJOR,
                false, true, 1, 400,
                10_000_000_000L, 0, 0, 0);

        assertThat(incident.isComputeIncident()).isTrue();
        assertThat(incident.hasCustomerImpact()).isTrue();
    }

    private CompanyServiceIncident incident(
            CompanyServiceIncidentSeverity severity,
            long fullResponseCost,
            int developmentWork,
            int operationsWork,
            long monthlyCost
    ) {
        Player player = new Player("incident-domain", "hash");
        PlayerCompany company = new PlayerCompany(
                player, "테스트AI", "AI플랫폼",
                1_000_000_000L, 1_000_000_000L, 10_000_000L, 1);
        return new CompanyServiceIncident(
                company, severity, false, 1, 400,
                fullResponseCost, developmentWork, operationsWork, monthlyCost);
    }
}
