package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedLuxuryItem;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 구매한 명품 아이템 저장소다.
 *
 * <p>명품은 보통 1회성 구매 효과를 가지므로 이미 구매했는지 확인하는 조회가 중요하다.</p>
 */
public interface OwnedLuxuryItemRepository extends JpaRepository<OwnedLuxuryItem, Long> {
    List<OwnedLuxuryItem> findByPlayerOrderById(Player player);

    Optional<OwnedLuxuryItem> findByPlayerAndItemKey(Player player, String itemKey);
}
