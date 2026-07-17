# OwnedBuildingRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/OwnedBuildingRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 플레이어 보유 건물 저장소다.
 *
 * <p>도시별 보유 건물 수, 입주 중인 건물, 수리 요청 건물처럼 게임 진행에서
 * 자주 필요한 상태를 조회한다.</p>
 */
public interface OwnedBuildingRepository extends JpaRepository<OwnedBuilding, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    List<OwnedBuilding> findByPlayerOrderById(Player player);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    List<OwnedBuilding> findByPlayerAndCityOrderById(Player player, String city);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    long countByPlayerAndCity(Player player, String city);
}
```