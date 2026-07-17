# LoanRepository.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/repo/LoanRepository.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 대출 저장소다.
 *
 * <p>대출 구매 시 Loan이 생성되고, 월초 정산에서 남은 대출들을 조회해 상환액을 계산한다.</p>
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {
// 해설: Spring Data JPA Repository 인터페이스다. 직접 구현체를 만들지 않아도 Spring이 런타임에 구현한다.
    List<Loan> findByPlayer(Player player);
    // 해설: Spring Data Repository 조회 메서드 호출이다. 메서드 이름의 조건으로 DB 데이터를 찾는다.
}
```