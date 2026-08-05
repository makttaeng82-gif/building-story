package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyShortTermProject;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyShortTermProjectStatus;
import com.game.buildingstory.domain.CompanyShortTermProjectRisk;
import com.game.buildingstory.domain.CompanyShortTermProjectRiskResolution;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyShortTermProjectRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 단기 사업 후보 생성, 수락, 월별 수행과 완료 후 수익 일정을 처리한다.
 *
 * <p>정산 전에 이번 달 수익과 비용을 조회하고, 정산이 정상적으로 끝난 뒤에만 작업 진도와
 * 수익 개월 수를 이동시킨다. 법인현금 고갈 상태에서 프로젝트만 진행되는 오류를 막기 위한 경계다.</p>
 */
@Service
public class CompanyShortTermProjectService {
    private static final long MINIMUM_BASE_REVENUE = 4_800_000_000L;
    private static final int SMALL_PROJECT_WORK = 120;
    private static final int MAXIMUM_MONTHLY_ASSIGNMENT = 45;
    private static final List<ProjectCatalogItem> CATALOG = List.of(
            new ProjectCatalogItem("industry-analysis", "산업별 AI 분석 패키지"),
            new ProjectCatalogItem("limited-module", "기간 한정 AI 자동화 모듈"),
            new ProjectCatalogItem("public-pilot", "공공 AI 실증사업"),
            new ProjectCatalogItem("data-processing", "단기 데이터 가공 서비스"),
            new ProjectCatalogItem("enterprise-training", "기업 교육용 AI 솔루션")
    );

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyShortTermProjectRepository projectRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyMonthlySettlementRepository settlementRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyExternalEventService externalEventService;

    public CompanyShortTermProjectService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyShortTermProjectRepository projectRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyMonthlySettlementRepository settlementRepository,
            CompanyWorkforceService workforceService,
            CompanyCashLedgerService cashLedgerService,
            CompanyExternalEventService externalEventService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.departmentRepository = departmentRepository;
        this.settlementRepository = settlementRepository;
        this.workforceService = workforceService;
        this.cashLedgerService = cashLedgerService;
        this.externalEventService = externalEventService;
    }

    @Transactional
    public String accept(long playerId, long projectId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational() || company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 단기 사업을 수락할 수 있음";
        }
        CompanyShortTermProject project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getCompany().getId().equals(company.getId())
                || project.getStatus() != CompanyShortTermProjectStatus.OFFERED) {
            return "수락 가능한 단기 사업이 아님";
        }
        if (activeBusiness(company).isPresent()) {
            return "이미 진행 중이거나 수익 정산 중인 단기 사업이 있음";
        }
        if (company.getActiveMajorWorkCount() >= workforceService.companyMajorWorkSlotLimit(company)) {
            return "회사 주요 업무 슬롯이 부족함";
        }

        int assignedWork = availableMonthlyWork(company);
        if (assignedWork <= 0) {
            return "영업마케팅팀 업무 여유가 없어 단기 사업을 시작할 수 없음";
        }
        workforceService.reserveMajorWork(company, CompanyDepartmentType.SALES_MARKETING, assignedWork);
        project.accept(assignedWork);
        return project.getName() + " 수락 · 예상 "
                + (int) Math.ceil(project.getTotalWork() / (double) assignedWork) + "개월";
    }

    @Transactional
    public String reject(long playerId, long projectId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyShortTermProject project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getCompany().getId().equals(company.getId())
                || project.getStatus() != CompanyShortTermProjectStatus.OFFERED) {
            return "거절 가능한 단기 사업 제안이 아님";
        }
        project.reject();
        return project.getName() + " 제안 거절";
    }

    @Transactional
    public String cancel(long playerId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        Optional<CompanyShortTermProject> active = activeProject(company);
        if (active.isEmpty()) {
            return "취소할 단기 사업이 없음";
        }
        CompanyShortTermProject project = active.get();
        project.cancel();
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.SALES_MARKETING, project.getMonthlyAssignedWork());
        return project.getName() + " 취소 · 기지급 비용은 반환되지 않음";
    }

    @Transactional
    public boolean failActiveBusiness(PlayerCompany company) {
        Optional<CompanyShortTermProject> project = activeProject(company)
                .or(() -> earningProject(company));
        if (project.isEmpty()) {
            return false;
        }
        if (project.get().getStatus() == CompanyShortTermProjectStatus.ACTIVE) {
            workforceService.releaseMajorWork(
                    company, CompanyDepartmentType.SALES_MARKETING,
                    project.get().getMonthlyAssignedWork());
        }
        project.get().fail();
        return true;
    }

    @Transactional(readOnly = true)
    public MonthlyFinancials monthlyFinancials(PlayerCompany company) {
        Optional<CompanyShortTermProject> active = activeProject(company);
        if (active.isPresent()) {
            return new MonthlyFinancials(0, active.get().currentMonthlyCost());
        }
        Optional<CompanyShortTermProject> earning = earningProject(company);
        return earning.map(project -> new MonthlyFinancials(
                        project.isRiskDecisionRequired() ? 0 : project.currentRevenue(),
                        project.currentMonthlyCost()))
                .orElseGet(() -> new MonthlyFinancials(0, 0));
    }

    @Transactional
    public ProjectMonthResult processSuccessfulMonth(PlayerCompany company) {
        String notice = "";
        Optional<CompanyShortTermProject> active = activeProject(company);
        if (active.isPresent()) {
            CompanyShortTermProject project = active.get();
            int completedWork = workforceService.effectiveMajorWork(
                    company, CompanyDepartmentType.SALES_MARKETING, project.getMonthlyAssignedWork());
            boolean completed = project.advanceWorkMonth(projectQuality(company), completedWork);
            if (completed) {
                CompanyShortTermProjectRisk risk = determinePostCompletionRisk(company, project);
                project.applyPostCompletionRisk(risk);
                workforceService.releaseMajorWork(
                        company, CompanyDepartmentType.SALES_MARKETING, project.getMonthlyAssignedWork());
                workforceService.grantDepartmentExperience(
                        company, CompanyDepartmentType.SALES_MARKETING, 3.0);
                notice = project.getName() + " 완료 · " + risk.getDisplayName();
            } else {
                notice = project.getName() + " " + project.progressPercent() + "%";
            }
        } else {
            Optional<CompanyShortTermProject> earning = earningProject(company);
            if (earning.isPresent()) {
                CompanyShortTermProject project = earning.get();
                if (project.isRiskDecisionRequired()) {
                    expireOffers(company);
                    return new ProjectMonthResult(true, false,
                            project.getName() + " 환불 대응 결정 대기");
                }
                long revenue = project.currentRevenue();
                project.clearPendingRiskCost();
                boolean completed = project.advanceRevenueMonth();
                notice = project.getName() + " 사업수익 " + revenue + "원"
                        + (completed ? " · 수익 종료" : "");
            }
        }

        expireOffers(company);
        boolean generated = generateCandidateIfDue(company);
        return new ProjectMonthResult(!notice.isBlank(), generated, notice);
    }

    @Transactional
    public String resolveRisk(
            long playerId,
            long projectId,
            CompanyShortTermProjectRiskResolution resolution
    ) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 사후 대응을 결정할 수 있음";
        }
        CompanyShortTermProject project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getCompany().getId().equals(company.getId()) || !project.isRiskDecisionRequired()) {
            return "대응을 선택할 수 있는 단기 사업이 아님";
        }
        if (resolution == CompanyShortTermProjectRiskResolution.EXTRA_SUPPORT
                && !cashLedgerService.withdraw(
                        company,
                        "short-project-support:" + projectId,
                        CompanyCashFlowType.OPERATING,
                        "단기 사업 추가 지원비",
                        project.extraSupportCost())) {
            return "추가 지원 비용이 부족함";
        }
        project.resolveRisk(resolution);
        return resolution.getDisplayName() + " 결정 완료";
    }

    @Transactional(readOnly = true)
    public List<CompanyShortTermProject> projects(PlayerCompany company) {
        return projectRepository.findByCompanyOrderByIdDesc(company);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyShortTermProject> activeProject(PlayerCompany company) {
        return projectRepository.findFirstByCompanyAndStatusOrderByIdDesc(
                company, CompanyShortTermProjectStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyShortTermProject> earningProject(PlayerCompany company) {
        return projectRepository.findFirstByCompanyAndStatusOrderByIdDesc(
                company, CompanyShortTermProjectStatus.EARNING);
    }

    @Transactional(readOnly = true)
    public List<CompanyShortTermProject> offeredProjects(PlayerCompany company) {
        return projectRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(project -> project.getStatus() == CompanyShortTermProjectStatus.OFFERED)
                .toList();
    }

    @Transactional(readOnly = true)
    public int availableMonthlyWork(PlayerCompany company) {
        CompanyDepartment sales = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.SALES_MARKETING)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, sales);
        return Math.min(MAXIMUM_MONTHLY_ASSIGNMENT,
                Math.max(0, (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload()));
    }

    @Transactional(readOnly = true)
    public boolean canStartProject(PlayerCompany company) {
        return !company.isOperationsSuspended()
                && activeBusiness(company).isEmpty()
                && company.getActiveMajorWorkCount() < workforceService.companyMajorWorkSlotLimit(company)
                && availableMonthlyWork(company) > 0;
    }

    private Optional<CompanyShortTermProject> activeBusiness(PlayerCompany company) {
        return projectRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(project -> project.getStatus() == CompanyShortTermProjectStatus.ACTIVE
                        || project.getStatus() == CompanyShortTermProjectStatus.EARNING)
                .findFirst();
    }

    private void expireOffers(PlayerCompany company) {
        projectRepository.findByCompanyOrderByIdDesc(company)
                .forEach(project -> project.expire(company.getMarketMonthsProcessed()));
    }

    private boolean generateCandidateIfDue(PlayerCompany company) {
        List<CompanyShortTermProject> history = projectRepository.findByCompanyOrderByIdDesc(company);
        if (activeBusiness(company).isPresent()
                || history.stream().anyMatch(project -> project.getStatus() == CompanyShortTermProjectStatus.OFFERED)) {
            return false;
        }
        boolean guaranteedFirstOffer = history.isEmpty() && company.getMarketMonthsProcessed() >= 1;
        boolean guaranteedPublicOffer = externalEventService.guaranteesPublicProject(company);
        int roll = Math.floorMod((int) (company.getId() * 31 + company.getMarketMonthsProcessed() * 17), 100);
        if (!guaranteedFirstOffer && !guaranteedPublicOffer && roll >= 35) {
            return false;
        }

        ProjectCatalogItem item = guaranteedPublicOffer
                ? CATALOG.stream()
                        .filter(candidate -> candidate.key().equals("public-pilot"))
                        .findFirst()
                        .orElseThrow()
                : nextAvailableCatalog(company, history);
        if (item == null) {
            return false;
        }
        long baseRevenue = baseSubscriptionRevenue(company);
        long firstMonthRevenue = baseRevenue / 10;
        long expectedRevenue = firstMonthRevenue * 210 / 100;
        long totalCost = expectedRevenue * 45 / 100;
        projectRepository.save(new CompanyShortTermProject(
                company,
                item.key(),
                item.name(),
                company.getMarketMonthsProcessed(),
                SMALL_PROJECT_WORK,
                totalCost,
                firstMonthRevenue
        ));
        return true;
    }

    private ProjectCatalogItem nextAvailableCatalog(
            PlayerCompany company,
            List<CompanyShortTermProject> history
    ) {
        int start = Math.floorMod(company.getMarketMonthsProcessed(), CATALOG.size());
        for (int offset = 0; offset < CATALOG.size(); offset++) {
            ProjectCatalogItem item = CATALOG.get((start + offset) % CATALOG.size());
            boolean coolingDown = history.stream()
                    .filter(project -> project.getCatalogKey().equals(item.key()))
                    .anyMatch(project -> company.getMarketMonthsProcessed() - project.getOfferedMarketMonth() < 6);
            if (!coolingDown) {
                return item;
            }
        }
        return null;
    }

    private long baseSubscriptionRevenue(PlayerCompany company) {
        List<com.game.buildingstory.domain.CompanyMonthlySettlement> settlements =
                settlementRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company);
        if (settlements.isEmpty()) {
            return Math.max(MINIMUM_BASE_REVENUE, company.getMonthlyRecurringRevenue());
        }
        long average = Math.round(settlements.stream()
                .mapToLong(com.game.buildingstory.domain.CompanyMonthlySettlement::getSubscriptionRevenue)
                .average()
                .orElse(MINIMUM_BASE_REVENUE));
        return Math.max(MINIMUM_BASE_REVENUE, average);
    }

    private int projectQuality(PlayerCompany company) {
        int development = workforceService.departmentExpertise(company, CompanyDepartmentType.AI_DEVELOPMENT);
        int sales = workforceService.departmentExpertise(company, CompanyDepartmentType.SALES_MARKETING);
        return Math.max(50, Math.min(100, (int) Math.round(development * 0.6 + sales * 0.4)));
    }

    private CompanyShortTermProjectRisk determinePostCompletionRisk(
            PlayerCompany company,
            CompanyShortTermProject project
    ) {
        int quality = project.getCompletionQuality() == null ? 50 : project.getCompletionQuality();
        int chancePercent = Math.max(8, Math.min(35, 10 + Math.max(0, 75 - quality)));
        int occurrenceRoll = deterministicRoll(company, project, 19);
        if (occurrenceRoll >= chancePercent * 100) {
            return CompanyShortTermProjectRisk.NONE;
        }
        int typeRoll = deterministicRoll(company, project, 47);
        if (typeRoll < 4_500) {
            return CompanyShortTermProjectRisk.AFTER_SERVICE;
        }
        if (typeRoll < 8_000) {
            return CompanyShortTermProjectRisk.REFUND_REQUEST;
        }
        return CompanyShortTermProjectRisk.TREND_FADE;
    }

    private int deterministicRoll(
            PlayerCompany company,
            CompanyShortTermProject project,
            int salt
    ) {
        long value = company.getId() * 1_000_003L
                + project.getId() * 97_409L
                + company.getMarketMonthsProcessed() * 65_537L
                + salt * 31_337L;
        value ^= value >>> 29;
        value *= 0x9E3779B97F4A7C15L;
        return Math.floorMod(value, 10_000);
    }

    private record ProjectCatalogItem(String key, String name) {
    }

    public record MonthlyFinancials(long revenue, long cost) {
    }

    public record ProjectMonthResult(boolean active, boolean candidateGenerated, String notice) {
    }
}
