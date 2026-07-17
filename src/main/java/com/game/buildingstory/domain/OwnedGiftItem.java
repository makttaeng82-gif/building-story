package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "owned_gift_item", uniqueConstraints =
        @UniqueConstraint(name = "uk_owned_gift_player_key", columnNames = {"player_id", "gift_key"}))
public class OwnedGiftItem {
    /*
     * 플레이어가 보유한 선물 아이템 수량이다.
     *
     * 선물은 여러 개를 구매하고 비서에게 줄 때마다 수량이 줄어든다.
     */
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
