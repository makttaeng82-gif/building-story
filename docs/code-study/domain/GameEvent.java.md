# GameEvent.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/GameEvent.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class GameEvent {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 화면에 표시되는 이벤트 모달 한 건이다.
     *
     * title/body/imagePath는 표시용이고, effect는 확인 버튼을 눌렀을 때 어떤 후속 처리를 할지 알려주는 코드다.
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

    @Column(nullable = false)
    private String eventKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false)
    private String title;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false, length = 1000)
    private String body;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false)
    private String imageLabel;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false)
    private String effectKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    private String actionLabel;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Enumerated(EnumType.STRING)
    // 해설: enum 값을 숫자가 아니라 문자열 이름으로 DB에 저장한다.
    private GameEventStatus status;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected GameEvent() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public GameEvent(Player player, GameEventDefinition definition) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.eventKey = definition.key();
        this.title = definition.title();
        this.body = definition.body();
        this.imageLabel = definition.imageLabel();
        this.effectKey = definition.effectKey();
        this.actionLabel = "계속 진행하기";
        this.status = GameEventStatus.ACTIVE;
    }

    public GameEvent(Player player, String eventKey, String title, String body, String imageLabel, String effectKey, String actionLabel) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.eventKey = eventKey;
        this.title = title;
        this.body = body;
        this.imageLabel = imageLabel;
        this.effectKey = effectKey;
        this.actionLabel = actionLabel;
        this.status = GameEventStatus.ACTIVE;
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

    public String getEventKey() {
    // 해설: `getEventKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return eventKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getTitle() {
    // 해설: `getTitle` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return title;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getBody() {
    // 해설: `getBody` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return body;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getImageLabel() {
    // 해설: `getImageLabel` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return imageLabel;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getEffectKey() {
    // 해설: `getEffectKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return effectKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getActionLabel() {
    // 해설: `getActionLabel` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return actionLabel == null || actionLabel.isBlank() ? "계속 진행하기" : actionLabel;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public GameEventStatus getStatus() {
    // 해설: `getStatus` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return status;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void complete() {
    // 해설: `complete` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.status = GameEventStatus.COMPLETED;
    }
}
```