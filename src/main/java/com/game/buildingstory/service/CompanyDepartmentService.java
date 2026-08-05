package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyTutorialStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCoreEmployeeRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 지원 부서의 출범 조건과 실제 생성 절차를 관리한다. */
@Service
public class CompanyDepartmentService {
    public static final int HR_PREPARATION_EMPLOYEES = 50;
    public static final int HR_FOUNDING_EMPLOYEES = 60;
    private static final int SUPPORT_DEPARTMENT_INITIAL_EMPLOYEES = 5;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyCoreEmployeeRepository coreEmployeeRepository;
    private final CompanyQuarterlyReportRepository quarterlyReportRepository;

    public CompanyDepartmentService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyCoreEmployeeRepository coreEmployeeRepository,
            CompanyQuarterlyReportRepository quarterlyReportRepository
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.coreEmployeeRepository = coreEmployeeRepository;
        this.quarterlyReportRepository = quarterlyReportRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentOpportunity> opportunities(PlayerCompany company) {
        return List.of(
                opportunity(company, CompanyDepartmentType.HR_ORGANIZATION),
                opportunity(company, CompanyDepartmentType.STRATEGY_FINANCE)
        ).stream().filter(opportunity -> !opportunity.established()).toList();
    }

    @Transactional
    public String establish(long playerId, CompanyDepartmentType type) {
        if (type != CompanyDepartmentType.HR_ORGANIZATION
                && type != CompanyDepartmentType.STRATEGY_FINANCE) {
            return "초기 부서는 설립 시 이미 출범함";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 부서를 출범할 수 없음";
        }
        if (!company.getTutorialStage().isOperational()) {
            return "제품 출시 후 지원 부서를 출범할 수 있음";
        }
        DepartmentOpportunity opportunity = opportunity(company, type);
        if (opportunity.established()) {
            return type.displayName() + "은 이미 출범함";
        }
        if (!opportunity.available()) {
            return opportunity.requirement();
        }

        CompanyDepartment department = new CompanyDepartment(company, type);
        department.assignFoundingWorkforce(SUPPORT_DEPARTMENT_INITIAL_EMPLOYEES);
        departmentRepository.save(department);
        return type.displayName() + " 출범 완료 · 일반인력 5명 배치";
    }

    @Transactional(readOnly = true)
    public boolean isEstablished(PlayerCompany company, CompanyDepartmentType type) {
        return departmentRepository.findByCompanyAndDepartmentType(company, type).isPresent();
    }

    @Transactional(readOnly = true)
    public int totalEmployees(PlayerCompany company) {
        int coreEmployees = (int) coreEmployeeRepository.findByCompanyOrderById(company).stream()
                .filter(employee -> !employee.isResigned())
                .count();
        int generalEmployees = departmentRepository.findByCompanyOrderById(company).stream()
                .mapToInt(CompanyDepartment::getGeneralEmployeeCount)
                .sum();
        return Math.addExact(coreEmployees, generalEmployees);
    }

    private DepartmentOpportunity opportunity(PlayerCompany company, CompanyDepartmentType type) {
        boolean established = isEstablished(company, type);
        if (!established && company.isOperationsSuspended()) {
            return new DepartmentOpportunity(
                    type, false, false, "운영중단",
                    "기업 재가동 후 출범 가능", 0
            );
        }
        if (type == CompanyDepartmentType.HR_ORGANIZATION) {
            int employees = totalEmployees(company);
            boolean available = employees >= HR_FOUNDING_EMPLOYEES;
            String status = available ? "출범 가능"
                    : employees >= HR_PREPARATION_EMPLOYEES ? "출범 준비" : "조건 미달";
            return new DepartmentOpportunity(
                    type, established, available, status,
                    "총 임직원 60명 필요 · 현재 " + employees + "명",
                    Math.min(100, employees * 100 / HR_FOUNDING_EMPLOYEES)
            );
        }
        int reports = quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company).size();
        return new DepartmentOpportunity(
                type, established, reports > 0, reports > 0 ? "출범 가능" : "결산 대기",
                reports > 0 ? "첫 분기 결산 완료" : "첫 분기 결산 후 출범 가능",
                reports > 0 ? 100 : company.getMarketMonthsProcessed() % 3 * 100 / 3
        );
    }

    public record DepartmentOpportunity(
            CompanyDepartmentType type,
            boolean established,
            boolean available,
            String status,
            String requirement,
            int progressPercent
    ) {
    }
}
