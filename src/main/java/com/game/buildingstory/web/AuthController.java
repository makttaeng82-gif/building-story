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
public class AuthController {
    /*
     * 로그인, 회원가입, 로그아웃 요청을 처리한다.
     *
     * 인증 성공 시 세션에 playerId를 저장한다. 이후 GameController는 세션의 playerId로
     * 현재 플레이어를 찾기 때문에, 세션은 브라우저와 플레이어 데이터를 연결하는 열쇠 역할을 한다.
     */
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Player player = authService.register(username, password);
            session.setAttribute(SessionKeys.PLAYER_ID, player.getId());
            return "redirect:/";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes redirectAttributes) {
        return authService.authenticate(username, password)
                .map(player -> {
                    session.setAttribute(SessionKeys.PLAYER_ID, player.getId());
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "로그인 실패");
                    return "redirect:/login";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
