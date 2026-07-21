package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.ListedCompanyValuationSnapshot;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyValuationSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/** 최근 4개 분기 실적만 사용해 TTM 지표와 적정가 범위를 확정한다. */
@Service
@Transactional
public class ListedCompanyValuationService {
    private final ListedCompanyQuarterlyReportRepository reportRepository;
    private final ListedCompanyValuationSnapshotRepository snapshotRepository;
    private final ListedCompanyValuationCatalog valuationCatalog;
    private final StockCatalog stockCatalog;

    public ListedCompanyValuationService(
            ListedCompanyQuarterlyReportRepository reportRepository,
            ListedCompanyValuationSnapshotRepository snapshotRepository,
            ListedCompanyValuationCatalog valuationCatalog,
            StockCatalog stockCatalog
    ) {
        this.reportRepository = reportRepository;
        this.snapshotRepository = snapshotRepository;
        this.valuationCatalog = valuationCatalog;
        this.stockCatalog = stockCatalog;
    }

    public ListedCompanyValuationSnapshot refresh(Player player, ListedCompany company, int fiscalPeriodIndex, boolean baseline) {
        Optional<ListedCompanyValuationSnapshot> existing =
                snapshotRepository.findByListedCompanyAndFiscalPeriodIndex(company, fiscalPeriodIndex);
        if (existing.isPresent()) {
            return existing.get();
        }
        List<ListedCompanyQuarterlyReport> reports = reportRepository
                .findTop4ByListedCompanyOrderByFiscalPeriodIndexDesc(company);
        if (reports.size() < 4) {
            throw new IllegalStateException("적정가 계산에는 최근 4개 분기 실적이 필요합니다.");
        }
        long revenue = reports.stream().mapToLong(ListedCompanyQuarterlyReport::getRevenue).sum();
        long operatingProfit = reports.stream().mapToLong(ListedCompanyQuarterlyReport::getOperatingProfit).sum();
        long netIncome = reports.stream().mapToLong(ListedCompanyQuarterlyReport::getNetIncome).sum();
        long shares = company.getIssuedShares();
        long eps = netIncome / shares;
        long bps = Math.max(0, reports.get(0).getEndingNetAssets() / shares);
        long sps = revenue / shares;

        StockSpec stock = stockCatalog.find(company.getStockKey()).orElseThrow();
        ListedCompanyValuationRule rule = valuationCatalog.require(stock.industry());
        int earningsWeight = rule.earningsWeightBasisPoints();
        int bookWeight = rule.bookWeightBasisPoints();
        int salesWeight = rule.salesWeightBasisPoints();
        if (eps <= 0) {
            bookWeight += earningsWeight * 4 / 10;
            salesWeight += earningsWeight - earningsWeight * 4 / 10;
            earningsWeight = 0;
        }

        long earningsValue = eps > 0 ? multiplyDivide(eps, rule.perHundredths(), 100) : 0;
        long bookValue = multiplyDivide(bps, rule.pbrHundredths(), 100);
        long salesValue = multiplyDivide(sps, rule.psrHundredths(), 100);
        long weightedValue = multiplyDivide(earningsValue, earningsWeight, 10_000)
                + multiplyDivide(bookValue, bookWeight, 10_000)
                + multiplyDivide(salesValue, salesWeight, 10_000);

        long assets = Math.max(1, company.getCash() + company.getNonCashAssets());
        int debtDiscount = (int) Math.min(2_000, multiplyDivide(company.getDebt(), 1_500, assets));
        long fairBase = Math.max(1, multiplyDivide(weightedValue, 10_000 - debtDiscount, 10_000));
        int band = switch (stock.riskType()) {
            case SAFE -> 1_000;
            case NORMAL -> 1_500;
            case AGGRESSIVE -> 2_200;
        };
        ListedCompanyValuationSnapshot snapshot = new ListedCompanyValuationSnapshot(
                company,
                fiscalPeriodIndex,
                player.getElapsedDays(),
                revenue,
                operatingProfit,
                netIncome,
                eps,
                bps,
                sps,
                multiplyDivide(fairBase, 10_000 - band, 10_000),
                fairBase,
                multiplyDivide(fairBase, 10_000 + band, 10_000),
                debtDiscount,
                baseline
        );
        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<ListedCompanyValuationSnapshot> latest(ListedCompany company) {
        return snapshotRepository.findFirstByListedCompanyOrderByFiscalPeriodIndexDesc(company);
    }

    private long multiplyDivide(long value, long multiplier, long divisor) {
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier))
                .divide(BigInteger.valueOf(divisor)).longValueExact();
    }
}
