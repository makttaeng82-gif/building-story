package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyBondRepository;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyComputeConstructionRepository;
import com.game.buildingstory.repo.CompanyCustomerContractRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyFinanceServiceTests {
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerCompanyRepository companyRepository;
    @Mock private CompanyQuarterlyReportRepository quarterlyRepository;
    @Mock private CompanyValuationSnapshotRepository valuationRepository;
    @Mock private CompanyBondRepository bondRepository;
    @Mock private CompanyComputeConstructionRepository constructionRepository;
    @Mock private CompanyCompetitorRepository competitorRepository;
    @Mock private CompanyCustomerContractRepository contractRepository;
    @Mock private CompanyServiceIncidentRepository incidentRepository;
    @Mock private CompanyCashLedgerService cashLedgerService;
    @Mock private PlayerCompanyStockDividendService stockDividendService;
    @Mock private CompanyNewsService newsService;
    @Mock private CompanyListingBenefitService listingBenefitService;
    @Mock private ListedCompanyRepository listedCompanyRepository;
    @InjectMocks private CompanyFinanceService service;

    @Test
    void contributionAfterListingUpdatesFounderSharesAndIssuedSharesTogether() {
        Player player = new Player("finance-sync", "1234");
        PlayerCompany company = new PlayerCompany(
                player, "플레이어 AI", "인공지능 AI 플랫폼",
                1_000_000_000L, 1_000_000_000L, 1_000_000L, 0);
        ListedCompany listedCompany = new ListedCompany(
                player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                1_250_000L, 1_000_000L, 0, 250_000L, 0, 0);
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.empty());
        when(listedCompanyRepository.findByPlayerAndStockKey(
                player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY))
                .thenReturn(Optional.of(listedCompany));

        long newShares = service.applyPlayerContribution(company, 100_000_000L);

        assertThat(newShares).isEqualTo(100_000L);
        assertThat(company.getIssuedShares()).isEqualTo(1_100_000L);
        assertThat(company.getPlayerShares()).isEqualTo(1_100_000L);
        assertThat(listedCompany.getIssuedShares()).isEqualTo(1_350_000L);
        assertThat(listedCompany.getFounderShares()).isEqualTo(1_100_000L);
        assertThat(listedCompany.hasConservedShares(0)).isTrue();
    }
}
