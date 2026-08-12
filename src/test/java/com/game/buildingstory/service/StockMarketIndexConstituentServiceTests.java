package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMarketIndexConstituentServiceTests {
    @Mock private StockCatalog stockCatalog;
    @Mock private NpcCompanyListingRepository npcListingRepository;
    @Mock private ListedCompanyRepository listedCompanyRepository;
    @Mock private ListedCompanyQuarterlyReportRepository listedReportRepository;
    @Mock private PlayerCompanyRepository playerCompanyRepository;
    @Mock private CompanyListingRepository companyListingRepository;
    @Mock private CompanyQuarterlyReportRepository companyReportRepository;
    @Mock private StockUniverseService stockUniverseService;

    private StockMarketIndexConstituentService service;

    @BeforeEach
    void setUp() {
        service = new StockMarketIndexConstituentService(
                stockCatalog,
                npcListingRepository,
                listedCompanyRepository,
                listedReportRepository,
                playerCompanyRepository,
                companyListingRepository,
                companyReportRepository,
                stockUniverseService
        );
    }

    @Test
    void playerCompanyJoinsAfterTwoPostListingQuarterlyReports() {
        Player player = new Player("index-player-company", "hash");
        PlayerCompany company = mock(PlayerCompany.class);
        CompanyListing listing = mock(CompanyListing.class);
        StockSpec playerStock = new StockSpec(
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                "IT",
                "플레이어 AI",
                StockRiskType.AGGRESSIVE,
                1.25,
                1.15,
                2.20,
                100_000L,
                1_000_000L,
                "플레이어 상장기업"
        );

        when(stockCatalog.initial()).thenReturn(List.of());
        when(npcListingRepository.findByPlayerAndStage(player,
                com.game.buildingstory.domain.NpcCompanyListingStage.LISTED)).thenReturn(List.of());
        when(playerCompanyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(companyListingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getStatus()).thenReturn(CompanyListingStatus.LISTED);
        when(listing.getListedElapsedDay()).thenReturn(100);
        when(listing.getQuarterSequenceAtListing()).thenReturn(4);
        when(listing.getStockKey()).thenReturn(CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY);
        when(stockUniverseService.find(player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY))
                .thenReturn(Optional.of(playerStock));
        when(companyReportRepository
                .countByCompanyAndQuarterSequenceGreaterThanAndPublishedElapsedDayLessThanEqual(
                        company, 4, 300))
                .thenReturn(1L, 2L);

        assertThat(service.currentAt(player, 300)).doesNotContain(playerStock);
        assertThat(service.currentAt(player, 300)).contains(playerStock);
    }
}
