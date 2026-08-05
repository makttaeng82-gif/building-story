package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyQuarterlyReportRepository extends JpaRepository<CompanyQuarterlyReport, Long> {
    List<CompanyQuarterlyReport> findByCompanyOrderByQuarterSequenceDesc(PlayerCompany company);
    Optional<CompanyQuarterlyReport> findByCompanyAndQuarterSequence(
            PlayerCompany company, int quarterSequence);
}
