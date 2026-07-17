# AuthController.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/web/AuthController.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
// 해설: Spring MVC 컨트롤러다. HTTP 요청을 받아 화면 이름이나 리다이렉트를 반환한다.
public class AuthController {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 로그인, 회원가입, 로그아웃 요청을 처리한다.
     *
     * 인증 성공 시 세션에 playerId를 저장한다. 이후 GameController는 세션의 playerId로
     * 현재 플레이어를 찾기 때문에, 세션은 브라우저와 플레이어 데이터를 연결하는 열쇠 역할을 한다.
     */
    private final AuthService authService;
    // 해설: 생성자에서 주입받거나 초기화한 뒤 바꾸지 않는 필드다. 객체의 의존성 또는 고정 상태를 담는다.

    public AuthController(AuthService authService) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
    // 해설: `login` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return "login";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `register` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        try {
            Player player = authService.register(username, password);
            session.setAttribute(SessionKeys.PLAYER_ID, player.getId());
            return "redirect:/";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
            return "redirect:/login";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
    // 해설: `login` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return authService.authenticate(username, password)
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                .map(player -> {
                // 해설: 값을 다른 형태로 변환한다. 객체에서 필요한 필드만 꺼내거나 숫자로 바꿀 때 쓴다.
                    session.setAttribute(SessionKeys.PLAYER_ID, player.getId());
                    return "redirect:/";
                    // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                })
                .orElseGet(() -> {
                // 해설: Optional이 비어 있을 때 사용할 대체값이나 대체 동작을 지정한다.
                    redirectAttributes.addFlashAttribute("error", "로그인 실패");
                    // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
                    return "redirect:/login";
                    // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
    // 해설: `logout` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        session.invalidate();
        return "redirect:/login";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }
}
```