# OwnedBuilding.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/OwnedBuilding.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class OwnedBuilding {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 플레이어가 실제로 소유한 건물이다.
     *
     * BuildingSpec/BuildingOffer가 "살 수 있는 후보"라면 OwnedBuilding은 구매 후 저장되는 결과다.
     * 입주 여부, 수리 요청, 매각 가능일, 보호 임차인/비서 거주 상태처럼 시간이 지나며 변하는 값은
     * 이 엔티티에 저장된다.
     */
    @Id
    // 해설: 엔티티의 기본키 필드다. DB에서 한 행을 식별한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 해설: DB가 기본키 값을 자동 증가 방식으로 생성한다.
    private Long id;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    private String city;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String typeName;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String name;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer buildingSlot;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long marketPrice;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long purchasePrice;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlyRent;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer purchaseDayCount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer tradeCooldownDays;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private boolean occupied;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private boolean repairRequested;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer repairNeglectedMonths;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Boolean protectedTenant;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String residentSecretaryKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer tenantMoveInDayCount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected OwnedBuilding() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public OwnedBuilding(Player player, BuildingOffer offer) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.city = offer.getCity();
        this.typeName = offer.getTypeName();
        this.name = offer.getName();
        this.buildingSlot = offer.getBuildingSlot();
        this.marketPrice = offer.getMarketPrice();
        this.purchasePrice = offer.getOfferPrice();
        this.monthlyRent = offer.getMonthlyRent();
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = offer.getTradeCooldownDays();
        this.occupied = false;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
        this.protectedTenant = false;
    }

    public OwnedBuilding(Player player, String city, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this(player, city, null, typeName, name, marketPrice, purchasePrice, monthlyRent, tradeCooldownDays);
    }

    public OwnedBuilding(Player player, String city, Integer buildingSlot, String typeName, String name, long marketPrice, long purchasePrice, long monthlyRent, int tradeCooldownDays) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.city = city;
        this.buildingSlot = buildingSlot;
        this.typeName = typeName;
        this.name = name;
        this.marketPrice = marketPrice;
        this.purchasePrice = purchasePrice;
        this.monthlyRent = monthlyRent;
        this.purchaseDayCount = player.getElapsedDays();
        this.tradeCooldownDays = tradeCooldownDays;
        this.occupied = false;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
        this.protectedTenant = false;
    }

    public Long getId() {
    // 해설: `getId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return id;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Player getPlayer() {
    // 해설: `getPlayer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return player;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getCity() {
    // 해설: `getCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return city;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getTypeName() {
    // 해설: `getTypeName` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return typeName;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getName() {
    // 해설: `getName` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return name;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Integer getBuildingSlot() {
    // 해설: `getBuildingSlot` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return buildingSlot;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMarketPrice() {
    // 해설: `getMarketPrice` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketPrice;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getPurchasePrice() {
    // 해설: `getPurchasePrice` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return purchasePrice;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMonthlyRent() {
    // 해설: `getMonthlyRent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyRent;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getTradeCooldownDays() {
    // 해설: `getTradeCooldownDays` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return tradeCooldownDays == null ? 15 : tradeCooldownDays;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getPurchaseDayCountForCalculation(int currentDayCount) {
    // 해설: `getPurchaseDayCountForCalculation` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return purchaseDayCount == null ? currentDayCount : purchaseDayCount;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int daysUntilSellable(int currentDayCount) {
    // 해설: `daysUntilSellable` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int purchaseDay = purchaseDayCount == null ? currentDayCount : purchaseDayCount;
        return Math.max(0, purchaseDay + getTradeCooldownDays() - currentDayCount);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isSellable(int currentDayCount) {
    // 해설: `isSellable` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return daysUntilSellable(currentDayCount) == 0;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean canSell(int currentDayCount) {
    // 해설: `canSell` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return isSellable(currentDayCount) && !isProtectedTenant();
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isOccupied() {
    // 해설: `isOccupied` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return occupied;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isRepairRequested() {
    // 해설: `isRepairRequested` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return repairRequested;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getRepairNeglectedMonths() {
    // 해설: `getRepairNeglectedMonths` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return repairNeglectedMonths == null ? 0 : repairNeglectedMonths;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long repairCost() {
    // 해설: `repairCost` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketPrice / 1000;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void requestRepair() {
    // 해설: `requestRepair` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (!repairRequested) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            this.repairRequested = true;
            this.repairNeglectedMonths = 0;
        }
    }

    public boolean repair() {
    // 해설: `repair` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (!repairRequested) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return false;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        boolean repairedWithinOneMonth = getRepairNeglectedMonths() == 0;
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
        return repairedWithinOneMonth;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void clearRepairRequest() {
    // 해설: `clearRepairRequest` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.repairRequested = false;
        this.repairNeglectedMonths = 0;
    }

    public void advanceRepairNeglectMonth() {
    // 해설: `advanceRepairNeglectMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (repairRequested) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            this.repairNeglectedMonths = getRepairNeglectedMonths() + 1;
        }
    }

    public void moveIn() {
    // 해설: `moveIn` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.occupied = true;
    }

    public void moveIn(int currentDayCount) {
    // 해설: `moveIn` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.occupied = true;
        this.tenantMoveInDayCount = currentDayCount;
    }

    public boolean canTenantMoveOut(int currentDayCount) {
    // 해설: `canTenantMoveOut` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (!occupied || isProtectedTenant()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return false;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        if (tenantMoveInDayCount == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return true;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        int moveInDay = tenantMoveInDayCount;
        return currentDayCount - moveInDay >= 60;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int tenantProtectedDaysLeft(int currentDayCount) {
    // 해설: `tenantProtectedDaysLeft` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (!occupied || isProtectedTenant()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 0;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        if (tenantMoveInDayCount == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 0;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        int moveInDay = tenantMoveInDayCount;
        return Math.max(0, 60 - (currentDayCount - moveInDay));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isProtectedTenant() {
    // 해설: `isProtectedTenant` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(protectedTenant);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void moveInProtectedTenant() {
    // 해설: `moveInProtectedTenant` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.occupied = true;
        this.protectedTenant = true;
    }

    public boolean isSecretaryResident() {
    // 해설: `isSecretaryResident` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return residentSecretaryKey != null && !residentSecretaryKey.isBlank();
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getResidentSecretaryKey() {
    // 해설: `getResidentSecretaryKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return residentSecretaryKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void moveInSecretaryTenant(String secretaryKey) {
    // 해설: `moveInSecretaryTenant` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.occupied = true;
        this.protectedTenant = true;
        this.residentSecretaryKey = secretaryKey;
    }

    public void moveOut() {
    // 해설: `moveOut` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.occupied = false;
        this.protectedTenant = false;
        this.residentSecretaryKey = null;
        this.tenantMoveInDayCount = null;
    }
}
```