# SecretaryTenantEventStatus.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/SecretaryTenantEventStatus.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

/**
 * 비서 임차인 이벤트의 진행 단계다.
 *
 * <p>비서가 임차인으로 등장하고, 요청을 수락하면 고용 가능 상태가 되며, 고용 후 완료된다.</p>
 */
public enum SecretaryTenantEventStatus {
// 해설: enum 선언이다. 이 파일의 핵심 타입을 정의한다.
    TENANT,
    REQUEST_AVAILABLE,
    REQUEST_ACCEPTED,
    HIRE_AVAILABLE,
    COMPLETED
}
```