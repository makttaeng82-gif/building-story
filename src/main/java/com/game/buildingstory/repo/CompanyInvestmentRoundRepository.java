package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyInvestmentRound;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyInvestmentRoundRepository extends JpaRepository<CompanyInvestmentRound, Long> {
    List<CompanyInvestmentRound> findByCompanyOrderByRoundNumberDesc(PlayerCompany company);
    Optional<CompanyInvestmentRound> findFirstByCompanyOrderByRoundNumberDesc(PlayerCompany company);
    long countByCompany(PlayerCompany company);
}
