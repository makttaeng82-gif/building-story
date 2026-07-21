package com.game.buildingstory.repo;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyValuationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListedCompanyValuationSnapshotRepository extends JpaRepository<ListedCompanyValuationSnapshot, Long> {
    Optional<ListedCompanyValuationSnapshot> findByListedCompanyAndFiscalPeriodIndex(ListedCompany company, int fiscalPeriodIndex);

    Optional<ListedCompanyValuationSnapshot> findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(ListedCompany company);
}
