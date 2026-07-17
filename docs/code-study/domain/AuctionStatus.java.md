# AuctionStatus.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/AuctionStatus.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 경매 이벤트의 진행 상태다.
 *
 * <p>ACTIVE는 입찰 가능, RESULT는 결과 표시 중, COMPLETED는 더 이상 화면에 표시하지 않는 종료 상태다.</p>
 */
public enum AuctionStatus {
// 해설: enum 선언이다. 이 파일의 핵심 타입을 정의한다.
    ACTIVE,
    RESULT,
    COMPLETED
}
```