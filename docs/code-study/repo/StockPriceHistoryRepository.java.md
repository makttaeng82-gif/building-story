# StockPriceHistoryRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/StockPriceHistoryRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 주식 가격 캔들 이력 저장소다.
 *
 * <p>주식 화면은 최신 가격, 직전 가격, 최근 60개 캔들을 조회한다.
 * 그래서 stockKey와 elapsedDays 역순 조회 메서드가 많다.</p>
 */
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    boolean existsByPlayerAndStockKey(Player player, String stockKey);

    long countByPlayer(Player player);

    Optional<StockPriceHistory> findFirstByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    Optional<StockPriceHistory> findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop2ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop3ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);

    List<StockPriceHistory> findTop60ByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(Player player, String stockKey);
}
```