package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 주식 가격 캔들 이력 저장소다.
 *
 * <p>주식 화면은 최신 가격, 직전 가격, 최근 60개 캔들을 조회한다.
 * 그래서 stockKey와 elapsedDays 역순 조회 메서드가 많다.</p>
 */
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
    boolean existsByPlayerAndStockKey(Player player, String stockKey);

    long countByPlayer(Player player);

    Optional<StockPriceHistory> findFirstByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    Optional<StockPriceHistory> findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);
}
