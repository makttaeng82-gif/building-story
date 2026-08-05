package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyBond;
import com.game.buildingstory.domain.CompanyBondStatus;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CompanyBondRepository extends JpaRepository<CompanyBond, Long> {
    List<CompanyBond> findByCompanyOrderByIdDesc(PlayerCompany company);
    List<CompanyBond> findByCompanyAndStatusInOrderByIdAsc(
            PlayerCompany company,
            Collection<CompanyBondStatus> statuses
    );
}
