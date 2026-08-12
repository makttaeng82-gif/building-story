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
    private final CompanyPageModelAssembler companyPageModelAssembler;

    public MainPageModelAssembler(GameService gameService, CompanyPageModelAssembler companyPageModelAssembler) {
        this.gameService = gameService;
        this.companyPageModelAssembler = companyPageModelAssembler;
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
        gameService.ensureCompanyUnlockSchedule(player);
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
        var propertyManagers = gameService.propertyManagers(player);
        var repairCountsByCity = gameService.repairRequestCountsByCity(player);
        var recentPropertyManagerRepairCountsByCity = gameService.recentPropertyManagerRepairCountsByCity(player);
        var propertyManagersByCity = propertyManagers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        manager -> manager.getCity(),
                        manager -> manager
                ));
        var propertyManager = gameService.propertyManager(player, player.getCurrentCity()).orElse(null);
        model.addAttribute("propertyManager", propertyManager);
        model.addAttribute("propertyManagers", propertyManagers);
        model.addAttribute("propertyManagersByCity", propertyManagersByCity);
        model.addAttribute("recentPropertyManagerRepairCountsByCity", recentPropertyManagerRepairCountsByCity);
        model.addAttribute("activePropertyManagerCount", propertyManagers.stream().filter(manager -> manager.isActive()).count());
        model.addAttribute("propertyManagerTotalSalaryDue", propertyManagers.stream()
                .mapToLong(gameService::propertyManagerSalaryDue)
                .sum());
        model.addAttribute("propertyManagerMonthlySalary", gameService.propertyManagerMonthlySalary());
        model.addAttribute("propertyManagerSalaryDue", propertyManager == null ? 0L : gameService.propertyManagerSalaryDue(propertyManager));
        model.addAttribute("propertyManagerFeatureVisible", gameService.propertyManagerFeatureVisible(player));
        model.addAttribute("propertyManagerHandoffReady", gameService.propertyManagerHandoffReady(player, player.getCurrentCity()));
        model.addAttribute("propertyManagerHandoffStatus", gameService.propertyManagerHandoffStatusText(player, player.getCurrentCity()));
        model.addAttribute("secretarySpecs", gameService.secretarySpecs());
        model.addAttribute("secretaryTenantEvents", gameService.secretaryTenantEvents(player));
        model.addAttribute("secretaryOffer", null);
        model.addAttribute("luxuryItems", gameService.luxuryItems());
        model.addAttribute("giftItems", gameService.giftItems());
        model.addAttribute("stockSpecs", gameService.stockSpecs(player));
        model.addAttribute("stockContentUnlocked", gameService.stockContentUnlocked(player));
        model.addAttribute("stockContentStatus", gameService.stockContentStatusText(player));
        model.addAttribute("companyContentUnlocked", gameService.companyContentUnlocked(player));
        model.addAttribute("companyContentStatus", gameService.companyContentStatusText(player));
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
            model.addAttribute("activeNpcIpo", gameService.activeNpcIpoSubscription(player).orElse(null));
            model.addAttribute("pendingNpcIpoResult", gameService.pendingNpcIpoResult(player).orElse(null));
        } else {
            model.addAttribute("stockQuotes", List.of());
        }
        if ("company".equals(viewMode)) {
            companyPageModelAssembler.addCompanyPageAttributes(player, model);
        }
        model.addAttribute("cities", gameService.cities());
        model.addAttribute("cityUnlocks", gameService.cityUnlocks(player));
        model.addAttribute("repairCountsByCity", repairCountsByCity);
        model.addAttribute("loanPrincipal", loans.stream().mapToLong(Loan::getPrincipal).sum());
        model.addAttribute("loanMonthlyPayment", loans.stream().mapToLong(Loan::getMonthlyPayment).sum());
        model.addAttribute("loanRemainingRepayment", gameService.remainingLoanRepayment(player));
        model.addAttribute("loanLimit", gameService.loanLimit(player));
        model.addAttribute("availableLoanLimit", gameService.availableLoanLimit(player));
        model.addAttribute("activeEvent", gameService.activeEvent(playerId).orElse(null));
        model.addAttribute("activeAuction", gameService.activeAuction(player).orElse(null));
    }
}
