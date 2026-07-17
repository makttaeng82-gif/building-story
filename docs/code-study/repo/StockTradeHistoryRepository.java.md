# StockTradeHistoryRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/StockTradeHistoryRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주식 체결 내역 저장소다.
 *
 * <p>보유 수량 계산과 별개로 사용자가 언제 무엇을 매수/매도했는지 보여주기 위해 별도 기록을 저장한다.</p>
 */
public interface StockTradeHistoryRepository extends JpaRepository<StockTradeHistory, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    List<StockTradeHistory> findTop12ByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    List<StockTradeHistory> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.
}
```