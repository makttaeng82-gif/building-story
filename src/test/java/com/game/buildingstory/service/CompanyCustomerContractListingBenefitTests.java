package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCustomerContractType;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCustomerContractRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyShortTermProjectRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCustomerContractListingBenefitTests {
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerCompanyRepository companyRepository;
    @Mock private CompanyCustomerContractRepository contractRepository;
    @Mock private CompanyShortTermProjectRepository shortTermProjectRepository;
    @Mock private CompanyDepartmentRepository departmentRepository;
    @Mock private CompanyMonthlySettlementRepository settlementRepository;
    @Mock private CompanyWorkforceService workforceService;
    @Mock private CompanyGrowthService growthService;
    @Mock private CompanyExternalEventService externalEventService;
    @Mock private CompanyListingBenefitService listingBenefitService;
    @Mock private PlayerCompany company;

    @InjectMocks private CompanyCustomerContractService service;

    @Test
    void newlyGeneratedLargeContractUsesListedRequirementReduction() {
        when(company.getPrototypeBenchmark()).thenReturn(700);
        when(company.getProductStability()).thenReturn(80);
        when(company.getProductSecurity()).thenReturn(80);
        when(listingBenefitService.contractRequirementReduction(
                company, CompanyCustomerContractType.LARGE))
                .thenReturn(new CompanyListingBenefitService.ContractRequirementReduction(10, 2, 2));

        var requirements = service.contractRequirements(company, CompanyCustomerContractType.LARGE);

        assertThat(requirements.benchmark()).isEqualTo(687);
        assertThat(requirements.stability()).isEqualTo(76);
        assertThat(requirements.security()).isEqualTo(76);
    }
}
