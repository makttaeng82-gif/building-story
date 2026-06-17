package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.QaService;
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
    private final QaService qaService;

    public GameController(GameService gameService, QaService qaService) {
        this.gameService = gameService;
        this.qaService = qaService;
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
        gameService.evaluateSecretaryTenantEvents(player);
        var loans = gameService.loans(player);
        model.addAttribute("player", player);
        model.addAttribute("offers", gameService.offers(player));
        model.addAttribute("buildings", gameService.ownedBuildings(player));
        model.addAttribute("loans", loans);
        model.addAttribute("records", gameService.recentRecords(player));
        model.addAttribute("totalMonthlyRent", gameService.totalMonthlyRent(player));
        model.addAttribute("ownedSecretaries", gameService.ownedSecretaries(player));
        model.addAttribute("assignedSecretary", gameService.assignedSecretary(player, player.getCurrentCity()).orElse(null));
        model.addAttribute("secretarySpecs", gameService.secretarySpecs());
        model.addAttribute("secretaryTenantEvents", gameService.secretaryTenantEvents(player));
        model.addAttribute("secretaryOffer", null);
        model.addAttribute("luxuryItems", gameService.luxuryItems());
        model.addAttribute("giftItems", gameService.giftItems());
        model.addAttribute("cities", gameService.cities());
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        model.addAttribute("repairCountsByCity", gameService.repairRequestCountsByCity(player));
        model.addAttribute("loanPrincipal", loans.stream().mapToLong(Loan::getPrincipal).sum());
        model.addAttribute("loanRepaymentTotal", loans.stream().mapToLong(Loan::remainingRepayment).sum());
        model.addAttribute("loanMonthlyPayment", loans.stream().mapToLong(Loan::getMonthlyPayment).sum());
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        model.addAttribute("loanLimit", gameService.loanLimit(player));
        model.addAttribute("availableLoanLimit", gameService.availableLoanLimit(player));
        model.addAttribute("activeEvent", gameService.activeEvent(playerId).orElse(null));
        model.addAttribute("activeAuction", gameService.activeAuction(player).orElse(null));
        return "main";
    }

    @GetMapping("/info")
    public String info(HttpSession session, Model model) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
            return "redirect:/story";
        }
        model.addAttribute("player", player);
        model.addAttribute("totalMonthlyRent", gameService.totalMonthlyRent(player));
        var loans = gameService.loans(player);
        model.addAttribute("loans", loans);
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        model.addAttribute("cities", gameService.cities());
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        model.addAttribute("repairCountsByCity", gameService.repairRequestCountsByCity(player));
        model.addAttribute("buildingSpecs", gameService.buildingSpecs());
        model.addAttribute("reputationTiers", gameService.reputationTiers());
        model.addAttribute("secretarySpecs", gameService.secretarySpecs());
        return "info";
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

    @PostMapping("/side-job/quick")
    @ResponseBody
    public Map<String, String> sideJobQuick(HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return Map.of("redirect", "/login");
        }
        String notice = gameService.sideJob(playerId);
        Player player = gameService.player(playerId);
        return Map.of(
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "totalMonthlyRent", String.format("%,d원", gameService.totalMonthlyRent(player))
        );
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

    @PostMapping("/resign")
    public String resign(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.resign(playerId));
        return "redirect:/main";
    }

    @PostMapping("/buildings/{buildingId}/sell")
    public String sellBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellBuilding(playerId, buildingId));
        return "redirect:/main";
    }

    @PostMapping("/loans/{loanId}/repay")
    public String repayLoan(@PathVariable long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repayLoan(playerId, loanId));
        return "redirect:/main";
    }

    @PostMapping("/buildings/{buildingId}/repair")
    public String repairBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repairBuilding(playerId, buildingId));
        return "redirect:/main";
    }

    @PostMapping("/secretary/first/hire")
    public String hireFirstSecretary(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireFirstSecretary(playerId));
        return "redirect:/main";
    }

    @PostMapping("/test/cash")
    public String addTestCash(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
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
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestChances(playerId, moveInChance, moveOutChance, repairChance));
        return "redirect:/main";
    }

    @PostMapping("/pause/toggle")
    public String togglePause(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.togglePause(playerId));
        return "redirect:/main";
    }

    @PostMapping("/test/reputation")
    public String updateTestReputation(@RequestParam int reputation, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestReputation(playerId, reputation));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-proficiency")
    public String updateTestSecretaryProficiency(
            @RequestParam String secretaryKey,
            @RequestParam int proficiency,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.updateTestSecretaryProficiency(playerId, secretaryKey, proficiency));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/conditions")
    public String prepareSecretaryEventConditions(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.prepareSecretaryEventTestConditions(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/building")
    public String grantSecretaryEventBuilding(@RequestParam String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.grantSecretaryEventBuilding(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/test/secretary-event/stage")
    public String setSecretaryEventStage(@RequestParam String secretaryKey, @RequestParam String stage, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", qaService.setSecretaryEventTestStage(playerId, secretaryKey, stage));
        return "redirect:/main";
    }

    @PostMapping("/donations")
    public String donate(@RequestParam(defaultValue = "1") int multiplier, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.donate(playerId, multiplier));
        return "redirect:/main";
    }

    @PostMapping("/luxury-items/{itemKey}/buy")
    public String buyLuxuryItem(@PathVariable String itemKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyLuxuryItem(playerId, itemKey));
        return "redirect:/main";
    }

    @PostMapping("/gift-items/{giftKey}/buy")
    public String buyGiftItem(
            @PathVariable String giftKey,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyGiftItem(playerId, giftKey, quantity));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/bid")
    public String bidAuction(@PathVariable long auctionId, @RequestParam int rate, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.bidAuction(playerId, auctionId, rate));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/cancel")
    public String cancelAuction(@PathVariable long auctionId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.cancelAuction(playerId, auctionId));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/complete")
    public String completeAuctionResult(@PathVariable long auctionId, HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeAuctionResult(playerId, auctionId);
        return "redirect:/main";
    }

    @PostMapping("/secretaries/{secretaryKey}/hire")
    public String hireSecretary(@PathVariable String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireSecretary(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/secretaries/{secretaryKey}/dismiss")
    public String dismissSecretaryOffer(@PathVariable String secretaryKey, HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.dismissSecretaryOffer(playerId, secretaryKey);
        return "redirect:/main";
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/assign")
    public String assignSecretary(@PathVariable long ownedSecretaryId, @RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.assignSecretary(playerId, ownedSecretaryId, city));
        return "redirect:/main";
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/unassign")
    public String unassignSecretary(@PathVariable long ownedSecretaryId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.unassignSecretary(playerId, ownedSecretaryId));
        return "redirect:/main";
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/gifts")
    public String giveGiftToSecretary(
            @PathVariable long ownedSecretaryId,
            @RequestParam String giftKey,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.giveGiftToSecretary(playerId, ownedSecretaryId, giftKey, quantity));
        return "redirect:/main";
    }

    @PostMapping("/city")
    public String changeCity(@RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.changeCity(playerId, city));
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
        if (result.startsWith("AUCTION:")) {
            return Map.of("auction", result.substring("AUCTION:".length()));
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

    @PostMapping("/events/{eventId}/cancel")
    public String cancelEvent(@PathVariable long eventId, HttpSession session) {
        Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.cancelEvent(playerId, eventId);
        return "redirect:/main";
    }
}
