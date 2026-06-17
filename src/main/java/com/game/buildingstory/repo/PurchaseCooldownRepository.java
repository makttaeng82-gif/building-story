package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PurchaseCooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseCooldownRepository extends JpaRepository<PurchaseCooldown, Long> {
    Optional<PurchaseCooldown> findByPlayerAndCityAndBuildingSlot(Player player, String city, int buildingSlot);

    boolean existsByPlayerAndCity(Player player, String city);
}
