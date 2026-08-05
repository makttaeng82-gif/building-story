package com.game.buildingstory.web;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.PlayerCompanyService;
import com.game.buildingstory.service.CompanyTutorialService;
import com.game.buildingstory.service.CompanySettlementService;
import com.game.buildingstory.service.CompanyWorkforceService;
import com.game.buildingstory.service.CompanyInfrastructureService;
import com.game.buildingstory.service.CompanyProductProjectService;
import com.game.buildingstory.service.CompanyComputeConstructionService;
import com.game.buildingstory.service.CompanyShortTermProjectService;
import com.game.buildingstory.service.CompanyCustomerContractService;
import com.game.buildingstory.service.CompanyServiceIncidentService;
import com.game.buildingstory.service.CompanyNewsService;
import com.game.buildingstory.service.CompanyReportingService;
import com.game.buildingstory.service.CompanyDepartmentService;
import com.game.buildingstory.service.CompanyOrganizationService;
import com.game.buildingstory.service.CompanyFinanceService;
import com.game.buildingstory.domain.CompanyCloudPlan;
import com.game.buildingstory.domain.CompanyProductImprovementType;
import com.game.buildingstory.domain.CompanyDevelopmentDirection;
import com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy;
import com.game.buildingstory.domain.CompanyMarketingBudgetPolicy;
import com.game.buildingstory.domain.CompanyServiceIncidentResolution;
import com.game.buildingstory.domain.CompanyShortTermProjectRiskResolution;
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
import java.util.List;

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
    private final PlayerCompanyService playerCompanyService;
    private final CompanyTutorialService companyTutorialService;
    private final CompanySettlementService companySettlementService;
    private final CompanyWorkforceService companyWorkforceService;
    private final CompanyInfrastructureService companyInfrastructureService;
    private final CompanyProductProjectService companyProductProjectService;
    private final CompanyComputeConstructionService companyComputeConstructionService;
    private final CompanyShortTermProjectService companyShortTermProjectService;
    private final CompanyCustomerContractService companyCustomerContractService;
    private final CompanyServiceIncidentService companyServiceIncidentService;
    private final CompanyNewsService companyNewsService;
    private final CompanyReportingService companyReportingService;
    private final CompanyDepartmentService companyDepartmentService;
    private final CompanyOrganizationService companyOrganizationService;
    private final CompanyFinanceService companyFinanceService;

    public GameController(
            GameService gameService,
            MainPageModelAssembler mainPageModelAssembler,
            InfoPageModelAssembler infoPageModelAssembler,
            PlayerCompanyService playerCompanyService,
            CompanyTutorialService companyTutorialService,
            CompanySettlementService companySettlementService,
            CompanyWorkforceService companyWorkforceService,
            CompanyInfrastructureService companyInfrastructureService,
            CompanyProductProjectService companyProductProjectService,
            CompanyComputeConstructionService companyComputeConstructionService,
            CompanyShortTermProjectService companyShortTermProjectService,
            CompanyCustomerContractService companyCustomerContractService,
            CompanyServiceIncidentService companyServiceIncidentService,
            CompanyNewsService companyNewsService,
            CompanyReportingService companyReportingService,
            CompanyDepartmentService companyDepartmentService,
            CompanyOrganizationService companyOrganizationService,
            CompanyFinanceService companyFinanceService
    ) {
        this.gameService = gameService;
        this.mainPageModelAssembler = mainPageModelAssembler;
        this.infoPageModelAssembler = infoPageModelAssembler;
        this.playerCompanyService = playerCompanyService;
        this.companyTutorialService = companyTutorialService;
        this.companySettlementService = companySettlementService;
        this.companyWorkforceService = companyWorkforceService;
        this.companyInfrastructureService = companyInfrastructureService;
        this.companyProductProjectService = companyProductProjectService;
        this.companyComputeConstructionService = companyComputeConstructionService;
        this.companyShortTermProjectService = companyShortTermProjectService;
        this.companyCustomerContractService = companyCustomerContractService;
        this.companyServiceIncidentService = companyServiceIncidentService;
        this.companyNewsService = companyNewsService;
        this.companyReportingService = companyReportingService;
        this.companyDepartmentService = companyDepartmentService;
        this.companyOrganizationService = companyOrganizationService;
        this.companyFinanceService = companyFinanceService;
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
        // 기업 화면은 개발 중 접근조건을 적용하지 않는다. 주식만 기존 해금조건을 유지한다.
        String viewMode = "company".equals(view)
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

    @PostMapping("/companies/projects/product")
    public String startCompanyProductProject(
            @RequestParam CompanyProductImprovementType type,
            @RequestParam(defaultValue = "BALANCED") CompanyDevelopmentDirection direction,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyProductProjectService.start(playerId, type, direction));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/projects/product/cancel")
    public String cancelCompanyProductProject(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyProductProjectService.cancel(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/projects/short-term")
    public String acceptCompanyShortTermProject(
            @RequestParam long projectId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyShortTermProjectService.accept(playerId, projectId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/projects/short-term/reject")
    public String rejectCompanyShortTermProject(
            @RequestParam long projectId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute(
                "notice", companyShortTermProjectService.reject(playerId, projectId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/projects/short-term/cancel")
    public String cancelCompanyShortTermProject(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyShortTermProjectService.cancel(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/projects/short-term/risk")
    public String resolveCompanyShortTermProjectRisk(
            @RequestParam long projectId,
            @RequestParam CompanyShortTermProjectRiskResolution resolution,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyShortTermProjectService.resolveRisk(playerId, projectId, resolution));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/contracts/customer")
    public String acceptCompanyCustomerContract(
            @RequestParam long contractId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyCustomerContractService.accept(playerId, contractId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/contracts/customer/reject")
    public String rejectCompanyCustomerContract(
            @RequestParam long contractId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute(
                "notice", companyCustomerContractService.rejectOffer(playerId, contractId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/budget-policy")
    public String changeCompanyBudgetPolicy(
            @RequestParam CompanyDevelopmentBudgetPolicy developmentPolicy,
            @RequestParam CompanyMarketingBudgetPolicy marketingPolicy,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companySettlementService.changeBudgetPolicies(
                playerId, developmentPolicy, marketingPolicy));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/contracts/customer/renew")
    public String renewCompanyCustomerContract(
            @RequestParam long contractId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyCustomerContractService.renew(playerId, contractId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/contracts/customer/decline-renewal")
    public String declineCompanyCustomerContractRenewal(
            @RequestParam long contractId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyCustomerContractService.declineRenewal(playerId, contractId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/incidents/resolve")
    public String resolveCompanyServiceIncident(
            @RequestParam long incidentId,
            @RequestParam CompanyServiceIncidentResolution resolution,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyServiceIncidentService.resolveMajor(playerId, incidentId, resolution));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/incidents/response")
    public String startCompanyServiceIncidentResponse(
            @RequestParam long incidentId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyServiceIncidentService.startCriticalResponse(playerId, incidentId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/news/{articleId}/read")
    @ResponseBody
    public Map<String, Boolean> readCompanyNews(
            @PathVariable long articleId,
            HttpSession session
    ) {
        Long playerId = currentPlayerId(session);
        return Map.of("read", playerId != null && companyNewsService.markRead(playerId, articleId));
    }

    @PostMapping("/companies/reports/{quarterSequence}/read")
    @ResponseBody
    public Map<String, Boolean> readCompanyReport(
            @PathVariable long quarterSequence,
            HttpSession session
    ) {
        Long playerId = currentPlayerId(session);
        return Map.of("read", playerId != null
                && companyReportingService.markRead(playerId, quarterSequence));
    }

    @PostMapping("/companies/infrastructure/compute-construction")
    public String startCompanyComputeConstruction(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "compute-construction", redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice", companyComputeConstructionService.startNext(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies")
    public String establishCompany(
            @RequestParam String companyName,
            @RequestParam String serviceName,
            @RequestParam long investment,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute(
                "notice",
                playerCompanyService.establish(playerId, companyName, serviceName, investment)
        );
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/tutorial/team")
    public String confirmCompanyFoundingTeam(
            @RequestParam(required = false) List<String> candidateKeys,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyTutorialService.confirmFoundingTeam(playerId, candidateKeys));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/tutorial/start")
    public String startCompanyCommercialization(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyTutorialService.startCommercialization(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/tutorial/launch")
    public String launchCompanyProduct(HttpSession session, RedirectAttributes redirectAttributes) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice", companyTutorialService.launch(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/funding")
    public String fundCompany(
            @RequestParam long amount,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "funding:" + amount, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice", companySettlementService.contributeAndResume(playerId, amount));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/finance/dividend")
    public String decideCompanyDividend(
            @RequestParam int rate,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "dividend:" + rate, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        var company = playerCompanyService.company(gameService.player(playerId)).orElseThrow();
        redirectAttributes.addFlashAttribute("notice", companyFinanceService.decideDividend(
                playerId, rate, companySettlementService.essentialMonthlyCost(company)));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/finance/investment")
    public String attractCompanyInvestment(
            @RequestParam int targetPercent,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "investment:" + targetPercent, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyFinanceService.attractInvestment(playerId, targetPercent));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/finance/bonds")
    public String issueCompanyBond(
            @RequestParam int valuationPercent,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "bond:" + valuationPercent, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyFinanceService.issueBond(playerId, valuationPercent));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/finance/bonds/{bondId}/repay")
    public String repayCompanyBond(
            @PathVariable long bondId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "bond-repay:" + bondId, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyFinanceService.repayOrdinaryBond(playerId, bondId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/headcount")
    public String changeCompanyHeadcount(
            @RequestParam CompanyDepartmentType departmentType,
            @RequestParam int approvedHeadcount,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.changeApprovedHeadcount(playerId, departmentType, approvedHeadcount));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/departments")
    public String establishCompanyDepartment(
            @RequestParam CompanyDepartmentType departmentType,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyDepartmentService.establish(playerId, departmentType));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/hire")
    public String hireCompanyWorkforce(
            @RequestParam CompanyDepartmentType departmentType,
            @RequestParam int count,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "general-hire:" + departmentType + ":" + count, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.requestGeneralHires(playerId, departmentType, count));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/organization/upgrade")
    public String upgradeCompanyOrganization(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyOrganizationService.startNextUpgrade(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/organization/automatic-hiring")
    public String toggleCompanyAutomaticHiring(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyOrganizationService.toggleAutomaticHiring(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/core-hire")
    public String hireCompanyCoreTalent(
            @RequestParam String candidateKey,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "core-hire:" + candidateKey, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.hireCoreTalent(playerId, candidateKey));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/team-leader")
    public String appointCompanyTeamLeader(
            @RequestParam long employeeId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.appointTeamLeader(playerId, employeeId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/training")
    public String trainCompanyCoreTalent(
            @RequestParam long employeeId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "training:" + employeeId, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.startTraining(playerId, employeeId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/workforce/retention")
    public String retainCompanyCoreTalent(
            @RequestParam long employeeId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyWorkforceService.offerRetentionAgreement(playerId, employeeId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/infrastructure/cloud-plan")
    public String changeCompanyCloudPlan(
            HttpSession session,
            @RequestParam CompanyCloudPlan plan,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyInfrastructureService.requestCloudPlan(playerId, plan));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/infrastructure/reserve-capacity")
    public String purchaseCompanyReserveCapacity(
            HttpSession session,
            @RequestParam int percentage,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "reserve-capacity:" + percentage, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice",
                companyInfrastructureService.purchaseReserveCapacity(playerId, percentage));
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

    /**
     * 브라우저의 연속 제출로 같은 비용 명령이 두 번 실행되는 것을 막는다.
     *
     * 상태가 유효한 반복 명령만 대상으로 하며, 서로 다른 입력이나 3초 뒤의 의도적인 재시도는 허용한다.
     */
    private boolean rejectDuplicateCompanyCommand(
            HttpSession session,
            String signature,
            RedirectAttributes redirectAttributes
    ) {
        long now = System.currentTimeMillis();
        String previousSignature = (String) session.getAttribute("lastCompanyCommandSignature");
        Object previousTimeValue = session.getAttribute("lastCompanyCommandTime");
        long previousTime = previousTimeValue instanceof Long value ? value : 0L;
        if (signature.equals(previousSignature) && now - previousTime < 3_000L) {
            redirectAttributes.addFlashAttribute("notice", "같은 기업 명령의 중복 제출을 차단함");
            return true;
        }
        session.setAttribute("lastCompanyCommandSignature", signature);
        session.setAttribute("lastCompanyCommandTime", now);
        return false;
    }
}

