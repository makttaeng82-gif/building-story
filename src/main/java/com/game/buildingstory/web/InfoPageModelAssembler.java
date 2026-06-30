package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class InfoPageModelAssembler {
    private final GameService gameService;

    public InfoPageModelAssembler(GameService gameService) {
        this.gameService = gameService;
    }

    public void addInfoPageAttributes(Player player, Model model) {
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
    }
}
