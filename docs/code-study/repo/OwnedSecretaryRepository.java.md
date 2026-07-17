# OwnedSecretaryRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/OwnedSecretaryRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 고용한 비서 저장소다.
 *
 * <p>비서는 플레이어별로 보유되며, 배치 도시 기준 보너스 계산을 위해
 * secretaryKey와 assignedCity 조회가 사용된다.</p>
 */
public interface OwnedSecretaryRepository extends JpaRepository<OwnedSecretary, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    List<OwnedSecretary> findByPlayerOrderById(Player player);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    List<OwnedSecretary> findByPlayerAndAssignedCityOrderById(Player player, String assignedCity);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    Optional<OwnedSecretary> findByPlayerAndSecretaryKey(Player player, String secretaryKey);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.
}
```