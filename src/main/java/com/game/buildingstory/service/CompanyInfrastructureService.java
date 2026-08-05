package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCloudPlan;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 외부 클라우드 계약, 제품 처리수요와 서비스운영팀 처리한도를 계산한다. */
@Service
public class CompanyInfrastructureService {
    private static final long USERS_PER_OPERATIONS_CAPACITY = 5_000L;
    private static final int RESERVE_DURATION_DAYS = 30;
    private static final List<Integer> RESERVE_PERCENTAGES = List.of(10, 25, 50);

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanyComputeConstructionService constructionService;
    private final CompanyCustomerContractService customerContractService;
    private final CompanyServiceIncidentService incidentService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyExternalEventService externalEventService;

    public CompanyInfrastructureService(PlayerRepository playerRepository,
                                        PlayerCompanyRepository companyRepository,
                                        CompanyDepartmentRepository departmentRepository,
                                        CompanyWorkforceService workforceService,
                                          CompanyComputeConstructionService constructionService,
                                          CompanyCustomerContractService customerContractService,
                                          CompanyServiceIncidentService incidentService,
                                          CompanyCashLedgerService cashLedgerService,
                                          CompanyExternalEventService externalEventService) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
        this.constructionService = constructionService;
        this.customerContractService = customerContractService;
        this.incidentService = incidentService;
        this.cashLedgerService = cashLedgerService;
        this.externalEventService = externalEventService;
    }

    @Transactional
    public String requestCloudPlan(long playerId, CompanyCloudPlan plan) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 계약을 변경할 수 없음";
        }
        try {
            company.requestCloudPlan(plan);
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
        return plan.getDisplayName() + " 클라우드 변경 예약 · 다음 월 정산부터 적용";
    }

    @Transactional
    public String purchaseReserveCapacity(long playerId, int percentage) {
        if (!RESERVE_PERCENTAGES.contains(percentage)) {
            return "예비용량은 10%·25%·50% 중에서 선택해야 함";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (!company.getTutorialStage().isOperational()) {
            return "제품 출시 후 예비용량을 구매할 수 있음";
        }
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 예비용량을 구매할 수 없음";
        }
        if (company.getActiveReserveComputeCapacity(player.getElapsedDays()) > 0) {
            return "활성 예비용량이 있어 추가 구매할 수 없음";
        }
        long permanentCapacity = permanentCapacity(company);
        long reserveCapacity = Math.max(1L, Math.multiplyExact(permanentCapacity, percentage) / 100L);
        long cost = reserveCost(reserveCapacity);
        if (!cashLedgerService.withdraw(
                company,
                "reserve-compute:" + player.getElapsedDays(),
                CompanyCashFlowType.OPERATING,
                "예비 연산용량 구매",
                cost)) {
            return "예비용량 구매금액이 부족함";
        }
        company.activateReserveComputeCapacity(
                reserveCapacity,
                cost,
                Math.addExact(player.getElapsedDays(), RESERVE_DURATION_DAYS)
        );
        return "예비용량 " + percentage + "% 구매 · 30일간 " + String.format("%,d", reserveCapacity) + " 추가";
    }

    @Transactional(readOnly = true)
    public InfrastructureSnapshot snapshot(PlayerCompany company) {
        return snapshot(company, company.getPlayer().getElapsedDays());
    }

    public InfrastructureSnapshot snapshot(PlayerCompany company, int currentElapsedDay) {
        CompanyCloudPlan plan = company.getCloudPlanType();
        long cloudCapacity = plan.getCapacity();
        var network = effectiveNetwork(company);
        long permanentCapacity = Math.addExact(cloudCapacity, network.capacity());
        long reserveCapacity = company.getActiveReserveComputeCapacity(currentElapsedDay);
        long totalCapacity = Math.addExact(permanentCapacity, reserveCapacity);
        long operationsCapacity = operationsCapacity(company);
        long usableCapacity = Math.min(totalCapacity, operationsCapacity);
        long demand = Math.addExact(
                processingDemand(company.getNormalSubscribers(), company.getProSubscribers(),
                        company.getMaxSubscribers(), company.getComputeEfficiency()),
                customerContractService.processingDemand(company));
        double utilization = usableCapacity == 0 ? 0.0 : demand * 100.0 / usableCapacity;
        String status = utilization > 100 ? "용량 초과" : utilization >= 85 ? "증설 검토" : "정상";
        String tone = utilization > 100 ? "danger" : utilization >= 85 ? "warn" : "good";
        return new InfrastructureSnapshot(plan, company.getPendingCloudPlanType(), cloudCapacity,
                network, permanentCapacity, reserveCapacity, totalCapacity,
                company.getReserveComputeRemainingDays(currentElapsedDay),
                operationsCapacity, usableCapacity, demand, utilization, status, tone);
    }

    public SubscriberCapacity applyCapacity(PlayerCompany company, long normal, long pro, long max) {
        var network = effectiveNetwork(company);
        long permanentCapacity = Math.addExact(company.getCloudPlanType().getCapacity(), network.capacity());
        long reserveCapacity = company.getActiveReserveComputeCapacity(company.getPlayer().getElapsedDays());
        long capacity = Math.min(Math.addExact(permanentCapacity, reserveCapacity), operationsCapacity(company));
        long subscriberDemand = processingDemand(normal, pro, max, company.getComputeEfficiency());
        long contractDemand = customerContractService.processingDemand(company);
        long demand = Math.addExact(subscriberDemand, contractDemand);
        if (demand <= capacity || subscriberDemand == 0) {
            return new SubscriberCapacity(normal, pro, max, false);
        }
        double ratio = Math.max(0, capacity - contractDemand) / (double) subscriberDemand;
        return new SubscriberCapacity(
                Math.max(0, Math.round(normal * ratio)),
                Math.max(0, Math.round(pro * ratio)),
                Math.max(0, Math.round(max * ratio)),
                true
        );
    }

    public long monthlyCloudCost(PlayerCompany company) {
        return monthlyCloudCost(company, company.getCloudPlanType());
    }

    public long monthlyCloudCost(PlayerCompany company, CompanyCloudPlan plan) {
        return Math.round(plan.getMonthlyCost() * externalEventService.cloudCostMultiplier(company));
    }

    public long monthlyInfrastructureCost(PlayerCompany company) {
        return Math.addExact(monthlyCloudCost(company), constructionService.networkState(company).monthlyCost());
    }

    public List<CompanyCloudPlan> plans() {
        return Arrays.asList(CompanyCloudPlan.values());
    }

    @Transactional(readOnly = true)
    public List<ReserveOption> reserveOptions(PlayerCompany company) {
        return reserveOptions(company, company.getPlayer().getElapsedDays());
    }

    public List<ReserveOption> reserveOptions(PlayerCompany company, int currentElapsedDay) {
        long permanentCapacity = permanentCapacity(company);
        boolean active = company.getActiveReserveComputeCapacity(currentElapsedDay) > 0;
        return RESERVE_PERCENTAGES.stream().map(percentage -> {
            long capacity = Math.max(1L, Math.multiplyExact(permanentCapacity, percentage) / 100L);
            long cost = reserveCost(capacity);
            boolean available = !active && !company.isOperationsSuspended() && company.getCorporateCash() >= cost;
            String reason = active ? "기존 예비용량 사용 중"
                    : company.isOperationsSuspended() ? "기업 운영중단"
                    : company.getCorporateCash() < cost ? "법인현금 부족"
                    : "즉시 구매 가능";
            return new ReserveOption(percentage, capacity, cost, available, reason);
        }).toList();
    }

    private long operationsCapacity(PlayerCompany company) {
        return departmentRepository.findByCompanyAndDepartmentType(company, CompanyDepartmentType.SERVICE_OPERATIONS)
                .map(department -> Math.multiplyExact((long) workforceService.monthlyCapacity(company, department),
                        USERS_PER_OPERATIONS_CAPACITY))
                .orElse(0L);
    }

    private long permanentCapacity(PlayerCompany company) {
        return Math.addExact(
                company.getCloudPlanType().getCapacity(),
                effectiveNetwork(company).capacity()
        );
    }

    private CompanyComputeConstructionService.NetworkState effectiveNetwork(PlayerCompany company) {
        var network = constructionService.networkState(company);
        int capacityPercent = incidentService.computeCapacityPercent(company);
        if (capacityPercent >= 100 || network.capacity() == 0) {
            return network;
        }
        return new CompanyComputeConstructionService.NetworkState(
                network.name() + " · 장애 영향",
                network.capacity() * capacityPercent / 100,
                network.monthlyCost()
        );
    }

    private long reserveCost(long reserveCapacity) {
        CompanyCloudPlan referencePlan = Arrays.stream(CompanyCloudPlan.values())
                .filter(plan -> plan.getCapacity() >= reserveCapacity)
                .findFirst()
                .orElse(CompanyCloudPlan.GLOBAL);
        return BigDecimal.valueOf(reserveCapacity)
                .multiply(BigDecimal.valueOf(referencePlan.getMonthlyCost()))
                .multiply(BigDecimal.valueOf(18))
                .divide(BigDecimal.valueOf(referencePlan.getCapacity() * 10L), 0, RoundingMode.CEILING)
                .longValueExact();
    }

    private long processingDemand(long normal, long pro, long max, int efficiency) {
        long baseDemand = Math.addExact(normal, Math.addExact(Math.multiplyExact(pro, 6L), Math.multiplyExact(max, 20L)));
        double efficiencyFactor = 1.25 - Math.max(0, Math.min(100, efficiency)) / 200.0;
        return Math.max(0, Math.round(baseDemand * efficiencyFactor));
    }

    public record SubscriberCapacity(long normal, long pro, long max, boolean limited) {
        public long total() { return normal + pro + max; }
    }

    public record InfrastructureSnapshot(CompanyCloudPlan plan, CompanyCloudPlan pendingPlan,
                                         long cloudCapacity, CompanyComputeConstructionService.NetworkState network,
                                         long permanentCapacity, long reserveCapacity, long totalCapacity,
                                         int reserveRemainingDays, long operationsCapacity, long usableCapacity,
                                         long demand, double utilizationPercent, String status, String tone) {
    }


    public record ReserveOption(int percentage, long capacity, long cost, boolean available, String reason) {
    }
}
