package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyComputeConstruction;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyComputeConstructionRepository extends JpaRepository<CompanyComputeConstruction, Long> {
    Optional<CompanyComputeConstruction> findFirstByCompanyAndStatusInOrderByIdDesc(
            PlayerCompany company,
            Collection<CompanyComputeConstructionStatus> statuses
    );

    Optional<CompanyComputeConstruction> findFirstByCompanyAndStatusOrderByIdDesc(
            PlayerCompany company,
            CompanyComputeConstructionStatus status
    );

    List<CompanyComputeConstruction> findByCompanyOrderByIdDesc(PlayerCompany company);
}
