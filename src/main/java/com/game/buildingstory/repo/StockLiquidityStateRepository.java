package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockLiquidityState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockLiquidityStateRepository extends JpaRepository<StockLiquidityState, Long> {
    List<StockLiquidityState> findByPlayer(Player player);

    Optional<StockLiquidityState> findByPlayerAndStockKey(Player player, String stockKey);
}
