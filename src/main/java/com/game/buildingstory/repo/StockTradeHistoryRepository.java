package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTradeHistoryRepository extends JpaRepository<StockTradeHistory, Long> {
    List<StockTradeHistory> findTop12ByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    List<StockTradeHistory> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);
}
