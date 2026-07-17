package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedGiftItem;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 플레이어가 보유한 선물 아이템 수량 저장소다.
 *
 * <p>선물은 여러 개를 살 수 있으므로 player + giftKey 조합으로 현재 수량을 찾는다.</p>
 */
public interface OwnedGiftItemRepository extends JpaRepository<OwnedGiftItem, Long> {
    List<OwnedGiftItem> findByPlayerOrderById(Player player);

    Optional<OwnedGiftItem> findByPlayerAndGiftKey(Player player, String giftKey);
}
