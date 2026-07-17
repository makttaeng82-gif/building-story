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
    /*
     * StockService는 주식 컨텐츠의 서버 규칙을 담당한다.
     *
     * 주요 책임:
     * 1. 서울 진출 후 주식 기능 개방 예약/이벤트 표시
     * 2. 종목별 가격 이력 생성과 5일 주기 가격 갱신
     * 3. 현금 <-> 코인 교환
     * 4. 현재가 기준 즉시 매수/매도
     * 5. 업종 호황/불황 뉴스 효과 적용
     *
     * 주식은 부동산과 별도 재화인 코인을 사용한다. 현금과 주식을 직접 섞지 않으면
     * 주식 손실이 부동산 구매력 전체를 즉시 망가뜨리는 상황을 줄일 수 있다.
     */
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
        // 주식은 서울 해금 후 2 elapsedDays 뒤에 열린다. 조건을 만족한 최초 1회만 예약한다.
        if (player.isStockContentUnlocked() || player.hasStockUnlockSchedule()) {
            return;
        }
        if (reputationCatalog.isCityUnlocked("서울", player.getReputation(), !player.isEmployed())) {
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
                "서울 진출 이후 증권 계좌가 개설되었습니다. 이제 주식 투자를 할 수 있습니다.",
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
        int lastUpdateDay = stockPriceHistoryRepository.findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
                .map(StockPriceHistory::getElapsedDays)
                .orElse(player.getElapsedDays());
        if (player.getElapsedDays() - lastUpdateDay < UPDATE_INTERVAL_DAYS) {
            return;
        }
        // marketEffectPercent는 이번 5일 구간의 시장 분위기다. 한 번 뽑아 모든 종목에 공통 적용한다.
        double marketEffectPercent = marketEffectPercent();
        stockCatalog.all().forEach(stock -> appendNextHistory(player, stock, marketEffectPercent));
        // 업종 뉴스 효과는 "주가 갱신 횟수" 기준으로 줄어든다. 날짜 기준으로 줄이면 갱신 없는 날에도 효과가 사라진다.
        player.consumeStockNewsRefresh();
    }

    public boolean activateIndustryNewsIfDue(Player player) {
        // 업종 뉴스는 월별로 미리 예약해 두고, 예약일이 되면 active 상태로 전환한다.
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
                progressPercent,
                activeStockNewsText(player),
                activeStockNewsDirection(player)
        );
    }

    public void ensureMarketInitialized(Player player) {
        // 새로 주식이 열린 플레이어에게 종목별 최초 가격 행을 만든다.
        // 이미 존재하는 종목은 건드리지 않아 기존 차트 이력을 보존한다.
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
        // 보유요약은 현재가 기준 평가금액과 평균단가 기준 원가를 비교해 전체 손익을 계산한다.
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
                profitText(totalProfit, totalCost),
                changeDirection(totalProfit)
        );
    }

    public String exchangeCashToCoin(Player player, long coinAmount) {
        // 주식 거래는 코인으로만 한다. 현금을 코인으로 바꿀 때 고정 환율을 적용한다.
        if (coinAmount <= 0) {
            return "교환 수량 오류";
        }
        Long cashCost = safeMultiply(coinAmount, CASH_PER_COIN);
        if (cashCost == null || safeAdd(player.getCoin(), coinAmount) == null) {
            return "교환 수량 오류";
        }
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
        Long cashPayout = safeMultiply(coinAmount, CASH_PER_COIN);
        if (cashPayout == null || safeAdd(player.getCash(), cashPayout) == null) {
            return "환전 수량 오류";
        }
        if (!player.spendCoin(coinAmount)) {
            return "코인 부족";
        }
        player.addCash(cashPayout);
        return coinText(coinAmount) + " 환전";
    }

    public String buyStock(Player player, String stockKey, long quantity) {
        // 매수는 현재가 시장가 주문으로 처리한다. 지정가 주문이나 주문 대기열은 없다.
        if (quantity <= 0) {
            return "매수 수량 오류";
        }
        if (!player.isStockContentUnlocked()) {
            return "주식 미개방";
        }
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        long price = currentPrice(player, stock);
        Long grossAmount = safeMultiply(price, quantity);
        if (grossAmount == null) {
            return "매수 수량 오류";
        }
        long fee = tradeFee(grossAmount);
        Long totalCost = safeAdd(grossAmount, fee);
        if (totalCost == null) {
            return "매수 수량 오류";
        }
        OwnedStock ownedStock = ownedStockRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        long ownedQuantity = ownedStock == null ? 0 : ownedStock.getQuantity();
        long ownedAveragePrice = ownedStock == null ? 0 : ownedStock.getAveragePrice();
        Long existingCostBasis = safeMultiply(ownedAveragePrice, ownedQuantity);
        if (safeAdd(ownedQuantity, quantity) == null
                || existingCostBasis == null
                || safeAdd(existingCostBasis, grossAmount) == null) {
            return "매수 수량 오류";
        }
        // 코인이 부족하면 보유 수량이나 거래 이력은 변경하지 않고 메시지만 반환한다.
        if (!player.spendCoin(totalCost)) {
            return "코인 부족 · 필요 " + coinText(totalCost) + " / 보유 " + coinText(player.getCoin());
        }
        if (ownedStock == null) {
            ownedStock = ownedStockRepository.save(new OwnedStock(player, stockKey));
        }
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
        // 매도도 현재가 시장가 주문이다. 공매도/마진이 없으므로 보유 수량보다 많이 팔 수 없다.
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
        Long grossAmount = safeMultiply(price, quantity);
        if (grossAmount == null) {
            return "매도 수량 오류";
        }
        long fee = tradeFee(grossAmount);
        long payout = Math.max(0, grossAmount - fee);
        if (safeAdd(player.getCoin(), payout) == null) {
            return "매도 수량 오류";
        }
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
        // OHLC 한 줄은 5일 단위 캔들 하나다. open은 직전 close, close는 이번 변동률을 적용한 가격이다.
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
        // 가격 변동률은 시장 공통 효과 + 업종 뉴스 + 추세 + 종목 위험도별 노이즈 + 희귀 충격을 합산한다.
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
        // active 뉴스의 업종과 종목 업종이 일치할 때만 효과를 준다.
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

    private String activeStockNewsText(Player player) {
        if (player.getActiveStockNewsRefreshesLeft() <= 0 || player.getActiveStockNewsIndustry() == null) {
            return "";
        }
        String trendLabel = STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend()) ? "호황" : "불황";
        return player.getActiveStockNewsIndustry() + " " + trendLabel + " 적용중 · "
                + player.getActiveStockNewsRefreshesLeft() + "회 남음";
    }

    private String activeStockNewsDirection(Player player) {
        if (player.getActiveStockNewsRefreshesLeft() <= 0) {
            return "flat";
        }
        if (STOCK_NEWS_BOOM.equals(player.getActiveStockNewsTrend())) {
            return "up";
        }
        if (STOCK_NEWS_RECESSION.equals(player.getActiveStockNewsTrend())) {
            return "down";
        }
        return "flat";
    }

    private void ensureMonthlyIndustryNewsSchedule(Player player) {
        // 매월 처음 확인할 때 이번 달 주식 뉴스 발생 여부와 날짜를 확정한다.
        // 확정값을 저장해야 새로고침이나 서버 재시작 후에도 같은 달 이벤트가 유지된다.
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
        // 화면에 필요한 주식 정보는 엔티티 그대로 넘기지 않고 View record로 조립한다.
        // 이렇게 하면 템플릿은 계산 없이 표시만 담당하고, 계산 규칙은 서비스에 남는다.
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
                profitText(valuationProfit, averagePrice * quantity),
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

    private long maxAffordableQuantity(long coin, long price) {
        // 수수료가 수량에 비례해 붙기 때문에 단순 coin / price로는 최대 매수 가능 수량이 틀릴 수 있다.
        // 이분 탐색으로 "수수료 포함 총액이 보유 코인 이하인 가장 큰 수량"을 찾는다.
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
        // 서버에서 SVG 좌표를 미리 계산한다. 브라우저는 좌표를 받아 그리기만 하므로 JS 차트 라이브러리가 필요 없다.
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
