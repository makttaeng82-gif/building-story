package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class InfoPageModelAssembler {
    /*
     * 정보 화면에 필요한 카탈로그성 데이터를 Model에 담는다.
     *
     * 정보 화면은 플레이어 상태를 크게 바꾸지 않고, 건물 가격/도시 해금/비서 스펙처럼
     * 읽기 전용 데이터가 많다. 이 조립 코드를 컨트롤러 밖에 두면 화면 구성이 명확해진다.
     */
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
        model.addAttribute("stockSpecs", gameService.stockSpecs());
        model.addAttribute("stockContentUnlocked", gameService.stockContentUnlocked(player));
        model.addAttribute("stockContentStatus", gameService.stockContentStatusText(player));
        gameService.ensureCompanyUnlockSchedule(player);
        model.addAttribute("companyContentUnlocked", gameService.companyContentUnlocked(player));
        model.addAttribute("companyContentStatus", gameService.companyContentStatusText(player));
    }
}
