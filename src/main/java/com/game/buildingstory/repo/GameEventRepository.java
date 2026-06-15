package com.game.buildingstory.repo;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    boolean existsByPlayerAndEventKey(Player player, String eventKey);

    Optional<GameEvent> findFirstByPlayerAndStatus(Player player, GameEventStatus status);
}
