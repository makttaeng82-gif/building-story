package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCustomerContractType;
import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyListingBenefitServiceTests {
    private final CompanyListingRepository listingRepository = mock(CompanyListingRepository.class);
    private final CompanyListingBenefitService service =
            new CompanyListingBenefitService(listingRepository);
    private final PlayerCompany company = mock(PlayerCompany.class);

    @Test
    void listedCompanyReceivesReducedBondInterest() {
        CompanyListing listing = mock(CompanyListing.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);

        assertThat(service.bondMonthlyInterestBasisPoints(company)).isEqualTo(45);
        assertThat(service.monthlyBondInterest(company, 1_000_000_000L))
                .isEqualTo(4_500_000L);
    }

    @Test
    void unlistedCompanyKeepsStandardBondInterest() {
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());

        assertThat(service.bondMonthlyInterestBasisPoints(company)).isEqualTo(50);
        assertThat(service.monthlyBondInterest(company, 1_000_000_000L))
                .isEqualTo(5_000_000L);
    }

    @Test
    void listedContractBenefitOnlyReducesLargeAndStrategicRequirements() {
        CompanyListing listing = mock(CompanyListing.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);

        assertThat(service.contractRequirementReduction(company, CompanyCustomerContractType.NORMAL))
                .isEqualTo(new CompanyListingBenefitService.ContractRequirementReduction(0, 0, 0));
        assertThat(service.contractRequirementReduction(company, CompanyCustomerContractType.LARGE))
                .isEqualTo(new CompanyListingBenefitService.ContractRequirementReduction(10, 2, 2));
        assertThat(service.contractRequirementReduction(company, CompanyCustomerContractType.STRATEGIC))
                .isEqualTo(new CompanyListingBenefitService.ContractRequirementReduction(20, 3, 3));
    }
}
