package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockMarketIndexHistory;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockMarketIndexHistoryRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockChartDataServiceTests {
    @Mock private StockCatalog stockCatalog;
    @Mock private StockMarketIndexCalculator indexCalculator;
    @Mock private StockPriceHistoryRepository priceRepository;
    @Mock private StockMarketIndexHistoryRepository indexRepository;
    @Mock private StockNewsArticleRepository newsRepository;
    @Mock private StockFinancialDataService financialDataService;

    private StockChartDataService service;

    @BeforeEach
    void setUp() {
        service = new StockChartDataService(
                stockCatalog,
                indexCalculator,
                priceRepository,
                indexRepository,
                newsRepository,
                financialDataService
        );
    }

    @Test
    void actualMarketIndexHistoryOverridesSyntheticComparisonValue() {
        Player player = new Player("chart-index-test", "hash");
        StockSpec stock = stock();
        StockPriceHistory synthetic = history(player, stock.key(), -5, 100_000L);
        StockPriceHistory actual = history(player, stock.key(), 10, 101_000L);
        when(stockCatalog.initial()).thenReturn(List.of(stock));
        when(priceRepository.findByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(player, stock.key()))
                .thenReturn(List.of(synthetic, actual));
        when(priceRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player))
                .thenReturn(List.of(synthetic, actual));
        when(indexCalculator.calculate(anyList(), anyMap())).thenReturn(100_000L);
        when(indexRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player))
                .thenReturn(List.of(new StockMarketIndexHistory(player, 1, 11, 10, 123_456L, 0)));
        when(newsRepository.findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                player, -5)).thenReturn(List.of());
        when(financialDataService.reportEvents(player, stock, null)).thenReturn(List.of());

        List<StockChartPoint> points = service.points(player, stock, null);

        assertThat(points).extracting(StockChartPoint::marketIndexBasisPoints)
                .containsExactly(100_000L, 123_456L);
    }

    @Test
    void chartNewsUsesChartRangeInsteadOfLatestFeedLimit() {
        Player player = new Player("chart-news-test", "hash");
        StockSpec stock = stock();
        StockPriceHistory first = history(player, stock.key(), -20, 100_000L);
        when(stockCatalog.initial()).thenReturn(List.of(stock));
        when(priceRepository.findByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(player, stock.key()))
                .thenReturn(List.of(first));
        when(priceRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player)).thenReturn(List.of(first));
        when(indexCalculator.calculate(anyList(), anyMap())).thenReturn(100_000L);
        when(indexRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player)).thenReturn(List.of());
        when(newsRepository.findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                player, -20)).thenReturn(List.of());
        when(financialDataService.reportEvents(player, stock, null)).thenReturn(List.of());

        service.points(player, stock, null);

        verify(newsRepository)
                .findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(player, -20);
        verify(newsRepository, never()).findTop20ByPlayerOrderByPublishedElapsedDaysDescIdDesc(player);
    }

    private StockSpec stock() {
        return new StockSpec(
                "chart-stock", "IT", "차트기업", StockRiskType.NORMAL,
                1.0, 1.0, 1.5, 100_000L, 10_000_000L, "차트 테스트 기업"
        );
    }

    private StockPriceHistory history(Player player, String stockKey, int elapsedDays, long price) {
        return StockPriceHistory.historical(
                player, stockKey, 1, 1, elapsedDays,
                price, price, price, price, 100L,
                0, 0, 0, 0, 0
        );
    }
}
