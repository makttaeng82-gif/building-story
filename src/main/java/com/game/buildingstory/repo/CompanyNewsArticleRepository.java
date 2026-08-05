package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyNewsArticleRepository extends JpaRepository<CompanyNewsArticle, Long> {
    List<CompanyNewsArticle> findByCompanyOrderByOccurredMarketMonthDescIdDesc(PlayerCompany company);
    Optional<CompanyNewsArticle> findByCompanyAndEventKey(PlayerCompany company, String eventKey);
}
