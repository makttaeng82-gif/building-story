package com.game.buildingstory.service;

import com.game.buildingstory.domain.NpcCompanyListingStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:npc-listing-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class NpcCompanyListingServiceIntegrationTests {
    @Autowired private NpcCompanyListingService listingService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private NpcCompanyListingRepository listingRepository;
    @Autowired private ListedCompanyRepository companyRepository;
    @Autowired private ListedCompanyQuarterlyReportRepository reportRepository;
    @Autowired private StockNewsArticleRepository newsRepository;
    @Autowired private StockPriceHistoryRepository priceRepository;
    @Autowired private OwnedStockRepository ownedStockRepository;
    @Autowired private NpcIpoSubscriptionService subscriptionService;
    @Autowired private ListedCompanyFinancialService financialService;
    @Autowired private StockService stockService;
    @Autowired private StockMarketIndexConstituentService constituentService;
    @Autowired private StockMarketIndexService marketIndexService;
    @Autowired private MockMvc mockMvc;

    @Test
    void firstCandidateAnnouncementCreatesFinancialHistoryWithoutListingTheStock() {
        Player player = new Player("npc-listing-test", "hash");
        player.scheduleStockUnlock(1);
        player.unlockStockContent();
        playerRepository.save(player);

        listingService.process(player);
        assertThat(listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player)).hasSize(16);

        var first = listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player).getFirst();
        while (player.getElapsedDays() < first.getTargetElapsedDay() - 120) {
            player.advanceDay();
        }
        listingService.process(player);

        assertThat(first.getStage()).isEqualTo(NpcCompanyListingStage.ANNOUNCED);
        var company = companyRepository.findByPlayerAndStockKey(player, first.getStockKey()).orElseThrow();
        assertThat(reportRepository.findByListedCompanyOrderByFiscalPeriodIndexDesc(company)).hasSize(4);
        assertThat(newsRepository.existsByPlayerAndEventKey(
                player, "npc_ipo:" + first.getStockKey() + ":announce:0")).isTrue();

        int finalCheckDay = first.getTargetElapsedDay() + 181;
        while (player.getElapsedDays() <= finalCheckDay && !first.isListed()) {
            player.advanceDay();
            listingService.process(player);
        }

        assertThat(first.isListed()).isTrue();
        assertThat(first.getOfferPrice()).isPositive();
        var firstCandle = priceRepository
                .findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, first.getStockKey())
                .orElseThrow();
        assertThat(firstCandle.getListingImpactBasisPoints()).isBetween(-1_800, 1_800);
        assertThat(firstCandle.getHighPrice()).isGreaterThanOrEqualTo(
                Math.max(firstCandle.getOpenPrice(), firstCandle.getClosePrice()));
        assertThat(firstCandle.getLowPrice()).isLessThanOrEqualTo(
                Math.min(firstCandle.getOpenPrice(), firstCandle.getClosePrice()));
    }

    @Test
    void subscriptionReservesCashAndListingAllocatesSharesThenRefundsTheRemainder() throws Exception {
        Player player = new Player("npc-subscription-test", "hash");
        player.completeStory();
        player.scheduleStockUnlock(1);
        player.unlockStockContent();
        player.addSecuritiesCash(10_000_000_000L);
        playerRepository.save(player);

        listingService.process(player);
        var first = listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player).getFirst();
        while (first.getStage() != NpcCompanyListingStage.OFFER_CONFIRMED) {
            player.advanceDay();
            listingService.process(player);
        }
        assertThat(first.getTargetElapsedDay() - player.getElapsedDays()).isEqualTo(30);

        mockMvc.perform(get("/main?view=stocks").sessionAttr("PLAYER_ID", player.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-stock-ipo-dialog")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("청약 판단 지표")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("최근 4개 분기")));

        long cashBefore = player.getSecuritiesCash();
        var subscriptionView = subscriptionService.active(player).orElseThrow();
        assertThat(subscriptionView.recentQuarters()).hasSize(4);
        assertThat(subscriptionView.offerDiscountText()).contains("할인");
        assertThat(subscriptionView.tradableSharesText()).contains("%");
        assertThat(subscriptionView.operatingMarginText()).contains("%");
        assertThat(subscriptionView.debtRatioText()).contains("%");
        assertThat(subscriptionService.subscribe(player, first.getStockKey(), 100)).contains("청약 신청");
        assertThat(first.hasSubscription()).isTrue();
        assertThat(player.getSecuritiesCash()).isEqualTo(
                cashBefore - first.getOfferPrice() * 100 - NpcIpoSubscriptionService.SUBSCRIPTION_FEE
        );

        while (!first.isListed()) {
            player.advanceDay();
            listingService.process(player);
        }

        assertThat(first.isSubscriptionSettled()).isTrue();
        assertThat(first.getSubscriptionAllocatedQuantity()).isBetween(1L, 100L);
        assertThat(first.getSubscriptionRefundAmount()).isEqualTo(
                (100 - first.getSubscriptionAllocatedQuantity()) * first.getOfferPrice()
        );
        assertThat(ownedStockRepository.findByPlayerAndStockKey(player, first.getStockKey()))
                .get().extracting(owned -> owned.getQuantity())
                .isEqualTo(first.getSubscriptionAllocatedQuantity());
        assertThat(player.getSecuritiesCash()).isEqualTo(
                cashBefore
                        - first.getSubscriptionAllocatedQuantity() * first.getOfferPrice()
                        - NpcIpoSubscriptionService.SUBSCRIPTION_FEE
        );
        assertThat(subscriptionService.pendingResult(player)).isPresent();
        assertThat(subscriptionService.acknowledgeResult(player, first.getStockKey())).isTrue();
        assertThat(subscriptionService.pendingResult(player)).isEmpty();
    }

    @Test
    void allSixteenCandidatesListOnceDuringTheLongRun() {
        Player player = new Player("npc-listing-long-run", "hash");
        player.scheduleStockUnlock(1);
        player.unlockStockContent();
        playerRepository.save(player);
        stockService.ensureMarketInitialized(player);
        listingService.process(player);

        int simulationEndDay = player.getElapsedDays() + 365 * 9;
        while (player.getElapsedDays() < simulationEndDay
                && listingRepository.findByPlayerAndStage(player, NpcCompanyListingStage.LISTED).size() < 16) {
            player.advanceDay();
            settleQuarterIfDue(player);
            processListingIfDue(player);
        }

        var listings = listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player);
        assertThat(listings).hasSize(16).allMatch(listing -> listing.isListed());
        assertThat(listings.stream().map(listing -> listing.getStockKey()).collect(java.util.stream.Collectors.toSet()))
                .hasSize(16);
        assertThat(listings).allMatch(listing -> listing.getOfferPrice() > 0)
                .allMatch(listing -> listing.getListedElapsedDay() > 0);
        var listingsByYear = listings.stream().collect(java.util.stream.Collectors.groupingBy(
                listing -> com.game.buildingstory.domain.GameCalendar.year(listing.getListedElapsedDay()),
                java.util.stream.Collectors.counting()
        ));
        assertThat(listingsByYear.values()).allMatch(annualCount -> annualCount <= 3);
        var candidateKeys = new HashSet<>(listings.stream().map(listing -> listing.getStockKey()).toList());
        assertThat(priceRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player).stream()
                .filter(price -> candidateKeys.contains(price.getStockKey()))
                .map(price -> price.getStockKey()).collect(java.util.stream.Collectors.toSet()))
                .hasSize(16);
    }

    @Test
    void npcListingJoinsIndexAfterTwoActualPostListingQuarters() {
        Player player = new Player("npc-index-inclusion", "hash");
        player.scheduleStockUnlock(1);
        player.unlockStockContent();
        playerRepository.save(player);
        stockService.ensureMarketInitialized(player);
        listingService.process(player);
        var first = listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player).getFirst();

        while (!first.isListed()) {
            player.advanceDay();
            settleQuarterIfDue(player);
            processMarketIfDue(player);
        }
        assertThat(constituentService.isIncluded(player, first.getStockKey())).isFalse();

        var company = companyRepository.findByPlayerAndStockKey(player, first.getStockKey()).orElseThrow();
        while (marketIndexService.current(player).constituentCount() < 16) {
            player.advanceDay();
            settleQuarterIfDue(player);
            processMarketIfDue(player);
        }

        assertThat(reportRepository
                .countByListedCompanyAndBaselineHistoryFalseAndPublishedElapsedDayBetween(
                        company, first.getListedElapsedDay(), player.getElapsedDays()))
                .isGreaterThanOrEqualTo(2);
        assertThat(constituentService.current(player)).hasSize(16);
        assertThat(marketIndexService.current(player).constituentCount()).isEqualTo(16);
    }

    private void settleQuarterIfDue(Player player) {
        if (player.getDay() == 1 && (player.getMonth() - 1) % 3 == 0) {
            financialService.settlePreviousQuarterIfDue(player);
        }
    }

    private void processListingIfDue(Player player) {
        if (player.getElapsedDays() % 5 == 0) {
            listingService.process(player);
        }
    }

    private void processMarketIfDue(Player player) {
        if (player.getElapsedDays() % 5 == 0) {
            listingService.process(player);
            stockService.processPriceUpdates(player);
        }
    }
}
