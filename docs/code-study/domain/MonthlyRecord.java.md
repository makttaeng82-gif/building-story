# MonthlyRecord.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/MonthlyRecord.java`

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
import jakarta.persistence.Column;

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class MonthlyRecord {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 월세, 구매, 판매, 이벤트 같은 게임 로그를 저장한다.
     *
     * 화면의 최근 기록 패널은 이 엔티티를 날짜 역순으로 읽는다. 금액 변화와 평판 변화를 함께 저장해
     * 플레이어가 왜 자금/평판이 바뀌었는지 추적할 수 있게 한다.
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

    @Column(name = "record_month")
    private int month;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(name = "record_day")
    private int day;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private int elapsedDays;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Enumerated(EnumType.STRING)
    // 해설: enum 값을 숫자가 아니라 문자열 이름으로 DB에 저장한다.
    @Column(name = "record_type")
    private RecordType type;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    private String title;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Long amount;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long cashAfter;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private int reputationChange;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String buildingName;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String memo;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected MonthlyRecord() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public MonthlyRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.month = player.getMonth();
        this.day = player.getDay();
        this.elapsedDays = player.getElapsedDays();
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.cashAfter = player.getCash();
        this.reputationChange = reputationChange;
        this.buildingName = buildingName;
        this.memo = memo;
    }

    public Long getId() {
    // 해설: `getId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return id;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getMonth() {
    // 해설: `getMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return month;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getDay() {
    // 해설: `getDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getElapsedDays() {
    // 해설: `getElapsedDays` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return elapsedDays;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public RecordType getType() {
    // 해설: `getType` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return type;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getTitle() {
    // 해설: `getTitle` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return title;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Long getAmount() {
    // 해설: `getAmount` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return amount;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getCashAfter() {
    // 해설: `getCashAfter` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return cashAfter;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getReputationChange() {
    // 해설: `getReputationChange` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return reputationChange;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getBuildingName() {
    // 해설: `getBuildingName` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return buildingName;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getMemo() {
    // 해설: `getMemo` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return memo;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```