package com.game.buildingstory.repo;

import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingOfferRepository extends JpaRepository<BuildingOffer, Long> {
    List<BuildingOffer> findByPlayerAndCityOrderById(Player player, String city);

    boolean existsByPlayerAndCity(Player player, String city);

    void deleteByPlayerAndCity(Player player, String city);
}
