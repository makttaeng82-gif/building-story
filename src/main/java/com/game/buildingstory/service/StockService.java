package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService {
    /*
     * StockService는 주식 컨텐츠의 서버 규칙을 담당한다.
     *
     * 주요 책임:
     * 1. 순자산과 평판 조건 달성 후 주식 기능 개방 예약/이벤트 표시
     * 2. 종목별 가격 이력 생성과 5일 주기 가격 갱신
     * 3. 개인 현금과 증권계좌 예수금 사이의 입출금
     * 4. 현재가 기준 즉시 매수/매도
     * 5. 업종 호황/불황 뉴스 효과 적용
     *
     * 예수금은 원 단위지만 개인 현금과 분리한다. 주문은 예수금 안에서만 체결된다.
     */
    private static final String STOCK_UNLOCK_EFFECT = "NONE";
    private static final String STOCK_UNLOCK_IMAGE = "AI 주식 이미지";
    private static final int UPDATE_INTERVAL_DAYS = 5;
    private static final int DEFAULT_CHART_CANDLE_COUNT = 73;
    private static final double TREND_EFFECT_PERCENT = 0.5;
    private static final double TRADE_FEE_RATE = 0.0025;
    private static final long STOCK_UNLOCK_NET_WORTH = 3_000_000_000L;
    private static final int STOCK_UNLOCK_REPUTATION = 8_250;

    private final Random random = new Random();
    private final StockCatalog stockCatalog;
    private final StockUniverseService stockUniverseService;
    private final StockCompanyOverviewCatalog stockCompanyOverviewCatalog;
    private final ListedCompanyService listedCompanyService;
    private final ListedCompanyFinancialService listedCompanyFinancialService;
    private final StockFinancialDataService stockFinancialDataService;
    private final StockMarketRegimeService stockMarketRegimeService;
    private final StockMarketIndexService stockMarketIndexService;
    private final StockPriceModel stockPriceModel;
    private final StockLiquidityService stockLiquidityService;
    private final StockMarketNewsService stockMarketNewsService;
    private final StockIndustryNewsService stockIndustryNewsService;
    private final StockCompanyNewsService stockCompanyNewsService;
    private final StockNewsFeedService stockNewsFeedService;
    private final StockChartDataService stockChartDataService;
    private final GameEventRepository gameEventRepository;
    private final OwnedStockRepository ownedStockRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockTradeHistoryRepository stockTradeHistoryRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final LoanRepository loanRepository;
    private final CityMarketIndexService cityMarketIndexService;
    private final CompanyListingRepository companyListingRepository;

    public StockService(
            StockCatalog stockCatalog,
            StockUniverseService stockUniverseService,
            StockCompanyOverviewCatalog stockCompanyOverviewCatalog,
            ListedCompanyService listedCompanyService,
            ListedCompanyFinancialService listedCompanyFinancialService,
            StockFinancialDataService stockFinancialDataService,
            StockMarketRegimeService stockMarketRegimeService,
            StockMarketIndexService stockMarketIndexService,
            StockPriceModel stockPriceModel,
            StockLiquidityService stockLiquidityService,
            StockMarketNewsService stockMarketNewsService,
            StockIndustryNewsService stockIndustryNewsService,
            StockCompanyNewsService stockCompanyNewsService,
            StockNewsFeedService stockNewsFeedService,
            StockChartDataService stockChartDataService,
            GameEventRepository gameEventRepository,
            OwnedStockRepository ownedStockRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockTradeHistoryRepository stockTradeHistoryRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            LoanRepository loanRepository,
            CityMarketIndexService cityMarketIndexService,
            CompanyListingRepository companyListingRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.stockUniverseService = stockUniverseService;
        this.stockCompanyOverviewCatalog = stockCompanyOverviewCatalog;
        this.listedCompanyService = listedCompanyService;
        this.listedCompanyFinancialService = listedCompanyFinancialService;
        this.stockFinancialDataService = stockFinancialDataService;
        this.stockMarketRegimeService = stockMarketRegimeService;
        this.stockMarketIndexService = stockMarketIndexService;
        this.stockPriceModel = stockPriceModel;
        this.stockLiquidityService = stockLiquidityService;
        this.stockMarketNewsService = stockMarketNewsService;
        this.stockIndustryNewsService = stockIndustryNewsService;
        this.stockCompanyNewsService = stockCompanyNewsService;
        this.stockNewsFeedService = stockNewsFeedService;
        this.stockChartDataService = stockChartDataService;
        this.gameEventRepository = gameEventRepository;
        this.ownedStockRepository = ownedStockRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockTradeHistoryRepository = stockTradeHistoryRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.loanRepository = loanRepository;
        this.cityMarketIndexService = cityMarketIndexService;
        this.companyListingRepository = companyListingRepository;
    }

    @Transactional(readOnly = true)
    public List<StockSpec> stocks() {
        return stockCatalog.initial();
    }

    @Transactional(readOnly = true)
    public List<StockSpec> stocks(Player player) {
        return stockUniverseService.stocks(player);
    }

    public void ensureUnlockSchedule(Player player) {
        // 순자산과 평판 조건을 충족하면 2일 뒤 계좌 개설 이벤트를 예약한다.
        if (player.isStockContentUnlocked() || player.hasStockUnlockSchedule()) {
            return;
        }
        if (player.getReputation() >= STOCK_UNLOCK_REPUTATION && netWorth(player) >= STOCK_UNLOCK_NET_WORTH) {
            player.scheduleStockUnlock(player.getElapsedDays() + 2);
        }
    }

    public boolean activateUnlockNoticeIfDue(Player player) {
        // 개방일이 되었더라도 이미 다른 이벤트 모달이 떠 있으면 새 이벤트를 겹치지 않는다.
        ensureUnlockSchedule(player);
        if (!player.isStockUnlockDue() || player.isStockUnlockNoticeShown()) {
            return false;
        }
        if (gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).isPresent()) {
            return false;
        }
        player.unlockStockContent();
        player.markStockUnlockNoticeShown();
        ensureMarketInitialized(player);
        gameEventRepository.save(new GameEvent(
                player,
                "stock_unlock_" + player.getId(),
                "주식 투자 개방",
                "자산과 신용 기준을 충족해 증권 계좌가 개설되었습니다. 이제 주식 투자를 할 수 있습니다.",
                STOCK_UNLOCK_IMAGE,
                STOCK_UNLOCK_EFFECT,
                "확인"
        ));
        player.pause();
        return true;
    }

    public void processPriceUpdates(Player player) {
        // 모든 종목의 가격은 같은 날 한 번에 갱신된다. 일부 종목만 갱신되면 차트 기준일이 어긋난다.
        if (!player.isStockContentUnlocked()) {
            return;
        }
        ensureMarketInitialized(player);
        int lastUpdateDay = lastMarketUpdateDay(player);
        if (player.getElapsedDays() - lastUpdateDay < UPDATE_INTERVAL_DAYS) {
            return;
        }
        StockMarketRegimeService.MarketPulse marketPulse = stockMarketRegimeService.currentPulse(player);
        double marketEffectPercent = marketPulse.effectPercent()
                + stockMarketNewsService.activePriceEffectPercent(player);
        Map<String, ListedCompany> companies = listedCompanyService.companiesByStockKey(player);
        stockUniverseService.stocks(player).forEach(
                stock -> appendNextHistory(player, stock, companies.get(stock.key()), marketEffectPercent));
        stockLiquidityService.refreshAll(player);
        stockMarketIndexService.recordCurrent(player);
        stockMarketRegimeService.recordQuarterExposure(player);
        StockMarketRegimeService.RegimeTransition transition = stockMarketRegimeService.advance(player);
        stockMarketNewsService.afterPriceUpdate(player, transition);
        stockIndustryNewsService.afterPriceUpdate(player);
        stockCompanyNewsService.afterPriceUpdate(player);
    }

    @Transactional(readOnly = true)
    public StockMarketStatusView marketStatus(Player player) {
        int lastUpdateDay = lastMarketUpdateDay(player);
        int daysSinceUpdate = Math.max(0, player.getElapsedDays() - lastUpdateDay);
        int daysUntilNextUpdate = Math.max(0, UPDATE_INTERVAL_DAYS - daysSinceUpdate);
        int progressPercent = Math.min(100, daysSinceUpdate * 100 / UPDATE_INTERVAL_DAYS);
        StockMarketIndexService.IndexSnapshot index = stockMarketIndexService.current(player);
        String marketNewsText = stockMarketNewsService.activeStatusText(player);
        boolean hasMarketNews = !marketNewsText.isBlank();
        return new StockMarketStatusView(
                player.dateTextAfterDays(daysUntilNextUpdate),
                daysUntilNextUpdate,
                progressPercent,
                hasMarketNews ? marketNewsText : stockIndustryNewsService.activeStatusText(player),
                hasMarketNews
                        ? stockMarketNewsService.activeStatusDirection(player)
                        : stockIndustryNewsService.activeStatusDirection(player),
                stockMarketRegimeService.currentRegime(player).label(),
                indexValueText(index.indexBasisPoints()),
                signedPercent(index.changeBasisPoints() / 100.0),
                changeDirection(index.changeBasisPoints()),
                index.constituentCount()
        );
    }

    public void ensureMarketInitialized(Player player) {
        // 상장기업 소유구조를 먼저 만든 뒤 가격 이력을 만든다. 기존 행은 보존하므로 화면 진입 때마다 초기화되지 않는다.
        listedCompanyService.ensureCompaniesInitialized(player);
        listedCompanyFinancialService.ensureBaselineHistory(player);
        stockMarketRegimeService.ensureInitialized(player);
        stockUniverseService.stocks(player).forEach(stock -> {
            if (!stockPriceHistoryRepository.existsByPlayerAndStockKey(player, stock.key())) {
                stockPriceHistoryRepository.save(new StockPriceHistory(
                        player,
                        stock.key(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        initialVolume(stock)
                ));
            }
        });
        stockChartDataService.ensureInitialHistory(player);
        stockLiquidityService.ensureInitialized(player);
        stockMarketIndexService.ensureInitialized(player);
    }

    @Transactional(readOnly = true)
    public List<StockQuoteView> stockQuotes(Player player) {
        Map<String, OwnedStock> ownedStocks = ownedStockRepository.findByPlayer(player).stream()
                .collect(Collectors.toMap(OwnedStock::getStockKey, Function.identity()));
        Map<String, ListedCompany> companies = listedCompanyService.companiesByStockKey(player);
        return stockUniverseService.stocks(player).stream()
                .map(stock -> quote(player, stock, ownedStocks.get(stock.key()), companies.get(stock.key())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockListQuoteView> stockListQuotes(Player player) {
        Map<String, OwnedStock> ownedStocks = ownedStockRepository.findByPlayer(player).stream()
                .collect(Collectors.toMap(OwnedStock::getStockKey, Function.identity()));
        Map<String, ListedCompany> companies = listedCompanyService.companiesByStockKey(player);
        int latestElapsedDays = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .map(StockPriceHistory::getElapsedDays)
                .orElse(player.getElapsedDays());
        List<Integer> quoteDays = new ArrayList<>();
        quoteDays.add(latestElapsedDays);
        stockPriceHistoryRepository.findPreviousElapsedDays(player, latestElapsedDays).ifPresent(quoteDays::add);
        Map<String, List<StockPriceHistory>> pricesByStock = stockPriceHistoryRepository
                .findByPlayerAndElapsedDaysInOrderByElapsedDaysDescIdDesc(player, quoteDays)
                .stream()
                .collect(Collectors.groupingBy(StockPriceHistory::getStockKey));
        return stockUniverseService.stocks(player).stream()
                .map(stock -> listQuote(
                        player,
                        stock,
                        ownedStocks.get(stock.key()),
                        companies.get(stock.key()),
                        pricesByStock.getOrDefault(stock.key(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockQuoteView selectedStockQuote(Player player, String stockKey) {
        StockSpec stock = stockUniverseService.find(player, stockKey)
                .orElseGet(() -> stockUniverseService.stocks(player).get(0));
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stock.key()).orElse(null);
        ListedCompany company = listedCompanyService.companiesByStockKey(player).get(stock.key());
        return quote(player, stock, ownedStock, company);
    }

    @Transactional(readOnly = true)
    public List<StockTradeHistory> tradeHistories(Player player) {
        return stockTradeHistoryRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(
                player,
                Math.max(1, player.getElapsedDays() - 89)
        );
    }

    @Transactional(readOnly = true)
    public List<StockNewsArticleView> newsArticles(Player player) {
        return stockNewsFeedService.latestArticles(player);
    }

    public boolean markNewsRead(Player player, long articleId) {
        return stockNewsFeedService.markRead(player, articleId);
    }

    @Transactional(readOnly = true)
    public StockHoldingSummaryView holdingSummary(Player player) {
        return holdingSummary(player, stockListQuotes(player));
    }

    public StockHoldingSummaryView holdingSummary(Player player, List<StockListQuoteView> stockQuotes) {
        // 보유요약은 현재가 기준 평가금액과 평균단가 기준 원가를 비교해 전체 손익을 계산한다.
        List<StockListQuoteView> ownedQuotes = stockQuotes.stream()
                .filter(quote -> quote.quantity() > 0)
                .toList();
        long totalQuantity = ownedQuotes.stream().mapToLong(StockListQuoteView::quantity).sum();
        long totalCost = ownedQuotes.stream().mapToLong(StockListQuoteView::totalCostBasis).sum();
        long totalValuation = ownedQuotes.stream().mapToLong(quote -> quote.currentPrice() * quote.quantity()).sum();
        long totalProfit = totalValuation - totalCost;
        long totalRealizedProfit = stockTradeHistoryRepository.sumRealizedProfitByPlayer(player);
        long totalDividendIncome = stockTradeHistoryRepository.sumDividendIncomeByPlayer(player);
        long totalFees = stockTradeHistoryRepository.sumFeeByPlayer(player);
        return new StockHoldingSummaryView(
                ownedQuotes.size(),
                totalQuantity,
                totalCost,
                totalValuation,
                totalProfit,
                totalRealizedProfit,
                totalDividendIncome,
                totalFees,
                stockPriceText(totalCost),
                stockPriceText(totalValuation),
                profitText(totalProfit, totalCost),
                changeDirection(totalProfit),
                signedPrice(totalRealizedProfit),
                changeDirection(totalRealizedProfit),
                stockPriceText(totalDividendIncome),
                stockPriceText(totalFees)
        );
    }

    public String deposit(Player player, long amount) {
        if (amount <= 0 || safeAdd(player.getSecuritiesCash(), amount) == null) {
            return "입금액 오류";
        }
        if (!player.spendCash(amount)) {
            return "현금 부족";
        }
        player.addSecuritiesCash(amount);
        return stockPriceText(amount) + " 입금";
    }

    public String withdraw(Player player, long amount) {
        if (amount <= 0 || safeAdd(player.getCash(), amount) == null) {
            return "출금액 오류";
        }
        if (!player.spendSecuritiesCash(amount)) {
            return "예수금 부족";
        }
        player.addCash(amount);
        return stockPriceText(amount) + " 출금";
    }

    public String buyStock(Player player, String stockKey, long quantity) {
        // 매수는 현재가 시장가 주문으로 처리한다. 지정가 주문이나 주문 대기열은 없다.
        if (quantity <= 0) {
            return "매수 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        if (player.isPaused()) {
            return "일시정지 중에는 주식을 거래할 수 없습니다.";
        }
        if (stockUniverseService.isPlayerCompany(stockKey)) {
            return "자기 회사 주식은 현재 거래할 수 없습니다.";
        }
        ensureMarketInitialized(player);
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        if (safeMultiply(currentPrice(player, stock), quantity) == null) {
            return "매수 수량 오류";
        }
        ListedCompany company = listedCompanyService.requireCompany(player, stockKey);
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        long ownedQuantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        long existingCostBasis = ownedStock == null ? 0 : ownedStock.getTotalCostBasis();
        long remainingMarketQuantity = remainingMarketBuyQuantity(company, ownedQuantity);
        if (quantity > remainingMarketQuantity) {
            return "일반시장 매집 한도 초과 · 최대 지분 20%";
        }
        long availableLiquidity = stockLiquidityService.availableBuyQuantity(player, company);
        if (quantity > availableLiquidity) {
            return "매수 유동성 부족 · 현재 체결 가능 " + availableLiquidity + "주";
        }
        StockLiquidityService.Execution execution = stockLiquidityService.preview(player, stockKey, quantity);
        long price = execution.averagePrice();
        Long grossAmount = safeMultiply(price, quantity);
        if (grossAmount == null) {
            return "매수 수량 오류";
        }
        long fee = tradeFee(grossAmount);
        Long totalCost = safeAdd(grossAmount, fee);
        if (totalCost == null) {
            return "매수 수량 오류";
        }
        if (safeAdd(ownedQuantity, quantity) == null
                || safeAdd(existingCostBasis, totalCost) == null) {
            return "매수 수량 오류";
        }
        if (!player.spendSecuritiesCash(totalCost)) {
            return "예수금 부족 · 필요 " + stockPriceText(totalCost) + " / 보유 " + stockPriceText(player.getSecuritiesCash());
        }
        if (ownedStock == null) {
            ownedStock = ownedStockRepository.save(new OwnedStock(player, stockKey));
        }
        if (!company.transferMarketSharesToPlayer(quantity)) {
            throw new IllegalStateException("일반시장 보유량 검증 후 주식 이전에 실패했습니다.");
        }
        ownedStock.buy(quantity, totalCost);
        stockLiquidityService.complete(player, stockKey, quantity, execution);
        stockTradeHistoryRepository.save(new StockTradeHistory(
                player, stock.key(), stock.name(), "매수", quantity, price,
                grossAmount, fee, totalCost, totalCost, 0, execution.averageImpactBasisPoints()
        ));
        return stock.name() + " " + quantity + "주 매수";
    }

    public String buyMaxStock(Player player, String stockKey) {
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        if (player.isPaused()) {
            return "일시정지 중에는 주식을 거래할 수 없습니다.";
        }
        if (stockUniverseService.isPlayerCompany(stockKey)) {
            return "자기 회사 주식은 현재 거래할 수 없습니다.";
        }
        ensureMarketInitialized(player);
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        long ownedQuantity = ownedStockRepository.findByPlayerAndStockKey(player, stockKey)
                .map(OwnedStock::getQuantity)
                .orElse(0L);
        ListedCompany company = listedCompanyService.requireCompany(player, stockKey);
        long orderLimit = Math.min(
                remainingMarketBuyQuantity(company, ownedQuantity),
                stockLiquidityService.availableBuyQuantity(player, company)
        );
        long quantity = maxAffordableQuantity(player, stockKey, orderLimit);
        if (quantity <= 0) {
            if (remainingMarketBuyQuantity(company, ownedQuantity) == 0) {
                return "일반시장 매집 한도 도달 · 최대 지분 20%";
            }
            long price = currentPrice(player, stock);
            long minimumCost = price + tradeFee(price);
            return "예수금 부족 · 필요 " + stockPriceText(minimumCost) + " / 보유 " + stockPriceText(player.getSecuritiesCash());
        }
        return buyStock(player, stockKey, quantity);
    }

    public String sellStock(Player player, String stockKey, long quantity) {
        // 매도도 현재가 시장가 주문이다. 공매도/마진이 없으므로 보유 수량보다 많이 팔 수 없다.
        if (quantity <= 0) {
            return "매도 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        if (player.isPaused()) {
            return "일시정지 중에는 주식을 거래할 수 없습니다.";
        }
        if (stockUniverseService.isPlayerCompany(stockKey)) {
            return "자기 회사 주식은 현재 거래할 수 없습니다.";
        }
        ensureMarketInitialized(player);
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (ownedStock == null || ownedStock.getQuantity() < quantity) {
            long ownedQuantity = ownedStock == null ? 0 : ownedStock.getQuantity();
            return "보유 수량 부족 · 보유 " + ownedQuantity + "주 / 매도 요청 " + quantity + "주";
        }
        long availableLiquidity = stockLiquidityService.availableSellQuantity(player, stockKey);
        if (quantity > availableLiquidity) {
            return "매도 유동성 부족 · 현재 체결 가능 " + availableLiquidity + "주";
        }
        StockLiquidityService.Execution execution = stockLiquidityService.preview(player, stockKey, -quantity);
        long price = execution.averagePrice();
        Long grossAmount = safeMultiply(price, quantity);
        if (grossAmount == null) {
            return "매도 수량 오류";
        }
        long fee = tradeFee(grossAmount);
        long payout = Math.max(0, grossAmount - fee);
        if (safeAdd(player.getSecuritiesCash(), payout) == null) {
            return "매도 수량 오류";
        }
        long soldCostBasis = ownedStock.sell(quantity);
        long realizedProfit = Math.subtractExact(payout, soldCostBasis);
        listedCompanyService.requireCompany(player, stockKey).receivePlayerShares(quantity);
        player.addSecuritiesCash(payout);
        stockLiquidityService.complete(player, stockKey, -quantity, execution);
        stockTradeHistoryRepository.save(new StockTradeHistory(
                player, stock.key(), stock.name(), "매도", quantity, price,
                grossAmount, fee, payout, soldCostBasis, realizedProfit, execution.averageImpactBasisPoints()
        ));
        return stock.name() + " " + quantity + "주 매도";
    }

    public String sellAllStock(Player player, String stockKey) {
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        if (player.isPaused()) {
            return "일시정지 중에는 주식을 거래할 수 없습니다.";
        }
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (ownedStock == null || ownedStock.getQuantity() <= 0) {
            return "보유 수량 부족 · 보유 0주 / 매도 요청 1주";
        }
        return sellStock(player, stockKey, ownedStock.getQuantity());
    }

    @Transactional(readOnly = true)
    public boolean isUnlocked(Player player) {
        return player.isStockContentUnlocked();
    }

    @Transactional(readOnly = true)
    public String statusText(Player player) {
        if (player.isStockContentUnlocked()) {
            return "개방";
        }
        if (player.hasStockUnlockSchedule()) {
            return "개방 준비중";
        }
        return "순자산 30억원 · 평판 8,250 필요";
    }

    private void appendNextHistory(Player player, StockSpec stock, ListedCompany company, double marketEffectPercent) {
        // 실적 충격은 시가 갭으로, 나머지 요인은 5일 동안의 가격 경로로 반영한다.
        StockPriceHistory latest = stockPriceHistoryRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key())
                .orElseGet(() -> stockPriceHistoryRepository.save(new StockPriceHistory(
                        player,
                        stock.key(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        stock.basePrice(),
                        initialVolume(stock)
                )));
        // 신규상장 첫 봉을 같은 시장 갱신일에 만든 경우 두 번째 봉을 중복 생성하지 않는다.
        if (latest.getElapsedDays() >= player.getElapsedDays()) {
            return;
        }
        double industryPercent = stockIndustryNewsService.activePriceEffectPercent(player, stock.industry())
                * stock.industryBeta();
        double companyPercent = stockCompanyNewsService.activePriceEffectPercent(player, stock.key());
        double trendPercent = trendEffectPercent(player, stock);
        StockFinancialDataService.PriceSignal signal =
                stockFinancialDataService.consumePriceSignal(player, stock, company);
        StockPriceModel.Result result = stockPriceModel.calculate(new StockPriceModel.Input(
                latest.getClosePrice(),
                signal.fairValue(),
                marketEffectPercent * stock.beta(),
                industryPercent,
                companyPercent,
                trendPercent,
                signal.earningsImpactBasisPoints(),
                stock.idiosyncraticVolatilityPercent(),
                stock.riskType()
        ), random);
        stockPriceHistoryRepository.save(new StockPriceHistory(
                player, stock.key(), result.open(), result.high(), result.low(), result.close(),
                randomVolume(stock, result.totalChangePercent()),
                result.marketImpactBasisPoints(),
                result.industryImpactBasisPoints(),
                result.companyImpactBasisPoints(),
                result.earningsImpactBasisPoints(),
                result.valuationImpactBasisPoints(),
                result.trendImpactBasisPoints(),
                result.idiosyncraticImpactBasisPoints(),
                result.noiseImpactBasisPoints(),
                result.pathImpactBasisPoints()
        ));
    }

    /** 동적 신규상장 캔들이 전체 시장의 공통 5일 갱신 기준일을 바꾸지 않도록 고정 종목을 기준으로 삼는다. */
    private int lastMarketUpdateDay(Player player) {
        String referenceStockKey = stockCatalog.all().getFirst().key();
        return stockPriceHistoryRepository
                .findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, referenceStockKey)
                .map(StockPriceHistory::getElapsedDays)
                .orElse(player.getElapsedDays());
    }

    private double trendEffectPercent(Player player, StockSpec stock) {
        List<StockPriceHistory> recentRows = stockPriceHistoryRepository.findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        long risingCount = recentRows.stream()
                .filter(row -> row.getClosePrice() > row.getOpenPrice())
                .count();
        long fallingCount = recentRows.stream()
                .filter(row -> row.getClosePrice() < row.getOpenPrice())
                .count();
        if (risingCount >= 2) {
            return TREND_EFFECT_PERCENT;
        }
        if (fallingCount >= 2) {
            return -TREND_EFFECT_PERCENT;
        }
        return 0.0;
    }

    private StockQuoteView quote(Player player, StockSpec stock, OwnedStock ownedStock, ListedCompany company) {
        // 화면에 필요한 주식 정보는 엔티티 그대로 넘기지 않고 View record로 조립한다.
        // 이렇게 하면 템플릿은 계산 없이 표시만 담당하고, 계산 규칙은 서비스에 남는다.
        List<StockChartPoint> chartPoints = stockChartDataService.points(player, stock, company);
        List<StockPriceHistory> history = chartPoints.stream().map(StockChartPoint::price).toList();
        List<StockPriceHistory> latestRows = history.isEmpty()
                ? List.of()
                : history.subList(Math.max(0, history.size() - 2), history.size()).reversed();
        StockPriceHistory current = latestRows.isEmpty()
                ? new StockPriceHistory(player, stock.key(), stock.basePrice(), stock.basePrice(), stock.basePrice(), stock.basePrice(), initialVolume(stock))
                : latestRows.get(0);
        long previousPrice = latestRows.size() > 1 ? latestRows.get(1).getClosePrice() : current.getClosePrice();
        long currentPrice = current.getClosePrice();
        StockListQuoteView listQuote = listQuote(player, stock, ownedStock, company, latestRows);
        StockFinancialSnapshot financial = stockFinancialDataService.snapshot(player, stock, company);
        boolean hasQuarterlyReport = financial.hasReport();
        long displayedRevenue = financial.revenue();
        long displayedNetIncome = financial.netIncome();
        String financialPeriodText = financial.periodText();
        String operatingProfitText = hasQuarterlyReport
                ? financialMoneyText(financial.operatingProfit())
                : "공시 전";
        String earningsSurpriseText = hasQuarterlyReport
                ? signedPercent(financial.performanceBasisPoints() / 100.0)
                : "기준 전망";
        String earningsSurpriseDirection = hasQuarterlyReport
                ? changeDirection(financial.performanceBasisPoints())
                : "flat";
        String dividendPerShareText = !hasQuarterlyReport
                ? "지급 전"
                : financial.dividendPerShare() <= 0
                        ? "무배당"
                        : stockPriceText(financial.dividendPerShare());
        int earningsDday = Math.max(0, financial.nextEarningsElapsedDay() - player.getElapsedDays());
        String fairValueRangeText = financial.fairValueBase() <= 0
                ? "산정 전"
                : stockPriceText(financial.fairValueLower()) + " - " + stockPriceText(financial.fairValueUpper());
        String valuationStatusText;
        if (financial.fairValueBase() <= 0) {
            valuationStatusText = "산정 전";
        } else if (currentPrice < financial.fairValueLower()) {
            valuationStatusText = "저평가";
        } else if (currentPrice > financial.fairValueUpper()) {
            valuationStatusText = "고평가";
        } else {
            valuationStatusText = "적정";
        }
        String valuationDirection = "저평가".equals(valuationStatusText) ? "up"
                : "고평가".equals(valuationStatusText) ? "down" : "flat";
        List<StockPriceHistory> defaultHistory = history.subList(
                Math.max(0, history.size() - DEFAULT_CHART_CANDLE_COUNT),
                history.size()
        );
        ChartScale scale = chartScale(defaultHistory, currentPrice);
        StockCompanyDetailView companyDetail = companyDetail(player, stock, currentPrice, financial);
        boolean playerCompanyStock = stockUniverseService.isPlayerCompany(stock.key());
        long availableBuyQuantity = company == null || playerCompanyStock ? 0 : Math.min(
                listQuote.remainingMarketBuyQuantity(),
                stockLiquidityService.availableBuyQuantity(player, company)
        );
        long availableSellQuantity = playerCompanyStock ? 0 : Math.min(
                listQuote.quantity(),
                stockLiquidityService.availableSellQuantity(player, stock.key())
        );
        long maxAffordableBuyQuantity = playerCompanyStock
                ? 0
                : maxAffordableQuantity(player, stock.key(), availableBuyQuantity);
        long displayedPlayerShares = playerCompanyStock && company != null
                ? company.getFounderShares()
                : listQuote.quantity();
        String displayedOwnership = playerCompanyStock && company != null
                ? ownershipPercentText(company.getFounderShares(), company.getIssuedShares())
                : listQuote.ownershipPercentText();
        ExpectedDividend expectedDividend = expectedDividend(
                financial, stock.issuedShares(), displayedPlayerShares);
        var listing = playerCompanyStock
                ? companyListingRepository.findByCompany_Player(player).orElse(null)
                : null;
        StockPriceHistory listingCandle = playerCompanyStock && !history.isEmpty() ? history.getFirst() : null;
        String listingDateText = listingCandle == null ? "-" : listingCandle.getDateText();
        String offerPriceText = listing == null ? "-" : stockPriceText(listing.getOfferPrice());
        String founderSharesText = company == null ? "-" : stockQuantityText(company.getFounderShares()) + "주";
        String founderMarketValueText = company == null
                ? "-"
                : financialMoneyText(Math.multiplyExact(currentPrice, company.getFounderShares()));
        return new StockQuoteView(
                stock,
                currentPrice,
                previousPrice,
                listQuote.changeAmount(),
                listQuote.changePercent(),
                listQuote.currentPriceText(),
                stockPriceText(previousPrice),
                listQuote.changePercentText(),
                listQuote.changeAmountText(),
                listQuote.changeDirection(),
                listQuote.quantity(),
                listQuote.averagePrice(),
                listQuote.averagePriceText(),
                listQuote.totalCostBasis(),
                listQuote.valuationProfit(),
                listQuote.valuationProfitText(),
                listQuote.valuationText(),
                listQuote.marketCapText(),
                fairValueRangeText,
                financial.fairValueLower(),
                financial.fairValueUpper(),
                valuationStatusText,
                valuationDirection,
                displayedOwnership,
                stockPriceText(expectedDividend.total()),
                expectedDividend.perShare() <= 0 ? "무배당 예상" : stockPriceText(expectedDividend.perShare()),
                financial.hasReport()
                        ? String.format(Locale.ROOT, "%.1f%%", financial.dividendPayoutBasisPoints() / 100.0)
                        : "산정 전",
                listQuote.remainingMarketBuyQuantity(),
                availableBuyQuantity,
                availableSellQuantity,
                maxAffordableBuyQuantity,
                stockQuantityText(availableBuyQuantity) + "주",
                hasQuarterlyReport,
                financialPeriodText,
                financialMoneyText(displayedRevenue),
                operatingProfitText,
                financialMoneyText(displayedNetIncome),
                earningsSurpriseText,
                dividendPerShareText,
                changeDirection(displayedNetIncome),
                earningsSurpriseDirection,
                "실적 발표 D-" + earningsDday,
                candleViews(chartPoints, scale),
                stockPriceText(scale.minPrice()),
                stockPriceText(scale.maxPrice()),
                String.format(Locale.ROOT, "%.1f", priceY(currentPrice, scale)),
                stockPriceText(currentPrice),
                companyDetail,
                listingDateText,
                offerPriceText,
                founderSharesText,
                founderMarketValueText
        );
    }

    private StockCompanyDetailView companyDetail(
            Player player,
            StockSpec stock,
            long currentPrice,
            StockFinancialSnapshot financial
    ) {
        StockCompanyOverview overview = companyOverview(player, stock);
        List<StockQuarterSummaryView> recentQuarters = financial.recentQuarters().stream()
                .map(quarter -> new StockQuarterSummaryView(
                        quarter.periodText(),
                        financialMoneyText(quarter.revenue()),
                        financialMoneyText(quarter.operatingProfit()),
                        financialMoneyText(quarter.netIncome()),
                        signedPercent(quarter.performanceBasisPoints() / 100.0),
                        changeDirection(quarter.performanceBasisPoints())
                ))
                .toList();

        long cash = financial.cash();
        long debt = financial.debt();
        long netAssets = financial.netAssets();
        double debtRatio = netAssets <= 0 ? 0.0 : debt * 100.0 / netAssets;
        String financialHealth = financialHealth(netAssets, debtRatio);
        String financialHealthDirection = "안정".equals(financialHealth) ? "up"
                : "보통".equals(financialHealth) ? "flat" : "down";
        long earningsPerShare = financial.earningsPerShare();
        long bookValuePerShare = financial.bookValuePerShare();

        return new StockCompanyDetailView(
                overview.chiefExecutive(),
                overview.foundedText(),
                overview.headquarters(),
                stock.description(),
                overview.mainRevenueSource(),
                overview.keyRisk(),
                overview.cyclicality(),
                financialMoneyText(cash),
                financialMoneyText(debt),
                financialMoneyText(netAssets),
                netAssets <= 0 ? "산정 불가" : String.format(Locale.ROOT, "%.1f%%", debtRatio),
                financialHealth,
                financialHealthDirection,
                perShareText(earningsPerShare),
                perShareText(bookValuePerShare),
                ratioText(currentPrice, earningsPerShare),
                ratioText(currentPrice, bookValuePerShare),
                recentQuarters
        );
    }

    private StockCompanyOverview companyOverview(Player player, StockSpec stock) {
        if (!stockUniverseService.isPlayerCompany(stock.key())) {
            return stockCompanyOverviewCatalog.require(stock.key());
        }
        return new StockCompanyOverview(
                stock.key(),
                "플레이어",
                "플레이어 설립기업",
                "서울",
                "AI 플랫폼 구독·기업계약",
                "기술 경쟁·연산비·서비스 장애",
                "매우 높음"
        );
    }

    private String financialHealth(long netAssets, double debtRatio) {
        if (netAssets <= 0 || debtRatio > 100.0) {
            return "주의";
        }
        if (debtRatio <= 50.0) {
            return "안정";
        }
        return "보통";
    }

    private String perShareText(long amount) {
        if (amount < 0) {
            return "-" + stockPriceText(Math.abs(amount));
        }
        return stockPriceText(amount);
    }

    private String ratioText(long currentPrice, long perShareValue) {
        if (perShareValue <= 0) {
            return "산정 불가";
        }
        return String.format(Locale.ROOT, "%.1f배", currentPrice / (double) perShareValue);
    }

    private StockListQuoteView listQuote(
            Player player,
            StockSpec stock,
            OwnedStock ownedStock,
            ListedCompany company,
            List<StockPriceHistory> latestRows
    ) {
        StockPriceHistory current = latestRows.isEmpty()
                ? new StockPriceHistory(player, stock.key(), stock.basePrice(), stock.basePrice(), stock.basePrice(), stock.basePrice(), initialVolume(stock))
                : latestRows.get(0);
        long previousPrice = latestRows.size() > 1 ? latestRows.get(1).getClosePrice() : current.getClosePrice();
        long currentPrice = current.getClosePrice();
        long changeAmount = currentPrice - previousPrice;
        double changePercent = previousPrice == 0 ? 0.0 : changeAmount * 100.0 / previousPrice;
        long quantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        long averagePrice = ownedStock == null ? 0 : ownedStock.getAveragePrice();
        long totalCostBasis = ownedStock == null ? 0 : ownedStock.getTotalCostBasis();
        long valuationProfit = quantity == 0 ? 0 : currentPrice * quantity - totalCostBasis;
        long issuedShares = company == null ? stock.issuedShares() : company.getIssuedShares();
        long marketParticipantShares = company == null ? issuedShares : company.getMarketParticipantShares();
        return new StockListQuoteView(
                stock,
                currentPrice,
                previousPrice,
                changeAmount,
                changePercent,
                stockPriceText(currentPrice),
                signedPercent(changePercent),
                signedPrice(changeAmount),
                changeDirection(changeAmount),
                quantity,
                averagePrice,
                stockPriceText(averagePrice),
                totalCostBasis,
                valuationProfit,
                profitText(valuationProfit, totalCostBasis),
                stockPriceText(currentPrice * quantity),
                marketCapText(currentPrice * issuedShares),
                ownershipPercentText(quantity, issuedShares),
                remainingMarketBuyQuantity(issuedShares, marketParticipantShares, quantity)
        );
    }

    private long currentPrice(Player player, StockSpec stock) {
        return stockPriceHistoryRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key())
                .map(StockPriceHistory::getClosePrice)
                .orElse(stock.basePrice());
    }

    private long tradeFee(long grossAmount) {
        return (long) Math.ceil(grossAmount * TRADE_FEE_RATE);
    }

    private ExpectedDividend expectedDividend(
            StockFinancialSnapshot financial,
            long issuedShares,
            long quantity
    ) {
        if (financial.netIncome() <= 0 || financial.dividendPayoutBasisPoints() <= 0 || issuedShares <= 0) {
            return new ExpectedDividend(0, 0);
        }
        long desiredDividend = BigInteger.valueOf(financial.netIncome())
                .multiply(BigInteger.valueOf(financial.dividendPayoutBasisPoints()))
                .divide(BigInteger.valueOf(10_000))
                .longValueExact();
        long dividendPerShare = Math.min(
                desiredDividend / issuedShares,
                financial.cash() / issuedShares
        );
        return new ExpectedDividend(dividendPerShare, Math.multiplyExact(dividendPerShare, quantity));
    }

    private String stockQuantityText(long quantity) {
        long eok = quantity / 100_000_000L;
        long man = quantity % 100_000_000L / 10_000L;
        long remainder = quantity % 10_000L;
        StringBuilder builder = new StringBuilder();
        if (eok > 0) builder.append(eok).append("억");
        if (man > 0) builder.append(man).append("만");
        if (remainder > 0 || builder.isEmpty()) builder.append(remainder);
        return builder.toString();
    }

    private Long safeMultiply(long left, long right) {
        // 사용자가 요청 파라미터를 직접 조작하면 일반 곱셈은 long 범위를 넘어 음수로 순환할 수 있다.
        // exact 연산은 범위를 넘을 때 예외를 발생시키며, 서비스는 null을 수량 오류 메시지로 변환한다.
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    private Long safeAdd(long left, long right) {
        // 거래 원금과 수수료 또는 기존 잔액과 지급액을 합칠 때도 같은 방식으로 범위를 검사한다.
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    private long maxAffordableQuantity(Player player, String stockKey, long orderLimit) {
        long low = 0;
        long high = Math.max(0, orderLimit);
        while (low < high) {
            long mid = low + (high - low + 1) / 2;
            StockLiquidityService.Execution execution = stockLiquidityService.preview(player, stockKey, mid);
            Long grossAmount = safeMultiply(execution.averagePrice(), mid);
            Long totalCost = grossAmount == null ? null : safeAdd(grossAmount, tradeFee(grossAmount));
            if (totalCost != null && totalCost <= player.getSecuritiesCash()) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private long remainingMarketBuyQuantity(ListedCompany company, long ownedQuantity) {
        return remainingMarketBuyQuantity(
                company.getIssuedShares(),
                company.getMarketParticipantShares(),
                ownedQuantity
        );
    }

    private long remainingMarketBuyQuantity(long issuedShares, long marketParticipantShares, long ownedQuantity) {
        long marketHoldingLimit = issuedShares / 5;
        return Math.min(marketParticipantShares, Math.max(0, marketHoldingLimit - ownedQuantity));
    }

    private List<StockCandleView> candleViews(List<StockChartPoint> points, ChartScale scale) {
        // 서버에서 SVG 좌표를 미리 계산한다. 브라우저는 좌표를 받아 그리기만 하므로 JS 차트 라이브러리가 필요 없다.
        if (points.isEmpty()) {
            return List.of();
        }
        int count = points.size();
        int visibleStart = Math.max(0, count - DEFAULT_CHART_CANDLE_COUNT);
        int visibleCount = count - visibleStart;
        List<StockCandleView> candles = new ArrayList<>(count);
        int leftEdgeX = 36;
        int rightEdgeX = 690;
        double candleSpacing = visibleCount <= 1
                ? 0.0
                : Math.min(44.0, (rightEdgeX - leftEdgeX) / (double) (visibleCount - 1));
        double candleSpan = candleSpacing * Math.max(0, visibleCount - 1);
        double startX = leftEdgeX + ((rightEdgeX - leftEdgeX - candleSpan) / 2.0);
        for (int index = 0; index < count; index++) {
            StockChartPoint point = points.get(index);
            StockPriceHistory row = point.price();
            boolean visible = index >= visibleStart;
            int x = visible ? (int) Math.round(startX + ((index - visibleStart) * candleSpacing)) : 0;
            int openY = (int) Math.round(priceY(row.getOpenPrice(), scale));
            int highY = (int) Math.round(priceY(row.getHighPrice(), scale));
            int lowY = (int) Math.round(priceY(row.getLowPrice(), scale));
            int closeY = (int) Math.round(priceY(row.getClosePrice(), scale));
            double changePercent = row.getOpenPrice() == 0
                    ? 0.0
                    : (row.getClosePrice() - row.getOpenPrice()) * 100.0 / row.getOpenPrice();
            candles.add(new StockCandleView(
                    x,
                    openY,
                    highY,
                    lowY,
                    closeY,
                    Math.min(openY, closeY),
                    Math.max(2, Math.abs(openY - closeY)),
                    row.getClosePrice() >= row.getOpenPrice(),
                    row.getShortDateText(),
                    visible,
                    row.getElapsedDays(),
                    row.getOpenPrice(),
                    row.getHighPrice(),
                    row.getLowPrice(),
                    row.getClosePrice(),
                    stockPriceText(row.getOpenPrice()),
                    stockPriceText(row.getHighPrice()),
                    stockPriceText(row.getLowPrice()),
                    stockPriceText(row.getClosePrice()),
                    signedPercent(changePercent),
                    chartFactorText(row),
                    point.marketIndexBasisPoints(),
                    point.newsArticleId(),
                    point.newsTitle(),
                    point.newsCount(),
                    point.earningsEvent(),
                    point.dividendEvent()
            ));
        }
        return candles;
    }

    private String chartFactorText(StockPriceHistory row) {
        int otherImpact = row.getCompanyImpactBasisPoints()
                + row.getTrendImpactBasisPoints()
                + row.getIdiosyncraticImpactBasisPoints()
                + row.getNoiseImpactBasisPoints();
        String listing = row.getListingImpactBasisPoints() == 0
                ? ""
                : "공모 " + signedPercent(row.getListingImpactBasisPoints() / 100.0) + " · ";
        return listing + "시장 " + signedPercent(row.getMarketImpactBasisPoints() / 100.0)
                + " · 업종 " + signedPercent(row.getIndustryImpactBasisPoints() / 100.0)
                + " · 실적 " + signedPercent(row.getEarningsImpactBasisPoints() / 100.0)
                + " · 가치 " + signedPercent(row.getValuationImpactBasisPoints() / 100.0)
                + " · 기타 " + signedPercent(otherImpact / 100.0);
    }

    private ChartScale chartScale(List<StockPriceHistory> history, long fallbackPrice) {
        long min = history.stream().map(StockPriceHistory::getLowPrice).min(Comparator.naturalOrder()).orElse(fallbackPrice);
        long max = history.stream().map(StockPriceHistory::getHighPrice).max(Comparator.naturalOrder()).orElse(fallbackPrice);
        if (min == max) {
            long padding = Math.max(1L, min / 20L);
            min = Math.max(1L, min - padding);
            max += padding;
        }
        return new ChartScale(min, max);
    }

    private double priceY(long price, ChartScale scale) {
        return 24.0 + (scale.maxPrice() - price) * 220.0 / Math.max(1L, scale.maxPrice() - scale.minPrice());
    }

    private double randomChangePercent(double min, double max) {
        return random.nextDouble(min, max);
    }


    private long initialVolume(StockSpec stock) {
        return Math.max(100L, stock.basePrice() / 100L);
    }

    private long randomVolume(StockSpec stock, double changePercent) {
        double multiplier = 0.8 + random.nextDouble(0.0, 1.2) + Math.abs(changePercent) / 10.0;
        return Math.max(100L, Math.round(initialVolume(stock) * multiplier));
    }

    private String changeDirection(long changeAmount) {
        if (changeAmount > 0) {
            return "up";
        }
        if (changeAmount < 0) {
            return "down";
        }
        return "flat";
    }

    private String signedPercent(double percent) {
        String sign = percent > 0 ? "+" : "";
        return sign + String.format(Locale.ROOT, "%.2f%%", percent);
    }

    private String profitText(long profit, long costBasis) {
        if (costBasis <= 0) {
            return signedPrice(profit);
        }
        return "(" + compactSignedPercent(profit * 100.0 / costBasis) + ") " + signedPrice(profit);
    }

    private String compactSignedPercent(double percent) {
        String sign = percent > 0 ? "+" : "";
        double rounded = Math.round(percent * 10.0) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return sign + String.format(Locale.ROOT, "%.0f%%", rounded);
        }
        return sign + String.format(Locale.ROOT, "%.1f%%", rounded);
    }

    private String ownershipPercentText(long quantity, long issuedShares) {
        if (quantity <= 0 || issuedShares <= 0) {
            return "0.0000%";
        }
        double percent = quantity * 100.0 / issuedShares;
        return percent >= 1.0
                ? String.format(Locale.ROOT, "%.2f%%", percent)
                : String.format(Locale.ROOT, "%.4f%%", percent);
    }

    private String marketCapText(long marketCap) {
        long roundedEok = (marketCap + 50_000_000L) / 100_000_000L;
        if (roundedEok < 10_000L) {
            return String.format("%,d억원", roundedEok);
        }
        long jo = roundedEok / 10_000L;
        long remainingEok = roundedEok % 10_000L;
        if (remainingEok == 0L) {
            return String.format("%,d조원", jo);
        }
        return String.format("%,d조 %,d억원", jo, remainingEok);
    }

    private String indexValueText(long indexBasisPoints) {
        return String.format(Locale.ROOT, "%,.2f", indexBasisPoints / 100.0);
    }

    private String signedPrice(long amount) {
        if (amount == 0) {
            return "0원";
        }
        return (amount > 0 ? "+" : "-") + stockPriceText(Math.abs(amount));
    }

    private String stockPriceText(long amount) {
        if (amount == 0) {
            return "0원";
        }
        long eok = amount / 100_000_000L;
        amount %= 100_000_000L;
        long man = amount / 10_000L;
        long won = amount % 10_000L;
        StringBuilder builder = new StringBuilder();
        if (eok > 0) {
            builder.append(eok).append("억");
        }
        if (man > 0) {
            builder.append(man).append("만");
        }
        if (won > 0 || builder.isEmpty()) {
            builder.append(won);
        }
        return builder.append("원").toString();
    }

    /** 조 단위가 흔한 기업 실적은 주가 포맷과 분리해 30000억원 대신 3조원으로 표시한다. */
    private String financialMoneyText(long amount) {
        if (Math.abs(amount) < 100_000_000L) {
            return signedFinancialAmount(amount, stockPriceText(Math.abs(amount)));
        }
        long absolute = Math.abs(amount);
        long jo = absolute / 1_000_000_000_000L;
        long eok = absolute % 1_000_000_000_000L / 100_000_000L;
        StringBuilder builder = new StringBuilder();
        if (jo > 0) {
            builder.append(jo).append("조");
        }
        if (eok > 0) {
            builder.append(eok).append("억");
        }
        return signedFinancialAmount(amount, builder.append("원").toString());
    }

    private String signedFinancialAmount(long amount, String absoluteText) {
        return amount < 0 ? "-" + absoluteText : absoluteText;
    }

    public String moneyText(long amount) {
        return stockPriceText(amount);
    }

    public String signedMoneyText(long amount) {
        return signedPrice(amount);
    }

    private long netWorth(Player player) {
        long buildingValue = ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .mapToLong(building -> cityMarketIndexService.marketValue(player, building))
                .sum();
        long stockValue = stockListQuotes(player).stream()
                .mapToLong(quote -> quote.currentPrice() * quote.quantity())
                .sum();
        long debt = loanRepository.findByPlayer(player).stream().mapToLong(loan -> loan.getPrincipal()).sum();
        return player.getCash() + player.getSecuritiesCash() + buildingValue + stockValue - debt;
    }

    private record ChartScale(long minPrice, long maxPrice) {
    }

    private record ExpectedDividend(long perShare, long total) {
    }

}
