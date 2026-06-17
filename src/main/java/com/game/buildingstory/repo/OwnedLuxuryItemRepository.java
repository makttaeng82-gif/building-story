package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedLuxuryItem;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnedLuxuryItemRepository extends JpaRepository<OwnedLuxuryItem, Long> {
    List<OwnedLuxuryItem> findByPlayerOrderById(Player player);

    Optional<OwnedLuxuryItem> findByPlayerAndItemKey(Player player, String itemKey);
}
