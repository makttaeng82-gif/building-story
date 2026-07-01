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

    protected StockPriceHistory() {
    }

    public StockPriceHistory(Player player, String stockKey, long openPrice, long highPrice, long lowPrice, long closePrice, long volume) {
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
}
