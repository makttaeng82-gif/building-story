package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.GameEventRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService {
    private static final String STOCK_UNLOCK_EFFECT = "NONE";
    private static final String STOCK_UNLOCK_IMAGE = "AI 주식 이미지";
    public static final String STOCK_NEWS_BOOM = "BOOM";
    public static final String STOCK_NEWS_RECESSION = "RECESSION";
    private static final int UPDATE_INTERVAL_DAYS = 5;
    private static final int INDUSTRY_NEWS_CHANCE_PERCENT = 15;
    private static final double TREND_EFFECT_PERCENT = 0.5;
    private static final double SHOCK_CHANCE = 0.02;
    private static final double NORMAL_LIMIT_PERCENT = 12.0;
    private static final double SHOCK_LIMIT_PERCENT = 25.0;
    private static final int CASH_PER_COIN = 100;
    private static final double TRADE_FEE_RATE = 0.005;

    private final Random random = new Random();
    private final StockCatalog stockCatalog;
    private final ReputationCatalog reputationCatalog;
    private final GameEventRepository gameEventRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final OwnedStockRepository ownedStockRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockTradeHistoryRepository stockTradeHistoryRepository;

    public StockService(
            StockCatalog stockCatalog,
            ReputationCatalog reputationCatalog,
            GameEventRepository gameEventRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            OwnedStockRepository ownedStockRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockTradeHistoryRepository stockTradeHistoryRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.reputationCatalog = reputationCatalog;
        this.gameEventRepository = gameEventRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.ownedStockRepository = ownedStockRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockTradeHistoryRepository = stockTradeHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<StockSpec> stocks() {
        return stockCatalog.all();
    }

    public void ensureUnlockSchedule(Player player) {
        if (player.isStockContentUnlocked() || player.hasStockUnlockSchedule()) {
            return;
        }
        if (reputationCatalog.isCityUnlocked("서울", player.getReputation(), !player.isEmployed())) {
            player.scheduleStockUnlock(player.getElapsedDays() + 2);
        }
    }

    public boolean activateUnlockNoticeIfDue(Player player) {
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
                "서울 진출 이후 증권 계좌가 개설되었습니다. 이제 주식 투자를 할 수 있습니다.",
                STOCK_UNLOCK_IMAGE,
                STOCK_UNLOCK_EFFECT,
                "확인"
        ));
        player.pause();
        return true;
    }

    public void processPriceUpdates(Player player) {
        if (!player.isStockContentUnlocked()) {
            return;
        }
        ensureMarketInitialized(player);
        int lastUpdateDay = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .map(StockPriceHistory::getElapsedDays)
                .orElse(player.getElapsedDays());
        if (player.getElapsedDays() - lastUpdateDay < UPDATE_INTERVAL_DAYS) {
            return;
        }
        double marketEffectPercent = marketEffectPercent();
        stockCatalog.all().forEach(stock -> appendNextHistory(player, stock, marketEffectPercent));
        player.consumeStockNewsRefresh();
    }

    public boolean activateIndustryNewsIfDue(Player player) {
        if (!player.isStockContentUnlocked()) {
            return false;
        }
        ensureMonthlyIndustryNewsSchedule(player);
        if (!player.isStockNewsEventDay()) {
            return false;
        }
        if (gameEventRepository.findFirstByPlayerAndStatus(player, GameEventStatus.ACTIVE).isPresent()) {
            return false;
        }
        String industry = player.getStockNewsEventIndustry();
        String trend = player.getStockNewsEventTrend();
        player.activateStockNews();
        String trendLabel = STOCK_NEWS_BOOM.equals(trend) ? "호황" : "불황";
        monthlyRecordRepository.save(new MonthlyRecord(
                player,
                RecordType.STOCK_EVENT,
                "주식 " + industry + " " + trendLabel + " 뉴스",
                null,
                0,
                industry,
                "다음 주가갱신 2회 적용"
        ));
        gameEventRepository.save(new GameEvent(
                player,
                "stock_news_" + player.getId() + "_" + player.getElapsedDays() + "_" + industry + "_" + trend,
                industry + " 업종 " + trendLabel + " 뉴스",
                STOCK_NEWS_BOOM.equals(trend)
                        ? industry + " 업종 수요가 살아나며 다음 주가갱신 2회 동안 상승 압력이 강해집니다."
                        : industry + " 업종 실적 우려가 커지며 다음 주가갱신 2회 동안 하락 압력이 강해집니다.",
                stockNewsImagePath(industry, trend),
                "NONE",
                "확인"
        ));
        player.pause();
        return true;
    }

    @Transactional(readOnly = true)
    public StockMarketStatusView marketStatus(Player player) {
        int lastUpdateDay = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .map(StockPriceHistory::getElapsedDays)
                .orElse(player.getElapsedDays());
        int daysSinceUpdate = Math.max(0, player.getElapsedDays() - lastUpdateDay);
        int daysUntilNextUpdate = Math.max(0, UPDATE_INTERVAL_DAYS - daysSinceUpdate);
        int progressPercent = Math.min(100, daysSinceUpdate * 100 / UPDATE_INTERVAL_DAYS);
        return new StockMarketStatusView(
                player.dateTextAfterDays(daysUntilNextUpdate),
                daysUntilNextUpdate,
                progressPercent
        );
    }

    public void ensureMarketInitialized(Player player) {
        stockCatalog.all().forEach(stock -> {
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
    }

    @Transactional(readOnly = true)
    public List<StockQuoteView> stockQuotes(Player player) {
        Map<String, OwnedStock> ownedStocks = ownedStockRepository.findByPlayer(player).stream()
                .collect(Collectors.toMap(OwnedStock::getStockKey, Function.identity()));
        return stockCatalog.all().stream()
                .map(stock -> quote(player, stock, ownedStocks.get(stock.key())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockTradeHistory> tradeHistories(Player player) {
        return stockTradeHistoryRepository.findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(
                player,
                Math.max(1, player.getElapsedDays() - 89)
        );
    }

    @Transactional(readOnly = true)
    public StockHoldingSummaryView holdingSummary(Player player) {
        List<StockQuoteView> ownedQuotes = stockQuotes(player).stream()
                .filter(quote -> quote.quantity() > 0)
                .toList();
        long totalQuantity = ownedQuotes.stream().mapToLong(StockQuoteView::quantity).sum();
        long totalCost = ownedQuotes.stream().mapToLong(quote -> quote.averagePrice() * quote.quantity()).sum();
        long totalValuation = ownedQuotes.stream().mapToLong(quote -> quote.currentPrice() * quote.quantity()).sum();
        long totalProfit = totalValuation - totalCost;
        return new StockHoldingSummaryView(
                ownedQuotes.size(),
                totalQuantity,
                totalCost,
                totalValuation,
                totalProfit,
                stockPriceText(totalCost),
                stockPriceText(totalValuation),
                signedPrice(totalProfit),
                changeDirection(totalProfit)
        );
    }

    public String exchangeCashToCoin(Player player, long coinAmount) {
        if (coinAmount <= 0) {
            return "교환 수량 오류";
        }
        long cashCost = coinAmount * CASH_PER_COIN;
        if (!player.spendCash(cashCost)) {
            return "현금 부족";
        }
        player.addCoin(coinAmount);
        return coinText(coinAmount) + " 교환";
    }

    public String exchangeCoinToCash(Player player, long coinAmount) {
        if (coinAmount <= 0) {
            return "교환 수량 오류";
        }
        if (!player.spendCoin(coinAmount)) {
            return "코인 부족";
        }
        player.addCash(coinAmount * CASH_PER_COIN);
        return coinText(coinAmount) + " 환전";
    }

    public String buyStock(Player player, String stockKey, long quantity) {
        if (quantity <= 0) {
            return "매수 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        long price = currentPrice(player, stock);
        long grossAmount = price * quantity;
        long fee = tradeFee(grossAmount);
        long totalCost = grossAmount + fee;
        if (!player.spendCoin(totalCost)) {
            return "코인 부족 · 필요 " + coinText(totalCost) + " / 보유 " + coinText(player.getCoin());
        }
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey)
                .orElseGet(() -> ownedStockRepository.save(new OwnedStock(player, stockKey)));
        ownedStock.buy(quantity, price);
        stockTradeHistoryRepository.save(new StockTradeHistory(player, stock.key(), stock.name(), "매수", quantity, price, grossAmount, fee, totalCost));
        return stock.name() + " " + quantity + "주 매수";
    }

    public String buyMaxStock(Player player, String stockKey) {
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        long price = currentPrice(player, stock);
        long quantity = maxAffordableQuantity(player.getCoin(), price);
        if (quantity <= 0) {
            long minimumCost = price + tradeFee(price);
            return "코인 부족 · 필요 " + coinText(minimumCost) + " / 보유 " + coinText(player.getCoin());
        }
        return buyStock(player, stockKey, quantity);
    }

    public String sellStock(Player player, String stockKey, long quantity) {
        if (quantity <= 0) {
            return "매도 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (ownedStock == null || ownedStock.getQuantity() < quantity) {
            long ownedQuantity = ownedStock == null ? 0 : ownedStock.getQuantity();
            return "보유 수량 부족 · 보유 " + ownedQuantity + "주 / 매도 요청 " + quantity + "주";
        }
        long price = currentPrice(player, stock);
        long grossAmount = price * quantity;
        long fee = tradeFee(grossAmount);
        long payout = Math.max(0, grossAmount - fee);
        ownedStock.sell(quantity);
        player.addCoin(payout);
        stockTradeHistoryRepository.save(new StockTradeHistory(player, stock.key(), stock.name(), "매도", quantity, price, grossAmount, fee, payout));
        return stock.name() + " " + quantity + "주 매도";
    }

    public String sellAllStock(Player player, String stockKey) {
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
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
        return "서울 해금 필요";
    }

    private void appendNextHistory(Player player, StockSpec stock, double marketEffectPercent) {
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
        long open = latest.getClosePrice();
        StockChange change = stockChangePercent(player, stock, marketEffectPercent);
        double changePercent = change.percent();
        long close = Math.max(1L, Math.round(open * (100.0 + changePercent) / 100.0));
        long highBase = Math.max(open, close);
        long lowBase = Math.min(open, close);
        long high = Math.max(highBase, Math.round(highBase * (100.0 + random.nextDouble(0.0, 3.0)) / 100.0));
        long low = Math.max(1L, Math.min(lowBase, Math.round(lowBase * (100.0 - random.nextDouble(0.0, 3.0)) / 100.0)));
        stockPriceHistoryRepository.save(new StockPriceHistory(player, stock.key(), open, high, low, close, randomVolume(stock, changePercent)));
    }

    private StockChange stockChangePercent(Player player, StockSpec stock, double marketEffectPercent) {
        double shockEffectPercent = 0.0;
        boolean hasShock = false;
        double shockRoll = random.nextDouble();
        if (shockRoll < SHOCK_CHANCE) {
            shockEffectPercent = randomChangePercent(8.0, 18.0);
            hasShock = true;
        } else if (shockRoll < SHOCK_CHANCE * 2) {
            shockEffectPercent = -randomChangePercent(8.0, 18.0);
            hasShock = true;
        }

        double rawPercent = marketEffectPercent
                + industryEffectPercent(player, stock)
                + trendEffectPercent(player, stock)
                + randomChangePercent(stock.riskType().minNoisePercent(), stock.riskType().maxNoisePercent())
                + shockEffectPercent;
        double limit = hasShock ? SHOCK_LIMIT_PERCENT : NORMAL_LIMIT_PERCENT;
        return new StockChange(clamp(rawPercent, -limit, limit), hasShock);
    }

    private double marketEffectPercent() {
        return switch (random.nextInt(3)) {
            case 0 -> randomChangePercent(0.5, 2.0);
            case 1 -> randomChangePercent(-0.7, 0.7);
            default -> randomChangePercent(-2.0, -0.5);
        };
    }

    private double industryEffectPercent(Player player, StockSpec stock) {
        if (!player.hasActiveStockNewsForIndustry(stock.industry())) {
            return 0.0;
        }
        if (STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend())) {
            return randomChangePercent(3.0, 8.0);
        }
        if (STOCK_NEWS_RECESSION.equals(player.getActiveStockNewsTrend())) {
            return -randomChangePercent(3.0, 8.0);
        }
        return 0.0;
    }

    private void ensureMonthlyIndustryNewsSchedule(Player player) {
        if (player.hasStockNewsScheduleForCurrentMonth()) {
            return;
        }
        if (!rollPercent(INDUSTRY_NEWS_CHANCE_PERCENT)) {
            player.scheduleNoMonthlyStockNews();
            return;
        }
        List<String> industries = stockCatalog.all().stream()
                .map(StockSpec::industry)
                .distinct()
                .toList();
        String industry = industries.get(random.nextInt(industries.size()));
        String trend = random.nextBoolean() ? STOCK_NEWS_BOOM : STOCK_NEWS_RECESSION;
        player.scheduleMonthlyStockNews(randomStockNewsDay(player), industry, trend);
    }

    private int randomStockNewsDay(Player player) {
        int firstDay = Math.max(2, player.getDay());
        int lastDay = player.getDaysInCurrentMonth();
        if (firstDay >= lastDay) {
            return lastDay;
        }
        return random.nextInt(lastDay - firstDay + 1) + firstDay;
    }

    private boolean rollPercent(int percent) {
        return random.nextInt(100) < percent;
    }

    private String stockNewsImagePath(String industry, String trend) {
        String industrySlug = switch (industry) {
            case "IT" -> "it";
            case "식품" -> "food";
            case "유통" -> "retail";
            case "제조" -> "manufacturing";
            case "통신" -> "telecom";
            default -> "unknown";
        };
        return "/assets/stock-news/" + industrySlug + "-" + (STOCK_NEWS_BOOM.equals(trend) ? "boom" : "recession") + ".jpg";
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

    private StockQuoteView quote(Player player, StockSpec stock, OwnedStock ownedStock) {
        List<StockPriceHistory> latestRows = stockPriceHistoryRepository.findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        StockPriceHistory current = latestRows.isEmpty()
                ? new StockPriceHistory(player, stock.key(), stock.basePrice(), stock.basePrice(), stock.basePrice(), stock.basePrice(), initialVolume(stock))
                : latestRows.get(0);
        long previousPrice = latestRows.size() > 1 ? latestRows.get(1).getClosePrice() : current.getClosePrice();
        long currentPrice = current.getClosePrice();
        long changeAmount = currentPrice - previousPrice;
        double changePercent = previousPrice == 0 ? 0.0 : changeAmount * 100.0 / previousPrice;
        long quantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        long averagePrice = ownedStock == null ? 0 : ownedStock.getAveragePrice();
        long valuationProfit = quantity == 0 ? 0 : (currentPrice - averagePrice) * quantity;
        List<StockPriceHistory> history = stockPriceHistoryRepository.findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key());
        Collections.reverse(history);
        ChartScale scale = chartScale(history, currentPrice);
        return new StockQuoteView(
                stock,
                currentPrice,
                previousPrice,
                changeAmount,
                changePercent,
                stockPriceText(currentPrice),
                stockPriceText(previousPrice),
                signedPercent(changePercent),
                signedPrice(changeAmount),
                changeDirection(changeAmount),
                quantity,
                averagePrice,
                stockPriceText(averagePrice),
                valuationProfit,
                signedPrice(valuationProfit),
                candleViews(history, scale),
                stockPriceText(scale.minPrice()),
                stockPriceText(scale.maxPrice()),
                String.format(Locale.ROOT, "%.1f", priceY(currentPrice, scale)),
                stockPriceText(currentPrice)
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

    private long maxAffordableQuantity(long coin, long price) {
        long low = 0;
        long high = Math.max(0, coin / price);
        while (low < high) {
            long mid = (low + high + 1) / 2;
            long grossAmount = price * mid;
            if (grossAmount + tradeFee(grossAmount) <= coin) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private List<StockCandleView> candleViews(List<StockPriceHistory> history, ChartScale scale) {
        if (history.isEmpty()) {
            return List.of();
        }
        int count = history.size();
        List<StockCandleView> candles = new ArrayList<>();
        int rightEdgeX = 690;
        int candleSpacing = 11;
        int startX = rightEdgeX - ((count - 1) * candleSpacing);
        for (int index = 0; index < count; index++) {
            StockPriceHistory row = history.get(index);
            int x = startX + (index * candleSpacing);
            int openY = (int) Math.round(priceY(row.getOpenPrice(), scale));
            int highY = (int) Math.round(priceY(row.getHighPrice(), scale));
            int lowY = (int) Math.round(priceY(row.getLowPrice(), scale));
            int closeY = (int) Math.round(priceY(row.getClosePrice(), scale));
            candles.add(new StockCandleView(
                    x,
                    openY,
                    highY,
                    lowY,
                    closeY,
                    Math.min(openY, closeY),
                    Math.max(2, Math.abs(openY - closeY)),
                    row.getClosePrice() >= row.getOpenPrice(),
                    row.getMonth() + "/" + row.getDay()
            ));
        }
        return candles;
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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

    private String signedPrice(long amount) {
        if (amount == 0) {
            return "0코인";
        }
        return (amount > 0 ? "+" : "-") + stockPriceText(Math.abs(amount));
    }

    private String stockPriceText(long amount) {
        if (amount == 0) {
            return "0코인";
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
        return builder.append("코인").toString();
    }

    public String coinText(long amount) {
        return stockPriceText(amount);
    }

    private record ChartScale(long minPrice, long maxPrice) {
    }

    private record StockChange(double percent, boolean hasShock) {
    }
}
