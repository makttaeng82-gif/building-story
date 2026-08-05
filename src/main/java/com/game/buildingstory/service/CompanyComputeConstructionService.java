package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyComputeConstruction;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.CompanyComputeTier;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyComputeConstructionRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 자체 연산망의 순차 착공, 월별 공사와 완공 대금 지급을 처리한다. */
@Service
public class CompanyComputeConstructionService {
    public static final int SERVICE_OPERATIONS_WORKLOAD = 30;
    private static final List<CompanyComputeConstructionStatus> IN_PROGRESS_STATUSES =
            List.of(CompanyComputeConstructionStatus.ACTIVE, CompanyComputeConstructionStatus.PAYMENT_DUE);

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyComputeConstructionRepository constructionRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyExternalEventService externalEventService;

    public CompanyComputeConstructionService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyComputeConstructionRepository constructionRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService workforceService,
            CompanyCashLedgerService cashLedgerService,
            CompanyExternalEventService externalEventService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.constructionRepository = constructionRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
        this.cashLedgerService = cashLedgerService;
        this.externalEventService = externalEventService;
    }

    @Transactional
    public String startNext(long playerId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational()) {
            return "제품 출시 후 자체 연산망을 건설할 수 있음";
        }
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 공사를 시작할 수 없음";
        }
        ConstructionOpportunity opportunity = opportunity(company);
        if (!opportunity.available()) {
            return opportunity.reason();
        }

        ConstructionStep step = opportunity.step();
        workforceService.reserveMajorWork(
                company,
                CompanyDepartmentType.SERVICE_OPERATIONS,
                SERVICE_OPERATIONS_WORKLOAD
        );
        if (!cashLedgerService.withdraw(
                company,
                "compute:start:" + step.tier() + ":" + step.phase(),
                CompanyCashFlowType.INVESTING,
                step.displayName() + " 착공금",
                step.upfrontPayment())) {
            workforceService.releaseMajorWork(
                    company,
                    CompanyDepartmentType.SERVICE_OPERATIONS,
                    SERVICE_OPERATIONS_WORKLOAD
            );
            return "착공금이 부족함";
        }
        constructionRepository.save(new CompanyComputeConstruction(
                company,
                  step.tier(),
                  step.phase(),
                  SERVICE_OPERATIONS_WORKLOAD,
                  player.getElapsedDays(),
                  step.months(),
                  step.totalCost()
        ));
        return step.displayName() + " 착공 · 착공금 지급 · " + step.months() + "개월 예정";
    }

    @Transactional
    public ConstructionMonthResult processMonth(PlayerCompany company) {
        Optional<CompanyComputeConstruction> current = activeConstruction(company);
        if (current.isEmpty()) {
            return new ConstructionMonthResult(false, false, "");
        }
        CompanyComputeConstruction construction = current.get();
        boolean paymentDue = construction.advanceMonth();
        if (!paymentDue) {
            return new ConstructionMonthResult(true, false,
                    construction.displayName() + " " + construction.progressPercent() + "%");
        }
        if (!cashLedgerService.withdraw(
                company,
                "compute:complete:" + construction.getId(),
                CompanyCashFlowType.INVESTING,
                construction.displayName() + " 완공금",
                construction.getCompletionPayment())) {
            return new ConstructionMonthResult(true, false,
                    construction.displayName() + " 완공 대금 부족");
        }
        construction.complete(company.getPlayer().getElapsedDays());
        workforceService.releaseMajorWork(
                company,
                CompanyDepartmentType.SERVICE_OPERATIONS,
                construction.getServiceOperationsWorkload()
        );
        return new ConstructionMonthResult(true, true, construction.displayName() + " 완공");
    }

    @Transactional
    public boolean failActiveConstruction(PlayerCompany company) {
        Optional<CompanyComputeConstruction> active = activeConstruction(company);
        if (active.isEmpty()) {
            return false;
        }
        CompanyComputeConstruction construction = active.get();
        construction.fail(company.getPlayer().getElapsedDays());
        workforceService.releaseMajorWork(
                company,
                CompanyDepartmentType.SERVICE_OPERATIONS,
                construction.getServiceOperationsWorkload()
        );
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<CompanyComputeConstruction> activeConstruction(PlayerCompany company) {
        return constructionRepository.findFirstByCompanyAndStatusInOrderByIdDesc(company, IN_PROGRESS_STATUSES);
    }

    @Transactional(readOnly = true)
    public NetworkState networkState(PlayerCompany company) {
        return constructionRepository
                .findFirstByCompanyAndStatusOrderByIdDesc(company, CompanyComputeConstructionStatus.COMPLETED)
                .map(construction -> new NetworkState(
                        construction.getTier().getDisplayName() + " " + construction.getTier().phaseName(construction.getPhase()),
                        construction.getTier().capacity(construction.getPhase()),
                        construction.getTier().monthlyCost(construction.getPhase())
                ))
                .orElse(new NetworkState("미보유", 0, 0));
    }

    @Transactional(readOnly = true)
    public ConstructionOpportunity opportunity(PlayerCompany company) {
        if (activeConstruction(company).isPresent()) {
            return new ConstructionOpportunity(nextStep(company), false, "이미 진행 중인 연산망 공사가 있음");
        }
        ConstructionStep step = nextStep(company);
        if (step == null) {
            return new ConstructionOpportunity(null, false, "글로벌 연산망 최종 증설 완료");
        }
        if (company.getActiveMajorWorkCount() >= workforceService.companyMajorWorkSlotLimit(company)) {
            return new ConstructionOpportunity(step, false, "회사의 동시 주요 업무 슬롯이 부족함");
        }
        var operations = departmentRepository
                .findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS)
                .orElseThrow();
        var load = workforceService.departmentLoad(company, operations);
        int maximumAdditional = Math.max(0, (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload());
        if (maximumAdditional < SERVICE_OPERATIONS_WORKLOAD) {
            return new ConstructionOpportunity(step, false, "서비스운영팀 업무 여유가 부족함");
        }
        if (company.getCorporateCash() < step.upfrontPayment()) {
            return new ConstructionOpportunity(step, false, "착공금이 부족함");
        }
        return new ConstructionOpportunity(step, true, "착공 가능");
    }

    private ConstructionStep nextStep(PlayerCompany company) {
        Optional<CompanyComputeConstruction> latest = constructionRepository
                .findFirstByCompanyAndStatusOrderByIdDesc(company, CompanyComputeConstructionStatus.COMPLETED);
        CompanyComputeTier tier;
        int phase;
        if (latest.isEmpty()) {
            tier = CompanyComputeTier.SMALL;
            phase = 0;
        } else if (latest.get().getPhase() < 3) {
            tier = latest.get().getTier();
            phase = latest.get().getPhase() + 1;
        } else {
            tier = latest.get().getTier().nextTier();
            phase = 0;
        }
        if (tier == null) {
            return null;
        }
        long totalCost = Math.round(tier.constructionCost(phase)
                * externalEventService.constructionCostMultiplier(company));
        int constructionMonths = Math.max(1, (int) Math.ceil(tier.constructionMonths(phase)
                * externalEventService.constructionWorkMultiplier(company)));
        return new ConstructionStep(
                tier,
                phase,
                tier.getDisplayName() + " 연산망 " + tier.phaseName(phase),
                tier.capacity(phase),
                tier.monthlyCost(phase),
                totalCost,
                totalCost / 2,
                constructionMonths
        );
    }

    public record NetworkState(String name, long capacity, long monthlyCost) {
    }

    public record ConstructionStep(CompanyComputeTier tier, int phase, String displayName, long capacity,
                                   long monthlyCost, long totalCost, long upfrontPayment, int months) {
    }

    public record ConstructionOpportunity(ConstructionStep step, boolean available, String reason) {
    }

    public record ConstructionMonthResult(boolean active, boolean completed, String notice) {
    }
}
