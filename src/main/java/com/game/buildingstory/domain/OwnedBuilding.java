package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class OwnedBuilding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;
    private String typeName;
    private String name;
    private long marketPrice;
    private long purchasePrice;
    private long monthlyRent;
    private Integer purchaseDayCount;
    private Integer tradeCooldownDays;
    private boolean occupied;
    private boolean repairRequested;
    private Boolean protectedTenant;

    protected OwnedBuilding() {
    }

    public OwnedBuilding(Player player, BuildingOffer offer) {
        this.player = player;
        this.city = offer.getCity();
        this.typeName = offer.getTypeName();
        this.name = offer.getName();
        this.marketPrice = offer.getMarketPrice();
        this.purchasePrice = offer.getOfferPrice();
        this.monthlyRent = offer.getMonthlyRent();
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = offer.getTradeCooldownDays();
        this.occupied = false;
        this.repairRequested = false;
        this.protectedTenant = false;
    }

    public OwnedBuilding(Player player, String city, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays) {
        this.player = player;
        this.city = city;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.purchasePrice = purchasePrice;
        this.monthlyRent = monthlyRent;
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = tradeCooldownDays;
        this.occupied = false;
        this.repairRequested = false;
        this.protectedTenant = false;
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

    public String getTypeName() {
        return typeName;
    }

    public String getName() {
        return name;
    }

    public long getMarketPrice() {
        return marketPrice;
    }

    public long getPurchasePrice() {
        return purchasePrice;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public int getTradeCooldownDays() {
        return tradeCooldownDays == null ? 15 : tradeCooldownDays;
    }

    public int daysUntilSellable(int currentDayCount) {
        int purchaseDay = purchaseDayCount == null ? currentDayCount : purchaseDayCount;
        return Math.max(0, purchaseDay + getTradeCooldownDays() - currentDayCount);
    }

    public boolean isSellable(int currentDayCount) {
        return daysUntilSellable(currentDayCount) == 0;
    }

    public boolean canSell(int currentDayCount) {
        return isSellable(currentDayCount) && !isProtectedTenant();
    }

    public boolean isOccupied() {
        return occupied;
    }

    public boolean isRepairRequested() {
        return repairRequested;
    }

    public void moveIn() {
        this.occupied = true;
    }

    public boolean isProtectedTenant() {
        return Boolean.TRUE.equals(protectedTenant);
    }

    public void moveInProtectedTenant() {
        this.occupied = true;
        this.protectedTenant = true;
    }

    public void moveOut() {
        this.occupied = false;
        this.protectedTenant = false;
    }
}
