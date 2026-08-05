package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyCompetitor;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyCompetitorRepository extends JpaRepository<CompanyCompetitor, Long> {
    List<CompanyCompetitor> findByCompanyOrderById(PlayerCompany company);
    boolean existsByCompany(PlayerCompany company);
}
