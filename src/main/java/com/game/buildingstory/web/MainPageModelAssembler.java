package com.game.buildingstory.web;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

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

    public void addMainPageAttributes(
            long playerId,
            Player player,
            Model model,
            String viewMode,
            String requestedStockKey
    ) {
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
        if ("stocks".equals(viewMode)) {
            gameService.ensureStockMarketInitialized(player);
            var stockQuotes = gameService.stockListQuotes(player);
            String selectedStockKey = stockQuotes.stream()
                    .map(quote -> quote.stock().key())
                    .filter(key -> key.equals(requestedStockKey))
                    .findFirst()
                    .orElseGet(() -> stockQuotes.get(0).stock().key());
            model.addAttribute("stockQuotes", stockQuotes);
            model.addAttribute("selectedStockKey", selectedStockKey);
            model.addAttribute("selectedStockQuote", gameService.selectedStockQuote(player, selectedStockKey));
            model.addAttribute("stockRisingCount", stockQuotes.stream().filter(quote -> quote.changePercent() > 0).count());
            model.addAttribute("stockFallingCount", stockQuotes.stream().filter(quote -> quote.changePercent() < 0).count());
            model.addAttribute("stockFlatCount", stockQuotes.stream().filter(quote -> quote.changePercent() == 0).count());
            model.addAttribute("stockMarketStatus", gameService.stockMarketStatus(player));
            model.addAttribute("stockNewsArticles", gameService.stockNewsArticles(player));
            model.addAttribute("stockTradeHistories", gameService.stockTradeHistories(player));
            model.addAttribute("stockHoldingSummary", gameService.stockHoldingSummary(player, stockQuotes));
        } else {
            model.addAttribute("stockQuotes", List.of());
        }
        model.addAttribute("cities", gameService.cities());
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        model.addAttribute("repairCountsByCity", gameService.repairRequestCountsByCity(player));
        model.addAttribute("loanPrincipal", loans.stream().mapToLong(Loan::getPrincipal).sum());
        model.addAttribute("loanMonthlyPayment", loans.stream().mapToLong(Loan::getMonthlyPayment).sum());
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        model.addAttribute("loanLimit", gameService.loanLimit(player));
        model.addAttribute("availableLoanLimit", gameService.availableLoanLimit(player));
        model.addAttribute("activeEvent", gameService.activeEvent(playerId).orElse(null));
        model.addAttribute("activeAuction", gameService.activeAuction(player).orElse(null));
    }
}
