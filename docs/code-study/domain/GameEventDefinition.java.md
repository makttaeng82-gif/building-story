# GameEventDefinition.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/GameEventDefinition.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 고정 날짜 이벤트의 원본 정의다.
 *
 * <p>이 record는 카탈로그 데이터이고 DB에 직접 저장되지 않는다. 실제 플레이어에게 표시될 때는
 * GameEvent 엔티티로 복사된다.</p>
 */
public record GameEventDefinition(
// 해설: record 선언이다. 이 파일의 핵심 타입을 정의한다.
        String key,
        int month,
        int day,
        String title,
        String body,
        String imageLabel,
        String effectKey
) {
}
```