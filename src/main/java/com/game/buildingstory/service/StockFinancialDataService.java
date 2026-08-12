package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyBond;
import com.game.buildingstory.domain.CompanyBondStatus;
import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.GameCalendar;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyBondRepository;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

/**
 * NPC 재무와 플레이어 기업의 실제 재무를 주식 가격·화면이 공유하는 형태로 제공한다.
 * 분기 계산 규칙을 StockService에 분산시키지 않기 위한 변환 경계다.
 */
@Service
@Transactional(readOnly = true)
public class StockFinancialDataService {
    private static final int PLAYER_COMPANY_FAIR_VALUE_BAND_BASIS_POINTS = 2_200;
    private static final int MAX_QUARTER_IMPACT_BASIS_POINTS = 1_500;

    private final ListedCompanyFinancialService npcFinancialService;
    private final ListedCompanyValuationService npcValuationService;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final CompanyListingRepository listingRepository;
    private final CompanyQuarterlyReportRepository quarterlyReportRepository;
    private final CompanyValuationSnapshotRepository valuationRepository;
    private final CompanyBondRepository bondRepository;

    public StockFinancialDataService(
            ListedCompanyFinancialService npcFinancialService,
            ListedCompanyValuationService npcValuationService,
            PlayerCompanyRepository playerCompanyRepository,
            CompanyListingRepository listingRepository,
            CompanyQuarterlyReportRepository quarterlyReportRepository,
            CompanyValuationSnapshotRepository valuationRepository,
            CompanyBondRepository bondRepository
    ) {
        this.npcFinancialService = npcFinancialService;
        this.npcValuationService = npcValuationService;
        this.playerCompanyRepository = playerCompanyRepository;
        this.listingRepository = listingRepository;
        this.quarterlyReportRepository = quarterlyReportRepository;
        this.valuationRepository = valuationRepository;
        this.bondRepository = bondRepository;
    }

    public StockFinancialSnapshot snapshot(Player player, StockSpec stock, ListedCompany listedCompany) {
        if (CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY.equals(stock.key())) {
            return playerCompanySnapshot(player, stock);
        }
        return npcSnapshot(listedCompany);
    }

    /** 가격 갱신에 사용할 적정가와 아직 반영하지 않은 분기 충격을 함께 반환한다. */
    @Transactional
    public PriceSignal consumePriceSignal(Player player, StockSpec stock, ListedCompany listedCompany) {
        if (!CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY.equals(stock.key())) {
            long fairValue = npcValuationService.latest(listedCompany)
                    .map(value -> value.getFairValueBase())
                    .orElse(0L);
            return new PriceSignal(fairValue, listedCompany.consumePendingEarningsImpactBasisPoints());
        }

        PlayerCompany company = requirePlayerCompany(player);
        CompanyListing listing = listingRepository.findByCompany(company).orElseThrow();
        List<CompanyQuarterlyReport> reports = quarterlyReportRepository
                .findByCompanyOrderByQuarterSequenceDesc(company);
        int impact = 0;
        if (!reports.isEmpty() && listing.markQuarterPriced(reports.get(0).getQuarterSequence())) {
            impact = playerEarningsSurpriseBasisPoints(reports);
        }
        CompanyValuationSnapshot valuation = valuationRepository
                .findFirstByCompanyOrderByQuarterSequenceDesc(company).orElse(null);
        long fairValue = playerCompanyValue(valuation, listing)
                / Math.max(1, company.getIssuedShares());
        return new PriceSignal(fairValue, impact);
    }

    public List<StockFinancialSnapshot.Quarter> reportEvents(
            Player player,
            StockSpec stock,
            ListedCompany listedCompany
    ) {
        if (!CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY.equals(stock.key())) {
            return npcFinancialService.recentReports(listedCompany).stream()
                    .filter(report -> !report.isBaselineHistory())
                    .map(this::npcQuarter)
                    .toList();
        }
        PlayerCompany company = requirePlayerCompany(player);
        CompanyListing listing = listingRepository.findByCompany(company).orElseThrow();
        int listedDay = listing.getListedElapsedDay() == null ? 0 : listing.getListedElapsedDay();
        int dividendSequenceAtListing = listing.getLastDividendQuarterSequenceAtListing();
        List<CompanyQuarterlyReport> reports = quarterlyReportRepository
                .findByCompanyOrderByQuarterSequenceDesc(company);
        return reports.stream()
                .map(report -> playerQuarterEvent(
                        report, reports, player, listedDay, dividendSequenceAtListing))
                .filter(quarter -> quarter.publishedElapsedDay() > 0
                        || quarter.dividendElapsedDay() > 0)
                .toList();
    }

    private StockFinancialSnapshot npcSnapshot(ListedCompany company) {
        var latestReport = npcFinancialService.latestReport(company);
        var valuation = npcValuationService.latest(company);
        List<StockFinancialSnapshot.Quarter> quarters = npcFinancialService.recentReports(company).stream()
                .map(this::npcQuarter)
                .toList();
        long revenue = latestReport.map(ListedCompanyQuarterlyReport::getRevenue)
                .orElse(company.getExpectedRevenue());
        long netIncome = latestReport.map(ListedCompanyQuarterlyReport::getNetIncome)
                .orElse(company.getExpectedNetIncome());
        return new StockFinancialSnapshot(
                latestReport.isPresent(),
                latestReport.map(report -> report.isBaselineHistory()
                        ? "기준 실적"
                        : npcPeriodText(report)).orElse("첫 분기 전망"),
                revenue,
                latestReport.map(ListedCompanyQuarterlyReport::getOperatingProfit).orElse(0L),
                netIncome,
                latestReport.map(ListedCompanyQuarterlyReport::getEarningsSurpriseBasisPoints).orElse(0),
                latestReport.filter(report -> !report.isBaselineHistory())
                        .map(ListedCompanyQuarterlyReport::getDividendPerShare).orElse(0L),
                company.getDividendPayoutBasisPoints(),
                company.getNextEarningsElapsedDay(),
                valuation.map(value -> value.getFairValueLower()).orElse(0L),
                valuation.map(value -> value.getFairValueBase()).orElse(0L),
                valuation.map(value -> value.getFairValueUpper()).orElse(0L),
                company.getCash(),
                company.getDebt(),
                company.getNetAssets(),
                valuation.map(value -> value.getEarningsPerShare()).orElse(0L),
                valuation.map(value -> value.getBookValuePerShare()).orElse(0L),
                quarters
        );
    }

    private StockFinancialSnapshot playerCompanySnapshot(Player player, StockSpec stock) {
        PlayerCompany company = requirePlayerCompany(player);
        List<CompanyQuarterlyReport> reports = quarterlyReportRepository
                .findByCompanyOrderByQuarterSequenceDesc(company);
        CompanyQuarterlyReport latest = reports.isEmpty() ? null : reports.get(0);
        CompanyValuationSnapshot valuation = valuationRepository
                .findFirstByCompanyOrderByQuarterSequenceDesc(company).orElse(null);
        CompanyListing listing = listingRepository.findByCompany(company).orElseThrow();
        long shares = Math.max(1, stock.issuedShares());
        long fairBase = playerCompanyValue(valuation, listing) / shares;
        long netAssets = valuation == null ? 0 : valuation.getAssetValue();
        long ttmNetIncome = reports.stream().limit(4).mapToLong(CompanyQuarterlyReport::getNetIncome).sum();
        int payoutRate = latest == null || !latest.isDividendDecided() ? 0 : latest.getDividendRate();
        long dividendPerShare = latest == null ? 0 : latest.getDividendAmount() / shares;
        return new StockFinancialSnapshot(
                latest != null,
                latest == null ? "첫 분기 전망" : playerPeriodText(latest, reports, player),
                latest == null ? company.getMonthlyRecurringRevenue() * 3 : latest.getRevenue(),
                latest == null ? 0 : latest.getOperatingProfit(),
                latest == null ? 0 : latest.getNetIncome(),
                playerEarningsSurpriseBasisPoints(reports),
                dividendPerShare,
                payoutRate * 100,
                FiscalQuarter.nextQuarterStartElapsedDay(player),
                scale(fairBase, 10_000 - PLAYER_COMPANY_FAIR_VALUE_BAND_BASIS_POINTS),
                fairBase,
                scale(fairBase, 10_000 + PLAYER_COMPANY_FAIR_VALUE_BAND_BASIS_POINTS),
                company.getCorporateCash(),
                outstandingDebt(company),
                netAssets,
                ttmNetIncome / shares,
                netAssets / shares,
                reports.stream().limit(4).map(report -> playerQuarter(report, reports, player)).toList()
        );
    }

    private StockFinancialSnapshot.Quarter npcQuarter(ListedCompanyQuarterlyReport report) {
        return new StockFinancialSnapshot.Quarter(
                npcPeriodText(report), report.getRevenue(), report.getOperatingProfit(), report.getNetIncome(),
                report.getEarningsSurpriseBasisPoints(), report.getPublishedElapsedDay(),
                report.getPublishedElapsedDay(), report.getDividendPerShare());
    }

    private StockFinancialSnapshot.Quarter playerQuarter(
            CompanyQuarterlyReport report,
            List<CompanyQuarterlyReport> reports,
            Player player
    ) {
        int index = reports.indexOf(report);
        int change = index >= 0 && index + 1 < reports.size()
                ? relativeChangeBasisPoints(report.getOperatingProfit(), reports.get(index + 1).getOperatingProfit())
                : 0;
        return new StockFinancialSnapshot.Quarter(
                playerPeriodText(report, reports, player), report.getRevenue(), report.getOperatingProfit(), report.getNetIncome(),
                change, report.getPublishedElapsedDay(), report.getDividendDecidedElapsedDay(),
                report.getDividendAmount() / Math.max(1, report.getCompany().getIssuedShares()));
    }

    /** 상장일과 같은 날 결정된 신규 배당도 표시하되, 상장 전에 이미 결정된 배당은 제외한다. */
    private StockFinancialSnapshot.Quarter playerQuarterEvent(
            CompanyQuarterlyReport report,
            List<CompanyQuarterlyReport> reports,
            Player player,
            int listedDay,
            int dividendSequenceAtListing
    ) {
        StockFinancialSnapshot.Quarter quarter = playerQuarter(report, reports, player);
        int publishedDay = report.getPublishedElapsedDay() > listedDay
                ? report.getPublishedElapsedDay()
                : 0;
        boolean dividendAfterListing = report.getDividendDecidedElapsedDay() > listedDay
                || (report.getDividendDecidedElapsedDay() == listedDay
                && report.getQuarterSequence() > dividendSequenceAtListing);
        int dividendDay = dividendAfterListing ? report.getDividendDecidedElapsedDay() : 0;
        return new StockFinancialSnapshot.Quarter(
                quarter.periodText(), quarter.revenue(), quarter.operatingProfit(), quarter.netIncome(),
                quarter.performanceBasisPoints(), publishedDay, dividendDay, quarter.dividendPerShare());
    }

    private int playerEarningsSurpriseBasisPoints(List<CompanyQuarterlyReport> reports) {
        if (reports.size() < 2) {
            return 0;
        }
        CompanyQuarterlyReport actual = reports.get(0);
        CompanyQuarterlyReport previous = reports.get(1);
        long expectedRevenue = previous.getRecurringRevenueAtEnd() > 0
                ? Math.multiplyExact(previous.getRecurringRevenueAtEnd(), 3)
                : previous.getRevenue();
        long expectedOperatingProfit = previous.getRevenue() == 0
                ? previous.getOperatingProfit()
                : BigInteger.valueOf(expectedRevenue)
                        .multiply(BigInteger.valueOf(previous.getOperatingProfit()))
                        .divide(BigInteger.valueOf(previous.getRevenue()))
                        .longValue();
        int revenueSurprise = relativeChangeBasisPoints(actual.getRevenue(), expectedRevenue);
        int profitSurprise = relativeChangeBasisPoints(actual.getOperatingProfit(), expectedOperatingProfit);
        return (int) Math.max(-MAX_QUARTER_IMPACT_BASIS_POINTS, Math.min(
                MAX_QUARTER_IMPACT_BASIS_POINTS,
                revenueSurprise * 4L / 10 + profitSurprise * 6L / 10
        ));
    }

    private int relativeChangeBasisPoints(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 1_000 : current < 0 ? -1_000 : 0;
        }
        long basisPoints = BigInteger.valueOf(current - previous)
                .multiply(BigInteger.valueOf(10_000))
                .divide(BigInteger.valueOf(Math.abs(previous)))
                .longValue();
        return (int) Math.max(-MAX_QUARTER_IMPACT_BASIS_POINTS,
                Math.min(MAX_QUARTER_IMPACT_BASIS_POINTS, basisPoints));
    }

    private String npcPeriodText(ListedCompanyQuarterlyReport report) {
        return report.isBaselineHistory()
                ? "기준 " + report.getFiscalQuarter() + "분기"
                : report.getFiscalYear() + "년 " + report.getFiscalQuarter() + "분기 확정";
    }

    private String playerPeriodText(
            CompanyQuarterlyReport report,
            List<CompanyQuarterlyReport> reports,
        Player player
    ) {
        int currentMonthIndex = (player.getYear() - 1) * 12 + player.getMonth() - 1;
        CompanyQuarterlyReport latest = reports.getFirst();
        if (latest.getEndingPeriodIndex() <= currentMonthIndex) {
            return GameCalendar.quarterTextFromMonthIndex(report.getEndingPeriodIndex()) + " 확정";
        }

        // 초기 테스트 데이터처럼 미래 월 인덱스를 가진 보고서는 최신 발행 분기를 기준으로 역산한다.
        int latestMonthIndex = latest.getPublishedElapsedDay() > 0
                ? (GameCalendar.year(latest.getPublishedElapsedDay()) - 1) * 12
                        + GameCalendar.month(latest.getPublishedElapsedDay()) - 1
                : currentMonthIndex;
        int quarterGap = Math.max(0, latest.getQuarterSequence() - report.getQuarterSequence());
        return GameCalendar.quarterTextFromMonthIndex(latestMonthIndex - quarterGap * 3) + " 확정";
    }

    private long outstandingDebt(PlayerCompany company) {
        long bondDebt = bondRepository.findByCompanyAndStatusInOrderByIdAsc(
                        company, List.of(CompanyBondStatus.ACTIVE, CompanyBondStatus.DEFAULTED)).stream()
                .mapToLong(CompanyBond::getPrincipal)
                .sum();
        long unpaidCosts = Math.addExact(
                company.getUnpaidOperatingAmount(),
                company.getUnpaidBondInterestAmount()
        );
        return Math.addExact(bondDebt, unpaidCosts);
    }

    /** 새 분기 가치평가 전에는 공모대금이 포함된 상장 직후 기업가치를 기준으로 삼는다. */
    private long playerCompanyValue(CompanyValuationSnapshot valuation, CompanyListing listing) {
        if (valuation == null || valuation.getQuarterSequence() <= listing.getQuarterSequenceAtListing()) {
            return Math.addExact(listing.getValuationAtListing(), listing.getProceeds());
        }
        return valuation.getEnterpriseValue();
    }

    private PlayerCompany requirePlayerCompany(Player player) {
        return playerCompanyRepository.findByPlayer(player).orElseThrow();
    }

    private long scale(long value, int basisPoints) {
        if (value <= 0) {
            return 0;
        }
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(basisPoints))
                .divide(BigInteger.valueOf(10_000)).longValueExact();
    }

    public record PriceSignal(long fairValue, int earningsImpactBasisPoints) {
    }
}
