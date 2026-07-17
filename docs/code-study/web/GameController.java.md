# GameController.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/GameController.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
// 해설: Spring MVC 컨트롤러다. HTTP 요청을 받아 화면 이름이나 리다이렉트를 반환한다.
public class GameController {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * GameController는 브라우저가 호출하는 URL을 서비스 메서드에 연결한다.
     *
     * 규칙:
     * - GET 요청은 화면을 보여준다.
     * - POST 요청은 게임 상태를 바꾼 뒤 redirect 또는 JSON을 돌려준다.
     * - 현재 로그인 사용자는 HttpSession의 playerId로 찾는다.
     *
     * 컨트롤러에는 계산 규칙을 넣지 않는다. 계산은 GameService가 담당하고,
     * 컨트롤러는 입력값을 전달하고 결과 메시지를 화면으로 보내는 역할만 한다.
     */
    private final GameService gameService;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.
    private final MainPageModelAssembler mainPageModelAssembler;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.
    private final InfoPageModelAssembler infoPageModelAssembler;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    public GameController(
            GameService gameService,
            MainPageModelAssembler mainPageModelAssembler,
            InfoPageModelAssembler infoPageModelAssembler
    ) {
        this.gameService = gameService;
        this.mainPageModelAssembler = mainPageModelAssembler;
        this.infoPageModelAssembler = infoPageModelAssembler;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
    // 해설: `index` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/story";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @GetMapping("/story")
    public String story(HttpSession session) {
    // 해설: `story` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (currentPlayerId(session) == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return "story";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/story/complete")
    public String completeStory(HttpSession session) {
    // 해설: `completeStory` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        gameService.completeStory(playerId);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @GetMapping("/main")
    public String main(@RequestParam(defaultValue = "city") String view, HttpSession session, Model model) {
    // 해설: `main` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/story";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        // view 파라미터는 사용자가 보고 싶은 화면이다. 주식이 아직 잠겨 있으면 강제로 도시 화면을 보여준다.
        String viewMode = "stocks".equals(view) && gameService.stockContentUnlocked(player) ? "stocks" : "city";
        mainPageModelAssembler.addMainPageAttributes(playerId, player, model);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        boolean hiddenCityModal = false;
        if ("stocks".equals(viewMode)) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            // 주식 화면에서는 도시 이벤트/경매 모달을 즉시 띄우지 않는다. 시간은 흐르지만 표시만 도시 화면으로 미룬다.
            hiddenCityModal = model.asMap().get("activeEvent") != null || model.asMap().get("activeAuction") != null;
            model.addAttribute("activeEvent", null);
            // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
            model.addAttribute("activeAuction", null);
            // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        }
        model.addAttribute("viewMode", viewMode);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        model.addAttribute("screenPaused", ("city".equals(viewMode) && (model.asMap().get("activeEvent") != null || model.asMap().get("activeAuction") != null))
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
                || (player.isPaused() && !hiddenCityModal));
        return "main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @GetMapping("/info")
    public String info(HttpSession session, Model model) {
    // 해설: `info` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/story";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        infoPageModelAssembler.addInfoPageAttributes(player, model);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "info";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @GetMapping("/stocks")
    public String stocks(HttpSession session, Model model) {
    // 해설: `stocks` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return "redirect:/main?view=stocks";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/side-job")
    public String sideJob(HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `sideJob` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sideJob(playerId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/side-job/quick")
    @ResponseBody
    public Map<String, String> sideJobQuick(HttpSession session) {
    // 해설: `sideJobQuick` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // quick 엔드포인트는 전체 페이지 새로고침 없이 상단 현금/월세 표시만 갱신하기 위한 JSON API다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("redirect", "/login");
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        String notice = gameService.sideJob(playerId);
        Player player = gameService.player(playerId);
        return Map.of(
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "totalMonthlyRent", String.format("%,d원", gameService.totalMonthlyRent(player))
        );
    }

    @PostMapping("/offers/{offerId}/buy")
    public String buyOffer(@PathVariable long offerId, @RequestParam(defaultValue = "cash") String mode, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `buyOffer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyOffer(playerId, offerId, "loan".equals(mode)));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/resign")
    public String resign(HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `resign` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.resign(playerId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/buildings/{buildingId}/sell")
    public String sellBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `sellBuilding` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellBuilding(playerId, buildingId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/loans/{loanId}/repay")
    public String repayLoan(@PathVariable long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `repayLoan` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repayLoan(playerId, loanId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/buildings/{buildingId}/repair")
    public String repairBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `repairBuilding` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repairBuilding(playerId, buildingId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/secretary/first/hire")
    public String hireFirstSecretary(HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `hireFirstSecretary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireFirstSecretary(playerId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/pause/toggle")
    public String togglePause(
            @RequestParam(defaultValue = "city") String redirectView,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.togglePause(playerId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        if ("stocks".equals(redirectView)) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/main?view=stocks";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/stocks/exchange/cash-to-coin")
    @ResponseBody
    public Map<String, String> exchangeCashToCoin(@RequestParam long coinAmount, HttpSession session) {
    // 해설: `exchangeCashToCoin` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("redirect", "/login");
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        String notice = gameService.exchangeCashToCoin(playerId, coinAmount);
        Player player = gameService.player(playerId);
        return Map.of(
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "cashRaw", String.valueOf(player.getCash()),
                "coin", gameService.stockCoinText(player.getCoin()),
                "coinRaw", String.valueOf(player.getCoin())
        );
    }

    @PostMapping("/stocks/exchange/coin-to-cash")
    @ResponseBody
    public Map<String, String> exchangeCoinToCash(@RequestParam long coinAmount, HttpSession session) {
    // 해설: `exchangeCoinToCash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("redirect", "/login");
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        String notice = gameService.exchangeCoinToCash(playerId, coinAmount);
        Player player = gameService.player(playerId);
        return Map.of(
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "cashRaw", String.valueOf(player.getCash()),
                "coin", gameService.stockCoinText(player.getCoin()),
                "coinRaw", String.valueOf(player.getCoin())
        );
    }

    @PostMapping("/stocks/{stockKey}/buy")
    public String buyStock(@PathVariable String stockKey, @RequestParam(defaultValue = "1") long quantity, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `buyStock` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyStock(playerId, stockKey, quantity));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main?view=stocks";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/stocks/{stockKey}/buy-max")
    public String buyMaxStock(@PathVariable String stockKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `buyMaxStock` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyMaxStock(playerId, stockKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main?view=stocks";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/stocks/{stockKey}/sell")
    public String sellStock(@PathVariable String stockKey, @RequestParam(defaultValue = "1") long quantity, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `sellStock` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellStock(playerId, stockKey, quantity));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main?view=stocks";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/stocks/{stockKey}/sell-all")
    public String sellAllStock(@PathVariable String stockKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `sellAllStock` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellAllStock(playerId, stockKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main?view=stocks";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/donations")
    public String donate(@RequestParam(defaultValue = "1") int multiplier, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `donate` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.donate(playerId, multiplier));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/luxury-items/{itemKey}/buy")
    public String buyLuxuryItem(@PathVariable String itemKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `buyLuxuryItem` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyLuxuryItem(playerId, itemKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/gift-items/{giftKey}/buy")
    public String buyGiftItem(
            @PathVariable String giftKey,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyGiftItem(playerId, giftKey, quantity));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/auctions/{auctionId}/bid")
    public String bidAuction(@PathVariable long auctionId, @RequestParam int rate, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `bidAuction` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.bidAuction(playerId, auctionId, rate));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/auctions/{auctionId}/cancel")
    public String cancelAuction(@PathVariable long auctionId, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `cancelAuction` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.cancelAuction(playerId, auctionId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/auctions/{auctionId}/complete")
    public String completeAuctionResult(@PathVariable long auctionId, HttpSession session) {
    // 해설: `completeAuctionResult` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        gameService.completeAuctionResult(playerId, auctionId);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/secretaries/{secretaryKey}/hire")
    public String hireSecretary(@PathVariable String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `hireSecretary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireSecretary(playerId, secretaryKey));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/secretaries/{secretaryKey}/dismiss")
    public String dismissSecretaryOffer(@PathVariable String secretaryKey, HttpSession session) {
    // 해설: `dismissSecretaryOffer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        gameService.dismissSecretaryOffer(playerId, secretaryKey);
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/assign")
    public String assignSecretary(@PathVariable long ownedSecretaryId, @RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `assignSecretary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.assignSecretary(playerId, ownedSecretaryId, city));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/unassign")
    public String unassignSecretary(@PathVariable long ownedSecretaryId, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `unassignSecretary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.unassignSecretary(playerId, ownedSecretaryId));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/gifts")
    public String giveGiftToSecretary(
            @PathVariable long ownedSecretaryId,
            @RequestParam String giftKey,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.giveGiftToSecretary(playerId, ownedSecretaryId, giftKey, quantity));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/city")
    public String changeCity(@RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `changeCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        redirectAttributes.addFlashAttribute("notice", gameService.changeCity(playerId, city));
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/tick")
    @ResponseBody
    public Map<String, String> tick(@RequestParam(defaultValue = "city") String view, HttpSession session) {
    // 해설: `tick` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 프론트의 공통 시간 루프가 호출한다. view=stocks면 도시 이벤트 표시를 지연시키는 모드로 하루를 진행한다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("redirect", "/login");
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        String result = gameService.tick(playerId, "stocks".equals(view));
        if (result.startsWith("EVENT:")) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("event", result.substring("EVENT:".length()));
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        if (result.startsWith("AUCTION:")) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return Map.of("auction", result.substring("AUCTION:".length()));
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return Map.of("notice", result);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/events/{eventId}/complete")
    public String completeEvent(@PathVariable long eventId, HttpSession session) {
    // 해설: `completeEvent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        gameService.completeEvent(playerId, eventId);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        return "redirect:/main";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/events/{eventId}/cancel")
    public String cancelEvent(@PathVariable long eventId, HttpSession session) {
    // 해설: `cancelEvent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        gameService.cancelEvent(playerId, eventId);
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