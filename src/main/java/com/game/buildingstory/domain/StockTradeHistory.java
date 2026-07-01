package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class StockTradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    private String stockName;
    private String tradeType;
    private long quantity;
    private long price;
    private long grossAmount;
    private long fee;
    private long netAmount;
    @Column(name = "record_month")
    private int month;
    @Column(name = "record_day")
    private int day;
    private int elapsedDays;

    protected StockTradeHistory() {
    }

    public StockTradeHistory(Player player, String stockKey, String stockName, String tradeType, long quantity, long price, long grossAmount, long fee, long netAmount) {
        this.player = player;
        this.stockKey = stockKey;
        this.stockName = stockName;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.grossAmount = grossAmount;
        this.fee = fee;
        this.netAmount = netAmount;
        this.month = player.getMonth();
        this.day = player.getDay();
        this.elapsedDays = player.getElapsedDays();
    }

    public String getStockName() {
        return stockName;
    }

    public String getStockKey() {
        return stockKey;
    }

    public String getTradeType() {
        return tradeType;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getPrice() {
        return price;
    }

    public long getFee() {
        return fee;
    }

    public long getNetAmount() {
        return netAmount;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }
}
