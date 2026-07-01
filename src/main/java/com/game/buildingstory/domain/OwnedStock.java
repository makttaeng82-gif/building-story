package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class OwnedStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    private long quantity;
    private long averagePrice;

    protected OwnedStock() {
    }

    public OwnedStock(Player player, String stockKey) {
        this.player = player;
        this.stockKey = stockKey;
        this.quantity = 0;
        this.averagePrice = 0;
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

    public long getQuantity() {
        return quantity;
    }

    public long getAveragePrice() {
        return averagePrice;
    }

    public void buy(long buyQuantity, long price) {
        if (buyQuantity <= 0) {
            return;
        }
        long totalCostBasis = averagePrice * quantity + price * buyQuantity;
        quantity += buyQuantity;
        averagePrice = totalCostBasis / quantity;
    }

    public boolean sell(long sellQuantity) {
        if (sellQuantity <= 0 || quantity < sellQuantity) {
            return false;
        }
        quantity -= sellQuantity;
        if (quantity == 0) {
            averagePrice = 0;
        }
        return true;
    }
}
