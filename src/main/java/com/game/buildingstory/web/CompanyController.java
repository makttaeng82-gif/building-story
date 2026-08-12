package com.game.buildingstory.web;

import com.game.buildingstory.domain.CompanyCloudPlan;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyDevelopmentBudgetPolicy;
import com.game.buildingstory.domain.CompanyDevelopmentDirection;
import com.game.buildingstory.domain.CompanyMarketingBudgetPolicy;
import com.game.buildingstory.domain.CompanyProductImprovementType;
import com.game.buildingstory.domain.CompanyServiceIncidentResolution;
import com.game.buildingstory.domain.CompanyShortTermProjectRiskResolution;
import com.game.buildingstory.service.CompanyComputeConstructionService;
import com.game.buildingstory.service.CompanyCustomerContractService;
import com.game.buildingstory.service.CompanyDepartmentService;
import com.game.buildingstory.service.CompanyFinanceService;
import com.game.buildingstory.service.CompanyInfrastructureService;
import com.game.buildingstory.service.CompanyIpoService;
import com.game.buildingstory.service.CompanyNewsService;
import com.game.buildingstory.service.CompanyOrganizationService;
import com.game.buildingstory.service.CompanyProductProjectService;
import com.game.buildingstory.service.CompanyReportingService;
import com.game.buildingstory.service.CompanyServiceIncidentService;
import com.game.buildingstory.service.CompanySettlementService;
import com.game.buildingstory.service.CompanyShortTermProjectService;
import com.game.buildingstory.service.CompanyTutorialService;
import com.game.buildingstory.service.CompanyWorkforceService;
import com.game.buildingstory.service.GameService;
import com.game.buildingstory.service.PlayerCompanyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class CompanyController {
    private final GameService gameService;
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
    private final CompanyIpoService companyIpoService;

    public CompanyController(
            GameService gameService,
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
            CompanyFinanceService companyFinanceService,
            CompanyIpoService companyIpoService
    ) {
        this.gameService = gameService;
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
        this.companyIpoService = companyIpoService;
    }

    @PostMapping("/companies/ipo")
    public String applyCompanyIpo(
            @RequestParam int offerPercent,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(
                session, "ipo:" + offerPercent, redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice", companyIpoService.apply(playerId, offerPercent));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/ipo/cancel")
    public String cancelCompanyIpo(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(session, "ipo:cancel", redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice", companyIpoService.cancel(playerId));
        return "redirect:/main?view=company";
    }

    @PostMapping("/companies/ipo/confirm")
    public String confirmCompanyIpo(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Long playerId = currentPlayerId(session);
        if (playerId == null) {
            return "redirect:/login";
        }
        if (rejectDuplicateCompanyCommand(session, "ipo:confirm", redirectAttributes)) {
            return "redirect:/main?view=company";
        }
        redirectAttributes.addFlashAttribute("notice", companyIpoService.confirmListing(playerId));
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
