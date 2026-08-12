package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 고정 NPC 종목과 플레이어별 상장기업 종목을 합성하는 단일 조회 경계다. */
@Service
@Transactional(readOnly = true)
public class StockUniverseService {
    private final StockCatalog stockCatalog;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyListingRepository listingRepository;
    private final StockRiskProfileService riskProfileService;
    private final NpcCompanyListingRepository npcListingRepository;

    public StockUniverseService(
            StockCatalog stockCatalog,
            PlayerCompanyRepository companyRepository,
            CompanyListingRepository listingRepository,
            StockRiskProfileService riskProfileService,
            NpcCompanyListingRepository npcListingRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.companyRepository = companyRepository;
        this.listingRepository = listingRepository;
        this.riskProfileService = riskProfileService;
        this.npcListingRepository = npcListingRepository;
    }

    public List<StockSpec> stocks(Player player) {
        List<StockSpec> stocks = new ArrayList<>(stockCatalog.initial().stream()
                .map(stock -> riskProfileService.currentSpec(player, stock))
                .toList());
        npcListingRepository.findByPlayerAndStage(player, com.game.buildingstory.domain.NpcCompanyListingStage.LISTED)
                .stream()
                .map(listing -> stockCatalog.find(listing.getStockKey()).orElseThrow())
                .map(stock -> riskProfileService.currentSpec(player, stock))
                .forEach(stocks::add);
        playerCompanyStock(player)
                .map(stock -> riskProfileService.currentSpec(player, stock))
                .ifPresent(stocks::add);
        return List.copyOf(stocks);
    }

    public Optional<StockSpec> find(Player player, String stockKey) {
        Optional<StockSpec> fixed = stockCatalog.findInitial(stockKey);
        if (fixed.isPresent()) {
            return fixed.map(stock -> riskProfileService.currentSpec(player, stock));
        }
        Optional<StockSpec> npcListing = npcListingRepository.findByPlayerAndStockKey(player, stockKey)
                .filter(com.game.buildingstory.domain.NpcCompanyListing::isListed)
                .flatMap(listing -> stockCatalog.find(listing.getStockKey()))
                .map(stock -> riskProfileService.currentSpec(player, stock));
        if (npcListing.isPresent()) {
            return npcListing;
        }
        return playerCompanyStock(player)
                .filter(stock -> stock.key().equals(stockKey))
                .map(stock -> riskProfileService.currentSpec(player, stock));
    }

    public boolean isPlayerCompany(String stockKey) {
        return CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY.equals(stockKey);
    }

    private Optional<StockSpec> playerCompanyStock(Player player) {
        return companyRepository.findByPlayer(player).flatMap(company ->
                listingRepository.findByCompany(company)
                        .filter(listing -> listing.getStatus() == CompanyListingStatus.LISTED)
                        .map(listing -> new StockSpec(
                                listing.getStockKey(),
                                "IT",
                                company.getCompanyName(),
                                StockRiskType.AGGRESSIVE,
                                1.25,
                                1.15,
                                2.20,
                                listing.getOfferPrice(),
                                company.getIssuedShares(),
                                company.getServiceName() + " 기반 서비스를 운영하는 플레이어 설립 AI 기업"
                        )));
    }
}
