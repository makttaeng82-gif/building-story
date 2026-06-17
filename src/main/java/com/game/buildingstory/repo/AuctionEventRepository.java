package com.game.buildingstory.repo;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AuctionEventRepository extends JpaRepository<AuctionEvent, Long> {
    Optional<AuctionEvent> findFirstByPlayerAndStatusInOrderByIdDesc(Player player, Collection<AuctionStatus> statuses);
}
