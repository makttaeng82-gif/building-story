package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockMarketIndexHistoryRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMarketIndexServiceTests {
    @Mock private StockMarketIndexCalculator calculator;
    @Mock private StockMarketIndexConstituentService constituentService;
    @Mock private StockPriceHistoryRepository priceRepository;
    @Mock private StockMarketIndexHistoryRepository indexRepository;

    private StockMarketIndexService service;

    @BeforeEach
    void setUp() {
        service = new StockMarketIndexService(
                calculator,
                constituentService,
                priceRepository,
                indexRepository
        );
    }

    @Test
    void firstIndexUsesCurrentConstituents() {
        Player player = new Player("index-initialization-test", "hash");
        StockSpec initial = stock("initial", 100_000L);
        StockSpec newlyIncluded = stock("new-listing", 50_000L);
        List<StockSpec> constituents = List.of(initial, newlyIncluded);
        StockPriceHistory initialPrice = price(player, initial);
        StockPriceHistory listingPrice = price(player, newlyIncluded);
        when(indexRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)).thenReturn(Optional.empty());
        when(priceRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)).thenReturn(Optional.of(initialPrice));
        when(constituentService.currentAt(player, player.getElapsedDays())).thenReturn(constituents);
        when(priceRepository.findByPlayerAndElapsedDaysInOrderByElapsedDaysDescIdDesc(
                player, List.of(player.getElapsedDays()))).thenReturn(List.of(initialPrice, listingPrice));
        when(priceRepository.findPreviousElapsedDays(player, player.getElapsedDays())).thenReturn(Optional.empty());
        when(indexRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(calculator.calculate(constituents, java.util.Map.of(
                initial.key(), initial.basePrice(), newlyIncluded.key(), newlyIncluded.basePrice()
        ))).thenReturn(123_456L);

        var history = service.ensureInitialized(player);

        assertThat(history.getIndexBasisPoints()).isEqualTo(123_456L);
        verify(calculator).calculate(constituents, java.util.Map.of(
                initial.key(), initial.basePrice(), newlyIncluded.key(), newlyIncluded.basePrice()
        ));
    }

    @Test
    void indexSummaryWithoutHistoryReportsCurrentConstituentCount() {
        Player player = new Player("index-summary-test", "hash");
        List<StockSpec> constituents = List.of(stock("one", 100_000L), stock("two", 50_000L));
        when(indexRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)).thenReturn(Optional.empty());
        when(constituentService.current(player)).thenReturn(constituents);

        var snapshot = service.current(player);

        assertThat(snapshot.constituentCount()).isEqualTo(2);
    }

    private StockSpec stock(String key, long price) {
        return new StockSpec(
                key, "IT", key, StockRiskType.NORMAL,
                1.0, 1.0, 1.5, price, 10_000_000L, "지수 테스트 기업"
        );
    }

    private StockPriceHistory price(Player player, StockSpec stock) {
        return new StockPriceHistory(
                player, stock.key(), stock.basePrice(), stock.basePrice(),
                stock.basePrice(), stock.basePrice(), 100L
        );
    }
}
