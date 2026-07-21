package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 주식 가격 캔들 이력 저장소다.
 *
 * <p>주식 화면은 목록용 최신 가격과 선택 종목의 전체 가격 이력을 나누어 조회한다.
 * 최신·직전 가격은 역순 조회하고, 차트 이력은 시간순으로 읽어 기간별 5일봉 또는 월봉으로 표시한다.</p>
 */
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
    boolean existsByPlayerAndStockKey(Player player, String stockKey);

    long countByPlayer(Player player);

    Optional<StockPriceHistory> findFirstByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    Optional<StockPriceHistory> findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    long countByPlayerAndStockKey(Player player, String stockKey);

    Optional<StockPriceHistory> findFirstByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(Player player, String stockKey);

    List<StockPriceHistory> findByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(Player player, String stockKey);

    List<StockPriceHistory> findByPlayerOrderByElapsedDaysAscIdAsc(Player player);

    @Query("select max(history.elapsedDays) from StockPriceHistory history where history.player = :player and history.elapsedDays < :latestElapsedDays")
    Optional<Integer> findPreviousElapsedDays(
            @Param("player") Player player,
            @Param("latestElapsedDays") int latestElapsedDays
    );

    List<StockPriceHistory> findByPlayerAndElapsedDaysInOrderByElapsedDaysDescIdDesc(
            Player player,
            List<Integer> elapsedDays
    );
}
