package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyServiceIncident;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyServiceIncidentRepository extends JpaRepository<CompanyServiceIncident, Long> {
    List<CompanyServiceIncident> findByCompanyOrderByIdDesc(PlayerCompany company);
}
