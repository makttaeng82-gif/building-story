package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockMarketIndexHistory;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockMarketIndexHistoryRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 종합지수를 가격 갱신일마다 계산하고 플레이어별 이력으로 보존한다. */
@Service
@Transactional
public class StockMarketIndexService {
    private final StockCatalog stockCatalog;
    private final StockMarketIndexCalculator calculator;
    private final StockPriceHistoryRepository priceHistoryRepository;
    private final StockMarketIndexHistoryRepository indexHistoryRepository;

    public StockMarketIndexService(
            StockCatalog stockCatalog,
            StockMarketIndexCalculator calculator,
            StockPriceHistoryRepository priceHistoryRepository,
            StockMarketIndexHistoryRepository indexHistoryRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.calculator = calculator;
        this.priceHistoryRepository = priceHistoryRepository;
        this.indexHistoryRepository = indexHistoryRepository;
    }

    public StockMarketIndexHistory ensureInitialized(Player player) {
        return indexHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .orElseGet(() -> saveCurrent(player, null));
    }

    public StockMarketIndexHistory recordCurrent(Player player) {
        StockMarketIndexHistory previous = ensureInitialized(player);
        int latestPriceDay = latestPriceDay(player);
        if (indexHistoryRepository.existsByPlayerAndElapsedDays(player, latestPriceDay)) {
            return previous;
        }
        return saveCurrent(player, previous);
    }

    @Transactional(readOnly = true)
    public IndexSnapshot current(Player player) {
        return indexHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .map(row -> new IndexSnapshot(row.getIndexBasisPoints(), row.getChangeBasisPoints()))
                .orElse(new IndexSnapshot(StockMarketIndexCalculator.BASE_INDEX_BASIS_POINTS, 0));
    }

    private StockMarketIndexHistory saveCurrent(Player player, StockMarketIndexHistory previous) {
        StockPriceHistory latestPrice = latestPrice(player);
        long currentIndex = calculator.calculate(stockCatalog.all(), latestPrices(player));
        int changeBasisPoints = previous == null ? 0 : changeBasisPoints(previous.getIndexBasisPoints(), currentIndex);
        return indexHistoryRepository.save(new StockMarketIndexHistory(
                player,
                latestPrice.getMonth(),
                latestPrice.getDay(),
                latestPrice.getElapsedDays(),
                currentIndex,
                changeBasisPoints
        ));
    }

    private Map<String, Long> latestPrices(Player player) {
        int latestPriceDay = latestPriceDay(player);
        return priceHistoryRepository.findByPlayerAndElapsedDaysInOrderByElapsedDaysDescIdDesc(
                        player, List.of(latestPriceDay)).stream()
                .collect(Collectors.toMap(
                        StockPriceHistory::getStockKey,
                        StockPriceHistory::getClosePrice,
                        (first, ignored) -> first
                ));
    }

    private int latestPriceDay(Player player) {
        return latestPrice(player).getElapsedDays();
    }

    private StockPriceHistory latestPrice(Player player) {
        return priceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .orElseThrow(() -> new IllegalStateException("종합지수를 계산할 가격 이력이 없습니다."));
    }

    private int changeBasisPoints(long previous, long current) {
        if (previous <= 0) {
            return 0;
        }
        return (int) Math.round((current - previous) * 10_000.0 / previous);
    }

    public record IndexSnapshot(long indexBasisPoints, int changeBasisPoints) {
    }
}
