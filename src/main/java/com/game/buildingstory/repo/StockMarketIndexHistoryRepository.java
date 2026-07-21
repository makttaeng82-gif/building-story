package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockMarketIndexHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockMarketIndexHistoryRepository extends JpaRepository<StockMarketIndexHistory, Long> {
    Optional<StockMarketIndexHistory> findFirstByPlayerOrderByElapsedDaysDescIdDesc(Player player);
    boolean existsByPlayerAndElapsedDays(Player player, int elapsedDays);
}
