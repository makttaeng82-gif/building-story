package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.CompanyServiceIncidentSeverity;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static com.game.buildingstory.service.CompanyIpoQualification.RequirementCheck;

/** 기업 운영 결과를 IPO 정책과 대조하며 상태를 변경하지 않는 판정 전용 서비스다. */
@Service
@Transactional(readOnly = true)
public class CompanyIpoQualificationService {
    private final CompanyListingRepository listingRepository;
    private final CompanyQuarterlyReportRepository quarterlyReportRepository;
    private final CompanyValuationSnapshotRepository valuationRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyServiceIncidentRepository incidentRepository;
    private final CompanyWorkforceService workforceService;

    public CompanyIpoQualificationService(
            CompanyListingRepository listingRepository,
            CompanyQuarterlyReportRepository quarterlyReportRepository,
            CompanyValuationSnapshotRepository valuationRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyServiceIncidentRepository incidentRepository,
            CompanyWorkforceService workforceService
    ) {
        this.listingRepository = listingRepository;
        this.quarterlyReportRepository = quarterlyReportRepository;
        this.valuationRepository = valuationRepository;
        this.departmentRepository = departmentRepository;
        this.incidentRepository = incidentRepository;
        this.workforceService = workforceService;
    }

    public CompanyIpoQualification evaluate(PlayerCompany company) {
        return evaluate(company, false);
    }

    /** 준비 완료 후에는 현재 IPO 레코드 자체를 결격사유로 보지 않고 운영조건만 다시 검사한다. */
    public CompanyIpoQualification evaluateForFinalReview(PlayerCompany company) {
        return evaluate(company, true);
    }

    private CompanyIpoQualification evaluate(PlayerCompany company, boolean finalReview) {
        var reports = quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        int profitableQuarters = (int) reports.stream()
                .limit(CompanyIpoPolicy.REQUIRED_PROFITABLE_QUARTERS)
                .takeWhile(report -> report.getOperatingProfit() > 0)
                .count();
        long equityValue = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company)
                .map(snapshot -> snapshot.getEnterpriseValue())
                .orElse(0L);
        int strategyExpertise = departmentRepository.findByCompanyAndDepartmentType(
                        company, CompanyDepartmentType.STRATEGY_FINANCE)
                .map(ignored -> workforceService.departmentExpertise(
                        company, CompanyDepartmentType.STRATEGY_FINANCE))
                .orElse(0);
        boolean financiallyHealthy = !company.isOperationsSuspended()
                && company.getUnpaidSettlementAmount() == 0
                && company.getUnpaidOperatingAmount() == 0
                && company.getUnpaidBondInterestAmount() == 0
                && company.getUnpaidBondPrincipalAmount() == 0;
        boolean hasCriticalIncident = incidentRepository.findByCompanyOrderByIdDesc(company).stream()
                .anyMatch(incident -> incident.getSeverity() == CompanyServiceIncidentSeverity.CRITICAL
                        && incident.hasCustomerImpact());

        var checks = new ArrayList<RequirementCheck>();
        boolean listingStateAllowed = finalReview || listingRepository.findByCompany(company)
                .map(listing -> listing.getStatus() == CompanyListingStatus.CANCELLED)
                .orElse(true);
        checks.add(booleanCheck(CompanyIpoRequirement.NO_EXISTING_LISTING, listingStateAllowed));
        checks.add(check(
                CompanyIpoRequirement.GROWTH_STAGE,
                company.getGrowthStage().ordinal(),
                CompanyIpoPolicy.MINIMUM_GROWTH_STAGE.ordinal()));
        checks.add(check(
                CompanyIpoRequirement.REPORT_HISTORY,
                reports.size(),
                CompanyIpoPolicy.MINIMUM_QUARTERLY_REPORTS));
        checks.add(check(
                CompanyIpoRequirement.PROFITABILITY,
                profitableQuarters,
                CompanyIpoPolicy.REQUIRED_PROFITABLE_QUARTERS));
        checks.add(check(
                CompanyIpoRequirement.RECURRING_REVENUE,
                company.getMonthlyRecurringRevenue(),
                CompanyIpoPolicy.MINIMUM_MONTHLY_RECURRING_REVENUE));
        checks.add(check(
                CompanyIpoRequirement.PAID_USERS,
                company.getPaidUsers(),
                CompanyIpoPolicy.MINIMUM_PAID_USERS));
        checks.add(check(
                CompanyIpoRequirement.BENCHMARK,
                company.getPrototypeBenchmark(),
                CompanyIpoPolicy.MINIMUM_BENCHMARK));
        checks.add(check(
                CompanyIpoRequirement.EQUITY_VALUE,
                equityValue,
                CompanyIpoPolicy.MINIMUM_EQUITY_VALUE));
        checks.add(check(
                CompanyIpoRequirement.STRATEGY_FINANCE,
                strategyExpertise,
                CompanyIpoPolicy.MINIMUM_STRATEGY_EXPERTISE));
        checks.add(booleanCheck(CompanyIpoRequirement.FINANCIAL_HEALTH, financiallyHealthy));
        checks.add(booleanCheck(CompanyIpoRequirement.NO_CRITICAL_INCIDENT, !hasCriticalIncident));
        return new CompanyIpoQualification(checks);
    }

    private RequirementCheck check(CompanyIpoRequirement requirement, long current, long required) {
        return new RequirementCheck(requirement, current, required, current >= required);
    }

    private RequirementCheck booleanCheck(CompanyIpoRequirement requirement, boolean met) {
        return new RequirementCheck(requirement, met ? 1 : 0, 1, met);
    }
}
