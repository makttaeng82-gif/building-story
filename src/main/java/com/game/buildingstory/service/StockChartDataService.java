package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockMarketIndexHistoryRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

/** 주식 차트의 과거 시세를 준비하고 가격 지점에 뉴스·실적·종합지수를 연결한다. */
@Service
@Transactional
public class StockChartDataService {
    public static final int INITIAL_HISTORY_CANDLES = 146;
    private static final int UPDATE_INTERVAL_DAYS = 5;
    private static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private final StockCatalog stockCatalog;
    private final StockMarketIndexCalculator marketIndexCalculator;
    private final StockPriceHistoryRepository priceRepository;
    private final StockMarketIndexHistoryRepository indexRepository;
    private final StockNewsArticleRepository newsRepository;
    private final StockFinancialDataService financialDataService;

    public StockChartDataService(
            StockCatalog stockCatalog,
            StockMarketIndexCalculator marketIndexCalculator,
            StockPriceHistoryRepository priceRepository,
            StockMarketIndexHistoryRepository indexRepository,
            StockNewsArticleRepository newsRepository,
            StockFinancialDataService financialDataService
    ) {
        this.stockCatalog = stockCatalog;
        this.marketIndexCalculator = marketIndexCalculator;
        this.priceRepository = priceRepository;
        this.indexRepository = indexRepository;
        this.newsRepository = newsRepository;
        this.financialDataService = financialDataService;
    }

    /** 기존 현재가를 앵커로 보존하면서 그 앞에 약 2년의 5일봉을 채운다. */
    public void ensureInitialHistory(Player player) {
        for (StockSpec stock : stockCatalog.initial()) {
            long existingCount = priceRepository.countByPlayerAndStockKey(player, stock.key());
            if (existingCount >= INITIAL_HISTORY_CANDLES) {
                continue;
            }
            StockPriceHistory anchor = priceRepository
                    .findFirstByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(player, stock.key())
                    .orElseThrow(() -> new IllegalStateException("과거 시세의 기준 가격이 없습니다."));
            int missingCount = Math.toIntExact(INITIAL_HISTORY_CANDLES - existingCount);
            priceRepository.saveAll(historicalCandles(player, stock, anchor, missingCount));
        }
    }

    @Transactional(readOnly = true)
    public List<StockChartPoint> points(Player player, StockSpec stock, ListedCompany company) {
        List<StockPriceHistory> selectedHistory = priceRepository
                .findByPlayerAndStockKeyOrderByElapsedDaysAscIdAsc(player, stock.key());
        Map<Integer, Long> indexByDay = marketIndexByDay(player);
        int firstChartDay = selectedHistory.isEmpty()
                ? player.getElapsedDays()
                : selectedHistory.getFirst().getElapsedDays();
        List<StockNewsArticle> relevantNews = newsRepository
                .findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                        player, firstChartDay).stream()
                .filter(article -> appliesTo(article, stock))
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays))
                .toList();
        List<StockFinancialSnapshot.Quarter> reports = financialDataService
                .reportEvents(player, stock, company).stream()
                .filter(report -> report.publishedElapsedDay() > 0 || report.dividendElapsedDay() > 0)
                .sorted(Comparator.comparingInt(StockFinancialSnapshot.Quarter::publishedElapsedDay))
                .toList();

        List<StockChartPoint> result = new ArrayList<>(selectedHistory.size());
        int previousDay = Integer.MIN_VALUE;
        for (StockPriceHistory price : selectedHistory) {
            int startDay = previousDay;
            List<StockNewsArticle> candleNews = relevantNews.stream()
                    .filter(article -> article.getPublishedElapsedDays() > startDay
                            && article.getPublishedElapsedDays() <= price.getElapsedDays())
                    .toList();
            List<StockFinancialSnapshot.Quarter> candleReports = reports.stream()
                    .filter(report -> report.publishedElapsedDay() > startDay
                            && report.publishedElapsedDay() <= price.getElapsedDays())
                    .toList();
            List<StockFinancialSnapshot.Quarter> candleDividends = reports.stream()
                    .filter(report -> report.dividendPerShare() > 0
                            && report.dividendElapsedDay() > startDay
                            && report.dividendElapsedDay() <= price.getElapsedDays())
                    .toList();
            StockNewsArticle headline = candleNews.isEmpty() ? null : candleNews.get(candleNews.size() - 1);
            result.add(new StockChartPoint(
                    price,
                    indexByDay.getOrDefault(price.getElapsedDays(), StockMarketIndexCalculator.BASE_INDEX_BASIS_POINTS),
                    headline == null ? null : headline.getId(),
                    headline == null ? "" : headline.getTitle(),
                    candleNews.size(),
                    !candleReports.isEmpty(),
                    !candleDividends.isEmpty()
            ));
            previousDay = price.getElapsedDays();
        }
        return result;
    }

    private List<StockPriceHistory> historicalCandles(
            Player player,
            StockSpec stock,
            StockPriceHistory anchor,
            int count
    ) {
        if (count <= 0) {
            return List.of();
        }
        SplittableRandom random = new SplittableRandom(31L * stock.key().hashCode() + anchor.getElapsedDays());
        double[] levels = new double[count + 1];
        int[] marketImpacts = new int[count];
        int[] industryImpacts = new int[count];
        int[] companyImpacts = new int[count];
        for (int index = 0; index < count; index++) {
            double marketReturn = 0.0004 + gaussian(random) * 0.010;
            double industryReturn = gaussian(random) * 0.006;
            double companyReturn = gaussian(random) * stock.idiosyncraticVolatilityPercent() / 100.0;
            marketImpacts[index] = (int) Math.round(marketReturn * stock.beta() * 10_000);
            industryImpacts[index] = (int) Math.round(industryReturn * stock.industryBeta() * 10_000);
            companyImpacts[index] = (int) Math.round(companyReturn * 10_000);
            levels[index + 1] = levels[index]
                    + (marketImpacts[index] + industryImpacts[index] + companyImpacts[index]) / 10_000.0;
        }

        double bridgePerCandle = -levels[count] / count;
        List<StockPriceHistory> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            double openLevel = levels[index] + bridgePerCandle * index;
            double closeLevel = levels[index + 1] + bridgePerCandle * (index + 1);
            long open = scaledPrice(anchor.getOpenPrice(), openLevel);
            long close = index == count - 1 ? anchor.getOpenPrice() : scaledPrice(anchor.getOpenPrice(), closeLevel);
            double wick = stock.riskType().pathNoise() * (0.35 + random.nextDouble() * 0.65);
            long high = Math.max(open, close) + Math.max(1L, Math.round(Math.max(open, close) * wick));
            long low = Math.max(1L, Math.min(open, close) - Math.max(1L, Math.round(Math.min(open, close) * wick)));
            int noiseImpact = (int) Math.round(bridgePerCandle * 10_000);
            int pathImpact = marketImpacts[index] + industryImpacts[index] + companyImpacts[index] + noiseImpact;
            int daysBeforeAnchor = (count - index) * UPDATE_INTERVAL_DAYS;
            MonthDay date = dateBefore(anchor.getMonth(), anchor.getDay(), daysBeforeAnchor);
            result.add(StockPriceHistory.historical(
                    player,
                    stock.key(),
                    date.month(),
                    date.day(),
                    anchor.getElapsedDays() - daysBeforeAnchor,
                    open,
                    high,
                    low,
                    close,
                    Math.max(100L, stock.basePrice() / 100L),
                    marketImpacts[index],
                    industryImpacts[index],
                    companyImpacts[index],
                    noiseImpact,
                    pathImpact
            ));
        }
        return result;
    }

    private Map<Integer, Long> marketIndexByDay(Player player) {
        Map<Integer, Map<String, Long>> pricesByDay = priceRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player).stream()
                .collect(Collectors.groupingBy(
                        StockPriceHistory::getElapsedDays,
                        LinkedHashMap::new,
                        Collectors.toMap(
                                StockPriceHistory::getStockKey,
                                StockPriceHistory::getClosePrice,
                                (first, ignored) -> first,
                                HashMap::new
                        )
                ));
        Map<Integer, Long> result = pricesByDay.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> marketIndexCalculator.calculate(stockCatalog.initial(), entry.getValue()),
                (first, ignored) -> first,
                LinkedHashMap::new
        ));
        // 시장 개방 전 합성 구간은 초기 종목으로 계산하고, 실제 플레이 구간은 저장된 체인링크 지수를 사용한다.
        indexRepository.findByPlayerOrderByElapsedDaysAscIdAsc(player)
                .forEach(history -> result.put(history.getElapsedDays(), history.getIndexBasisPoints()));
        return result;
    }

    private boolean appliesTo(StockNewsArticle article, StockSpec stock) {
        return switch (article.getCategory()) {
            case MARKET -> true;
            case INDUSTRY -> stock.industry().equals(article.getIndustry());
            case COMPANY, IPO -> stock.key().equals(article.getStockKey());
        };
    }

    private long scaledPrice(long anchorPrice, double logLevel) {
        return Math.max(1L, Math.round(anchorPrice * Math.exp(logLevel)));
    }

    private double gaussian(SplittableRandom random) {
        double first = Math.max(1.0e-12, random.nextDouble());
        return Math.sqrt(-2.0 * Math.log(first)) * Math.cos(2.0 * Math.PI * random.nextDouble());
    }

    private MonthDay dateBefore(int month, int day, int daysBefore) {
        int dayOfYear = day;
        for (int index = 0; index < month - 1; index++) {
            dayOfYear += DAYS_IN_MONTH[index];
        }
        int target = Math.floorMod(dayOfYear - 1 - daysBefore, 365) + 1;
        int targetMonth = 1;
        while (target > DAYS_IN_MONTH[targetMonth - 1]) {
            target -= DAYS_IN_MONTH[targetMonth - 1];
            targetMonth++;
        }
        return new MonthDay(targetMonth, target);
    }

    private record MonthDay(int month, int day) {
    }
}
