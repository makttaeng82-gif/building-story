package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

@Entity
public class StockPriceHistory {
    /*
     * 종목별 가격 캔들 이력이다.
     *
     * 한 행은 한 번의 주가 갱신 결과를 뜻한다. open/high/low/close/volume 구조를 쓰기 때문에
     * 나중에 차트, 수익률, 추세 계산을 같은 데이터로 처리할 수 있다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    @Column(name = "record_month")
    private int month;
    @Column(name = "record_day")
    private int day;
    private int elapsedDays;
    private long openPrice;
    private long highPrice;
    private long lowPrice;
    private long closePrice;
    private long volume;
    private Integer marketImpactBasisPoints;
    private Integer industryImpactBasisPoints;
    private Integer companyImpactBasisPoints;
    private Integer earningsImpactBasisPoints;
    private Integer valuationImpactBasisPoints;
    private Integer trendImpactBasisPoints;
    private Integer idiosyncraticImpactBasisPoints;
    private Integer noiseImpactBasisPoints;
    private Integer pathImpactBasisPoints;
    private Integer listingImpactBasisPoints;

    protected StockPriceHistory() {
    }

    public StockPriceHistory(Player player, String stockKey, long openPrice, long highPrice, long lowPrice, long closePrice, long volume) {
        this(player, stockKey, openPrice, highPrice, lowPrice, closePrice, volume, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public StockPriceHistory(
            Player player,
            String stockKey,
            long openPrice,
            long highPrice,
            long lowPrice,
            long closePrice,
            long volume,
            int marketImpactBasisPoints,
            int industryImpactBasisPoints,
            int companyImpactBasisPoints,
            int earningsImpactBasisPoints,
            int valuationImpactBasisPoints,
            int trendImpactBasisPoints,
            int idiosyncraticImpactBasisPoints,
            int noiseImpactBasisPoints,
            int pathImpactBasisPoints
    ) {
        this.player = player;
        this.stockKey = stockKey;
        this.month = player.getMonth();
        this.day = player.getDay();
        this.elapsedDays = player.getElapsedDays();
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.marketImpactBasisPoints = marketImpactBasisPoints;
        this.industryImpactBasisPoints = industryImpactBasisPoints;
        this.companyImpactBasisPoints = companyImpactBasisPoints;
        this.earningsImpactBasisPoints = earningsImpactBasisPoints;
        this.valuationImpactBasisPoints = valuationImpactBasisPoints;
        this.trendImpactBasisPoints = trendImpactBasisPoints;
        this.idiosyncraticImpactBasisPoints = idiosyncraticImpactBasisPoints;
        this.noiseImpactBasisPoints = noiseImpactBasisPoints;
        this.pathImpactBasisPoints = pathImpactBasisPoints;
        this.listingImpactBasisPoints = 0;
    }

    /**
     * 주식시장 개방 전에 존재했던 과거 캔들을 만든다.
     * 플레이어의 현재 날짜를 바꾸지 않기 위해 기록 날짜와 경과일을 명시적으로 받는다.
     */
    public static StockPriceHistory historical(
            Player player,
            String stockKey,
            int month,
            int day,
            int elapsedDays,
            long openPrice,
            long highPrice,
            long lowPrice,
            long closePrice,
            long volume,
            int marketImpactBasisPoints,
            int industryImpactBasisPoints,
            int idiosyncraticImpactBasisPoints,
            int noiseImpactBasisPoints,
            int pathImpactBasisPoints
    ) {
        StockPriceHistory history = new StockPriceHistory(
                player, stockKey, openPrice, highPrice, lowPrice, closePrice, volume,
                marketImpactBasisPoints, industryImpactBasisPoints, 0, 0, 0, 0,
                idiosyncraticImpactBasisPoints, noiseImpactBasisPoints, pathImpactBasisPoints
        );
        history.month = month;
        history.day = day;
        history.elapsedDays = elapsedDays;
        return history;
    }

    /** 신규상장 첫 5일봉을 공모 수요 기여도와 함께 저장한다. */
    public static StockPriceHistory listing(
            Player player,
            String stockKey,
            long openPrice,
            long highPrice,
            long lowPrice,
            long closePrice,
            long volume,
            int listingImpactBasisPoints,
            int marketImpactBasisPoints,
            int industryImpactBasisPoints,
            int valuationImpactBasisPoints,
            int idiosyncraticImpactBasisPoints,
            int pathImpactBasisPoints
    ) {
        StockPriceHistory history = new StockPriceHistory(
                player, stockKey, openPrice, highPrice, lowPrice, closePrice, volume,
                marketImpactBasisPoints, industryImpactBasisPoints, 0, 0,
                valuationImpactBasisPoints, 0, idiosyncraticImpactBasisPoints,
                idiosyncraticImpactBasisPoints, pathImpactBasisPoints
        );
        history.listingImpactBasisPoints = listingImpactBasisPoints;
        return history;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getStockKey() {
        return stockKey;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getElapsedDays() {
        return elapsedDays;
    }

    public String getDateText() {
        return GameCalendar.dateText(elapsedDays);
    }

    public String getShortDateText() {
        return GameCalendar.shortDateText(elapsedDays);
    }

    public long getOpenPrice() {
        return openPrice;
    }

    public long getHighPrice() {
        return highPrice;
    }

    public long getLowPrice() {
        return lowPrice;
    }

    public long getClosePrice() {
        return closePrice;
    }

    public long getVolume() {
        return volume;
    }

    public int getMarketImpactBasisPoints() { return marketImpactBasisPoints == null ? 0 : marketImpactBasisPoints; }
    public int getIndustryImpactBasisPoints() { return industryImpactBasisPoints == null ? 0 : industryImpactBasisPoints; }
    public int getCompanyImpactBasisPoints() { return companyImpactBasisPoints == null ? 0 : companyImpactBasisPoints; }
    public int getEarningsImpactBasisPoints() { return earningsImpactBasisPoints == null ? 0 : earningsImpactBasisPoints; }
    public int getValuationImpactBasisPoints() { return valuationImpactBasisPoints == null ? 0 : valuationImpactBasisPoints; }
    public int getTrendImpactBasisPoints() { return trendImpactBasisPoints == null ? 0 : trendImpactBasisPoints; }
    public int getIdiosyncraticImpactBasisPoints() { return idiosyncraticImpactBasisPoints == null ? 0 : idiosyncraticImpactBasisPoints; }
    public int getNoiseImpactBasisPoints() { return noiseImpactBasisPoints == null ? 0 : noiseImpactBasisPoints; }
    public int getPathImpactBasisPoints() { return pathImpactBasisPoints == null ? 0 : pathImpactBasisPoints; }
    public int getListingImpactBasisPoints() { return listingImpactBasisPoints == null ? 0 : listingImpactBasisPoints; }

    /** 체결 직후 현재 캔들의 종가와 고가·저가를 함께 보정한다. 갱신일은 바꾸지 않는다. */
    public void applyTradePrice(long tradedPrice) {
        if (tradedPrice <= 0) {
            throw new IllegalArgumentException("체결 가격은 1원 이상이어야 합니다.");
        }
        closePrice = tradedPrice;
        highPrice = Math.max(highPrice, tradedPrice);
        lowPrice = Math.min(lowPrice, tradedPrice);
    }

    /** 배당금만큼 기업가치가 빠지는 배당락을 현재 캔들 가격에 반영한다. */
    public void applyDividendExDate(long dividendPerShare) {
        if (dividendPerShare <= 0) {
            return;
        }
        long adjusted = Math.max(1L, closePrice - dividendPerShare);
        closePrice = adjusted;
        openPrice = Math.max(1L, openPrice - dividendPerShare);
        highPrice = Math.max(adjusted, highPrice - dividendPerShare);
        lowPrice = Math.max(1L, lowPrice - dividendPerShare);
    }
}
