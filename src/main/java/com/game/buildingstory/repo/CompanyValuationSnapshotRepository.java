package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyValuationSnapshotRepository extends JpaRepository<CompanyValuationSnapshot, Long> {
    Optional<CompanyValuationSnapshot> findFirstByCompanyOrderByQuarterSequenceDesc(PlayerCompany company);
    List<CompanyValuationSnapshot> findByCompanyOrderByQuarterSequenceDesc(PlayerCompany company);
}
