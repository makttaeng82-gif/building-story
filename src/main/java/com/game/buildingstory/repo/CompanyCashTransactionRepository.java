package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyCashTransaction;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyCashTransactionRepository extends JpaRepository<CompanyCashTransaction, Long> {
    Optional<CompanyCashTransaction> findByCompanyAndEventKey(PlayerCompany company, String eventKey);

    List<CompanyCashTransaction> findByCompanyAndPeriodIndexBetweenOrderByIdAsc(
            PlayerCompany company, int firstPeriodIndex, int lastPeriodIndex);
}
