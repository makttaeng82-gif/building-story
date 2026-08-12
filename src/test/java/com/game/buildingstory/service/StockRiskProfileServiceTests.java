package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockRiskProfileServiceTests {
    @Mock private ListedCompanyRepository listedCompanyRepository;
    @Mock private ListedCompanyQuarterlyReportRepository listedReportRepository;
    @Mock private PlayerCompanyRepository playerCompanyRepository;
    @Mock private CompanyQuarterlyReportRepository playerReportRepository;
    @Mock private CompanyListingRepository listingRepository;
    @Mock private Player player;
    @Mock private PlayerCompany company;
    @Mock private CompanyListing listing;
    @Mock private ListedCompany listedCompany;
    @Mock private ListedCompanyQuarterlyReport listedReport;

    private StockRiskProfileService service;
    private StockSpec playerStock;

    @BeforeEach
    void setUp() {
        service = new StockRiskProfileService(
                listedCompanyRepository,
                listedReportRepository,
                playerCompanyRepository,
                playerReportRepository,
                listingRepository
        );
        playerStock = new StockSpec(
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                "IT",
                "플레이어 AI",
                StockRiskType.AGGRESSIVE,
                1.25,
                1.15,
                2.20,
                270_000,
                13_333_334,
                "플레이어 설립 AI 기업"
        );
    }

    @Test
    void newlyListedPlayerCompanyHasHigherPriceSensitivity() {
        when(playerCompanyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getListedElapsedDay()).thenReturn(700);
        when(player.getElapsedDays()).thenReturn(730);
        when(company.getTechnicalDebt()).thenReturn(30);
        when(playerReportRepository.findByCompanyOrderByQuarterSequenceDesc(company)).thenReturn(List.of());

        StockSpec adjusted = service.currentSpec(player, playerStock);

        assertThat(adjusted.beta()).isEqualTo(1.45);
        assertThat(adjusted.idiosyncraticVolatilityPercent()).isEqualTo(3.10);
    }

    @Test
    void suspendedCompanyRiskIsCappedAtConfiguredMaximum() {
        when(playerCompanyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(listing.getListedElapsedDay()).thenReturn(700);
        when(player.getElapsedDays()).thenReturn(700);
        when(company.isOperationsSuspended()).thenReturn(true);
        when(company.getTechnicalDebt()).thenReturn(80);
        when(playerReportRepository.findByCompanyOrderByQuarterSequenceDesc(company)).thenReturn(List.of());

        StockSpec adjusted = service.currentSpec(player, playerStock);

        assertThat(adjusted.beta()).isEqualTo(1.82);
        assertThat(adjusted.idiosyncraticVolatilityPercent()).isEqualTo(4.80);
    }

    @Test
    void indebtedLossMakingNpcCompanyBecomesMoreVolatile() {
        StockSpec base = new StockSpec(
                "npc", "IT", "NPC", StockRiskType.NORMAL,
                1.00, 1.00, 1.50, 100_000, 10_000_000, "NPC company"
        );
        when(listedCompanyRepository.findByPlayerAndStockKey(player, "npc"))
                .thenReturn(Optional.of(listedCompany));
        when(listedCompany.isFinancialInitialized()).thenReturn(true);
        when(listedCompany.getCash()).thenReturn(20L);
        when(listedCompany.getNonCashAssets()).thenReturn(80L);
        when(listedCompany.getDebt()).thenReturn(60L);
        when(listedCompany.getAnnualGrowthBasisPoints()).thenReturn(1_300);
        when(listedReportRepository.findTop4ByListedCompanyOrderByFiscalPeriodIndexDesc(listedCompany))
                .thenReturn(List.of(listedReport));
        when(listedReport.getNetIncome()).thenReturn(-1L);

        StockSpec adjusted = service.currentSpec(player, base);

        assertThat(adjusted.beta()).isEqualTo(1.30);
        assertThat(adjusted.idiosyncraticVolatilityPercent()).isEqualTo(2.65);
    }
}
