package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 보유 주식 저장소다.
 *
 * <p>현재 보유 수량과 평균단가만 저장한다. 현재가는 StockPriceHistory에서 따로 조회한다.</p>
 */
public interface OwnedStockRepository extends JpaRepository<OwnedStock, Long> {
    List<OwnedStock> findByPlayer(Player player);

    Optional<OwnedStock> findByPlayerAndStockKey(Player player, String stockKey);
}
