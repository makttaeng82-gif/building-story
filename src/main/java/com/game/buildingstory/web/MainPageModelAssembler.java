package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class MainPageModelAssembler {
    /*
     * 메인 화면은 표시할 데이터가 많다.
     * 컨트롤러에 이 코드를 모두 두면 URL 처리와 화면 데이터 조립이 섞여 읽기 어려워진다.
     * 그래서 이 조립 전용 컴포넌트가 Model에 필요한 값을 한 곳에서 채운다.
     */
    private final GameService gameService;

    public MainPageModelAssembler(GameService gameService) {
        this.gameService = gameService;
    }

    public void addMainPageAttributes(long playerId, Player player, Model model) {
        // 화면 렌더링 전에 필요한 상태를 먼저 준비한다. 예: 매물이 없으면 생성, 주식 unlock 예약, 비서 이벤트 평가.
        gameService.ensureOffers(player);
        gameService.ensureStockUnlockSchedule(player);
        gameService.evaluateSecretaryTenantEvents(player);
        var loans = gameService.loans(player);
        // 아래 attribute 이름은 Thymeleaf 템플릿에서 ${player}, ${offers}처럼 직접 참조된다.
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
        model.addAttribute("stockSpecs", gameService.stockSpecs());
        model.addAttribute("stockContentUnlocked", gameService.stockContentUnlocked(player));
        model.addAttribute("stockContentStatus", gameService.stockContentStatusText(player));
        // 주식 화면이 아니어도 모델은 준비한다. 내비게이션과 우측 요약이 같은 모델을 참조하기 때문이다.
        gameService.ensureStockMarketInitialized(player);
        model.addAttribute("stockQuotes", gameService.stockQuotes(player));
        model.addAttribute("stockMarketStatus", gameService.stockMarketStatus(player));
        model.addAttribute("stockTradeHistories", gameService.stockTradeHistories(player));
        model.addAttribute("stockHoldingSummary", gameService.stockHoldingSummary(player));
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
