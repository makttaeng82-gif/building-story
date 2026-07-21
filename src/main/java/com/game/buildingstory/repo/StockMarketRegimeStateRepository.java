package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockMarketRegimeState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockMarketRegimeStateRepository extends JpaRepository<StockMarketRegimeState, Long> {
    Optional<StockMarketRegimeState> findByPlayer(Player player);
}
