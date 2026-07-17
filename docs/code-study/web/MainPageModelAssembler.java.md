# MainPageModelAssembler.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/MainPageModelAssembler.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class MainPageModelAssembler {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 메인 화면은 표시할 데이터가 많다.
     * 컨트롤러에 이 코드를 모두 두면 URL 처리와 화면 데이터 조립이 섞여 읽기 어려워진다.
     * 그래서 이 조립 전용 컴포넌트가 Model에 필요한 값을 한 곳에서 채운다.
     */
    private final GameService gameService;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    public MainPageModelAssembler(GameService gameService) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.gameService = gameService;
    }

    public void addMainPageAttributes(long playerId, Player player, Model model) {
    // 해설: `addMainPageAttributes` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 화면 렌더링 전에 필요한 상태를 먼저 준비한다. 예: 매물이 없으면 생성, 주식 unlock 예약, 비서 이벤트 평가.
        gameService.ensureOffers(player);
        gameService.ensureStockUnlockSchedule(player);
        gameService.evaluateSecretaryTenantEvents(player);
        var loans = gameService.loans(player);
        // 아래 attribute 이름은 Thymeleaf 템플릿에서 ${player}, ${offers}처럼 직접 참조된다.
        model.addAttribute("player", player);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("offers", gameService.offers(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("buildings", gameService.ownedBuildings(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("loans", loans);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("records", gameService.recentRecords(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("totalMonthlyRent", gameService.totalMonthlyRent(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("ownedSecretaries", gameService.ownedSecretaries(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("assignedSecretary", gameService.assignedSecretary(player, player.getCurrentCity()).orElse(null));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("secretarySpecs", gameService.secretarySpecs());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("secretaryTenantEvents", gameService.secretaryTenantEvents(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("secretaryOffer", null);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("luxuryItems", gameService.luxuryItems());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("giftItems", gameService.giftItems());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockSpecs", gameService.stockSpecs());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockContentUnlocked", gameService.stockContentUnlocked(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockContentStatus", gameService.stockContentStatusText(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        // 주식 화면이 아니어도 모델은 준비한다. 내비게이션과 우측 요약이 같은 모델을 참조하기 때문이다.
        gameService.ensureStockMarketInitialized(player);
        model.addAttribute("stockQuotes", gameService.stockQuotes(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockMarketStatus", gameService.stockMarketStatus(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockTradeHistories", gameService.stockTradeHistories(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("stockHoldingSummary", gameService.stockHoldingSummary(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("cities", gameService.cities());
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("repairCountsByCity", gameService.repairRequestCountsByCity(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("loanPrincipal", loans.stream().mapToLong(Loan::getPrincipal).sum());
        // 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
        model.addAttribute("loanRepaymentTotal", loans.stream().mapToLong(Loan::remainingRepayment).sum());
        // 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
        model.addAttribute("loanMonthlyPayment", loans.stream().mapToLong(Loan::getMonthlyPayment).sum());
        // 해설: 컬렉션을 Stream으로 바꿔 filter/map/sum 같은 연산을 이어 붙인다.
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("loanLimit", gameService.loanLimit(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("availableLoanLimit", gameService.availableLoanLimit(player));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("activeEvent", gameService.activeEvent(playerId).orElse(null));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("activeAuction", gameService.activeAuction(player).orElse(null));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
    }
}
```