# OwnedStock.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/OwnedStock.java`

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
public class OwnedStock {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 플레이어가 보유한 특정 주식의 수량과 평균단가다.
     *
     * 가격 이력은 StockPriceHistory에 따로 저장된다. 이 엔티티는 "몇 주를 얼마 평균에 샀는지"만
     * 들고 있어 평가손익 계산의 원가 기준으로 사용된다.
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

    private String stockKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long quantity;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long averagePrice;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected OwnedStock() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public OwnedStock(Player player, String stockKey) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.stockKey = stockKey;
        this.quantity = 0;
        this.averagePrice = 0;
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

    public String getStockKey() {
    // 해설: `getStockKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return stockKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getQuantity() {
    // 해설: `getQuantity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return quantity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getAveragePrice() {
    // 해설: `getAveragePrice` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return averagePrice;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void buy(long buyQuantity, long price) {
    // 해설: `buy` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 새 평균단가는 기존 원가 총액과 신규 매수 원가를 합친 뒤 전체 수량으로 나눈 값이다.
        if (buyQuantity <= 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        long totalCostBasis = averagePrice * quantity + price * buyQuantity;
        quantity += buyQuantity;
        averagePrice = totalCostBasis / quantity;
    }

    public boolean sell(long sellQuantity) {
    // 해설: `sell` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 일부 매도는 평균단가를 유지한다. 전량 매도하면 다음 매수를 새 원가로 시작해야 하므로 평균단가를 0으로 초기화한다.
        if (sellQuantity <= 0 || quantity < sellQuantity) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return false;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        quantity -= sellQuantity;
        if (quantity == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            averagePrice = 0;
        }
        return true;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```