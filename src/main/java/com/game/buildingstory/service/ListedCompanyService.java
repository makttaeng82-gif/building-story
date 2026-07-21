package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상장기업의 영속 상태와 주주별 주식 수량을 관리한다.
 *
 * <p>주가 계산과 매매 회계는 StockService가 담당한다. 이 서비스는 누가 몇 주를 보유하는지와
 * 모든 주주의 수량 합계가 발행주식 수와 일치하는지만 책임진다.</p>
 */
@Service
@Transactional
public class ListedCompanyService {
    private final StockCatalog stockCatalog;
    private final ListedCompanyFinancialCatalog financialCatalog;
    private final ListedCompanyRepository listedCompanyRepository;
    private final OwnedStockRepository ownedStockRepository;

    public ListedCompanyService(
            StockCatalog stockCatalog,
            ListedCompanyFinancialCatalog financialCatalog,
            ListedCompanyRepository listedCompanyRepository,
            OwnedStockRepository ownedStockRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.financialCatalog = financialCatalog;
        this.listedCompanyRepository = listedCompanyRepository;
        this.ownedStockRepository = ownedStockRepository;
    }

    /** 기존 기업은 유지하고, 카탈로그에는 있지만 아직 없는 기업만 생성한다. */
    public void ensureCompaniesInitialized(Player player) {
        Map<String, ListedCompany> companies = companiesByStockKey(player);
        Map<String, Long> personalShares = ownedStockRepository.findByPlayer(player).stream()
                .collect(Collectors.toMap(OwnedStock::getStockKey, OwnedStock::getQuantity));

        stockCatalog.all().stream()
                .filter(stock -> !companies.containsKey(stock.key()))
                .map(stock -> createInitialCompany(player, stock, personalShares.getOrDefault(stock.key(), 0L)))
                .forEach(listedCompanyRepository::save);

        listedCompanyRepository.findByPlayer(player).stream()
                .filter(company -> !company.isFinancialInitialized())
                .forEach(company -> initializeFinancialState(player, company));
    }

    @Transactional(readOnly = true)
    public Map<String, ListedCompany> companiesByStockKey(Player player) {
        return listedCompanyRepository.findByPlayer(player).stream()
                .collect(Collectors.toMap(ListedCompany::getStockKey, Function.identity()));
    }

    public ListedCompany requireCompany(Player player, String stockKey) {
        return listedCompanyRepository.findByPlayerAndStockKey(player, stockKey).orElseThrow();
    }

    private ListedCompany createInitialCompany(Player player, StockSpec stock, long personalPlayerShares) {
        int marketPercent = switch (stock.riskType()) {
            case SAFE -> 40;
            case NORMAL -> 50;
            case AGGRESSIVE -> 65;
        };
        int institutionalPercent = switch (stock.riskType()) {
            case SAFE, NORMAL -> 20;
            case AGGRESSIVE -> 15;
        };

        long institutionalShares = stock.issuedShares() * institutionalPercent / 100;
        long initialMarketShares = stock.issuedShares() * marketPercent / 100;
        long founderShares = stock.issuedShares() - institutionalShares - initialMarketShares;
        long remainingMarketShares = initialMarketShares - personalPlayerShares;
        ListedCompany company = new ListedCompany(
                player,
                stock.key(),
                stock.issuedShares(),
                founderShares,
                institutionalShares,
                remainingMarketShares,
                0,
                personalPlayerShares
        );
        initializeFinancialState(player, company);
        return company;
    }

    private void initializeFinancialState(Player player, ListedCompany company) {
        ListedCompanyFinancialProfile profile = financialCatalog.require(company.getStockKey());
        long expectedNetIncome = expectedNetIncome(profile.quarterlyRevenue(), profile, profile.debt());
        company.initializeFinancialState(
                profile.quarterlyRevenue(),
                profile.quarterlyRevenue(),
                expectedNetIncome,
                profile.cash(),
                profile.nonCashAssets(),
                profile.debt(),
                profile.otherLiabilities(),
                profile.annualGrowthBasisPoints(),
                profile.grossMarginBasisPoints(),
                profile.operatingExpenseBasisPoints(),
                profile.capitalExpenditureBasisPoints(),
                profile.annualDepreciationBasisPoints(),
                profile.dividendPayoutBasisPoints(),
                FiscalQuarter.currentPeriodIndex(player) - 1,
                FiscalQuarter.nextQuarterStartElapsedDay(player)
        );
    }

    private long expectedNetIncome(long revenue, ListedCompanyFinancialProfile profile, long debt) {
        long grossProfit = scale(revenue, profile.grossMarginBasisPoints());
        long operatingExpenses = scale(revenue, profile.operatingExpenseBasisPoints());
        long operatingProfit = grossProfit - operatingExpenses;
        long quarterlyInterest = scale(debt, 100);
        long pretaxIncome = operatingProfit - quarterlyInterest;
        long tax = pretaxIncome > 0 ? scale(pretaxIncome, 2_200) : 0;
        return pretaxIncome - tax;
    }

    private long scale(long amount, int basisPoints) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(basisPoints))
                .divide(BigInteger.valueOf(10_000))
                .longValueExact();
    }
}
