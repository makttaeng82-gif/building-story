# PurchaseCooldownRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/PurchaseCooldownRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PurchaseCooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 건물 재구매 쿨다운 저장소다.
 *
 * <p>건물을 산 직후 같은 도시/슬롯을 바로 다시 사지 못하게 elapsedDays 기준 대기일을 기록한다.</p>
 */
public interface PurchaseCooldownRepository extends JpaRepository<PurchaseCooldown, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    Optional<PurchaseCooldown> findByPlayerAndCityAndBuildingSlot(Player player, String city, int buildingSlot);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    boolean existsByPlayerAndCity(Player player, String city);
}
```