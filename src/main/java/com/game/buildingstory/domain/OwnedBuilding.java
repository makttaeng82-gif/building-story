package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class OwnedBuilding {
    private static final int GOVERNMENT_SUPPORT_HOLDING_DAYS = 365;

    /*
     * 플레이어가 실제로 소유한 건물이다.
     *
     * BuildingSpec/BuildingOffer가 "살 수 있는 후보"라면 OwnedBuilding은 구매 후 저장되는 결과다.
     * 입주 여부, 수리 요청, 매각 가능일, 보호 임차인/비서 거주 상태처럼 시간이 지나며 변하는 값은
     * 이 엔티티에 저장된다.
     */
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
    private long purchasePrice;
    private Long governmentSupportAmount;
    private long monthlyRent;
    private Integer purchaseDayCount;
    private Integer tradeCooldownDays;
    private boolean occupied;
    private boolean repairRequested;
    private Integer repairNeglectedMonths;
    private Boolean protectedTenant;
    private String residentSecretaryKey;
    private Integer tenantMoveInDayCount;

    protected OwnedBuilding() {
    }

    public OwnedBuilding(Player player, BuildingOffer offer) {
        this.player = player;
        this.city = offer.getCity();
        this.typeName = offer.getTypeName();
        this.name = offer.getName();
        this.buildingSlot = offer.getBuildingSlot();
        this.marketPrice = offer.getMarketPrice();
        this.purchasePrice = offer.effectivePurchasePrice();
        this.governmentSupportAmount = offer.governmentSupportAmount();
        this.monthlyRent = offer.getMonthlyRent();
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = offer.getTradeCooldownDays();
        this.occupied = false;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
        this.protectedTenant = false;
    }

    public OwnedBuilding(Player player, String city, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays) {
        this(player, city, null, typeName, name, marketPrice, purchasePrice, monthlyRent, tradeCooldownDays, 0L);
    }

    public OwnedBuilding(Player player, String city, Integer buildingSlot, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays) {
        this(player, city, buildingSlot, typeName, name, marketPrice, purchasePrice, monthlyRent, tradeCooldownDays, 0L);
    }

    public OwnedBuilding(Player player, String city, Integer buildingSlot, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays, long governmentSupportAmount) {
        this.player = player;
        this.city = city;
        this.buildingSlot = buildingSlot;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.purchasePrice = purchasePrice;
        this.governmentSupportAmount = governmentSupportAmount;
        this.monthlyRent = monthlyRent;
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = tradeCooldownDays;
        this.occupied = false;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
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

    public Integer getBuildingSlot() {
        return buildingSlot;
    }

    public long getMarketPrice() {
        return marketPrice;
    }

    public long getPurchasePrice() {
        return purchasePrice;
    }

    public long getGovernmentSupportAmount() {
        return governmentSupportAmount == null ? 0L : governmentSupportAmount;
    }

    public int governmentSupportClawbackDaysLeft(int currentDayCount) {
        if (getGovernmentSupportAmount() == 0L) {
            return 0;
        }
        int purchaseDay = getPurchaseDayCountForCalculation(currentDayCount);
        return Math.max(0, purchaseDay + GOVERNMENT_SUPPORT_HOLDING_DAYS - currentDayCount);
    }

    public long governmentSupportClawback(int currentDayCount) {
        return governmentSupportClawbackDaysLeft(currentDayCount) > 0 ? getGovernmentSupportAmount() : 0L;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public int getTradeCooldownDays() {
        return tradeCooldownDays == null ? 15 : tradeCooldownDays;
    }

    public int getPurchaseDayCountForCalculation(int currentDayCount) {
        return purchaseDayCount == null ? currentDayCount : purchaseDayCount;
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

    public int getRepairNeglectedMonths() {
        return repairNeglectedMonths == null ? 0 : repairNeglectedMonths;
    }

    public long repairCost() {
        return marketPrice / 1000;
    }

    public void requestRepair() {
        if (!repairRequested) {
            this.repairRequested = true;
            this.repairNeglectedMonths = 0;
        }
    }

    public boolean repair() {
        if (!repairRequested) {
            return false;
        }
        boolean repairedWithinOneMonth = getRepairNeglectedMonths() == 0;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
        return repairedWithinOneMonth;
    }

    public void clearRepairRequest() {
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
    }

    public void advanceRepairNeglectMonth() {
        if (repairRequested) {
            this.repairNeglectedMonths = getRepairNeglectedMonths() + 1;
        }
    }

    public void moveIn() {
        this.occupied = true;
    }

    public void moveIn(int currentDayCount) {
        this.occupied = true;
        this.tenantMoveInDayCount = currentDayCount;
    }

    public boolean canTenantMoveOut(int currentDayCount) {
        if (!occupied || isProtectedTenant()) {
            return false;
        }
        if (tenantMoveInDayCount == null) {
            return true;
        }
        int moveInDay = tenantMoveInDayCount;
        return currentDayCount - moveInDay >= 60;
    }

    public int tenantProtectedDaysLeft(int currentDayCount) {
        if (!occupied || isProtectedTenant()) {
            return 0;
        }
        if (tenantMoveInDayCount == null) {
            return 0;
        }
        int moveInDay = tenantMoveInDayCount;
        return Math.max(0, 60 - (currentDayCount - moveInDay));
    }

    public boolean isProtectedTenant() {
        return Boolean.TRUE.equals(protectedTenant);
    }

    public void moveInProtectedTenant() {
        this.occupied = true;
        this.protectedTenant = true;
    }

    public boolean isSecretaryResident() {
        return residentSecretaryKey != null && !residentSecretaryKey.isBlank();
    }

    public String getResidentSecretaryKey() {
        return residentSecretaryKey;
    }

    public void moveInSecretaryTenant(String secretaryKey) {
        this.occupied = true;
        this.protectedTenant = true;
        this.residentSecretaryKey = secretaryKey;
    }

    public void moveOut() {
        this.occupied = false;
        this.protectedTenant = false;
        this.residentSecretaryKey = null;
        this.tenantMoveInDayCount = null;
    }
}
