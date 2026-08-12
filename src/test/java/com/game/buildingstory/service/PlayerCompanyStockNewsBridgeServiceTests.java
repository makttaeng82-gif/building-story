package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.CompanyNewsCategory;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerCompanyStockNewsBridgeServiceTests {
    @Mock private CompanyListingRepository listingRepository;
    @Mock private StockNewsArticleRepository stockNewsRepository;
    @Mock private PlayerCompany company;
    @Mock private Player player;
    @Mock private CompanyListing listing;

    private PlayerCompanyStockNewsBridgeService service;
    private CompanyNewsArticle source;

    @BeforeEach
    void setUp() {
        service = new PlayerCompanyStockNewsBridgeService(listingRepository, stockNewsRepository);
        when(company.getPlayer()).thenReturn(player);
        source = new CompanyNewsArticle(
                company,
                "product-12-model-upgrade",
                CompanyNewsCategory.PRODUCT,
                "플레이어 AI, 모델 개선 완료",
                "신규 모델이 상용 서비스에 반영됐다.",
                "회사 공시",
                12
        );
    }

    @Test
    void unlistedCompanyNewsIsNotPublishedToStockMarket() {
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());

        service.publish(source, StockNewsDirection.POSITIVE, 80, 1);

        verify(stockNewsRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listedCompanyNewsIsPublishedOnceWithPlayerStockKey() {
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);
        when(company.getPlayer()).thenReturn(player);
        when(company.getCompanyName()).thenReturn("플레이어 AI");
        when(stockNewsRepository.existsByPlayerAndEventKey(
                player, "player-company:product-12-model-upgrade")).thenReturn(false);

        service.publish(source, StockNewsDirection.POSITIVE, 80, 1);

        ArgumentCaptor<StockNewsArticle> captor = ArgumentCaptor.forClass(StockNewsArticle.class);
        verify(stockNewsRepository).save(captor.capture());
        StockNewsArticle article = captor.getValue();
        assertThat(article.getStockKey()).isEqualTo(CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY);
        assertThat(article.getCategory()).isEqualTo(StockNewsCategory.COMPANY);
        assertThat(article.getDirection()).isEqualTo(StockNewsDirection.POSITIVE);
        assertThat(article.getPriceImpactBasisPoints()).isEqualTo(80);
        assertThat(article.getRemainingPriceRefreshes()).isEqualTo(1);
        assertThat(article.isResolved()).isTrue();
    }

    @Test
    void duplicateCompanyEventIsNotPublishedTwice() {
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);
        when(company.getPlayer()).thenReturn(player);
        when(stockNewsRepository.existsByPlayerAndEventKey(
                player, "player-company:product-12-model-upgrade")).thenReturn(true);

        service.publish(source, StockNewsDirection.POSITIVE, 80, 1);

        verify(stockNewsRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
