# ValuationStatus.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/ValuationStatus.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 부동산 가격 평가 상태다.
 *
 * <p>매물 생성이나 건물 매각 시 기준가에 rate를 곱해 실제 거래가를 만든다.</p>
 */
public enum ValuationStatus {
// 해설: enum 선언이다. 이 파일의 핵심 타입을 정의한다.
    UNDER("저평가", 80),
    FAIR("시장가", 100),
    OVER("고평가", 120);

    private final String label;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.
    private final int rate;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    ValuationStatus(String label, int rate) {
        this.label = label;
        this.rate = rate;
    }

    public String label() {
    // 해설: `label` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return label;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int rate() {
    // 해설: `rate` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return rate;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```