package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedGiftItem;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnedGiftItemRepository extends JpaRepository<OwnedGiftItem, Long> {
    List<OwnedGiftItem> findByPlayerOrderById(Player player);

    Optional<OwnedGiftItem> findByPlayerAndGiftKey(Player player, String giftKey);
}
