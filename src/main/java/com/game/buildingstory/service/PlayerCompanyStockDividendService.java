package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기업 배당 지급은 건드리지 않고 상장기업의 주당배당과 배당락만 주식시장에 반영한다. */
@Service
public class PlayerCompanyStockDividendService {
    private final CompanyListingRepository listingRepository;
    private final StockPriceHistoryRepository priceRepository;

    public PlayerCompanyStockDividendService(
            CompanyListingRepository listingRepository,
            StockPriceHistoryRepository priceRepository
    ) {
        this.listingRepository = listingRepository;
        this.priceRepository = priceRepository;
    }

    @Transactional
    public long applyExDividend(PlayerCompany company, long totalDividend) {
        boolean listed = listingRepository.findByCompany(company)
                .filter(listing -> listing.getStatus() == CompanyListingStatus.LISTED)
                .isPresent();
        if (!listed || totalDividend <= 0 || company.getIssuedShares() <= 0) {
            return 0;
        }

        long dividendPerShare = totalDividend / company.getIssuedShares();
        if (dividendPerShare <= 0) {
            return 0;
        }
        var price = priceRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(
                        company.getPlayer(), CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY)
                .orElseThrow(() -> new IllegalStateException("상장기업의 기준 주가가 없습니다."));
        price.applyDividendExDate(dividendPerShare);
        return dividendPerShare;
    }
}
