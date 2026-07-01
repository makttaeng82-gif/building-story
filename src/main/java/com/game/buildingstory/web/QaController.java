package com.game.buildingstory.web;

import com.game.buildingstory.service.QaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class QaController {
    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/test/cash")
    public String addTestCash(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.addTestCash(playerId));
        return "redirect:/main";
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
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestChances(playerId, moveInChance, moveOutChance, repairChance));
        return "redirect:/main";
    }

    @PostMapping("/test/reputation")
    public String updateTestReputation(@RequestParam int reputation, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestReputation(playerId, reputation));
        return "redirect:/main";
    }

    @PostMapping("/test/market-news")
    public String activateMarketNews(@RequestParam String trend, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.activateMarketNewsEvent(playerId, trend));
        return "redirect:/main";
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
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestSecretaryProficiency(playerId, secretaryKey, proficiency));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/conditions")
    public String prepareSecretaryEventConditions(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.prepareSecretaryEventTestConditions(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/building")
    public String grantSecretaryEventBuilding(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.grantSecretaryEventBuilding(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/stage")
    public String setSecretaryEventStage(@RequestParam String secretaryKey, @RequestParam String stage, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.setSecretaryEventTestStage(playerId, secretaryKey, stage));
        return "redirect:/main";
    }

    private Long currentPlayerId(HttpSession session) {
        return (Long) session.getAttribute(SessionKeys.PLAYER_ID);
    }
}
