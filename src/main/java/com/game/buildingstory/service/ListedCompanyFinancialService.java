package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Random;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 상장기업의 분기 손익과 재무상태를 계산하고 확정 이력으로 저장한다. */
@Service
@Transactional
public class ListedCompanyFinancialService {
    private static final int QUARTERLY_INTEREST_BASIS_POINTS = 100;
    private static final int CORPORATE_TAX_BASIS_POINTS = 2_200;

    private final ListedCompanyService listedCompanyService;
    private final ListedCompanyRepository listedCompanyRepository;
    private final ListedCompanyQuarterlyReportRepository reportRepository;
    private final ListedCompanyFinancialCatalog financialCatalog;
    private final ListedCompanyValuationService valuationService;
    private final StockCatalog stockCatalog;
    private final StockMarketRegimeService marketRegimeService;
    private final StockMarketRegimeFinancialPolicy marketRegimeFinancialPolicy;
    private final StockDividendService stockDividendService;

    public ListedCompanyFinancialService(
            ListedCompanyService listedCompanyService,
            ListedCompanyRepository listedCompanyRepository,
            ListedCompanyQuarterlyReportRepository reportRepository,
            ListedCompanyFinancialCatalog financialCatalog,
            ListedCompanyValuationService valuationService,
            StockCatalog stockCatalog,
            StockMarketRegimeService marketRegimeService,
            StockMarketRegimeFinancialPolicy marketRegimeFinancialPolicy,
            StockDividendService stockDividendService
    ) {
        this.listedCompanyService = listedCompanyService;
        this.listedCompanyRepository = listedCompanyRepository;
        this.reportRepository = reportRepository;
        this.financialCatalog = financialCatalog;
        this.valuationService = valuationService;
        this.stockCatalog = stockCatalog;
        this.marketRegimeService = marketRegimeService;
        this.marketRegimeFinancialPolicy = marketRegimeFinancialPolicy;
        this.stockDividendService = stockDividendService;
    }

    /**
     * 확정된 업종 사건을 해당 업종의 모든 기업 전망과 다음 실제 결산에 반영한다.
     * 전망과 실제값을 함께 움직여 뉴스가 단순한 주가 보정치로 끝나지 않게 한다.
     */
    public void applyIndustryNews(Player player, String industry, int revenueImpactBasisPoints) {
        if (revenueImpactBasisPoints == 0) {
            return;
        }
        var targetKeys = stockCatalog.all().stream()
                .filter(stock -> stock.industry().equals(industry))
                .map(StockSpec::key)
                .collect(java.util.stream.Collectors.toSet());
        for (ListedCompany company : listedCompanyRepository.findByPlayer(player)) {
            if (!targetKeys.contains(company.getStockKey())) {
                continue;
            }
            company.registerIndustryRevenueImpact(revenueImpactBasisPoints);
            long currentExpectation = Math.max(1, company.getExpectedRevenue());
            long revisedRevenue = Math.max(1, scale(currentExpectation, 10_000 + revenueImpactBasisPoints));
            company.reviseExpectedResults(
                    revisedRevenue,
                    expectedNetIncome(revisedRevenue, company, company.getDebt())
            );
        }
    }

    /** 확정 기업 사건을 대상 기업 한 곳의 전망, 다음 결산과 현재 재무상태에 반영한다. */
    public void applyCompanyNews(Player player, String stockKey, StockCompanyFinancialEffect effect) {
        ListedCompany company = listedCompanyRepository.findByPlayerAndStockKey(player, stockKey).orElseThrow();
        company.registerCompanyOperatingImpact(
                effect.revenueImpactBasisPoints(),
                effect.operatingExpenseImpactBasisPoints()
        );

        long revenueBase = Math.max(1, company.getQuarterlyRevenue());
        company.applyDebtFinancing(scale(revenueBase, Math.max(0, effect.debtFinancingBasisPoints())));
        company.applyImmediateProfitOrLoss(scale(revenueBase, effect.immediateCashImpactBasisPoints()));

        long revisedRevenue = Math.max(1, scale(
                Math.max(1, company.getExpectedRevenue()),
                10_000 + effect.revenueImpactBasisPoints()
        ));
        company.reviseExpectedResults(
                revisedRevenue,
                expectedNetIncome(
                        revisedRevenue,
                        company,
                        company.getDebt(),
                        effect.operatingExpenseImpactBasisPoints()
                )
        );
        if (!company.hasBalancedFinancialPosition()) {
            throw new IllegalStateException("기업 사건 반영 후 재무상태표가 일치하지 않습니다.");
        }
    }

    /**
     * 4·7·10·1월 1일에 직전 분기를 한 번만 결산한다.
     * 조회할 때 계산하지 않으므로 새로고침이나 화면 이동으로 실적이 달라지지 않는다.
     */
    public int settlePreviousQuarterIfDue(Player player) {
        if (!player.isStockContentUnlocked() || !FiscalQuarter.isQuarterOpeningDay(player)) {
            return 0;
        }
        listedCompanyService.ensureCompaniesInitialized(player);
        ensureBaselineHistory(player);
        int fiscalPeriodIndex = FiscalQuarter.currentPeriodIndex(player) - 1;
        List<ListedCompany> companiesToSettle = listedCompanyRepository.findByPlayer(player).stream()
                .filter(company -> company.getLatestSettledFiscalPeriod() < fiscalPeriodIndex)
                .filter(company -> !reportRepository.existsByListedCompanyAndFiscalPeriodIndex(company, fiscalPeriodIndex))
                .toList();
        if (companiesToSettle.isEmpty()) {
            return 0;
        }
        StockMarketRegimeService.RegimeExposure regimeExposure = marketRegimeService.consumeQuarterExposure(player);
        int settledCount = 0;
        for (ListedCompany company : companiesToSettle) {
            String industry = stockCatalog.find(company.getStockKey()).orElseThrow().industry();
            StockMarketRegimeFinancialPolicy.FinancialImpact regimeImpact =
                    marketRegimeFinancialPolicy.impact(regimeExposure, industry);
            settleCompany(player, company, fiscalPeriodIndex, regimeImpact);
            settledCount++;
        }
        return settledCount;
    }

    @Transactional(readOnly = true)
    public Optional<ListedCompanyQuarterlyReport> latestReport(ListedCompany company) {
        return reportRepository.findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(company);
    }

    @Transactional(readOnly = true)
    public List<ListedCompanyQuarterlyReport> recentReports(ListedCompany company) {
        return reportRepository.findTop4ByListedCompanyOrderByFiscalPeriodIndexDesc(company);
    }

    /**
     * 주식 개방 시점에도 TTM 지표를 계산할 수 있도록 게임 시작 이전 4개 분기를 만든다.
     * 기준 실적은 재무 이벤트나 배당을 발생시키지 않고, 실제 발표가 쌓이면 최근 4분기에서 자연스럽게 밀려난다.
     */
    public void ensureBaselineHistory(Player player) {
        for (ListedCompany company : listedCompanyRepository.findByPlayer(player)) {
            List<ListedCompanyQuarterlyReport> reports = reportRepository
                    .findByListedCompanyOrderByFiscalPeriodIndexDesc(company);
            if (reports.size() < 4) {
                if (reports.isEmpty()) {
                    createBaselineHistory(
                            player, company, 4, company.getLatestSettledFiscalPeriod(), company.getQuarterlyRevenue(),
                            company.getCash(), company.getNonCashAssets(), company.getDebt(),
                            company.getOtherLiabilities(), company.getNetAssets()
                    );
                } else {
                    ListedCompanyQuarterlyReport earliest = reports.getLast();
                    long previousNetAssets = earliest.getEndingNetAssets()
                            - earliest.getNetIncome() + earliest.getTotalDividend();
                    long previousNonCashAssets = earliest.getEndingNonCashAssets()
                            - earliest.getCapitalExpenditure() + earliest.getDepreciation();
                    long previousCash = earliest.getEndingDebt() + earliest.getEndingOtherLiabilities()
                            + previousNetAssets - previousNonCashAssets;
                    long previousRevenue = Math.max(1, earliest.getExpectedRevenue());
                    createBaselineHistory(
                            player, company, 4 - reports.size(), earliest.getFiscalPeriodIndex() - 1, previousRevenue,
                            previousCash, previousNonCashAssets, earliest.getEndingDebt(),
                            earliest.getEndingOtherLiabilities(), previousNetAssets
                    );
                }
            }
            ListedCompanyQuarterlyReport latest = reportRepository
                    .findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(company).orElseThrow();
            valuationService.refresh(player, company, latest.getFiscalPeriodIndex(), latest.isBaselineHistory());
        }
    }

    private void settleCompany(
            Player player,
            ListedCompany company,
            int fiscalPeriodIndex,
            StockMarketRegimeFinancialPolicy.FinancialImpact regimeImpact
    ) {
        ListedCompanyFinancialProfile profile = financialCatalog.require(company.getStockKey());
        Random random = new Random(financialSeed(player, company, fiscalPeriodIndex));
        int noise = random.nextInt(-profile.quarterlyNoiseBasisPoints(), profile.quarterlyNoiseBasisPoints() + 1);
        int industryImpact = company.consumeIndustryRevenueImpactBasisPoints();
        int companyRevenueImpact = company.consumeCompanyRevenueImpactBasisPoints();
        int companyExpenseImpact = company.consumeCompanyOperatingExpenseImpactBasisPoints();
        int revenueGrowthBasisPoints = Math.max(
                -9_000,
                company.getAnnualGrowthBasisPoints() / 4 + noise + industryImpact + companyRevenueImpact
                        + regimeImpact.revenueBasisPoints()
        );
        long revenue = scale(company.getQuarterlyRevenue(), 10_000 + revenueGrowthBasisPoints);
        long grossProfit = scale(revenue, company.getGrossMarginBasisPoints());
        long costOfRevenue = revenue - grossProfit;
        long operatingExpenses = scale(
                revenue,
                Math.max(0, company.getOperatingExpenseBasisPoints() + companyExpenseImpact
                        + regimeImpact.operatingExpenseBasisPoints())
        );
        long operatingProfit = grossProfit - operatingExpenses;
        long interestExpense = scale(company.getDebt(), QUARTERLY_INTEREST_BASIS_POINTS);
        long pretaxIncome = operatingProfit - interestExpense;
        long taxExpense = pretaxIncome > 0 ? scale(pretaxIncome, CORPORATE_TAX_BASIS_POINTS) : 0;
        long netIncome = pretaxIncome - taxExpense;

        // 영업비용에 포함된 감가상각은 현금 유출이 아니므로 현금흐름에서 다시 더한다.
        long depreciation = scale(company.getNonCashAssets(), company.getAnnualDepreciationBasisPoints() / 4);
        long capitalExpenditure = scale(revenue, company.getCapitalExpenditureBasisPoints());
        long endingCash = company.getCash() + netIncome + depreciation - capitalExpenditure;
        long additionalDebt = 0;
        if (endingCash < 0) {
            additionalDebt = -endingCash;
            endingCash = 0;
        }
        long endingNonCashAssets = company.getNonCashAssets() + capitalExpenditure - depreciation;
        long endingDebt = company.getDebt() + additionalDebt;
        long endingOtherLiabilities = company.getOtherLiabilities();
        long endingNetAssets = company.getNetAssets() + netIncome;
        long desiredDividend = netIncome > 0 ? scale(netIncome, company.getDividendPayoutBasisPoints()) : 0;
        long dividendPerShare = Math.min(
                desiredDividend / company.getIssuedShares(),
                endingCash / company.getIssuedShares()
        );
        long totalDividend = Math.multiplyExact(dividendPerShare, company.getIssuedShares());
        endingCash -= totalDividend;
        endingNetAssets -= totalDividend;
        long earningsPerShare = netIncome / company.getIssuedShares();
        int earningsSurpriseBasisPoints = ratioBasisPoints(netIncome - company.getExpectedNetIncome(), Math.abs(company.getExpectedNetIncome()));

        long nextExpectedRevenue = scale(revenue, 10_000 + company.getAnnualGrowthBasisPoints() / 4);
        long nextExpectedNetIncome = expectedNetIncome(nextExpectedRevenue, company, endingDebt);
        int nextEarningsElapsedDay = FiscalQuarter.nextQuarterStartElapsedDay(player);

        reportRepository.save(new ListedCompanyQuarterlyReport(
                company,
                fiscalPeriodIndex,
                FiscalQuarter.fiscalYear(fiscalPeriodIndex),
                FiscalQuarter.quarter(fiscalPeriodIndex),
                player.getElapsedDays(),
                company.getExpectedRevenue(),
                company.getExpectedNetIncome(),
                revenue,
                costOfRevenue,
                operatingExpenses,
                operatingProfit,
                interestExpense,
                taxExpense,
                netIncome,
                capitalExpenditure,
                depreciation,
                endingCash,
                endingNonCashAssets,
                endingDebt,
                endingOtherLiabilities,
                endingNetAssets,
                earningsPerShare,
                revenueGrowthBasisPoints,
                earningsSurpriseBasisPoints,
                totalDividend,
                dividendPerShare,
                false
        ));
        company.applyQuarterlySettlement(
                fiscalPeriodIndex,
                revenue,
                nextExpectedRevenue,
                nextExpectedNetIncome,
                endingCash,
                endingNonCashAssets,
                endingDebt,
                endingOtherLiabilities,
                endingNetAssets,
                nextEarningsElapsedDay
        );
        valuationService.refresh(player, company, fiscalPeriodIndex, false);
        company.registerEarningsImpact(fiscalPeriodIndex, earningsSurpriseBasisPoints);
        stockDividendService.payPersonalDividend(player, company, dividendPerShare);
    }

    private void createBaselineHistory(
            Player player,
            ListedCompany company,
            int count,
            int lastPeriod,
            long latestRevenue,
            long latestCash,
            long latestNonCashAssets,
            long latestDebt,
            long latestOtherLiabilities,
            long latestNetAssets
    ) {
        ListedCompanyFinancialProfile profile = financialCatalog.require(company.getStockKey());
        List<Long> revenues = new ArrayList<>();
        long revenue = latestRevenue;
        revenues.add(revenue);
        for (int index = 0; index < count - 1; index++) {
            Random random = new Random(financialSeed(player, company, lastPeriod - index));
            int noise = random.nextInt(-profile.quarterlyNoiseBasisPoints(), profile.quarterlyNoiseBasisPoints() + 1);
            int growth = Math.max(-9_000, company.getAnnualGrowthBasisPoints() / 4 + noise);
            revenue = Math.max(1, scale(revenue, 10_000) * 10_000 / Math.max(1, 10_000 + growth));
            revenues.add(revenue);
        }
        Collections.reverse(revenues);

        List<BaselineResult> reversed = new ArrayList<>();
        long endingCash = latestCash;
        long endingNonCash = latestNonCashAssets;
        long endingDebt = latestDebt;
        long endingOtherLiabilities = latestOtherLiabilities;
        long endingNetAssets = latestNetAssets;
        for (int index = count - 1; index >= 0; index--) {
            long periodRevenue = revenues.get(index);
            long grossProfit = scale(periodRevenue, company.getGrossMarginBasisPoints());
            long costOfRevenue = periodRevenue - grossProfit;
            long operatingExpenses = scale(periodRevenue, company.getOperatingExpenseBasisPoints());
            long operatingProfit = grossProfit - operatingExpenses;
            long interestExpense = scale(endingDebt, QUARTERLY_INTEREST_BASIS_POINTS);
            long pretaxIncome = operatingProfit - interestExpense;
            long taxExpense = pretaxIncome > 0 ? scale(pretaxIncome, CORPORATE_TAX_BASIS_POINTS) : 0;
            long netIncome = pretaxIncome - taxExpense;
            long depreciation = scale(endingNonCash, company.getAnnualDepreciationBasisPoints() / 4);
            long capitalExpenditure = scale(periodRevenue, company.getCapitalExpenditureBasisPoints());

            reversed.add(new BaselineResult(
                    lastPeriod - (count - 1 - index), periodRevenue, costOfRevenue, operatingExpenses,
                    operatingProfit, interestExpense, taxExpense, netIncome, capitalExpenditure,
                    depreciation, endingCash, endingNonCash, endingDebt, endingOtherLiabilities,
                    endingNetAssets
            ));
            endingNetAssets -= netIncome;
            endingNonCash = endingNonCash - capitalExpenditure + depreciation;
            endingCash = endingDebt + endingOtherLiabilities + endingNetAssets - endingNonCash;
        }
        Collections.reverse(reversed);

        long previousRevenue = reversed.get(0).revenue();
        for (BaselineResult result : reversed) {
            long expectedRevenue = result == reversed.get(0)
                    ? result.revenue()
                    : scale(previousRevenue, 10_000 + company.getAnnualGrowthBasisPoints() / 4);
            long expectedNetIncome = expectedNetIncome(expectedRevenue, company, result.endingDebt());
            int revenueGrowth = ratioBasisPoints(result.revenue() - previousRevenue, previousRevenue);
            int surprise = ratioBasisPoints(result.netIncome() - expectedNetIncome, Math.abs(expectedNetIncome));
            reportRepository.save(new ListedCompanyQuarterlyReport(
                    company,
                    result.period(),
                    FiscalQuarter.fiscalYear(result.period()),
                    FiscalQuarter.quarter(result.period()),
                    0,
                    expectedRevenue,
                    expectedNetIncome,
                    result.revenue(),
                    result.costOfRevenue(),
                    result.operatingExpenses(),
                    result.operatingProfit(),
                    result.interestExpense(),
                    result.taxExpense(),
                    result.netIncome(),
                    result.capitalExpenditure(),
                    result.depreciation(),
                    result.endingCash(),
                    result.endingNonCashAssets(),
                    result.endingDebt(),
                    result.endingOtherLiabilities(),
                    result.endingNetAssets(),
                    result.netIncome() / company.getIssuedShares(),
                    revenueGrowth,
                    surprise,
                    0,
                    0,
                    true
            ));
            previousRevenue = result.revenue();
        }
    }

    private long expectedNetIncome(long revenue, ListedCompany company, long debt) {
        return expectedNetIncome(revenue, company, debt, 0);
    }

    private long expectedNetIncome(long revenue, ListedCompany company, long debt, int operatingExpenseImpactBasisPoints) {
        long grossProfit = scale(revenue, company.getGrossMarginBasisPoints());
        long operatingExpenses = scale(
                revenue,
                Math.max(0, company.getOperatingExpenseBasisPoints() + operatingExpenseImpactBasisPoints)
        );
        long operatingProfit = grossProfit - operatingExpenses;
        long interestExpense = scale(debt, QUARTERLY_INTEREST_BASIS_POINTS);
        long pretaxIncome = operatingProfit - interestExpense;
        long taxExpense = pretaxIncome > 0 ? scale(pretaxIncome, CORPORATE_TAX_BASIS_POINTS) : 0;
        return pretaxIncome - taxExpense;
    }

    private long financialSeed(Player player, ListedCompany company, int fiscalPeriodIndex) {
        long seed = player.getId() == null ? 0 : player.getId();
        seed = seed * 31 + company.getStockKey().hashCode();
        return seed * 31 + fiscalPeriodIndex;
    }

    private long scale(long amount, int basisPoints) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(basisPoints))
                .divide(BigInteger.valueOf(10_000))
                .longValueExact();
    }

    private int ratioBasisPoints(long numerator, long denominator) {
        if (denominator == 0) {
            return numerator == 0 ? 0 : numerator > 0 ? 10_000 : -10_000;
        }
        long ratio = BigInteger.valueOf(numerator)
                .multiply(BigInteger.valueOf(10_000))
                .divide(BigInteger.valueOf(denominator))
                .longValue();
        return (int) Math.max(-100_000, Math.min(100_000, ratio));
    }

    private record BaselineResult(
            int period,
            long revenue,
            long costOfRevenue,
            long operatingExpenses,
            long operatingProfit,
            long interestExpense,
            long taxExpense,
            long netIncome,
            long capitalExpenditure,
            long depreciation,
            long endingCash,
            long endingNonCashAssets,
            long endingDebt,
            long endingOtherLiabilities,
            long endingNetAssets
    ) {
    }
}
