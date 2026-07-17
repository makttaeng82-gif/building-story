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
@Table(name = "purchase_cooldown", uniqueConstraints =
        @UniqueConstraint(name = "uk_purchase_cooldown_player_slot", columnNames = {"player_id", "city", "building_slot"}))
public class PurchaseCooldown {
    /*
     * 특정 도시/건물 슬롯의 재구매 대기 기록이다.
     *
     * 건물을 산 직후 같은 슬롯 매물을 바로 다시 사는 것을 막기 위해 elapsedDays 기준 만료일을 저장한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;
    private int buildingSlot;
    private int availableDayCount;

    protected PurchaseCooldown() {
    }

    public PurchaseCooldown(Player player, String city, int buildingSlot, int availableDayCount) {
        this.player = player;
        this.city = city;
        this.buildingSlot = buildingSlot;
        this.availableDayCount = availableDayCount;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCity() {
        return city;
    }

    public int getBuildingSlot() {
        return buildingSlot;
    }

    public int getAvailableDayCount() {
        return availableDayCount;
    }

    public int daysLeft(int currentDayCount) {
        return Math.max(0, availableDayCount - currentDayCount);
    }

    public void reset(int availableDayCount) {
        this.availableDayCount = availableDayCount;
    }
}
