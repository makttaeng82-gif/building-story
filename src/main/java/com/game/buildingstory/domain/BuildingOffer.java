package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class BuildingOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;
    private String typeName;
    private String name;
    private long marketPrice;
    private long offerPrice;
    private long monthlyRent;

    @Enumerated(EnumType.STRING)
    private ValuationStatus valuationStatus;

    protected BuildingOffer() {
    }

    public BuildingOffer(Player player, String city, String typeName, String name, long marketPrice, long monthlyRent, ValuationStatus valuationStatus) {
        this.player = player;
        this.city = city;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.monthlyRent = monthlyRent;
        this.valuationStatus = valuationStatus;
        this.offerPrice = marketPrice * valuationStatus.rate() / 100;
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

    public long getOfferPrice() {
        return offerPrice;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public ValuationStatus getValuationStatus() {
        return valuationStatus;
    }

    public long loanAmount() {
        return offerPrice * 30 / 100;
    }

    public long cashForLoanPurchase() {
        return offerPrice - loanAmount();
    }
}
