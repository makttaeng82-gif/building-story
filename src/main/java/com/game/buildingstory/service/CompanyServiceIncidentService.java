package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyServiceIncident;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyServiceIncidentResolution;
import com.game.buildingstory.domain.CompanyServiceIncidentSeverity;
import com.game.buildingstory.domain.CompanyServiceIncidentStatus;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 제품 상태 기반 장애 판정, 중대 장애 선택과 치명 장애 대응 프로젝트를 처리한다. */
@Service
public class CompanyServiceIncidentService {
    private static final int CRITICAL_DEVELOPMENT_WORK = 180;
    private static final int CRITICAL_OPERATIONS_WORK = 120;
    private static final int MAXIMUM_DEVELOPMENT_ASSIGNMENT = 60;
    private static final int MAXIMUM_OPERATIONS_ASSIGNMENT = 40;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyServiceIncidentRepository incidentRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;
    private final CompanySecretaryService secretaryService;
    private final CompanyComputeConstructionService constructionService;
    private final CompanyCashLedgerService cashLedgerService;

    public CompanyServiceIncidentService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyServiceIncidentRepository incidentRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService workforceService,
            CompanySecretaryService secretaryService,
            CompanyComputeConstructionService constructionService,
            CompanyCashLedgerService cashLedgerService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.incidentRepository = incidentRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
        this.secretaryService = secretaryService;
        this.constructionService = constructionService;
        this.cashLedgerService = cashLedgerService;
    }

    @Transactional(readOnly = true)
    public MonthlyFinancials monthlyFinancials(PlayerCompany company) {
        long responseCost = incidents(company).stream()
                .mapToLong(CompanyServiceIncident::currentMonthlyCost)
                .sum();
        return new MonthlyFinancials(responseCost);
    }

    @Transactional
    public IncidentMonthResult processSuccessfulMonth(
            PlayerCompany company,
            double infrastructureUtilizationPercent
    ) {
        StringBuilder notice = new StringBuilder();
        Optional<CompanyServiceIncident> current = activeIncident(company);
        if (current.isPresent()
                && current.get().getStatus() == CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS) {
            CompanyServiceIncident incident = current.get();
            if (incident.advanceResponseMonth()) {
                releaseResponseWork(company, incident);
                company.applyIncidentRecovery(CompanyServiceIncidentSeverity.CRITICAL);
                workforceService.grantDepartmentExperience(
                        company, CompanyDepartmentType.AI_DEVELOPMENT, 3.0);
                workforceService.grantDepartmentExperience(
                        company, CompanyDepartmentType.SERVICE_OPERATIONS, 3.0);
                notice.append("치명 장애 긴급 대응 완료");
            } else {
                notice.append("치명 장애 대응 ").append(incident.progressPercent()).append("%");
            }
        }

        boolean customerImpact = activeIncident(company)
                .map(CompanyServiceIncident::hasCustomerImpact)
                .orElse(false);
        boolean generated = activeIncident(company).isEmpty()
                && generateIncidentIfDue(company, infrastructureUtilizationPercent, notice);
        return new IncidentMonthResult(!notice.isEmpty(), generated, customerImpact, notice.toString());
    }

    @Transactional
    public String resolveMajor(
            long playerId,
            long incidentId,
            CompanyServiceIncidentResolution resolution
    ) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 장애 대응을 결정할 수 있음";
        }
        CompanyServiceIncident incident = ownedIncident(company, incidentId);
        if (incident.getStatus() != CompanyServiceIncidentStatus.AWAITING_DECISION) {
            return "대응을 선택할 수 있는 중대 장애가 아님";
        }
        long cost = incident.resolutionCost(resolution);
        if (!cashLedgerService.withdraw(
                company,
                "incident:" + incidentId + ":" + resolution.name(),
                CompanyCashFlowType.OPERATING,
                "서비스 장애 대응비",
                cost)) {
            return "장애 대응 비용이 부족함";
        }
        incident.resolveMajor(resolution);
        if (resolution == CompanyServiceIncidentResolution.EMERGENCY_RECOVERY) {
            company.applyIncidentRecovery(CompanyServiceIncidentSeverity.MAJOR);
        }
        return resolution.getDisplayName() + " 완료 · " + cost + "원 지출";
    }

    @Transactional
    public String startCriticalResponse(long playerId, long incidentId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 정상 운영 중에만 긴급 대응을 시작할 수 있음";
        }
        CompanyServiceIncident incident = ownedIncident(company, incidentId);
        if (!startCriticalResponseIfPossible(company, incident)) {
            return "AI개발팀·서비스운영팀과 회사 주요 업무 슬롯 2개가 필요함";
        }
        return "치명 장애 긴급 대응 시작";
    }

    @Transactional
    public boolean failActiveResponse(PlayerCompany company) {
        Optional<CompanyServiceIncident> current = activeIncident(company);
        if (current.isEmpty()
                || current.get().getStatus() != CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS) {
            return false;
        }
        CompanyServiceIncident incident = current.get();
        releaseResponseWork(company, incident);
        incident.failResponse();
        return true;
    }

    @Transactional(readOnly = true)
    public List<CompanyServiceIncident> incidents(PlayerCompany company) {
        return incidentRepository.findByCompanyOrderByIdDesc(company);
    }

    @Transactional(readOnly = true)
    public Optional<CompanyServiceIncident> activeIncident(PlayerCompany company) {
        return incidents(company).stream()
                .filter(incident -> incident.getStatus() == CompanyServiceIncidentStatus.AWAITING_DECISION
                        || incident.getStatus() == CompanyServiceIncidentStatus.RESPONSE_REQUIRED
                        || incident.getStatus() == CompanyServiceIncidentStatus.RESPONSE_IN_PROGRESS)
                .findFirst();
    }

    public int incidentProbabilityBasisPoints(
            PlayerCompany company,
            double highestServiceUtilizationPercent
    ) {
        double probabilityPercent = 1.0
                + company.getTechnicalDebt() * 0.10
                + Math.max(0, 75 - company.getProductStability()) * 0.15
                + Math.max(0, highestServiceUtilizationPercent - 90) * 0.10;
        probabilityPercent *= secretaryService.incidentProbabilityMultiplier(company);
        return (int) Math.round(Math.max(0.5, Math.min(20.0, probabilityPercent)) * 100);
    }

    @Transactional(readOnly = true)
    public int currentProbabilityBasisPoints(
            PlayerCompany company,
            double infrastructureUtilizationPercent
    ) {
        CompanyDepartment operations = department(company, CompanyDepartmentType.SERVICE_OPERATIONS);
        return incidentProbabilityBasisPoints(company, Math.max(
                infrastructureUtilizationPercent,
                workforceService.departmentLoad(company, operations).utilizationPercent()));
    }

    @Transactional(readOnly = true)
    public int computeCapacityPercent(PlayerCompany company) {
        return activeIncident(company)
                .filter(CompanyServiceIncident::isComputeIncident)
                .map(incident -> incident.getSeverity() == CompanyServiceIncidentSeverity.CRITICAL ? 50 : 75)
                .orElse(100);
    }

    private boolean generateIncidentIfDue(
            PlayerCompany company,
            double infrastructureUtilizationPercent,
            StringBuilder notice
    ) {
        CompanyDepartment operations = department(company, CompanyDepartmentType.SERVICE_OPERATIONS);
        double highestUtilization = Math.max(
                infrastructureUtilizationPercent,
                workforceService.departmentLoad(company, operations).utilizationPercent());
        int probability = incidentProbabilityBasisPoints(company, highestUtilization);
        int occurrenceRoll = deterministicRoll(company, 17);
        if (occurrenceRoll >= probability) {
            return false;
        }

        int severityRoll = deterministicRoll(company, 43);
        CompanyServiceIncidentSeverity severity = severityRoll < 7_000
                ? CompanyServiceIncidentSeverity.MINOR
                : severityRoll < 9_500
                ? CompanyServiceIncidentSeverity.MAJOR
                : CompanyServiceIncidentSeverity.CRITICAL;
        boolean securityIncident = severity != CompanyServiceIncidentSeverity.MINOR
                && deterministicRoll(company, 71)
                < Math.max(0, 70 - company.getProductSecurity()) * 200;
        boolean computeIncident = !securityIncident
                && constructionService.networkState(company).capacity() > 0
                && deterministicRoll(company, 83) < 2_500;
        long fullResponseCost = Math.max(
                1_000_000_000L, company.getMonthlyRecurringRevenue() * 2 / 100);
        long monthlyResponseCost = Math.max(
                2_000_000_000L, company.getMonthlyRecurringRevenue() * 2 / 100);
        CompanyServiceIncident incident = incidentRepository.save(new CompanyServiceIncident(
                company,
                severity,
                securityIncident,
                computeIncident,
                company.getMarketMonthsProcessed(),
                probability,
                fullResponseCost,
                severity == CompanyServiceIncidentSeverity.CRITICAL ? CRITICAL_DEVELOPMENT_WORK : 0,
                severity == CompanyServiceIncidentSeverity.CRITICAL ? CRITICAL_OPERATIONS_WORK : 0,
                monthlyResponseCost
        ));
        company.applyServiceIncident(severity, securityIncident);
        double brandDamage = switch (severity) {
            case MINOR -> 0.0;
            case MAJOR -> -2.0;
            case CRITICAL -> -5.0;
        };
        if (securityIncident) {
            brandDamage -= 5.0;
        }
        company.adjustBrandScore(brandDamage);

        if (severity == CompanyServiceIncidentSeverity.CRITICAL) {
            startCriticalResponseIfPossible(company, incident);
        }
        if (!notice.isEmpty()) {
            notice.append(" · ");
        }
        notice.append(incidentName(securityIncident, computeIncident))
                .append(" · ").append(severity.getDisplayName());
        return true;
    }

    private String incidentName(boolean securityIncident, boolean computeIncident) {
        if (securityIncident) {
            return "보안사고";
        }
        return computeIncident ? "연산장비 장애" : "서비스 장애";
    }

    private boolean startCriticalResponseIfPossible(
            PlayerCompany company,
            CompanyServiceIncident incident
    ) {
        if (incident.getStatus() != CompanyServiceIncidentStatus.RESPONSE_REQUIRED
                || company.getActiveMajorWorkCount() + 2
                > workforceService.companyMajorWorkSlotLimit(company)) {
            return false;
        }
        int developmentWork = availableWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, MAXIMUM_DEVELOPMENT_ASSIGNMENT);
        int operationsWork = availableWork(
                company, CompanyDepartmentType.SERVICE_OPERATIONS, MAXIMUM_OPERATIONS_ASSIGNMENT);
        if (developmentWork <= 0 || operationsWork <= 0) {
            return false;
        }
        workforceService.reserveMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT, developmentWork);
        try {
            workforceService.reserveMajorWork(
                    company, CompanyDepartmentType.SERVICE_OPERATIONS, operationsWork);
        } catch (RuntimeException exception) {
            workforceService.releaseMajorWork(
                    company, CompanyDepartmentType.AI_DEVELOPMENT, developmentWork);
            return false;
        }
        incident.startCriticalResponse(developmentWork, operationsWork);
        return true;
    }

    private int availableWork(
            PlayerCompany company,
            CompanyDepartmentType type,
            int maximumAssignment
    ) {
        CompanyDepartment department = department(company, type);
        var load = workforceService.departmentLoad(company, department);
        return Math.min(maximumAssignment,
                Math.max(0, (int) Math.floor(load.capacity() * 1.30) - load.totalWorkload()));
    }

    private void releaseResponseWork(PlayerCompany company, CompanyServiceIncident incident) {
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.AI_DEVELOPMENT,
                incident.getAssignedDevelopmentWork());
        workforceService.releaseMajorWork(
                company, CompanyDepartmentType.SERVICE_OPERATIONS,
                incident.getAssignedOperationsWork());
    }

    private CompanyDepartment department(PlayerCompany company, CompanyDepartmentType type) {
        return departmentRepository.findByCompanyAndDepartmentType(company, type).orElseThrow();
    }

    private CompanyServiceIncident ownedIncident(PlayerCompany company, long incidentId) {
        CompanyServiceIncident incident = incidentRepository.findById(incidentId).orElseThrow();
        if (!incident.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("다른 기업의 장애에는 접근할 수 없음");
        }
        return incident;
    }

    private int deterministicRoll(PlayerCompany company, int salt) {
        long value = company.getId() * 1_000_003L
                + company.getMarketMonthsProcessed() * 97_409L
                + salt * 65_537L;
        value ^= value >>> 29;
        value *= 0x9E3779B97F4A7C15L;
        return Math.floorMod(value, 10_000);
    }

    public record MonthlyFinancials(long cost) {
    }

    public record IncidentMonthResult(
            boolean active,
            boolean generated,
            boolean customerImpact,
            String notice
    ) {
    }
}
