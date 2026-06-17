package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class PurchaseCooldown {
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
