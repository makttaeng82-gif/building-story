package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyCustomerContract;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyCustomerContractRepository extends JpaRepository<CompanyCustomerContract, Long> {
    List<CompanyCustomerContract> findByCompanyOrderByIdDesc(PlayerCompany company);

    Optional<CompanyCustomerContract> findFirstByCompanyAndStatusOrderByIdDesc(
            PlayerCompany company,
            CompanyCustomerContractStatus status
    );
}
