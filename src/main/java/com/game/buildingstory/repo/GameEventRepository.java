package com.game.buildingstory.repo;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    boolean existsByPlayerAndEventKey(Player player, String eventKey);

    boolean existsByPlayerIdAndEventKey(Long playerId, String eventKey);

    Optional<GameEvent> findFirstByPlayerAndStatus(Player player, GameEventStatus status);

    @Query("select event from GameEvent event where event.player.id = :playerId and event.status = :status order by event.id desc")
    List<GameEvent> findLatestByPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") GameEventStatus status, Pageable pageable);

    void deleteByPlayerAndEventKey(Player player, String eventKey);

    void deleteByPlayerIdAndEventKey(Long playerId, String eventKey);
}
