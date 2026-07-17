# GameEventRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/GameEventRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.GameEvent;
import com.game.buildingstory.domain.GameEventStatus;
import com.game.buildingstory.domain.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 스토리/뉴스/비서 이벤트 모달 저장소다.
 *
 * <p>이벤트는 DB에 저장된 뒤 화면에서 ACTIVE 상태로 표시되고, 사용자가 확인하면
 * COMPLETED가 된다. 중복 이벤트 방지를 위해 eventKey 기반 존재 여부 조회도 제공한다.</p>
 */
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    boolean existsByPlayerAndEventKey(Player player, String eventKey);

    boolean existsByPlayerIdAndEventKey(Long playerId, String eventKey);

    Optional<GameEvent> findFirstByPlayerAndStatus(Player player, GameEventStatus status);

    @Query("select event from GameEvent event where event.player.id = :playerId and event.status = :status order by event.id desc")
    List<GameEvent> findLatestByPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") GameEventStatus status, Pageable pageable);

    void deleteByPlayerAndEventKey(Player player, String eventKey);

    void deleteByPlayerIdAndEventKey(Long playerId, String eventKey);
}
```