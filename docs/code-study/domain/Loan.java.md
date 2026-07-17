# Loan.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/Loan.java`

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
public class Loan {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 건물 대출 한 건이다.
     *
     * principal은 남은 원금이고, monthlyPayment는 매월 상환되는 금액이다.
     * 월초 정산에서 상환 후 원금이 0이 되면 대출은 종료된다.
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

    private long principal;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long totalRepayment;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private int remainingMonths;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlyPayment;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected Loan() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public Loan(Player player, long principal) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.principal = principal;
        this.totalRepayment = principal * 120 / 100;
        this.remainingMonths = 6;
        this.monthlyPayment = totalRepayment / 6;
    }

    public long getPrincipal() {
    // 해설: `getPrincipal` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return principal;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Player getPlayer() {
    // 해설: `getPlayer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return player;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Long getId() {
    // 해설: `getId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return id;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getTotalRepayment() {
    // 해설: `getTotalRepayment` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return totalRepayment;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getRemainingMonths() {
    // 해설: `getRemainingMonths` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return remainingMonths;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMonthlyPayment() {
    // 해설: `getMonthlyPayment` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyPayment;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long remainingRepayment() {
    // 해설: `remainingRepayment` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyPayment * remainingMonths;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void advanceMonth() {
    // 해설: `advanceMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (remainingMonths > 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            remainingMonths--;
        }
    }

    public boolean isMatured() {
    // 해설: `isMatured` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return remainingMonths <= 0;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void extendGracePeriod() {
    // 해설: `extendGracePeriod` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.remainingMonths = 6;
    }
}
```