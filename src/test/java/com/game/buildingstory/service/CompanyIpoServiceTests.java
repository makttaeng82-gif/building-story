package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyIpoServiceTests {
    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerCompanyRepository companyRepository;
    @Mock private CompanyListingRepository listingRepository;
    @Mock private CompanyIpoQualificationService qualificationService;
    @Mock private CompanyWorkforceService workforceService;
    @Mock private CompanyCashLedgerService cashLedgerService;
    @Mock private CompanyValuationSnapshotRepository valuationRepository;
    @Mock private CompanyQuarterlyReportRepository quarterlyReportRepository;
    @Mock private ListedCompanyRepository listedCompanyRepository;
    @Mock private StockPriceHistoryRepository priceHistoryRepository;
    @Mock private CompanyNewsService companyNewsService;
    @Mock private Player player;
    @Mock private PlayerCompany company;
    @InjectMocks private CompanyIpoService ipoService;

    @Test
    void applicationReservesWorkChargesCostAndStoresOneListing() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(qualificationService.evaluate(company)).thenReturn(eligibleQualification());
        when(company.getIssuedShares()).thenReturn(10_000_000L);
        when(company.getPlayerShares()).thenReturn(10_000_000L);
        when(company.getCorporateCash()).thenReturn(CompanyIpoPolicy.PREPARATION_COST);
        when(workforceService.canReserveMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        )).thenReturn(true);
        when(player.getElapsedDays()).thenReturn(730);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());
        when(cashLedgerService.withdraw(
                any(), any(), any(), any(), any(Long.class))).thenReturn(true);

        String result = ipoService.apply(1L, 25);

        assertThat(result).contains("IPO 신청 완료", "25%");
        verify(workforceService).reserveMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        verify(cashLedgerService).withdraw(
                company,
                "ipo:preparation:1",
                CompanyCashFlowType.FINANCING,
                "IPO 준비비",
                CompanyIpoPolicy.PREPARATION_COST
        );
        ArgumentCaptor<CompanyListing> listingCaptor = ArgumentCaptor.forClass(CompanyListing.class);
        verify(listingRepository).save(listingCaptor.capture());
        assertThat(listingCaptor.getValue().getStatus()).isEqualTo(CompanyListingStatus.PREPARING);
        verify(companyNewsService).recordIpoApplication(company, listingCaptor.getValue());
    }

    @Test
    void overviewUsesTheSamePolicyForEveryOfferOption() {
        CompanyValuationSnapshot valuation = org.mockito.Mockito.mock(CompanyValuationSnapshot.class);
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());
        when(qualificationService.evaluate(company)).thenReturn(eligibleQualification());
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(valuation.getEnterpriseValue()).thenReturn(3_000_000_000_000L);
        when(company.getIssuedShares()).thenReturn(10_000_000L);
        when(company.getPlayerShares()).thenReturn(10_000_000L);

        CompanyIpoOverview overview = ipoService.overview(company);

        assertThat(overview.status()).isNull();
        assertThat(overview.offerOptions()).extracting(CompanyIpoOverview.OfferOption::percent)
                .containsExactly(15, 25, 35);
        assertThat(overview.offerOptions()).allSatisfy(option -> {
            assertThat(option.offerPrice()).isEqualTo(270_000L);
            assertThat(option.proceeds()).isEqualTo(
                    CompanyIpoPolicy.calculateProceeds(option.newShares(), option.offerPrice()));
        });
        assertThat(overview.offerOptions().get(1).playerOwnershipPercent())
                .isBetween(74.99, 75.01);
    }

    @Test
    void overviewTreatsMissingStrategyFinanceDepartmentAsUnavailableCapacity() {
        var qualification = new CompanyIpoQualification(List.of(
                new CompanyIpoQualification.RequirementCheck(
                        CompanyIpoRequirement.STRATEGY_FINANCE,
                        0,
                        CompanyIpoPolicy.MINIMUM_STRATEGY_EXPERTISE,
                        false
                )
        ));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.empty());
        when(qualificationService.evaluate(company)).thenReturn(qualification);
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.empty());

        CompanyIpoOverview overview = ipoService.overview(company);

        assertThat(overview.workCapacityAvailable()).isFalse();
        verify(workforceService, never()).canReserveMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
    }

    @Test
    void unmetQualificationDoesNotReserveWorkOrChargeCash() {
        var unmet = new CompanyIpoQualification.RequirementCheck(
                CompanyIpoRequirement.PROFITABILITY, 1, 2, false);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(qualificationService.evaluate(company))
                .thenReturn(new CompanyIpoQualification(List.of(unmet)));

        String result = ipoService.apply(1L, 25);

        assertThat(result).contains("조건 미충족");
        verify(workforceService, never()).reserveMajorWork(any(), any(), any(Integer.class));
        verify(cashLedgerService, never()).withdraw(any(), any(), any(), any(), any(Long.class));
    }

    @Test
    void applicationRejectsOfferThatWouldReducePlayerOwnershipBelowTwentyPercent() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(qualificationService.evaluate(company)).thenReturn(eligibleQualification());
        when(company.getIssuedShares()).thenReturn(100L);
        when(company.getPlayerShares()).thenReturn(21L);

        String result = ipoService.apply(1L, 15);

        assertThat(result).contains("20% 미만");
        verify(workforceService, never()).reserveMajorWork(any(), any(), any(Integer.class));
        verify(cashLedgerService, never()).withdraw(any(), any(), any(), any(), any(Long.class));
    }

    @Test
    void preparationBecomesReadyAfterExactlySixSuccessfulMonths() {
        CompanyListing listing = listing();
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(qualificationService.evaluateForFinalReview(company)).thenReturn(eligibleQualification());

        for (int month = 1; month < CompanyIpoPolicy.PREPARATION_MONTHS; month++) {
            assertThat(ipoService.processSuccessfulMonth(company)).contains(month + "/6개월");
            assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.PREPARING);
        }
        assertThat(ipoService.processSuccessfulMonth(company)).contains("최종 상장 확정 가능");
        assertThat(listing.getPreparationMonthsCompleted()).isEqualTo(6);
        assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.READY);
        verify(companyNewsService).recordIpoReady(company, listing);
        assertThat(ipoService.processSuccessfulMonth(company)).isEmpty();
    }

    @Test
    void cancellationReleasesReservedWorkWithoutRefundingPreparationCost() {
        CompanyListing listing = listing();
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));

        String result = ipoService.cancel(1L);

        assertThat(result).contains("준비비는 반환되지 않음");
        assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.CANCELLED);
        verify(workforceService).releaseMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        verify(cashLedgerService, never()).deposit(any(), any(), any(), any(), any(Long.class));
    }

    @Test
    void cancelledListingCanRestartWithAUniqueLedgerSequence() {
        CompanyListing listing = listing();
        listing.cancel();

        listing.restartApplication(35, 730, CompanyIpoPolicy.PREPARATION_COST);

        assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.PREPARING);
        assertThat(listing.getApplicationSequence()).isEqualTo(2);
        assertThat(listing.getSelectedOfferPercent()).isEqualTo(35);
        assertThat(listing.getPreparationMonthsCompleted()).isZero();
    }

    @Test
    void listingConfirmationIssuesOnlyPublicSharesAndDepositsProceedsToCompany() {
        CompanyListing listing = listing();
        for (int month = 0; month < CompanyIpoPolicy.PREPARATION_MONTHS; month++) {
            listing.advancePreparationMonth(CompanyIpoPolicy.PREPARATION_MONTHS);
        }
        CompanyValuationSnapshot valuation = org.mockito.Mockito.mock(CompanyValuationSnapshot.class);
        when(valuation.getEnterpriseValue()).thenReturn(3_000_000_000_000L);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(companyRepository.findByPlayer(player)).thenReturn(Optional.of(company));
        when(listingRepository.findByCompany(company)).thenReturn(Optional.of(listing));
        when(qualificationService.evaluateForFinalReview(company)).thenReturn(eligibleQualification());
        when(listedCompanyRepository.findByPlayerAndStockKey(
                player, CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY)).thenReturn(Optional.empty());
        when(valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(Optional.of(valuation));
        when(quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company))
                .thenReturn(List.of());
        when(company.getIssuedShares()).thenReturn(10_000_000L);
        when(company.getPlayerShares()).thenReturn(9_000_000L);
        when(player.getElapsedDays()).thenReturn(900);
        when(cashLedgerService.deposit(
                company,
                "ipo:proceeds:1",
                CompanyCashFlowType.FINANCING,
                "IPO 공모대금",
                900_000_180_000L
        )).thenReturn(true);

        String result = ipoService.confirmListing(1L);

        assertThat(result).contains("상장 완료");
        assertThat(listing.getStatus()).isEqualTo(CompanyListingStatus.LISTED);
        assertThat(listing.getOfferPrice()).isEqualTo(270_000L);
        assertThat(listing.getNewShares()).isEqualTo(3_333_334L);
        assertThat(listing.getProceeds()).isEqualTo(900_000_180_000L);
        verify(company).issueExternalShares(3_333_334L);
        ArgumentCaptor<ListedCompany> companyCaptor = ArgumentCaptor.forClass(ListedCompany.class);
        verify(listedCompanyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getFounderShares()).isEqualTo(9_000_000L);
        assertThat(companyCaptor.getValue().getInstitutionalShares()).isEqualTo(1_000_000L);
        assertThat(companyCaptor.getValue().getMarketParticipantShares()).isEqualTo(3_333_334L);
        ArgumentCaptor<StockPriceHistory> priceCaptor = ArgumentCaptor.forClass(StockPriceHistory.class);
        verify(priceHistoryRepository).save(priceCaptor.capture());
        assertThat(priceCaptor.getValue().getClosePrice()).isEqualTo(270_000L);
        verify(workforceService).releaseMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        verify(companyNewsService).recordIpoListing(company, listing);
    }

    private CompanyIpoQualification eligibleQualification() {
        return new CompanyIpoQualification(List.of());
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
}
