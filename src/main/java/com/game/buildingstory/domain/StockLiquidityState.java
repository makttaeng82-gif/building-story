package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 한 종목에서 현재 5일 갱신 구간 동안 누적된 플레이어 주문 흐름을 저장한다. */
@Entity
@Table(name = "stock_liquidity_state", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_liquidity_player_key", columnNames = {"player_id", "stock_key"}))
public class StockLiquidityState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    private long capacity;
    private long netPlayerBuyQuantity;
    private long referencePrice;
    private int refreshedElapsedDays;

    protected StockLiquidityState() {
    }

    public StockLiquidityState(Player player, String stockKey, long capacity, long referencePrice) {
        this.player = player;
        this.stockKey = stockKey;
        refresh(capacity, referencePrice, player.getElapsedDays());
    }

    public String getStockKey() { return stockKey; }
    public long getCapacity() { return capacity; }
    public long getNetPlayerBuyQuantity() { return netPlayerBuyQuantity; }
    public long getReferencePrice() { return referencePrice; }
    public int getRefreshedElapsedDays() { return refreshedElapsedDays; }

    /** 새 5일 가격이 만들어지면 주문 누적량을 비우고 그 가격을 새 충격 기준가로 삼는다. */
    public void refresh(long capacity, long referencePrice, int elapsedDays) {
        if (capacity <= 0 || referencePrice <= 0) {
            throw new IllegalArgumentException("유동성 수량과 기준가는 1 이상이어야 합니다.");
        }
        this.capacity = capacity;
        this.netPlayerBuyQuantity = 0;
        this.referencePrice = referencePrice;
        this.refreshedElapsedDays = elapsedDays;
    }

    public void applyNetBuyChange(long signedQuantity) {
        long next = Math.addExact(netPlayerBuyQuantity, signedQuantity);
        if (Math.abs(next) > capacity) {
            throw new IllegalArgumentException("현재 유동성을 초과한 주문입니다.");
        }
        netPlayerBuyQuantity = next;
    }
}
