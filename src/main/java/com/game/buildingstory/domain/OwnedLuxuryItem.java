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
@Table(name = "owned_luxury_item", uniqueConstraints =
        @UniqueConstraint(name = "uk_owned_luxury_player_key", columnNames = {"player_id", "item_key"}))
public class OwnedLuxuryItem {
    /*
     * 플레이어가 구매한 명품 아이템이다.
     *
     * 대부분 1회 구매 여부가 중요하므로 itemKey로 어떤 명품을 샀는지 저장한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String itemKey;

    protected OwnedLuxuryItem() {
    }

    public OwnedLuxuryItem(Player player, String itemKey) {
        this.player = player;
        this.itemKey = itemKey;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getItemKey() {
        return itemKey;
    }
}
