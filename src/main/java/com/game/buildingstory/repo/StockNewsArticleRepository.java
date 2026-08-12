package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockNewsArticleRepository extends JpaRepository<StockNewsArticle, Long> {
    List<StockNewsArticle> findTop20ByPlayerOrderByPublishedElapsedDaysDescIdDesc(Player player);

    Optional<StockNewsArticle> findByIdAndPlayer(long id, Player player);

    boolean existsByPlayerAndEventKey(Player player, String eventKey);

    List<StockNewsArticle> findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
            Player player,
            int elapsedDays
    );

    List<StockNewsArticle> findByPlayerAndRemainingPriceRefreshesGreaterThan(Player player, int remainingPriceRefreshes);

    List<StockNewsArticle> findByPlayerAndResolvedFalseAndResolutionElapsedDaysLessThanEqual(
            Player player,
            int elapsedDays
    );

    void deleteByPlayerAndPublishedElapsedDaysLessThan(Player player, int elapsedDays);

}
