package com.game.buildingstory.repo;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 확정된 상장기업 분기 실적 이력을 조회한다. */
public interface ListedCompanyQuarterlyReportRepository extends JpaRepository<ListedCompanyQuarterlyReport, Long> {
    boolean existsByListedCompanyAndFiscalPeriodIndex(ListedCompany listedCompany, int fiscalPeriodIndex);

    List<ListedCompanyQuarterlyReport> findByListedCompanyOrderByFiscalPeriodIndexDesc(ListedCompany listedCompany);

    List<ListedCompanyQuarterlyReport> findTop4ByListedCompanyOrderByFiscalPeriodIndexDesc(ListedCompany listedCompany);

    Optional<ListedCompanyQuarterlyReport> findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(ListedCompany listedCompany);

    long countByListedCompanyAndBaselineHistoryFalseAndPublishedElapsedDayBetween(
            ListedCompany listedCompany,
            int firstPublishedElapsedDay,
            int lastPublishedElapsedDay
    );
}
