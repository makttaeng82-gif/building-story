package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyDevelopmentDirection;
import com.game.buildingstory.domain.CompanyProductImprovementType;
import com.game.buildingstory.domain.CompanyProductProject;
import com.game.buildingstory.domain.CompanyProductProjectStatus;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyProductProjectRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 핵심제품 개선의 시작, 월별 작업 진행과 완료 효과를 담당한다. */
@Service
public class CompanyProductProjectService {
    private static final int MAXIMUM_MONTHLY_ASSIGNMENT = 60;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyProductProjectRepository projectRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyExternalEventService externalEventService;
    private final CompanySecretaryService secretaryService;

    public CompanyProductProjectService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyProductProjectRepository projectRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService workforceService,
            CompanyExternalEventService externalEventService,
            CompanySecretaryService secretaryService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.projectRepository = projectRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
        this.externalEventService = externalEventService;
        this.secretaryService = secretaryService;
    }

    @Transactional
    public String start(long playerId, CompanyProductImprovementType type) {
        return start(playerId, type, CompanyDevelopmentDirection.BALANCED);
    }

    @Transactional
    public String start(long playerId, CompanyProductImprovementType type, CompanyDevelopmentDirection direction) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational()) {
            return "제품 출시 후 개선 프로젝트를 시작할 수 있음";
        }
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 프로젝트를 시작할 수 없음";
        }
        if (activeProject(company).isPresent()) {
            return "이미 진행 중인 제품 개선 프로젝트가 있음";
        }
        if (type == CompanyProductImprovementType.NEXT_GENERATION_MODEL) {
            var eligibility = nextGenerationEligibility(company);
            if (!eligibility.available()) {
                return eligibility.reason();
            }
        }
        CompanyDevelopmentDirection requestedDirection = direction == null
                ? CompanyDevelopmentDirection.BALANCED
                : direction;
        CompanyDevelopmentDirection appliedDirection = type.supportsDevelopmentDirection()
                ? requestedDirection
                : CompanyDevelopmentDirection.BALANCED;

        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, development);
        if (load.availableSlots() <= 0) {
            return "AI개발팀 주요 업무 슬롯이 부족함";
        }
        int maximumAdditionalWorkload = Math.max(0,
                (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload());
        int assignedWorkload = Math.min(MAXIMUM_MONTHLY_ASSIGNMENT, maximumAdditionalWorkload);
        if (assignedWorkload <= 0) {
            return "AI개발팀 업무 여유가 없어 프로젝트를 시작할 수 없음";
        }
        if (!workforceService.canReserveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, assignedWorkload)) {
            return "회사의 동시 주요 업무 슬롯이 부족함";
        }

        workforceService.reserveMajorWork(company, CompanyDepartmentType.AI_DEVELOPMENT, assignedWorkload);
        int totalWork = Math.max(1, (int) Math.round(
                totalWork(type, company.getPrototypeBenchmark())
                        * externalEventService.productWorkMultiplier(company)
                        * secretaryService.developmentWorkMultiplier(company)));
        projectRepository.save(new CompanyProductProject(
                company, type, appliedDirection, totalWork, assignedWorkload, player.getElapsedDays()));
        int effectiveMonthlyWork = workforceService.effectiveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, assignedWorkload);
        int expectedMonths = (int) Math.ceil(totalWork / (double) Math.max(1, effectiveMonthlyWork));
        String directionText = type.supportsDevelopmentDirection()
                ? " · " + appliedDirection.getDisplayName()
                : "";
        return type.getDisplayName() + directionText + " 시작 · 예상 " + expectedMonths + "개월";
    }

    @Transactional
    public ProjectMonthResult processMonth(PlayerCompany company) {
        Optional<CompanyProductProject> active = activeProject(company);
        if (active.isEmpty()) {
            return new ProjectMonthResult(false, false, "");
        }
        CompanyProductProject project = active.get();
        int departmentWork = workforceService.effectiveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, project.getMonthlyAssignedWork());
        if (departmentWork == 0) {
            return new ProjectMonthResult(true, false,
                    project.getImprovementType().getDisplayName() + " · AI개발팀 처리 여력 부족");
        }
        int effectiveWork = Math.max(1, (int) Math.round(
                departmentWork * company.getDevelopmentBudgetPolicy().effectMultiplier()));
        boolean completed = project.advanceMonth(effectiveWork);
        if (!completed) {
            return new ProjectMonthResult(true, false,
                    project.getImprovementType().getDisplayName() + " " + project.progressPercent() + "%");
        }

        int quality = projectQuality(company);
        company.applyProductImprovement(
                project.getImprovementType(), project.getDevelopmentDirection(), quality);
        company.adjustBrandScore(
                project.getImprovementType() == CompanyProductImprovementType.NEXT_GENERATION_MODEL
                        ? 2.0
                        : 0.5
        );
        project.complete(company.getPlayer().getElapsedDays(), quality);
        workforceService.releaseMajorWork(
                company,
                CompanyDepartmentType.AI_DEVELOPMENT,
                project.getMonthlyAssignedWork()
        );
        workforceService.grantDepartmentExperience(
                company,
                CompanyDepartmentType.AI_DEVELOPMENT,
                project.getImprovementType() == CompanyProductImprovementType.NEXT_GENERATION_MODEL ? 6.0 : 3.0
        );
        return new ProjectMonthResult(true, true,
                project.getImprovementType().getDisplayName() + " 완료 · 품질 " + quality);
    }

    @Transactional
    public String cancel(long playerId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        Optional<CompanyProductProject> active = activeProject(company);
        if (active.isEmpty()) {
            return "취소할 제품 프로젝트가 없음";
        }
        CompanyProductProject project = active.get();
        project.cancel(player.getElapsedDays());
        releaseWork(company, project);
        return project.getImprovementType().getDisplayName() + " 취소 · 진행 효과 없음";
    }

    @Transactional
    public boolean failActiveProject(PlayerCompany company) {
        Optional<CompanyProductProject> active = activeProject(company);
        if (active.isEmpty()) {
            return false;
        }
        CompanyProductProject project = active.get();
        project.fail(company.getPlayer().getElapsedDays());
        releaseWork(company, project);
        return true;
    }

    private void releaseWork(PlayerCompany company, CompanyProductProject project) {
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, project.getMonthlyAssignedWork());
    }

    @Transactional(readOnly = true)
    public Optional<CompanyProductProject> activeProject(PlayerCompany company) {
        return projectRepository.findFirstByCompanyAndStatusOrderByIdDesc(company, CompanyProductProjectStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<CompanyProductProject> projects(PlayerCompany company) {
        return projectRepository.findByCompanyOrderByIdDesc(company);
    }

    @Transactional(readOnly = true)
    public int availableMonthlyWork(PlayerCompany company) {
        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, development);
        return Math.min(MAXIMUM_MONTHLY_ASSIGNMENT,
                Math.max(0, (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload()));
    }

    @Transactional(readOnly = true)
    public ProjectEligibility eligibility(PlayerCompany company, CompanyProductImprovementType type) {
        if (type != CompanyProductImprovementType.NEXT_GENERATION_MODEL) {
            return new ProjectEligibility(true, "착수 가능");
        }
        return nextGenerationEligibility(company);
    }

    public int totalWork(CompanyProductImprovementType type, int benchmark) {
        double multiplier = benchmark >= 1_800 ? 4.5
                : benchmark >= 1_100 ? 3.0
                : benchmark >= 600 ? 2.0
                : benchmark >= 300 ? 1.4
                : 1.0;
        return (int) Math.round(type.getBaseWork() * multiplier);
    }

    private ProjectEligibility nextGenerationEligibility(PlayerCompany company) {
        if (company.getPrototypeBenchmark() < 300) {
            return new ProjectEligibility(false, "벤치마크 300점 필요");
        }
        if (modelRefinementsSinceLastGeneration(company) < 2) {
            return new ProjectEligibility(false, "최근 차세대 모델 이후 모델 고도화 2회 필요");
        }
        if (company.getProductStability() < 70) {
            return new ProjectEligibility(false, "안정성 70 이상 필요");
        }
        if (company.getTechnicalDebt() > 40) {
            return new ProjectEligibility(false, "기술부채 40 이하 필요");
        }
        return new ProjectEligibility(true, "차세대 모델 착수 가능");
    }

    private int modelRefinementsSinceLastGeneration(PlayerCompany company) {
        int completedRefinements = 0;
        for (CompanyProductProject project : projectRepository.findByCompanyOrderByIdDesc(company)) {
            if (project.getStatus() != CompanyProductProjectStatus.COMPLETED) {
                continue;
            }
            if (project.getImprovementType() == CompanyProductImprovementType.NEXT_GENERATION_MODEL) {
                break;
            }
            if (project.getImprovementType() == CompanyProductImprovementType.MODEL_REFINEMENT) {
                completedRefinements++;
            }
        }
        return completedRefinements;
    }

    private int projectQuality(PlayerCompany company) {
        CompanyDepartment development = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.AI_DEVELOPMENT)
                .orElseThrow();
        int expertise = workforceService.departmentExpertise(company, CompanyDepartmentType.AI_DEVELOPMENT);
        return Math.max(50, Math.min(100,
                (int) Math.round(expertise * 0.65 + development.getAverageGeneralSkill() * 0.35)));
    }

    public record ProjectMonthResult(boolean active, boolean completed, String notice) {
    }

    public record ProjectEligibility(boolean available, String reason) {
    }
}
