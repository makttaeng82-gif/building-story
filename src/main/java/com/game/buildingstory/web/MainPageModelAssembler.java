package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class MainPageModelAssembler {
    private final GameService gameService;

    public MainPageModelAssembler(GameService gameService) {
        this.gameService = gameService;
    }

    public void addMainPageAttributes(long playerId, Player player, Model model) {
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
    }
}
