package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 최근 두 개 확정 분기로 회사 성장단계를 일관되게 판정한다. */
@Service
public class CompanyGrowthService {
    private final CompanyQuarterlyReportRepository quarterlyReportRepository;
    private final PlayerCompanyRepository companyRepository;

    public CompanyGrowthService(
            CompanyQuarterlyReportRepository quarterlyReportRepository,
            PlayerCompanyRepository companyRepository
    ) {
        this.quarterlyReportRepository = quarterlyReportRepository;
        this.companyRepository = companyRepository;
    }

    public CompanyGrowthStage stage(PlayerCompany company) {
        return company.getGrowthStage();
    }

    /**
     * 확정 분기보고서가 생성된 직후 최근 두 분기를 평가해 성장단계를 올린다.
     * 화면 조회와 무관하게 정산 흐름에서만 상태가 변경되도록 승급 명령을 분리했다.
     */
    @Transactional
    public CompanyGrowthStage evaluateAndPromote(PlayerCompany company) {
        CompanyGrowthStage current = company.getGrowthStage();
        var reports = quarterlyReportRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        if (reports.size() < 2) {
            return current;
        }
        var latest = reports.stream().limit(2).toList();
        CompanyGrowthStage[] stages = CompanyGrowthStage.values();
        int nextIndex = current.ordinal() + 1;
        if (nextIndex >= stages.length) {
            return current;
        }
        CompanyGrowthStage candidate = stages[nextIndex];
        boolean qualified = latest.stream().allMatch(report -> candidate.qualifies(
                report.getRecurringRevenueAtEnd(), report.getPaidUsersAtEnd()));
        if (qualified && company.promoteGrowthStage(candidate)) {
            companyRepository.save(company);
        }
        return company.getGrowthStage();
    }
}
