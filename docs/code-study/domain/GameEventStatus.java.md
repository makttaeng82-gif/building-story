# GameEventStatus.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/GameEventStatus.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 일반 게임 이벤트 모달의 상태다.
 *
 * <p>ACTIVE 이벤트는 화면에 표시되고, COMPLETED 이벤트는 다시 표시하지 않는다.</p>
 */
public enum GameEventStatus {
// 해설: enum 선언이다. 이 파일의 핵심 타입을 정의한다.
    ACTIVE,
    COMPLETED
}
```