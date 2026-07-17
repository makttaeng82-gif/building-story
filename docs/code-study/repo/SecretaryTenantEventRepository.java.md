# SecretaryTenantEventRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/SecretaryTenantEventRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 비서 임차인 이벤트 저장소다.
 *
 * <p>특정 비서가 건물에 임차인으로 등장하고, 요청을 해결하면 고용 가능 상태로 바뀌는 흐름을 저장한다.</p>
 */
public interface SecretaryTenantEventRepository extends JpaRepository<SecretaryTenantEvent, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    Optional<SecretaryTenantEvent> findByPlayerAndSecretaryKey(Player player, String secretaryKey);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    Optional<SecretaryTenantEvent> findByBuildingAndStatusNot(OwnedBuilding building, SecretaryTenantEventStatus status);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    @EntityGraph(attributePaths = "building")
    List<SecretaryTenantEvent> findByPlayerAndStatusNot(Player player, SecretaryTenantEventStatus status);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    List<SecretaryTenantEvent> findByPlayerAndStatus(Player player, SecretaryTenantEventStatus status);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.
}
```