package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyMonthlySettlement;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyMonthlySettlementRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.web.CompanyManagementReportView;
import com.game.buildingstory.web.CompanyFinancialForecastView;
import com.game.buildingstory.web.MoneyText;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 확정 분기와 구성 월 정산을 읽어 손익 원인을 설명하는 경영보고를 만든다. */
@Service
public class CompanyReportingService {
    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyMonthlySettlementRepository monthlyRepository;
    private final CompanyQuarterlyReportRepository quarterlyRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService companyWorkforceService;
    private final CompanySecretaryService companySecretaryService;
    private final MoneyText moneyText;

    public CompanyReportingService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyMonthlySettlementRepository monthlyRepository,
            CompanyQuarterlyReportRepository quarterlyRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService companyWorkforceService,
            CompanySecretaryService companySecretaryService,
            MoneyText moneyText
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.monthlyRepository = monthlyRepository;
        this.quarterlyRepository = quarterlyRepository;
        this.departmentRepository = departmentRepository;
        this.companyWorkforceService = companyWorkforceService;
        this.companySecretaryService = companySecretaryService;
        this.moneyText = moneyText;
    }

    /**
     * 최근 실제 월 결산을 이용해 다음 달 매출, 영업손익, 기말 현금의 범위를 추정한다.
     *
     * 전략재무팀 비서의 기업 숙련도가 오를수록 오차율이 줄어든다. 예측값의 중심을 임의로
     * 유리하게 바꾸는 효과가 아니라 같은 기준값을 더 좁은 범위로 보여 주는 정보 정확도 효과다.
     */
    @Transactional(readOnly = true)
    public CompanyFinancialForecastView financialForecast(PlayerCompany company) {
        if (departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.STRATEGY_FINANCE).isEmpty()) {
            return CompanyFinancialForecastView.unavailable("전략재무팀 설립 후 전망을 제공합니다.");
        }
        List<CompanyMonthlySettlement> months =
                monthlyRepository.findTop3ByCompanyOrderByPeriodIndexDesc(company);
        if (months.isEmpty()) {
            return CompanyFinancialForecastView.unavailable("첫 월 결산 후 전망을 제공합니다.");
        }

        long revenue = average(months, CompanyMonthlySettlement::getTotalRevenue);
        long operatingProfit = average(months, CompanyMonthlySettlement::getOperatingProfit);
        long netIncome = average(months, CompanyMonthlySettlement::getNetIncome);
        int strategyExpertise = companyWorkforceService.departmentExpertise(
                company, CompanyDepartmentType.STRATEGY_FINANCE);
        var strategyLoad = companyWorkforceService.departmentLoad(
                company, CompanyDepartmentType.STRATEGY_FINANCE);
        if (strategyLoad.capacity() == 0) {
            return CompanyFinancialForecastView.unavailable("전략재무팀 인력이 없어 전망을 산출할 수 없습니다.");
        }
        int overloadError = (int) Math.ceil(Math.max(0, strategyLoad.utilizationPercent() - 100) / 10.0);
        int expertiseAdjustedError = 20 - strategyExpertise / 10 + Math.min(10, overloadError);
        int errorPercent = (int) Math.round(
                expertiseAdjustedError * companySecretaryService.forecastErrorMultiplier(company));
        long cashError = Math.round(Math.abs(netIncome) * errorPercent / 100.0);
        long cashLow = Math.addExact(company.getCorporateCash(), netIncome - cashError);
        long cashHigh = Math.addExact(company.getCorporateCash(), netIncome + cashError);

        return new CompanyFinancialForecastView(
                true,
                range(revenue, errorPercent),
                range(operatingProfit, errorPercent),
                range(cashLow, cashHigh),
                "예상 오차 ±" + errorPercent + "%",
                "최근 " + months.size() + "개월 실제 결산 기준 · 법인세·채권이자 반영 · 투자·원금상환·자금조달 제외"
        );
    }

    @Transactional(readOnly = true)
    public List<CompanyManagementReportView> reports(PlayerCompany company) {
        List<CompanyQuarterlyReport> reports =
                quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        List<CompanyManagementReportView> result = new ArrayList<>(
                reports.stream().limit(8).map(report -> view(company, report)).toList());
        Map<Integer, List<CompanyQuarterlyReport>> olderByYear = new LinkedHashMap<>();
        reports.stream().skip(8).forEach(report ->
                olderByYear.computeIfAbsent(gameYear(report), ignored -> new ArrayList<>()).add(report));
        olderByYear.forEach((year, annualReports) -> {
            boolean completeAndRead = annualReports.size() == 4
                    && annualReports.stream().noneMatch(CompanyQuarterlyReport::isUnread);
            if (completeAndRead) {
                result.add(annualView(year, annualReports));
            } else {
                annualReports.forEach(report -> result.add(view(company, report)));
            }
        });
        return result;
    }

    @Transactional
    public boolean markRead(long playerId, long quarterSequence) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        return quarterlyRepository.findByCompanyAndQuarterSequence(company, (int) quarterSequence)
                .map(report -> {
                    report.markRead();
                    company.completeTutorialAfterFirstQuarterRead(report.getQuarterSequence());
                    return true;
                })
                .orElse(false);
    }

    private CompanyManagementReportView view(
            PlayerCompany company,
            CompanyQuarterlyReport report
    ) {
        List<CompanyMonthlySettlement> months =
                monthlyRepository.findByCompanyAndPeriodIndexBetweenOrderByPeriodIndexAsc(
                        company, report.getEndingPeriodIndex() - 2, report.getEndingPeriodIndex());
        long openingCash = months.isEmpty() ? report.getClosingCash() : months.getFirst().getOpeningCash();
        boolean hasLedgerSnapshot = report.getOperatingCashFlow() != null;
        long operatingCashFlow = hasLedgerSnapshot
                ? report.getOperatingCashFlow()
                : report.getClosingCash() - openingCash;
        long investingCashFlow = hasLedgerSnapshot ? report.getInvestingCashFlow() : 0;
        long financingCashFlow = hasLedgerSnapshot ? report.getFinancingCashFlow() : 0;
        long cashChange = Math.addExact(
                Math.addExact(operatingCashFlow, investingCashFlow),
                financingCashFlow
        );
        long subscription = sum(months, CompanyMonthlySettlement::getSubscriptionRevenue);
        long projects = sum(months, CompanyMonthlySettlement::getProjectRevenue);
        long contracts = sum(months, CompanyMonthlySettlement::getContractRevenue);
        long payroll = sum(months, CompanyMonthlySettlement::getPayrollCost);
        long infrastructure = sum(months, CompanyMonthlySettlement::getCloudCost);
        long development = sum(months, CompanyMonthlySettlement::getDevelopmentCost);
        long marketing = sum(months, CompanyMonthlySettlement::getMarketingCost);
        long platform = sum(months, CompanyMonthlySettlement::getPlatformCost);
        long projectCost = sum(months, CompanyMonthlySettlement::getProjectCost);
        long contractCost = sum(months, CompanyMonthlySettlement::getContractCost);
        long incidentCost = sum(months, CompanyMonthlySettlement::getIncidentCost);
        String cause = primaryCause(
                report.getOperatingProfit(),
                List.of(
                        new Amount("구독매출", subscription),
                        new Amount("단기 사업", projects),
                        new Amount("기업계약", contracts)
                ),
                List.of(
                        new Amount("인건비", payroll),
                        new Amount("연산비", infrastructure),
                        new Amount("제품개발비", development),
                        new Amount("마케팅비", marketing),
                        new Amount("플랫폼 운영비", platform),
                        new Amount("단기 사업비", projectCost),
                        new Amount("계약 수행비", contractCost),
                        new Amount("장애 대응비", incidentCost)
                )
        );
        List<CompanyManagementReportView.Line> lines = new ArrayList<>();
        lines.add(line("총매출", report.getRevenue(), "good"));
        lines.add(line("구독매출", subscription, ""));
        lines.add(line("단기 사업매출", projects, ""));
        lines.add(line("기업계약매출", contracts, ""));
        lines.add(line("영업비용", report.getOperatingExpenses(), "warn"));
        lines.add(line("인건비", payroll, ""));
        lines.add(line("연산비", infrastructure, ""));
        lines.add(line("개발·마케팅비", development + marketing, ""));
        lines.add(line("플랫폼 운영비", platform, ""));
        lines.add(line("사업·계약·장애비", projectCost + contractCost + incidentCost, ""));
        lines.add(line("영업이익", report.getOperatingProfit(),
                report.getOperatingProfit() >= 0 ? "good" : "danger"));
        lines.add(line("법인세", report.getCorporateTax(), ""));
        lines.add(line("순이익", report.getNetIncome(),
                report.getNetIncome() >= 0 ? "good" : "danger"));
        lines.add(line("영업현금흐름", operatingCashFlow, operatingCashFlow >= 0 ? "good" : "danger"));
        lines.add(line("투자현금흐름", investingCashFlow, investingCashFlow >= 0 ? "good" : "danger"));
        lines.add(line("재무현금흐름", financingCashFlow, financingCashFlow >= 0 ? "good" : "danger"));
        lines.add(line("전체 현금증감", cashChange, cashChange >= 0 ? "good" : "danger"));
        lines.add(line("기말 법인현금", report.getClosingCash(), ""));
        return new CompanyManagementReportView(
                report.getQuarterSequence(),
                "기업 " + report.getQuarterSequence() + "분기",
                signedMoney(report.getOperatingProfit()),
                signedMoney(cashChange),
                cause,
                report.isUnread(),
                lines
        );
    }

    private CompanyManagementReportView annualView(
            int year,
            List<CompanyQuarterlyReport> reports
    ) {
        long revenue = reports.stream().mapToLong(CompanyQuarterlyReport::getRevenue).sum();
        long expenses = reports.stream().mapToLong(CompanyQuarterlyReport::getOperatingExpenses).sum();
        long operatingProfit = reports.stream().mapToLong(CompanyQuarterlyReport::getOperatingProfit).sum();
        long tax = reports.stream().mapToLong(CompanyQuarterlyReport::getCorporateTax).sum();
        long netIncome = reports.stream().mapToLong(CompanyQuarterlyReport::getNetIncome).sum();
        long operatingCash = reports.stream().mapToLong(report ->
                report.getOperatingCashFlow() == null ? 0 : report.getOperatingCashFlow()).sum();
        long investingCash = reports.stream().mapToLong(report ->
                report.getInvestingCashFlow() == null ? 0 : report.getInvestingCashFlow()).sum();
        long financingCash = reports.stream().mapToLong(report ->
                report.getFinancingCashFlow() == null ? 0 : report.getFinancingCashFlow()).sum();
        long cashChange = operatingCash + investingCash + financingCash;
        CompanyQuarterlyReport latest = reports.stream()
                .max(Comparator.comparingInt(CompanyQuarterlyReport::getEndingPeriodIndex))
                .orElseThrow();
        List<CompanyManagementReportView.Line> lines = List.of(
                line("연간 총매출", revenue, "good"),
                line("연간 영업비용", expenses, "warn"),
                line("연간 영업이익", operatingProfit, operatingProfit >= 0 ? "good" : "danger"),
                line("연간 법인세", tax, ""),
                line("연간 순이익", netIncome, netIncome >= 0 ? "good" : "danger"),
                line("영업현금흐름", operatingCash, operatingCash >= 0 ? "good" : "danger"),
                line("투자현금흐름", investingCash, investingCash >= 0 ? "good" : "danger"),
                line("재무현금흐름", financingCash, financingCash >= 0 ? "good" : "danger"),
                line("전체 현금증감", cashChange, cashChange >= 0 ? "good" : "danger"),
                line("연말 법인현금", latest.getClosingCash(), "")
        );
        return new CompanyManagementReportView(
                -year,
                "기업 " + year + "년 연간",
                signedMoney(operatingProfit),
                signedMoney(cashChange),
                operatingProfit >= 0 ? "연간 영업흑자" : "연간 영업적자",
                false,
                lines
        );
    }

    private int gameYear(CompanyQuarterlyReport report) {
        return report.getEndingPeriodIndex() / 12 + 1;
    }

    private String primaryCause(
            long operatingProfit,
            List<Amount> revenues,
            List<Amount> costs
    ) {
        Amount primary = (operatingProfit >= 0 ? revenues : costs).stream()
                .max(Comparator.comparingLong(Amount::value))
                .orElse(new Amount("기록 없음", 0));
        return (operatingProfit >= 0 ? "주요 매출 " : "최대 비용 ")
                + primary.label() + " " + moneyText.format(primary.value());
    }

    private CompanyManagementReportView.Line line(String label, long value, String tone) {
        return new CompanyManagementReportView.Line(label, moneyText.format(value), tone);
    }

    private String signedMoney(long value) {
        return (value > 0 ? "+" : "") + moneyText.format(value);
    }

    private long sum(
            List<CompanyMonthlySettlement> months,
            java.util.function.ToLongFunction<CompanyMonthlySettlement> getter
    ) {
        return months.stream().mapToLong(getter).sum();
    }

    private long average(
            List<CompanyMonthlySettlement> months,
            java.util.function.ToLongFunction<CompanyMonthlySettlement> getter
    ) {
        return Math.round(months.stream().mapToLong(getter).average().orElse(0));
    }

    private String range(long center, int errorPercent) {
        long error = Math.round(Math.abs(center) * errorPercent / 100.0);
        return range(center - error, center + error);
    }

    private String range(long low, long high) {
        return moneyText.format(low) + " - " + moneyText.format(high);
    }

    private record Amount(String label, long value) {
    }
}
