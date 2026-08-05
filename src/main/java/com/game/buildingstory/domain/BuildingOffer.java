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
    /*
     * 현재 시장에 나온 부동산 매물이다.
     *
     * 매물은 일정 주기로 새로 생성된다. 플레이어가 구매하면 이 정보가 OwnedBuilding으로 복사되고,
     * 해당 offer는 더 이상 의미가 없어 다음 갱신 때 사라진다.
     */
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
    private Integer buildingSlot;
    private Integer tradeCooldownDays;

    @Enumerated(EnumType.STRING)
    private ValuationStatus valuationStatus;

    protected BuildingOffer() {
    }

    public BuildingOffer(Player player, String city, int buildingSlot, String typeName, String name, long marketPrice, long monthlyRent, int tradeCooldownDays, ValuationStatus valuationStatus) {
        this.player = player;
        this.city = city;
        this.buildingSlot = buildingSlot;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.monthlyRent = monthlyRent;
        this.tradeCooldownDays = tradeCooldownDays;
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

    public int getBuildingSlot() {
        return buildingSlot == null ? 1 : buildingSlot;
    }

    public int getTradeCooldownDays() {
        return tradeCooldownDays == null ? 15 : tradeCooldownDays;
    }

    public ValuationStatus getValuationStatus() {
        return valuationStatus;
    }

    public long loanAmount() {
        return loanAmount(player);
    }

    public long loanAmount(Player supportPlayer) {
        long loanToValueLimit = Math.min(effectivePurchasePrice(supportPlayer), marketPrice) * 80 / 100;
        long expectedNetRent = monthlyRent * 75 / 100 * 90 / 100;
        long maximumInterestForDscr = expectedNetRent * 100 / 120;
        long cashFlowLimit = maximumInterestForDscr * 250;
        return Math.min(loanToValueLimit, cashFlowLimit);
    }

    public long cashForLoanPurchase() {
        return cashForLoanPurchase(player);
    }

    public long cashForLoanPurchase(Player supportPlayer) {
        return effectivePurchasePrice(supportPlayer) - loanAmount(supportPlayer) + purchaseFee(supportPlayer);
    }

    public long cashForPurchase() {
        return cashForPurchase(player);
    }

    public long cashForPurchase(Player supportPlayer) {
        return effectivePurchasePrice(supportPlayer) + purchaseFee(supportPlayer);
    }

    public long purchaseFee() {
        return purchaseFee(player);
    }

    public long purchaseFee(Player supportPlayer) {
        return EconomyBalanceRules.purchaseFee(effectivePurchasePrice(supportPlayer));
    }

    public boolean isGovernmentSupportEligible() {
        return isGovernmentSupportEligible(player);
    }

    public boolean isGovernmentSupportEligible(Player supportPlayer) {
        return false;
    }

    public long governmentSupportAmount() {
        return governmentSupportAmount(player);
    }

    public long governmentSupportAmount(Player supportPlayer) {
        return 0L;
    }

    public int governmentSupportPercent() {
        return governmentSupportPercent(player);
    }

    public int governmentSupportPercent(Player supportPlayer) {
        return 0;
    }

    public long effectivePurchasePrice() {
        return effectivePurchasePrice(player);
    }

    public long effectivePurchasePrice(Player supportPlayer) {
        return offerPrice;
    }
}
