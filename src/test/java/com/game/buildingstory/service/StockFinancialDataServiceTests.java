package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyBondRepository;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
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
class StockFinancialDataServiceTests {
    @Mock private ListedCompanyFinancialService npcFinancialService;
    @Mock private ListedCompanyValuationService npcValuationService;
    @Mock private PlayerCompanyRepository companyRepository;
    @Mock private CompanyListingRepository listingRepository;
    @Mock private CompanyQuarterlyReportRepository reportRepository;
    @Mock private CompanyValuationSnapshotRepository valuationRepository;
    @Mock private CompanyBondRepository bondRepository;
    @Mock private Player player;
    @Mock private PlayerCompany company;
    @Mock private ListedCompany listedCompany;

    private StockFinancialDataService service;
    private StockSpec stock;

    @BeforeEach
    void setUp() {
        service = new StockFinancialDataService(
                npcFinancialService,
                npcValuationService,
                companyRepository,
                listingRepository,
                reportRepository,
                valuationRepository,
                bondRepository
        );
        stock = new StockSpec(
                CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                "IT",
                "플레이어 AI",
                StockRiskType.AGGRESSIVE,
                1.25,
                1.15,
                2.20,
                270_000L,
                13_333_334L,
                "플레이어 설립 AI 기업"
        );
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
    }

    @Test
    void playerCompanySnapshotUsesActualQuarterAndValuation() {
        when(company.getPlayer()).thenReturn(player);
        when(player.getYear()).thenReturn(2);
        when(player.getMonth()).thenReturn(5);
        CompanyQuarterlyReport latest = report(8, 900_000_000L, 180_000_000L);
        CompanyQuarterlyReport previous = report(7, 800_000_000L, 150_000_000L);
        CompanyValuationSnapshot valuation = mock(CompanyValuationSnapshot.class);
        CompanyListing listing = mock(CompanyListing.class);
        when(reportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of(latest, previous));
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(valuation.getEnterpriseValue()).thenReturn(4_000_000_200_000L);
        when(valuation.getAssetValue()).thenReturn(2_000_000_100_000L);
        when(valuation.getQuarterSequence()).thenReturn(8);
        when(listing.getQuarterSequenceAtListing()).thenReturn(7);
        when(company.getCorporateCash()).thenReturn(500_000_000_000L);
        when(bondRepository.findByCompanyAndStatusInOrderByIdAsc(company, List.of(
                com.game.buildingstory.domain.CompanyBondStatus.ACTIVE,
                com.game.buildingstory.domain.CompanyBondStatus.DEFAULTED))).thenReturn(List.of());

        StockFinancialSnapshot snapshot = service.snapshot(player, stock, listedCompany);

        assertThat(snapshot.revenue()).isEqualTo(900_000_000L);
        assertThat(snapshot.operatingProfit()).isEqualTo(180_000_000L);
        assertThat(snapshot.fairValueBase()).isEqualTo(300_000L);
        assertThat(snapshot.recentQuarters()).hasSize(2);
        assertThat(snapshot.periodText()).isEqualTo("2년 2분기 확정");
        assertThat(snapshot.recentQuarters())
                .extracting(StockFinancialSnapshot.Quarter::periodText)
                .containsExactly("2년 2분기 확정", "2년 1분기 확정");
    }

    @Test
    void newQuarterImpactIsConsumedOnlyOnce() {
        when(company.getPlayer()).thenReturn(player);
        CompanyQuarterlyReport latest = report(8, 900_000_000L, 180_000_000L);
        CompanyQuarterlyReport previous = report(7, 800_000_000L, 150_000_000L);
        CompanyListing listing = mock(CompanyListing.class);
        CompanyValuationSnapshot valuation = mock(CompanyValuationSnapshot.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(reportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of(latest, previous));
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(valuation.getEnterpriseValue()).thenReturn(4_000_000_200_000L);
        when(valuation.getQuarterSequence()).thenReturn(8);
        when(company.getIssuedShares()).thenReturn(13_333_334L);
        when(listing.getQuarterSequenceAtListing()).thenReturn(7);
        when(listing.markQuarterPriced(8)).thenReturn(true, false);

        StockFinancialDataService.PriceSignal first = service.consumePriceSignal(player, stock, listedCompany);
        StockFinancialDataService.PriceSignal second = service.consumePriceSignal(player, stock, listedCompany);

        assertThat(first.fairValue()).isEqualTo(300_000L);
        assertThat(first.earningsImpactBasisPoints()).isPositive();
        assertThat(second.earningsImpactBasisPoints()).isZero();
    }

    @Test
    void growingResultsStillCreateNegativeImpactWhenTheyMissPriorRunRate() {
        when(company.getPlayer()).thenReturn(player);
        CompanyQuarterlyReport latest = report(8, 900_000_000L, 180_000_000L, 300_000_000L);
        CompanyQuarterlyReport previous = report(7, 800_000_000L, 160_000_000L, 400_000_000L);
        CompanyListing listing = mock(CompanyListing.class);
        CompanyValuationSnapshot valuation = mock(CompanyValuationSnapshot.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(reportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of(latest, previous));
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(company.getIssuedShares()).thenReturn(13_333_334L);
        when(listing.getQuarterSequenceAtListing()).thenReturn(7);
        when(listing.markQuarterPriced(8)).thenReturn(true);

        StockFinancialDataService.PriceSignal signal = service.consumePriceSignal(player, stock, listedCompany);

        assertThat(latest.getRevenue()).isGreaterThan(previous.getRevenue());
        assertThat(latest.getOperatingProfit()).isGreaterThan(previous.getOperatingProfit());
        assertThat(signal.earningsImpactBasisPoints()).isNegative();
    }

    @Test
    void listingQuarterFairValueIncludesIpoProceeds() {
        CompanyListing listing = mock(CompanyListing.class);
        CompanyValuationSnapshot valuation = mock(CompanyValuationSnapshot.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(reportRepository.findByCompanyOrderByQuarterSequenceDesc(company)).thenReturn(List.of());
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(valuation.getQuarterSequence()).thenReturn(7);
        when(listing.getQuarterSequenceAtListing()).thenReturn(7);
        when(listing.getValuationAtListing()).thenReturn(3_000_000_000_000L);
        when(listing.getProceeds()).thenReturn(900_000_180_000L);
        when(company.getIssuedShares()).thenReturn(13_333_334L);

        StockFinancialDataService.PriceSignal signal =
                service.consumePriceSignal(player, stock, listedCompany);

        assertThat(signal.fairValue()).isEqualTo(292_499L);
        assertThat(signal.earningsImpactBasisPoints()).isZero();
    }

    @Test
    void dividendDecidedOnListingDayIsIncludedOnlyWhenItWasDecidedAfterListing() {
        when(company.getPlayer()).thenReturn(player);
        when(player.getElapsedDays()).thenReturn(100);
        CompanyQuarterlyReport beforeListing = report(8, 800_000_000L, 150_000_000L);
        beforeListing.decideDividend(10, 10_000_000L);
        CompanyQuarterlyReport afterListing = report(9, 900_000_000L, 180_000_000L);
        afterListing.decideDividend(10, 12_000_000L);
        CompanyListing listing = mock(CompanyListing.class);
        when(listing.getListedElapsedDay()).thenReturn(100);
        when(listing.getLastDividendQuarterSequenceAtListing()).thenReturn(8);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(reportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of(afterListing, beforeListing));

        var events = service.reportEvents(player, stock, listedCompany);

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.publishedElapsedDay()).isZero();
            assertThat(event.dividendElapsedDay()).isEqualTo(100);
        });
    }

    private CompanyQuarterlyReport report(int sequence, long revenue, long operatingProfit) {
        return report(sequence, revenue, operatingProfit, revenue / 3);
    }

    private CompanyQuarterlyReport report(
            int sequence,
            long revenue,
            long operatingProfit,
            long recurringRevenueAtEnd
    ) {
        return new CompanyQuarterlyReport(
                company,
                sequence,
                sequence * 3,
                revenue,
                revenue - operatingProfit,
                operatingProfit,
                0,
                operatingProfit * 8 / 10,
                500_000_000L,
                recurringRevenueAtEnd,
                100_000L
        );
    }
}
