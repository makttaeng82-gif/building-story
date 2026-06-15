package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
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
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
            return "redirect:/story";
        }
        return "redirect:/main";
    }

    @GetMapping("/story")
    public String story(HttpSession session) {
        if (session.getAttribute(SessionKeys.PLAYER_ID) == null) {
            return "redirect:/login";
        }
        return "story";
    }

    @PostMapping("/story/complete")
    public String completeStory(HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeStory(playerId);
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
            return "redirect:/story";
        }
        gameService.ensureOffers(player);
        model.addAttribute("player", player);
        model.addAttribute("offers", gameService.offers(player));
        model.addAttribute("buildings", gameService.ownedBuildings(player));
        model.addAttribute("loans", gameService.loans(player));
        model.addAttribute("loanPrincipal", gameService.loans(player).stream().mapToLong(Loan::getPrincipal).sum());
        model.addAttribute("activeEvent", gameService.activeEvent(player).orElse(null));
        return "main";
    }

    @PostMapping("/side-job")
    public String sideJob(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sideJob(playerId));
        return "redirect:/main";
    }

    @PostMapping("/offers/{offerId}/buy")
    public String buyOffer(@PathVariable long offerId, @RequestParam(defaultValue = "cash") String mode, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyOffer(playerId, offerId, "loan".equals(mode)));
        return "redirect:/main";
    }

    @PostMapping("/tick")
    @ResponseBody
    public Map<String, String> tick(HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return Map.of("redirect", "/login");
        }
        String result = gameService.tick(playerId);
        if (result.startsWith("EVENT:")) {
            return Map.of("event", result.substring("EVENT:".length()));
        }
        return Map.of("notice", result);
    }

    @PostMapping("/events/{eventId}/complete")
    public String completeEvent(@PathVariable long eventId, HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeEvent(playerId, eventId);
        return "redirect:/main";
    }
}
