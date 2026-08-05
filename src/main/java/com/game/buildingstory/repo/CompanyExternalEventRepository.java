package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyExternalEvent;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyExternalEventRepository extends JpaRepository<CompanyExternalEvent, Long> {
    List<CompanyExternalEvent> findByCompanyOrderByStartMarketMonthDescIdDesc(PlayerCompany company);
    boolean existsByCompanyAndStartMarketMonth(PlayerCompany company, int startMarketMonth);
}
