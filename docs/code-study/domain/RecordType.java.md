# RecordType.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/RecordType.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 최근 기록의 종류다.
 *
 * <p>MonthlyRecord가 어떤 사건에서 생성됐는지 구분한다. 화면 필터나 색상,
 * 향후 통계 기능을 만들 때 이 enum을 기준으로 분류할 수 있다.</p>
 */
public enum RecordType {
// 해설: enum 선언이다. 이 파일의 핵심 타입을 정의한다.
    MOVE_IN,
    MOVE_OUT,
    REPAIR_REQUEST,
    REPAIR_COMPLETE,
    RENT_INCOME,
    SALARY_INCOME,
    SECRETARY_SALARY,
    DONATION,
    LUXURY_ITEM,
    AD_COST,
    LOAN_PAYMENT,
    BUILDING_BUY,
    BUILDING_SELL,
    STOCK_EVENT
}
```