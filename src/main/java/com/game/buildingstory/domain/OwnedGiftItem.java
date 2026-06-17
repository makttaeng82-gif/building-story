package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class OwnedGiftItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String giftKey;
    private int quantity;

    protected OwnedGiftItem() {
    }

    public OwnedGiftItem(Player player, String giftKey, int quantity) {
        this.player = player;
        this.giftKey = giftKey;
        this.quantity = Math.max(0, quantity);
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getGiftKey() {
        return giftKey;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int amount) {
        if (amount > 0) {
            quantity += amount;
        }
    }

    public boolean spendQuantity(int amount) {
        if (amount <= 0 || quantity < amount) {
            return false;
        }
        quantity -= amount;
        return true;
    }
}
