package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
    boolean existsByPlayerAndStockKey(Player player, String stockKey);

    long countByPlayer(Player player);

    Optional<StockPriceHistory> findFirstByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    Optional<StockPriceHistory> findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);
}
