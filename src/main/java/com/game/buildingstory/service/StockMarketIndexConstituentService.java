package com.game.buildingstory.service;

import com.game.buildingstory.domain.NpcCompanyListingStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 종합지수 구성 종목과 신규상장 종목의 정기편입 자격을 한곳에서 결정한다. */
@Service
@Transactional(readOnly = true)
public class StockMarketIndexConstituentService {
    private static final int REQUIRED_POST_LISTING_QUARTERS = 2;

    private final StockCatalog stockCatalog;
    private final NpcCompanyListingRepository listingRepository;
    private final ListedCompanyRepository companyRepository;
    private final ListedCompanyQuarterlyReportRepository reportRepository;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final CompanyListingRepository companyListingRepository;
    private final CompanyQuarterlyReportRepository companyQuarterlyReportRepository;
    private final StockUniverseService stockUniverseService;

    public StockMarketIndexConstituentService(
            StockCatalog stockCatalog,
            NpcCompanyListingRepository listingRepository,
            ListedCompanyRepository companyRepository,
            ListedCompanyQuarterlyReportRepository reportRepository,
            PlayerCompanyRepository playerCompanyRepository,
            CompanyListingRepository companyListingRepository,
            CompanyQuarterlyReportRepository companyQuarterlyReportRepository,
            StockUniverseService stockUniverseService
    ) {
        this.stockCatalog = stockCatalog;
        this.listingRepository = listingRepository;
        this.companyRepository = companyRepository;
        this.reportRepository = reportRepository;
        this.playerCompanyRepository = playerCompanyRepository;
        this.companyListingRepository = companyListingRepository;
        this.companyQuarterlyReportRepository = companyQuarterlyReportRepository;
        this.stockUniverseService = stockUniverseService;
    }

    public List<StockSpec> current(Player player) {
        return currentAt(player, player.getElapsedDays());
    }

    public List<StockSpec> currentAt(Player player, int elapsedDay) {
        List<StockSpec> constituents = new ArrayList<>(stockCatalog.initial());
        listingRepository.findByPlayerAndStage(player, NpcCompanyListingStage.LISTED).stream()
                .filter(listing -> actualQuarterCountAfterListing(player, listing.getStockKey(),
                        listing.getListedElapsedDay(), elapsedDay) >= REQUIRED_POST_LISTING_QUARTERS)
                .map(listing -> stockCatalog.find(listing.getStockKey()).orElseThrow())
                .forEach(constituents::add);
        playerCompanyRepository.findByPlayer(player)
                .flatMap(company -> companyListingRepository.findByCompany(company)
                        .filter(listing -> listing.getStatus() == CompanyListingStatus.LISTED)
                        .filter(listing -> listing.getListedElapsedDay() != null
                                && listing.getListedElapsedDay() <= elapsedDay)
                        .filter(listing -> companyQuarterlyReportRepository
                                .countByCompanyAndQuarterSequenceGreaterThanAndPublishedElapsedDayLessThanEqual(
                                        company,
                                        listing.getQuarterSequenceAtListing(),
                                        elapsedDay
                                ) >= REQUIRED_POST_LISTING_QUARTERS)
                        .flatMap(listing -> stockUniverseService.find(player, listing.getStockKey())))
                .ifPresent(constituents::add);
        return List.copyOf(constituents);
    }

    public boolean isIncluded(Player player, String stockKey) {
        return current(player).stream().anyMatch(stock -> stock.key().equals(stockKey));
    }

    private long actualQuarterCountAfterListing(
            Player player,
            String stockKey,
            int listedElapsedDay,
            int elapsedDay
    ) {
        if (listedElapsedDay <= 0) {
            return 0;
        }
        return companyRepository.findByPlayerAndStockKey(player, stockKey)
                .map(company -> reportRepository
                        .countByListedCompanyAndBaselineHistoryFalseAndPublishedElapsedDayBetween(
                                company, listedElapsedDay, elapsedDay))
                .orElse(0L);
    }
}
