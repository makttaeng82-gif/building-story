package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.CompanyNewsCategory;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyNewsArticleRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.web.MoneyText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyNewsServiceTests {
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerCompanyRepository companyRepository;
    @Mock private CompanyNewsArticleRepository newsRepository;
    @Mock private CompanyServiceIncidentService incidentService;
    @Mock private CompanyQuarterlyReportRepository quarterlyRepository;
    @Mock private MoneyText moneyText;
    @Mock private PlayerCompanyStockNewsBridgeService stockNewsBridgeService;
    @Mock private PlayerCompany company;
    @Mock private Player player;

    private CompanyNewsService service;

    @BeforeEach
    void setUp() {
        service = new CompanyNewsService(
                playerRepository,
                companyRepository,
                newsRepository,
                incidentService,
                quarterlyRepository,
                moneyText,
                stockNewsBridgeService
        );
    }

    @Test
    void ipoApplicationCreatesFinanceNewsWithoutPublishingToStockMarket() {
        CompanyListing listing = listing();
        when(newsRepository.findByCompanyAndEventKey(company, "ipo-application-1"))
                .thenReturn(Optional.empty());
        when(newsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(company.getCompanyName()).thenReturn("플레이어 AI");
        when(company.getPlayer()).thenReturn(player);
        when(company.getMarketMonthsProcessed()).thenReturn(24);
        when(moneyText.format(CompanyIpoPolicy.PREPARATION_COST)).thenReturn("120억원");

        service.recordIpoApplication(company, listing);

        ArgumentCaptor<CompanyNewsArticle> captor = ArgumentCaptor.forClass(CompanyNewsArticle.class);
        verify(newsRepository).save(captor.capture());
        assertThat(captor.getValue().getEventKey()).isEqualTo("ipo-application-1");
        assertThat(captor.getValue().getCategory()).isEqualTo(CompanyNewsCategory.FINANCE);
        assertThat(captor.getValue().getTitle()).contains("기업공개 절차 착수");
        verify(stockNewsBridgeService, never()).publish(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void completedListingPublishesOneNeutralStockNewsArticle() {
        CompanyListing listing = listedListing();
        when(newsRepository.findByCompanyAndEventKey(company, "ipo-listed-1"))
                .thenReturn(Optional.empty());
        when(newsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(company.getCompanyName()).thenReturn("플레이어 AI");
        when(company.getPlayer()).thenReturn(player);
        when(company.getMarketMonthsProcessed()).thenReturn(30);
        when(moneyText.format(listing.getOfferPrice())).thenReturn("27만원");
        when(moneyText.format(listing.getProceeds())).thenReturn("9000억원");

        service.recordIpoListing(company, listing);

        ArgumentCaptor<CompanyNewsArticle> captor = ArgumentCaptor.forClass(CompanyNewsArticle.class);
        verify(newsRepository).save(captor.capture());
        CompanyNewsArticle article = captor.getValue();
        assertThat(article.getCategory()).isEqualTo(CompanyNewsCategory.FINANCE);
        assertThat(article.getBody()).contains("3,333,334주", "9000억원");
        verify(stockNewsBridgeService).publish(article, StockNewsDirection.NEUTRAL, 0, 0);
    }

    @Test
    void duplicateListingEventDoesNotCreateOrPublishAnotherArticle() {
        CompanyListing listing = listedListing();
        when(newsRepository.findByCompanyAndEventKey(company, "ipo-listed-1"))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(CompanyNewsArticle.class)));

        service.recordIpoListing(company, listing);

        verify(newsRepository, never()).save(any());
        verify(stockNewsBridgeService, never()).publish(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void dividendDecisionIsMergedIntoExistingPerformanceArticle() {
        CompanyQuarterlyReport report = mock(CompanyQuarterlyReport.class);
        CompanyNewsArticle performance = mock(CompanyNewsArticle.class);
        when(report.getQuarterSequence()).thenReturn(4);
        when(report.getDividendAmount()).thenReturn(12_000_000L);
        when(report.getDividendRate()).thenReturn(20);
        when(company.getIssuedShares()).thenReturn(100_000L);
        when(moneyText.format(120L)).thenReturn("120원");
        when(moneyText.format(12_000_000L)).thenReturn("1200만원");
        when(newsRepository.findByCompanyAndEventKey(company, "performance-4"))
                .thenReturn(Optional.of(performance));

        service.recordDividendDecision(company, report);

        verify(performance).appendUpdate(anyString());
        verify(newsRepository, never()).save(any());
    }

    @Test
    void backfillRepairsHistoricalPerformanceDateAndMergesDividendArticle() {
        CompanyQuarterlyReport report = mock(CompanyQuarterlyReport.class);
        CompanyNewsArticle performance = mock(CompanyNewsArticle.class);
        CompanyNewsArticle dividend = mock(CompanyNewsArticle.class);
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company)).thenReturn(List.of(report));
        when(report.getQuarterSequence()).thenReturn(1);
        when(report.getEndingPeriodIndex()).thenReturn(0);
        when(company.getPlayer()).thenReturn(player);
        when(company.getEstablishedElapsedDay()).thenReturn(15);
        when(company.getCompanyName()).thenReturn("플레이어 AI");
        when(player.getElapsedDays()).thenReturn(100);
        when(performance.getBody()).thenReturn("분기 실적 본문");
        when(newsRepository.findByCompanyAndEventKey(company, "performance-1"))
                .thenReturn(Optional.of(performance));
        when(newsRepository.findByCompanyAndEventKey(company, "dividend-1"))
                .thenReturn(Optional.of(dividend));

        service.backfillPerformanceArticles();

        verify(performance).revisePublication(anyString(), anyString(), eq(3), eq(15));
        verify(performance).mergeUpdate(dividend);
        verify(newsRepository).delete(dividend);
    }

    private CompanyListing listing() {
        return new CompanyListing(
                company,
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                25,
                730,
                CompanyIpoPolicy.PREPARATION_COST
        );
    }

    private CompanyListing listedListing() {
        CompanyListing listing = listing();
        for (int month = 0; month < CompanyIpoPolicy.PREPARATION_MONTHS; month++) {
            listing.advancePreparationMonth(CompanyIpoPolicy.PREPARATION_MONTHS);
        }
        listing.completeListing(
                3_000_000_000_000L,
                270_000L,
                3_333_334L,
                900_000_180_000L,
                900,
                8,
                -1
        );
        return listing;
    }
}
