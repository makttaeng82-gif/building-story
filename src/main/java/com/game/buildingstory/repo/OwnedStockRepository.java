package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnedStockRepository extends JpaRepository<OwnedStock, Long> {
    List<OwnedStock> findByPlayer(Player player);

    Optional<OwnedStock> findByPlayerAndStockKey(Player player, String stockKey);
}
