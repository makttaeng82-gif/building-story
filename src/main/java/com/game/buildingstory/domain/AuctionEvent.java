package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class AuctionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;
    private String typeName;
    private String name;
    private Integer buildingSlot;
    private long marketPrice;
    private long monthlyRent;
    private Integer tradeCooldownDays;
    private LocalDateTime createdAt;
    private Integer selectedRate;
    private Integer successChance;
    private Boolean successful;
    private String resultMessage;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    protected AuctionEvent() {
    }

    public AuctionEvent(Player player, BuildingOffer offer) {
        this.player = player;
        this.city = offer.getCity();
        this.typeName = offer.getTypeName();
        this.name = offer.getName();
        this.buildingSlot = offer.getBuildingSlot();
        this.marketPrice = offer.getMarketPrice();
        this.monthlyRent = offer.getMonthlyRent();
        this.tradeCooldownDays = offer.getTradeCooldownDays();
        this.createdAt = LocalDateTime.now();
        this.status = AuctionStatus.ACTIVE;
    }

    public AuctionEvent(Player player, String city, String typeName, String name, long marketPrice, long monthlyRent, int tradeCooldownDays) {
        this(player, city, null, typeName, name, marketPrice, monthlyRent, tradeCooldownDays);
    }

    public AuctionEvent(Player player, String city, Integer buildingSlot, String typeName, String name, long marketPrice, long monthlyRent, int tradeCooldownDays) {
        this.player = player;
        this.city = city;
        this.buildingSlot = buildingSlot;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.monthlyRent = monthlyRent;
        this.tradeCooldownDays = tradeCooldownDays;
        this.createdAt = LocalDateTime.now();
        this.status = AuctionStatus.ACTIVE;
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

    public Integer getBuildingSlot() {
        return buildingSlot;
    }

    public long getMarketPrice() {
        return marketPrice;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public int getTradeCooldownDays() {
        return tradeCooldownDays == null ? 15 : tradeCooldownDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getSelectedRate() {
        return selectedRate == null ? 0 : selectedRate;
    }

    public int getSuccessChance() {
        return successChance == null ? 0 : successChance;
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(successful);
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public long bidPrice(int rate) {
        return marketPrice * rate / 100;
    }

    public long remainingSeconds() {
        if (createdAt == null) {
            return 0;
        }
        long elapsed = java.time.Duration.between(createdAt, LocalDateTime.now()).toSeconds();
        return Math.max(0, 20 - elapsed);
    }

    public void resolve(int selectedRate, int successChance, boolean successful, String resultMessage) {
        this.selectedRate = selectedRate;
        this.successChance = successChance;
        this.successful = successful;
        this.resultMessage = resultMessage;
        this.status = AuctionStatus.RESULT;
    }

    public void complete() {
        this.status = AuctionStatus.COMPLETED;
    }
}
