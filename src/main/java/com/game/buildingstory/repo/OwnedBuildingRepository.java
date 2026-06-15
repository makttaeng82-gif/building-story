package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnedBuildingRepository extends JpaRepository<OwnedBuilding, Long> {
    List<OwnedBuilding> findByPlayerAndCityOrderById(Player player, String city);

    long countByPlayerAndCity(Player player, String city);
}
