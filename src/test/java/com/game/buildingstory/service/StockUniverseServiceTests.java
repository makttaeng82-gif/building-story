package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class StockUniverseServiceTests {
    private final StockCatalog stockCatalog = new StockCatalog();
    private final PlayerCompanyRepository companyRepository = mock(PlayerCompanyRepository.class);
    private final CompanyListingRepository listingRepository = mock(CompanyListingRepository.class);
    private final StockRiskProfileService riskProfileService = mock(StockRiskProfileService.class);
    private final NpcCompanyListingRepository npcListingRepository = mock(NpcCompanyListingRepository.class);
    private final StockUniverseService stockUniverseService = new StockUniverseService(
            stockCatalog, companyRepository, listingRepository, riskProfileService, npcListingRepository);

    @Test
    void addsPlayerCompanyOnlyAfterListingIsComplete() {
        when(riskProfileService.currentSpec(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(npcListingRepository.findByPlayerAndStage(any(), any())).thenReturn(java.util.List.of());
        Player player = mock(Player.class);
        PlayerCompany company = mock(PlayerCompany.class);
        when(company.getCompanyName()).thenReturn("플레이어 AI");
        when(company.getServiceName()).thenReturn("인공지능 AI 플랫폼");
        when(company.getIssuedShares()).thenReturn(13_333_334L);
        CompanyListing listing = new CompanyListing(
                company,
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                25,
                730,
                CompanyIpoPolicy.PREPARATION_COST
        );
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));

        assertThat(stockUniverseService.stocks(player)).hasSize(stockCatalog.initial().size());

        for (int month = 0; month < CompanyIpoPolicy.PREPARATION_MONTHS; month++) {
            listing.advancePreparationMonth(CompanyIpoPolicy.PREPARATION_MONTHS);
        }
        listing.completeListing(3_000_000_000_000L, 270_000L, 3_333_334L,
                900_000_180_000L, 900, 8, -1);

        assertThat(stockUniverseService.stocks(player)).hasSize(stockCatalog.initial().size() + 1);
        assertThat(stockUniverseService.find(player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY))
                .get()
                .satisfies(stock -> {
                    assertThat(stock.name()).isEqualTo("플레이어 AI");
                    assertThat(stock.basePrice()).isEqualTo(270_000L);
                    assertThat(stock.issuedShares()).isEqualTo(13_333_334L);
                });
    }
}
