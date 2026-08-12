package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCustomerContractType;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import org.springframework.stereotype.Service;

/** 상장 완료 기업에만 적용되는 후속 운영 혜택을 한곳에서 관리한다. */
@Service
public class CompanyListingBenefitService {
    private static final int UNLISTED_BOND_MONTHLY_INTEREST_BASIS_POINTS = 50;
    private static final int LISTED_BOND_MONTHLY_INTEREST_BASIS_POINTS = 45;

    private final CompanyListingRepository listingRepository;

    public CompanyListingBenefitService(CompanyListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public boolean isListed(PlayerCompany company) {
        return listingRepository.findByCompany(company)
                .map(listing -> listing.getStatus() == CompanyListingStatus.LISTED)
                .orElse(false);
    }

    public int bondMonthlyInterestBasisPoints(PlayerCompany company) {
        return isListed(company)
                ? LISTED_BOND_MONTHLY_INTEREST_BASIS_POINTS
                : UNLISTED_BOND_MONTHLY_INTEREST_BASIS_POINTS;
    }

    public long monthlyBondInterest(PlayerCompany company, long principal) {
        return Math.multiplyExact(principal, bondMonthlyInterestBasisPoints(company)) / 10_000;
    }

    /**
     * 상장 신뢰도는 대형·전략 계약의 심사 기준만 완화한다.
     * 제안 확률, 계약 수익, 구축 업무량과 SLA 기준은 바꾸지 않는다.
     */
    public ContractRequirementReduction contractRequirementReduction(
            PlayerCompany company,
            CompanyCustomerContractType type
    ) {
        if (!isListed(company)) {
            return ContractRequirementReduction.NONE;
        }
        return switch (type) {
            case TRIAL, NORMAL -> ContractRequirementReduction.NONE;
            case LARGE -> new ContractRequirementReduction(10, 2, 2);
            case STRATEGIC -> new ContractRequirementReduction(20, 3, 3);
        };
    }

    public record ContractRequirementReduction(int benchmark, int stability, int security) {
        private static final ContractRequirementReduction NONE =
                new ContractRequirementReduction(0, 0, 0);
    }
}
