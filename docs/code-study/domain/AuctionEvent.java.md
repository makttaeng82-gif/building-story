# AuctionEvent.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/AuctionEvent.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
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
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class AuctionEvent {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 플레이어에게 열린 경매 한 건이다.
     *
     * 일반 매물과 달리 제한 시간이 있고, 입찰 성공/실패 결과를 화면에 보여줘야 하므로
     * status와 endsAt을 함께 저장한다.
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
    private long monthlyRent;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer tradeCooldownDays;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private LocalDateTime createdAt;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer selectedRate;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer successChance;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Boolean successful;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String resultMessage;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Enumerated(EnumType.STRING)
    // 해설: enum 값을 숫자가 아니라 문자열 이름으로 DB에 저장한다.
    private AuctionStatus status;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected AuctionEvent() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public AuctionEvent(Player player, BuildingOffer offer) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
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
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this(player, city, null, typeName, name, marketPrice, monthlyRent, tradeCooldownDays);
    }

    public AuctionEvent(Player player, String city, Integer buildingSlot, String typeName, String name, long marketPrice, long monthlyRent, int tradeCooldownDays) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
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

    public LocalDateTime getCreatedAt() {
    // 해설: `getCreatedAt` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return createdAt;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getSelectedRate() {
    // 해설: `getSelectedRate` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return selectedRate == null ? 0 : selectedRate;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getSuccessChance() {
    // 해설: `getSuccessChance` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return successChance == null ? 0 : successChance;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isSuccessful() {
    // 해설: `isSuccessful` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(successful);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getResultMessage() {
    // 해설: `getResultMessage` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return resultMessage;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public AuctionStatus getStatus() {
    // 해설: `getStatus` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return status;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long bidPrice(int rate) {
    // 해설: `bidPrice` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketPrice * rate / 100;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long remainingSeconds() {
    // 해설: `remainingSeconds` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (createdAt == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 0;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        long elapsed = java.time.Duration.between(createdAt, LocalDateTime.now()).toSeconds();
        return Math.max(0, 20 - elapsed);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void resolve(int selectedRate, int successChance, boolean successful, String resultMessage) {
    // 해설: `resolve` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.selectedRate = selectedRate;
        this.successChance = successChance;
        this.successful = successful;
        this.resultMessage = resultMessage;
        this.status = AuctionStatus.RESULT;
    }

    public void complete() {
    // 해설: `complete` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.status = AuctionStatus.COMPLETED;
    }
}
```