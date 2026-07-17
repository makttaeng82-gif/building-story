package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CityMarketIndex;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityMarketIndexRepository extends JpaRepository<CityMarketIndex, Long> {
    Optional<CityMarketIndex> findByPlayerAndCity(Player player, String city);

    List<CityMarketIndex> findByPlayerOrderById(Player player);
}
