package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 확정 분기 배당을 개인 증권계좌에 지급하고 같은 날 배당락 가격을 반영한다. */
@Service
@Transactional
public class StockDividendService {
    private final StockCatalog stockCatalog;
    private final OwnedStockRepository ownedStockRepository;
    private final StockPriceHistoryRepository priceHistoryRepository;
    private final StockTradeHistoryRepository tradeHistoryRepository;
    private final StockLiquidityService stockLiquidityService;

    public StockDividendService(
            StockCatalog stockCatalog,
            OwnedStockRepository ownedStockRepository,
            StockPriceHistoryRepository priceHistoryRepository,
            StockTradeHistoryRepository tradeHistoryRepository,
            StockLiquidityService stockLiquidityService
    ) {
        this.stockCatalog = stockCatalog;
        this.ownedStockRepository = ownedStockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.stockLiquidityService = stockLiquidityService;
    }

    public long payPersonalDividend(Player player, ListedCompany company, long dividendPerShare) {
        if (dividendPerShare <= 0) {
            return 0;
        }
        StockSpec stock = stockCatalog.find(company.getStockKey()).orElseThrow();
        StockPriceHistory latestPrice = priceHistoryRepository
                .findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, company.getStockKey())
                .orElseThrow();
        latestPrice.applyDividendExDate(dividendPerShare);
        stockLiquidityService.rebase(player, company.getStockKey());

        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, company.getStockKey()).orElse(null);
        long quantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        if (quantity <= 0) {
            return 0;
        }
        long payment = Math.multiplyExact(dividendPerShare, quantity);
        player.addSecuritiesCash(payment);
        tradeHistoryRepository.save(new StockTradeHistory(
                player, stock.key(), stock.name(), "배당", quantity, dividendPerShare,
                payment, 0, payment, 0, 0
        ));
        return payment;
    }
}
