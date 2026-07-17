# QaController.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/QaController.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import com.game.buildingstory.service.QaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
// 해설: Spring MVC 컨트롤러다. HTTP 요청을 받아 화면 이름이나 리다이렉트를 반환한다.
public class QaController {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 개발/테스트 편의를 위한 QA 엔드포인트다.
     *
     * 실제 게임 규칙을 우회해 현금, 평판, 이벤트 상태를 빠르게 조정한다.
     * 수동 테스트에서 특정 후반 컨텐츠를 확인하려면 정상 플레이로 오래 진행해야 하므로,
     * 이 컨트롤러가 테스트 시간을 줄여준다.
     */
    private final QaService qaService;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    public QaController(QaService qaService) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.qaService = qaService;
    }

    @PostMapping("/test/cash")
    public String addTestCash(HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `addTestCash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.addTestCash(playerId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/chances")
    public String updateTestChances(
            @RequestParam int moveInChance,
            @RequestParam int moveOutChance,
            @RequestParam int repairChance,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestChances(playerId, moveInChance, moveOutChance, repairChance));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/reputation")
    public String updateTestReputation(@RequestParam int reputation, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `updateTestReputation` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestReputation(playerId, reputation));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/market-news")
    public String activateMarketNews(@RequestParam String trend, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `activateMarketNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.activateMarketNewsEvent(playerId, trend));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/secretary-proficiency")
    public String updateTestSecretaryProficiency(
            @RequestParam String secretaryKey,
            @RequestParam int proficiency,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestSecretaryProficiency(playerId, secretaryKey, proficiency));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/secretary-event/conditions")
    public String prepareSecretaryEventConditions(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `prepareSecretaryEventConditions` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.prepareSecretaryEventTestConditions(playerId, secretaryKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/secretary-event/building")
    public String grantSecretaryEventBuilding(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `grantSecretaryEventBuilding` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.grantSecretaryEventBuilding(playerId, secretaryKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/test/secretary-event/stage")
    public String setSecretaryEventStage(@RequestParam String secretaryKey, @RequestParam String stage, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `setSecretaryEventStage` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", qaService.setSecretaryEventTestStage(playerId, secretaryKey, stage));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    private Long currentPlayerId(HttpSession session) {
    // 해설: `currentPlayerId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```