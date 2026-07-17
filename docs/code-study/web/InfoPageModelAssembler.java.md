# InfoPageModelAssembler.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/InfoPageModelAssembler.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class InfoPageModelAssembler {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 정보 화면에 필요한 카탈로그성 데이터를 Model에 담는다.
     *
     * 정보 화면은 플레이어 상태를 크게 바꾸지 않고, 건물 가격/도시 해금/비서 스펙처럼
     * 읽기 전용 데이터가 많다. 이 조립 코드를 컨트롤러 밖에 두면 화면 구성이 명확해진다.
     */
    private final GameService gameService;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    public InfoPageModelAssembler(GameService gameService) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.gameService = gameService;
    }

    public void addInfoPageAttributes(Player player, Model model) {
    // 해설: `addInfoPageAttributes` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        model.addAttribute("player", player);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("totalMonthlyRent", gameService.totalMonthlyRent(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        var loans = gameService.loans(player);
        model.addAttribute("loans", loans);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("cities", gameService.cities());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("repairCountsByCity", gameService.repairRequestCountsByCity(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("buildingSpecs", gameService.buildingSpecs());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("reputationTiers", gameService.reputationTiers());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("secretarySpecs", gameService.secretarySpecs());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockSpecs", gameService.stockSpecs());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockContentUnlocked", gameService.stockContentUnlocked(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockContentStatus", gameService.stockContentStatusText(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
    }
}
```