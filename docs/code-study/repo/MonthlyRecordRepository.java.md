# MonthlyRecordRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/MonthlyRecordRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 최근 기록 패널에 표시할 월간/일간 기록 저장소다.
 *
 * <p>수입, 지출, 평판 변화, 이벤트 로그가 모두 MonthlyRecord로 저장된다.
 * 화면은 최신 기록을 먼저 보여주므로 elapsedDays와 id 역순 조회를 사용한다.</p>
 */
public interface MonthlyRecordRepository extends JpaRepository<MonthlyRecord, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    List<MonthlyRecord> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.

    void deleteByPlayerAndElapsedDaysLessThan(Player player, int elapsedDays);
}
```