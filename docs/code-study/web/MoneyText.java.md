# MoneyText.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/MoneyText.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import org.springframework.stereotype.Component;

@Component("moneyText")
public class MoneyText {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 화면 표시용 금액 문자열 변환 유틸리티다.
     *
     * 내부 계산은 long 원 단위로 하지만, 화면에는 1억/1만 단위처럼 읽기 쉬운 한국식 단위를 쓴다.
     */
    public String format(long amount) {
    // 해설: `format` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (amount == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "0원";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }

        long jo = amount / 1_000_000_000_000L;
        amount %= 1_000_000_000_000L;
        long eok = amount / 100_000_000L;
        amount %= 100_000_000L;
        long man = amount / 10_000L;

        StringBuilder builder = new StringBuilder();
        if (jo > 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            builder.append(jo).append("조");
        }
        if (eok > 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            builder.append(eok).append("억");
        }
        if (man > 0 && jo == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            builder.append(man).append("만");
        }
        if (builder.isEmpty()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            builder.append(amount);
        }
        return builder.append("원").toString();
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```