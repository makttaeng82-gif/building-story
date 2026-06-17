package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnedSecretaryRepository extends JpaRepository<OwnedSecretary, Long> {
    List<OwnedSecretary> findByPlayerOrderById(Player player);

    List<OwnedSecretary> findByPlayerAndAssignedCityOrderById(Player player, String assignedCity);

    Optional<OwnedSecretary> findByPlayerAndSecretaryKey(Player player, String secretaryKey);
}
