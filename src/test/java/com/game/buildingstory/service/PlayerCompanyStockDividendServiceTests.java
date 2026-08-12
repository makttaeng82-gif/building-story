package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerCompanyStockDividendServiceTests {
    @Mock private CompanyListingRepository listingRepository;
    @Mock private StockPriceHistoryRepository priceRepository;
    @Mock private PlayerCompany company;
    @Mock private Player player;
    @Mock private CompanyListing listing;

    private PlayerCompanyStockDividendService service;

    @BeforeEach
    void setUp() {
        service = new PlayerCompanyStockDividendService(listingRepository, priceRepository);
    }

    @Test
    void unlistedCompanyDividendDoesNotChangeStockPrice() {
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());

        assertThat(service.applyExDividend(company, 100_000L)).isZero();

        verify(priceRepository, never())
                .findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void listedCompanyDividendAppliesPerShareExDividendPrice() {
        StockPriceHistory price = new StockPriceHistory(
                player,
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                1_000,
                1_000,
                1_000,
                1_000,
                100
        );
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);
        when(company.getIssuedShares()).thenReturn(1_000L);
        when(company.getPlayer()).thenReturn(player);
        when(priceRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(
                player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY)).thenReturn(Optional.of(price));

        long dividendPerShare = service.applyExDividend(company, 100_000L);

        assertThat(dividendPerShare).isEqualTo(100L);
        assertThat(price.getClosePrice()).isEqualTo(900L);
        assertThat(price.getOpenPrice()).isEqualTo(900L);
    }
}
