package com.game.buildingstory.repo;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 플레이어별 상장기업 상태를 조회하고 저장한다. */
public interface ListedCompanyRepository extends JpaRepository<ListedCompany, Long> {
    List<ListedCompany> findByPlayer(Player player);

    Optional<ListedCompany> findByPlayerAndStockKey(Player player, String stockKey);
}
