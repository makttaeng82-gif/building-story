package com.game.buildingstory.web;

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
    /*
     * GameController는 브라우저가 호출하는 URL을 서비스 메서드에 연결한다.
     *
     * 규칙:
     * - GET 요청은 화면을 보여준다.
     * - POST 요청은 게임 상태를 바꾼 뒤 redirect 또는 JSON을 돌려준다.
     * - 현재 로그인 사용자는 HttpSession의 playerId로 찾는다.
     *
     * 컨트롤러에는 계산 규칙을 넣지 않는다. 계산은 GameService가 담당하고,
     * 컨트롤러는 입력값을 전달하고 결과 메시지를 화면으로 보내는 역할만 한다.
     */
    private final GameService gameService;
    private final MainPageModelAssembler mainPageModelAssembler;
    private final InfoPageModelAssembler infoPageModelAssembler;

    public GameController(
            GameService gameService,
            MainPageModelAssembler mainPageModelAssembler,
            InfoPageModelAssembler infoPageModelAssembler
    ) {
        this.gameService = gameService;
        this.mainPageModelAssembler = mainPageModelAssembler;
        this.infoPageModelAssembler = infoPageModelAssembler;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        Long playerId = currentPlayerId(session);
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
        if (currentPlayerId(session) == null) {
            return "redirect:/login";
        }
        return "story";
    }

    @PostMapping("/story/complete")
    public String completeStory(HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeStory(playerId);
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(
            @RequestParam(defaultValue = "city") String view,
            @RequestParam(required = false) String stockKey,
            HttpSession session,
            Model model
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
            return "redirect:/story";
        }
        gameService.ensureCompanyUnlockSchedule(player);
        String viewMode = "company".equals(view) && gameService.companyContentUnlocked(player)
                ? "company"
                : ("stocks".equals(view) && gameService.stockContentUnlocked(player) ? "stocks" : "city");
        if ("city".equals(viewMode)) {
            String governmentSupportNotice = gameService.enterCurrentCityScreen(playerId);
            if (!governmentSupportNotice.isBlank()) {
                model.addAttribute("notice", governmentSupportNotice);
                player = gameService.player(playerId);
            }
        }
        mainPageModelAssembler.addMainPageAttributes(playerId, player, model, viewMode, stockKey);
        if (!"city".equals(viewMode)) {
            // 주식·기업 화면에서는 도시 이벤트/경매 모달을 즉시 띄우지 않는다. 시간은 흐르지만 표시만 도시 화면으로 미룬다.
            model.addAttribute("activeEvent", null);
            model.addAttribute("activeAuction", null);
        }
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("screenPaused", player.isPaused()
                || ("city".equals(viewMode) && (model.asMap().get("activeEvent") != null || model.asMap().get("activeAuction") != null)));
        return "main";
    }

    @GetMapping("/info")
    public String info(HttpSession session, Model model) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        Player player = gameService.player(playerId);
        if (!player.isStorySeen()) {
            return "redirect:/story";
        }
        infoPageModelAssembler.addInfoPageAttributes(player, model);
        return "info";
    }

    @GetMapping("/stocks")
    public String stocks(HttpSession session, Model model) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        return "redirect:/main?view=stocks";
    }

    @GetMapping("/company")
    public String company(HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        return "redirect:/main?view=company";
    }

    @PostMapping("/offers/{offerId}/buy")
    public String buyOffer(@PathVariable long offerId, @RequestParam(defaultValue = "cash") String mode, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyOffer(playerId, offerId, "loan".equals(mode)));
        return "redirect:/main";
    }

    @PostMapping("/resign")
    public String resign(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.resign(playerId));
        return "redirect:/main";
    }

    @PostMapping("/buildings/{buildingId}/sell")
    public String sellBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellBuilding(playerId, buildingId));
        return "redirect:/main";
    }

    @PostMapping("/loans/{loanId}/repay")
    public String repayLoan(@PathVariable long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repayLoan(playerId, loanId));
        return "redirect:/main";
    }

    @PostMapping("/buildings/{buildingId}/repair")
    public String repairBuilding(@PathVariable long buildingId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.repairBuilding(playerId, buildingId));
        return "redirect:/main";
    }

    @PostMapping("/secretary/first/hire")
    public String hireFirstSecretary(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireFirstSecretary(playerId));
        return "redirect:/main";
    }

    @PostMapping("/pause/toggle")
    public String togglePause(
            @RequestParam(defaultValue = "city") String redirectView,
            @RequestParam(required = false) String selectedStockKey,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.togglePause(playerId));
        if ("stocks".equals(redirectView)) {
            return stockRedirect(selectedStockKey);
        }
        if ("company".equals(redirectView)) {
            return "redirect:/main?view=company";
        }
        return "redirect:/main";
    }

    @PostMapping("/stocks/account/deposit")
    @ResponseBody
    public Map<String, String> depositSecuritiesCash(@RequestParam long amount, HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return Map.of("redirect", "/login");
        }
        String notice = gameService.depositSecuritiesCash(playerId, amount);
        Player player = gameService.player(playerId);
        return Map.of(
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "cashRaw", String.valueOf(player.getCash()),
                "securitiesCash", gameService.stockMoneyText(player.getSecuritiesCash()),
                "securitiesCashRaw", String.valueOf(player.getSecuritiesCash())
        );
    }

    @PostMapping("/stocks/account/withdraw")
    @ResponseBody
    public Map<String, String> withdrawSecuritiesCash(@RequestParam long amount, HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return Map.of("redirect", "/login");
        }
        String notice = gameService.withdrawSecuritiesCash(playerId, amount);
        Player player = gameService.player(playerId);
        return Map.of(
                "notice", notice,
                "cash", String.format("%,d원", player.getCash()),
                "cashRaw", String.valueOf(player.getCash()),
                "securitiesCash", gameService.stockMoneyText(player.getSecuritiesCash()),
                "securitiesCashRaw", String.valueOf(player.getSecuritiesCash())
        );
    }

    @PostMapping("/stocks/{stockKey}/buy")
    public String buyStock(@PathVariable String stockKey, @RequestParam(defaultValue = "1") long quantity, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyStock(playerId, stockKey, quantity));
        return stockRedirect(stockKey);
    }

    @PostMapping("/stocks/{stockKey}/buy-max")
    public String buyMaxStock(@PathVariable String stockKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyMaxStock(playerId, stockKey));
        return stockRedirect(stockKey);
    }

    @PostMapping("/stocks/{stockKey}/sell")
    public String sellStock(@PathVariable String stockKey, @RequestParam(defaultValue = "1") long quantity, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellStock(playerId, stockKey, quantity));
        return stockRedirect(stockKey);
    }

    @PostMapping("/stocks/{stockKey}/sell-all")
    public String sellAllStock(@PathVariable String stockKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.sellAllStock(playerId, stockKey));
        return stockRedirect(stockKey);
    }

    @PostMapping("/stocks/ipo/{stockKey}/subscribe")
    public String subscribeNpcIpo(
            @PathVariable String stockKey,
            @RequestParam(defaultValue = "1") long quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.subscribeNpcIpo(playerId, stockKey, quantity));
        return "redirect:/main?view=stocks";
    }

    @PostMapping("/stocks/ipo/{stockKey}/result/read")
    @ResponseBody
    public Map<String, Boolean> acknowledgeNpcIpoResult(@PathVariable String stockKey, HttpSession session) {
        Long playerId = currentPlayerId(session);
        return Map.of("acknowledged", playerId != null && gameService.acknowledgeNpcIpoResult(playerId, stockKey));
    }

    @PostMapping("/stocks/news/{articleId}/read")
    @ResponseBody
    public Map<String, Boolean> readStockNews(@PathVariable long articleId, HttpSession session) {
        Long playerId = currentPlayerId(session);
        return Map.of("read", playerId != null && gameService.markStockNewsRead(playerId, articleId));
    }

    @PostMapping("/donations")
    public String donate(@RequestParam(defaultValue = "1") int multiplier, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.donate(playerId, multiplier));
        return "redirect:/main";
    }

    @PostMapping("/luxury-items/{itemKey}/buy")
    public String buyLuxuryItem(@PathVariable String itemKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
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
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.buyGiftItem(playerId, giftKey, quantity));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/bid")
    public String bidAuction(@PathVariable long auctionId, @RequestParam int rate, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.bidAuction(playerId, auctionId, rate));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/cancel")
    public String cancelAuction(@PathVariable long auctionId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.cancelAuction(playerId, auctionId));
        return "redirect:/main";
    }

    @PostMapping("/auctions/{auctionId}/complete")
    public String completeAuctionResult(@PathVariable long auctionId, HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeAuctionResult(playerId, auctionId);
        return "redirect:/main";
    }

    @PostMapping("/secretaries/{secretaryKey}/hire")
    public String hireSecretary(@PathVariable String secretaryKey, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hireSecretary(playerId, secretaryKey));
        return "redirect:/main";
    }

    @PostMapping("/secretaries/{secretaryKey}/dismiss")
    public String dismissSecretaryOffer(@PathVariable String secretaryKey, HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.dismissSecretaryOffer(playerId, secretaryKey);
        return "redirect:/main";
    }

    @PostMapping("/property-managers/hire")
    public String hirePropertyManager(
            @RequestParam String city,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.hirePropertyManager(playerId, city));
        return "redirect:/main";
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/assign")
    public String assignSecretary(@PathVariable long ownedSecretaryId, @RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.assignSecretary(playerId, ownedSecretaryId, city));
        return "redirect:/main";
    }

    @PostMapping("/owned-secretaries/{ownedSecretaryId}/unassign")
    public String unassignSecretary(@PathVariable long ownedSecretaryId, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
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
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.giveGiftToSecretary(playerId, ownedSecretaryId, giftKey, quantity));
        return "redirect:/main";
    }

    @PostMapping("/city")
    public String changeCity(@RequestParam String city, HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", gameService.changeCity(playerId, city));
        return "redirect:/main";
    }

    @PostMapping("/tick")
    @ResponseBody
    public Map<String, String> tick(
            @RequestParam(defaultValue = "city") String view,
            @RequestParam int expectedElapsedDays,
            HttpSession session
    ) {
        // 프론트의 공통 시간 루프가 호출한다. 도시 외 화면은 도시 이벤트 표시를 지연시키는 모드로 하루를 진행한다.
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return Map.of("redirect", "/login");
        }
        String result = gameService.tick(playerId, !"city".equals(view), expectedElapsedDays);
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
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.completeEvent(playerId, eventId);
        return "redirect:/main";
    }

    @PostMapping("/events/{eventId}/cancel")
    public String cancelEvent(@PathVariable long eventId, HttpSession session) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        gameService.cancelEvent(playerId, eventId);
        return "redirect:/main";
    }

    private String stockRedirect(String stockKey) {
        if (stockKey == null || stockKey.isBlank()) {
            return "redirect:/main?view=stocks";
        }
        return "redirect:/main?view=stocks&stockKey=" + stockKey;
    }

    private Long currentPlayerId(HttpSession session) {
        return (Long) session.getAttribute(SessionKeys.PLAYER_ID);
    }

}

