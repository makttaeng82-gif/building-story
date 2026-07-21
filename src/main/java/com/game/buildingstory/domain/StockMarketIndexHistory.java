package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 플레이어별 종합지수의 5일 단위 값을 저장한다. 지수값은 소수 둘째 자리까지 보존하기 위해 100배 정수로 저장한다. */
@Entity
@Table(name = "stock_market_index_history", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_market_index_player_day", columnNames = {"player_id", "elapsed_days"}))
public class StockMarketIndexHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @Column(name = "record_month")
    private int month;
    @Column(name = "record_day")
    private int day;
    private int elapsedDays;
    private long indexBasisPoints;
    private int changeBasisPoints;

    protected StockMarketIndexHistory() {
    }

    public StockMarketIndexHistory(
            Player player,
            int month,
            int day,
            int elapsedDays,
            long indexBasisPoints,
            int changeBasisPoints
    ) {
        this.player = player;
        this.month = month;
        this.day = day;
        this.elapsedDays = elapsedDays;
        this.indexBasisPoints = indexBasisPoints;
        this.changeBasisPoints = changeBasisPoints;
    }

    public Player getPlayer() { return player; }
    public int getMonth() { return month; }
    public int getDay() { return day; }
    public int getElapsedDays() { return elapsedDays; }
    public long getIndexBasisPoints() { return indexBasisPoints; }
    public int getChangeBasisPoints() { return changeBasisPoints; }
}
