package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyServiceIncident;
import com.game.buildingstory.domain.CompanyServiceIncidentSeverity;
import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyIpoQualificationServiceTests {
    @Mock private CompanyListingRepository listingRepository;
    @Mock private CompanyQuarterlyReportRepository quarterlyReportRepository;
    @Mock private CompanyValuationSnapshotRepository valuationRepository;
    @Mock private CompanyDepartmentRepository departmentRepository;
    @Mock private CompanyServiceIncidentRepository incidentRepository;
    @Mock private CompanyWorkforceService workforceService;
    @InjectMocks private CompanyIpoQualificationService qualificationService;

    @Test
    void policyCalculatesOfferingWithoutDuplicatingFormulasInCallers() {
        assertThat(CompanyIpoPolicy.calculateNewShares(10_000_000L, 25))
                .isEqualTo(3_333_334L);
        assertThat(CompanyIpoPolicy.calculateOfferPrice(3_000_000_000_000L, 10_000_000L))
                .isEqualTo(270_000L);
        assertThat(CompanyIpoPolicy.calculateProceeds(3_333_334L, 270_000L))
                .isEqualTo(900_000_180_000L);
        assertThat(CompanyIpoPolicy.preservesMinimumPlayerOwnership(2_000_000L, 10_000_000L))
                .isTrue();
        assertThat(CompanyIpoPolicy.preservesMinimumPlayerOwnership(1_999_999L, 10_000_000L))
                .isFalse();
        assertThatThrownBy(() -> CompanyIpoPolicy.calculateNewShares(10_000_000L, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listingStartsAsPreparationRecordWithoutChangingCompanyOrStockState() {
        PlayerCompany company = mock(PlayerCompany.class);

        CompanyListing listing = new CompanyListing(
                company,
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                25,
                730,
                CompanyIpoPolicy.PREPARATION_COST
        );

        assertThat(listing.getCompany()).isSameAs(company);
        assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.PREPARING);
        assertThat(listing.getSelectedOfferPercent()).isEqualTo(25);
        assertThat(listing.getPreparationMonthsCompleted()).isZero();
        assertThat(listing.getOfferPrice()).isZero();
    }

    @Test
    void allowsApplicationOnlyWhenEveryOperationalRequirementIsMet() {
        PlayerCompany company = eligibleCompany();
        CompanyQuarterlyReport profitable = mock(CompanyQuarterlyReport.class);
        when(profitable.getOperatingProfit()).thenReturn(1L);
        CompanyValuationSnapshot valuation = mock(CompanyValuationSnapshot.class);
        when(valuation.getEnterpriseValue()).thenReturn(CompanyIpoPolicy.MINIMUM_EQUITY_VALUE);
        CompanyDepartment strategy = mock(CompanyDepartment.class);

        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());
        when(quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Collections.nCopies(CompanyIpoPolicy.MINIMUM_QUARTERLY_REPORTS, profitable));
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(departmentRepository.findByCompanyAndDepartmentType(
                company, com.game.buildingstory.domain.CompanyDepartmentType.STRATEGY_FINANCE))
                .thenReturn(Optional.of(strategy));
        when(workforceService.departmentExpertise(
                company, com.game.buildingstory.domain.CompanyDepartmentType.STRATEGY_FINANCE))
                .thenReturn(CompanyIpoPolicy.MINIMUM_STRATEGY_EXPERTISE);
        when(incidentRepository.findByCompanyOrderByIdDesc(company)).thenReturn(List.of());

        CompanyIpoQualification result = qualificationService.evaluate(company);

        assertThat(result.canApply()).isTrue();
        assertThat(result.unmetChecks()).isEmpty();
        assertThat(result.check(CompanyIpoRequirement.EQUITY_VALUE).currentValue())
                .isEqualTo(CompanyIpoPolicy.MINIMUM_EQUITY_VALUE);
    }

    @Test
    void reportsEveryUnmetConditionInsteadOfReturningOnlyTheFirstFailure() {
        PlayerCompany company = mock(PlayerCompany.class);
        CompanyQuarterlyReport loss = mock(CompanyQuarterlyReport.class);
        when(loss.getOperatingProfit()).thenReturn(-1L);
        CompanyServiceIncident incident = mock(CompanyServiceIncident.class);
        when(incident.getSeverity()).thenReturn(CompanyServiceIncidentSeverity.CRITICAL);
        when(incident.hasCustomerImpact()).thenReturn(true);

        when(company.getGrowthStage()).thenReturn(CompanyGrowthStage.FOUNDED);
        when(company.isOperationsSuspended()).thenReturn(true);
        CompanyListing existingListing = new CompanyListing(
                company,
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                25,
                1,
                CompanyIpoPolicy.PREPARATION_COST
        );
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(existingListing));
        when(quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of(loss));
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.empty());
        when(departmentRepository.findByCompanyAndDepartmentType(
                company, com.game.buildingstory.domain.CompanyDepartmentType.STRATEGY_FINANCE))
                .thenReturn(Optional.empty());
        when(incidentRepository.findByCompanyOrderByIdDesc(company)).thenReturn(List.of(incident));

        CompanyIpoQualification result = qualificationService.evaluate(company);

        assertThat(result.canApply()).isFalse();
        assertThat(result.unmetChecks())
                .extracting(CompanyIpoQualification.RequirementCheck::requirement)
                .containsExactlyInAnyOrder(CompanyIpoRequirement.values());
    }

    private PlayerCompany eligibleCompany() {
        PlayerCompany company = mock(PlayerCompany.class);
        when(company.getGrowthStage()).thenReturn(CompanyGrowthStage.GROWTH);
        when(company.getMonthlyRecurringRevenue())
                .thenReturn(CompanyIpoPolicy.MINIMUM_MONTHLY_RECURRING_REVENUE);
        when(company.getPaidUsers()).thenReturn(CompanyIpoPolicy.MINIMUM_PAID_USERS);
        when(company.getPrototypeBenchmark()).thenReturn(CompanyIpoPolicy.MINIMUM_BENCHMARK);
        when(company.isOperationsSuspended()).thenReturn(false);
        when(company.getUnpaidSettlementAmount()).thenReturn(0L);
        when(company.getUnpaidOperatingAmount()).thenReturn(0L);
        when(company.getUnpaidBondInterestAmount()).thenReturn(0L);
        when(company.getUnpaidBondPrincipalAmount()).thenReturn(0L);
        return company;
    }
}
