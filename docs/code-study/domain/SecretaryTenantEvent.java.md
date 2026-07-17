# SecretaryTenantEvent.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/SecretaryTenantEvent.java`

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

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class SecretaryTenantEvent {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 비서가 임차인으로 등장하는 장기 이벤트 상태다.
     *
     * 특정 건물에 비서가 거주하고, 플레이어가 요청을 해결하면 고용 가능 상태로 전환된다.
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private OwnedBuilding building;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    private String secretaryKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String city;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer startedDayCount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer acceptedDayCount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer dueDayCount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Enumerated(EnumType.STRING)
    // 해설: enum 값을 숫자가 아니라 문자열 이름으로 DB에 저장한다.
    private SecretaryTenantEventStatus status;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected SecretaryTenantEvent() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public SecretaryTenantEvent(Player player, OwnedBuilding building, String secretaryKey, String city, int startedDayCount) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.building = building;
        this.secretaryKey = secretaryKey;
        this.city = city;
        this.startedDayCount = startedDayCount;
        this.status = SecretaryTenantEventStatus.TENANT;
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

    public OwnedBuilding getBuilding() {
    // 해설: `getBuilding` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return building;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getSecretaryKey() {
    // 해설: `getSecretaryKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return secretaryKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getCity() {
    // 해설: `getCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return city;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getStartedDayCount() {
    // 해설: `getStartedDayCount` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return startedDayCount == null ? 1 : startedDayCount;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getAcceptedDayCount() {
    // 해설: `getAcceptedDayCount` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return acceptedDayCount == null ? 0 : acceptedDayCount;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getDueDayCount() {
    // 해설: `getDueDayCount` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return dueDayCount == null ? 0 : dueDayCount;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public SecretaryTenantEventStatus getStatus() {
    // 해설: `getStatus` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return status;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isActiveTenant() {
    // 해설: `isActiveTenant` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return status != SecretaryTenantEventStatus.COMPLETED;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void makeTenant() {
    // 해설: `makeTenant` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.acceptedDayCount = null;
        this.dueDayCount = null;
        this.status = SecretaryTenantEventStatus.TENANT;
    }

    public void makeRequestAvailable() {
    // 해설: `makeRequestAvailable` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.status = SecretaryTenantEventStatus.REQUEST_AVAILABLE;
    }

    public void acceptRequest(int currentDayCount, int durationDays) {
    // 해설: `acceptRequest` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.acceptedDayCount = currentDayCount;
        this.dueDayCount = currentDayCount + durationDays;
        this.status = durationDays == 0 ? SecretaryTenantEventStatus.HIRE_AVAILABLE : SecretaryTenantEventStatus.REQUEST_ACCEPTED;
    }

    public void makeHireAvailable() {
    // 해설: `makeHireAvailable` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.status = SecretaryTenantEventStatus.HIRE_AVAILABLE;
    }

    public void complete() {
    // 해설: `complete` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.status = SecretaryTenantEventStatus.COMPLETED;
    }
}
```