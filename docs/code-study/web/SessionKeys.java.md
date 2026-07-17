# SessionKeys.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/SessionKeys.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

public final class SessionKeys {
    /*
     * 세션 attribute 이름을 한 곳에 모아둔다.
     *
     * 문자열을 컨트롤러마다 직접 쓰면 오타가 나도 컴파일러가 잡지 못한다.
     * 상수로 관리하면 이름 변경과 검색이 쉬워진다.
     */
    public static final String PLAYER_ID = "PLAYER_ID";

    private SessionKeys() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }
}
```