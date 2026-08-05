package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyMonthlySettlement;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMonthlySettlementRepository extends JpaRepository<CompanyMonthlySettlement, Long> {
    Optional<CompanyMonthlySettlement> findByCompanyAndPeriodIndex(PlayerCompany company, int periodIndex);
    List<CompanyMonthlySettlement> findByCompanyOrderByPeriodIndexDesc(PlayerCompany company);
    List<CompanyMonthlySettlement> findTop3ByCompanyOrderByPeriodIndexDesc(PlayerCompany company);
    List<CompanyMonthlySettlement> findByCompanyAndPeriodIndexBetweenOrderByPeriodIndexAsc(
            PlayerCompany company, int firstPeriodIndex, int lastPeriodIndex);
    long countByCompany(PlayerCompany company);
}
