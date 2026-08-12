package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyBond;
import com.game.buildingstory.domain.CompanyBondStatus;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyMonthlySettlement;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.CompanyValuationSnapshot;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyBondRepository;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyComputeConstructionRepository;
import com.game.buildingstory.repo.CompanyCustomerContractRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyServiceIncidentRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CompanyFinanceService {
    private static final Set<Integer> DIVIDEND_RATES = Set.of(0, 10, 25, 50);
    private static final Set<Integer> BOND_ISSUE_OPTIONS = Set.of(5, 10);

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyQuarterlyReportRepository quarterlyRepository;
    private final CompanyValuationSnapshotRepository valuationRepository;
    private final CompanyBondRepository bondRepository;
    private final CompanyComputeConstructionRepository constructionRepository;
    private final CompanyCompetitorRepository competitorRepository;
    private final CompanyCustomerContractRepository contractRepository;
    private final CompanyServiceIncidentRepository incidentRepository;
    private final CompanyCashLedgerService cashLedgerService;
    private final PlayerCompanyStockDividendService stockDividendService;
    private final CompanyNewsService newsService;
    private final CompanyListingBenefitService listingBenefitService;
    private final ListedCompanyRepository listedCompanyRepository;

    public CompanyFinanceService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyQuarterlyReportRepository quarterlyRepository,
            CompanyValuationSnapshotRepository valuationRepository,
            CompanyBondRepository bondRepository,
            CompanyComputeConstructionRepository constructionRepository,
            CompanyCompetitorRepository competitorRepository,
            CompanyCustomerContractRepository contractRepository,
            CompanyServiceIncidentRepository incidentRepository,
            CompanyCashLedgerService cashLedgerService,
            PlayerCompanyStockDividendService stockDividendService,
            CompanyNewsService newsService,
            CompanyListingBenefitService listingBenefitService,
            ListedCompanyRepository listedCompanyRepository
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.quarterlyRepository = quarterlyRepository;
        this.valuationRepository = valuationRepository;
        this.bondRepository = bondRepository;
        this.constructionRepository = constructionRepository;
        this.competitorRepository = competitorRepository;
        this.contractRepository = contractRepository;
        this.incidentRepository = incidentRepository;
        this.cashLedgerService = cashLedgerService;
        this.stockDividendService = stockDividendService;
        this.newsService = newsService;
        this.listingBenefitService = listingBenefitService;
        this.listedCompanyRepository = listedCompanyRepository;
    }

    @Transactional
    public CompanyValuationSnapshot recordQuarterlyValuation(
            PlayerCompany company,
            CompanyQuarterlyReport report,
            List<CompanyMonthlySettlement> recentSettlements,
            long essentialMonthlyCost
    ) {
        var existing = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company);
        if (existing.isPresent() && existing.get().getQuarterSequence() == report.getQuarterSequence()) {
            return existing.get();
        }

        long unpaidNonPrincipal = Math.addExact(
                company.getUnpaidOperatingAmount(), company.getUnpaidBondInterestAmount());
        long debt = Math.addExact(outstandingPrincipal(company), unpaidNonPrincipal);
        long completedComputeInvestment = constructionRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(construction -> construction.getStatus() == CompanyComputeConstructionStatus.COMPLETED)
                .mapToLong(construction -> construction.getTotalCost() * 60 / 100)
                .sum();
        long completedProductInvestment = company.getCommercializationAccumulatedCost() * 50 / 100;
        long assetValue = Math.max(0, company.getCorporateCash()
                + completedComputeInvestment + completedProductInvestment - debt);

        long annualRecurringRevenue = Math.max(0, report.getRecurringRevenueAtEnd() * 12);
        long annualizedOperatingProfit = annualizedOperatingProfit(recentSettlements);
        int revenueMultiple = revenueMultiple(company, report);
        long salesValue = annualRecurringRevenue * revenueMultiple;
        long profitValue = Math.max(0, annualizedOperatingProfit) * 16;
        long baseBusinessValue = Math.max(salesValue, profitValue);
        int adjustmentBasisPoints = adjustmentBasisPoints(
                company, report, essentialMonthlyCost);
        long adjustedBusinessValue = multiplyBasisPoints(baseBusinessValue, adjustmentBasisPoints);
        long incomeValue = Math.max(0, adjustedBusinessValue + company.getCorporateCash() - debt);
        long enterpriseValue = Math.max(assetValue, incomeValue);

        return valuationRepository.save(new CompanyValuationSnapshot(
                company,
                report.getQuarterSequence(),
                assetValue,
                incomeValue,
                enterpriseValue,
                annualRecurringRevenue,
                annualizedOperatingProfit,
                revenueMultiple,
                adjustmentBasisPoints,
                debt
        ));
    }

    @Transactional
    public String decideDividend(long playerId, int rate, long essentialMonthlyCost) {
        if (!DIVIDEND_RATES.contains(rate)) {
            return "배당성향은 0%, 10%, 25%, 50% 중에서 선택해야 합니다.";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        List<CompanyQuarterlyReport> reports =
                quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        if (reports.isEmpty()) {
            return "확정된 분기 실적이 없습니다.";
        }
        CompanyQuarterlyReport report = reports.getFirst();
        if (report.isDividendDecided()) {
            return "최근 분기의 배당 결정을 이미 완료했습니다.";
        }
        if (rate == 0) {
            report.decideDividend(0, 0);
            newsService.recordDividendDecision(company, report);
            return "이번 분기는 무배당으로 결정했습니다.";
        }
        if (report.getNetIncome() <= 0 || company.isOperationsSuspended()
                || company.getUnpaidSettlementAmount() > 0 || hasMaturedUnpaidBond(company)) {
            return "적자·운영중단·미지급 채무가 있으면 배당할 수 없습니다.";
        }

        long reserve = Math.addExact(
                upcomingConstructionPayment(company),
                Math.multiplyExact(essentialMonthlyCost, 6));
        long distributableCash = Math.max(0, company.getCorporateCash() - reserve);
        long dividend = Math.min(report.getNetIncome() * rate / 100, distributableCash);
        if (dividend <= 0) {
            return "필수 운영자금과 예정 투자금을 제외하면 배당 가능 금액이 없습니다.";
        }
        long playerReceipt = multiplyRatio(dividend, company.getPlayerShares(), company.getIssuedShares());
        boolean withdrawn = cashLedgerService.withdraw(
                company,
                "dividend:" + report.getQuarterSequence(),
                CompanyCashFlowType.FINANCING,
                FiscalQuarter.periodText(report, player) + " 주주배당",
                dividend
        );
        if (!withdrawn) {
            return "배당금 지급 처리에 실패했습니다. 거래원장을 확인해 주세요.";
        }
        player.addCash(playerReceipt);
        report.decideDividend(rate, dividend);
        stockDividendService.applyExDividend(company, dividend);
        newsService.recordDividendDecision(company, report);
        return "배당 완료 · 개인 수령액 " + playerReceipt + "원";
    }

    @Transactional
    public String attractInvestment(long playerId, int targetPercent) {
        return "외부투자는 IPO 기능과 함께 다시 제공될 예정입니다. 현재 자본조달은 회사채만 가능합니다.";
    }

    @Transactional
    public String issueBond(long playerId, int valuationPercent) {
        if (!BOND_ISSUE_OPTIONS.contains(valuationPercent)) {
            return "회사채 발행비율은 5% 또는 10%여야 합니다.";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 채권을 발행할 수 없습니다.";
        }
        var latestValuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company);
        if (latestValuation.isEmpty()) {
            return "첫 분기 기업가치가 확정된 후 채권을 발행할 수 있습니다.";
        }
        CompanyValuationSnapshot valuation = latestValuation.get();
        long issueAmount = valuation.getEnterpriseValue() * valuationPercent / 100;
        long cap = valuation.getEnterpriseValue() * 10 / 100;
        if (issueAmount <= 0 || outstandingPrincipal(company) + issueAmount > cap) {
            return "회사채 원금 합계는 최근 기업가치의 10%를 넘을 수 없습니다.";
        }
        int periodIndex = periodIndex(player);
        long monthlyInterest = listingBenefitService.monthlyBondInterest(company, issueAmount);
        CompanyBond bond = bondRepository.save(new CompanyBond(
                company,
                issueAmount,
                monthlyInterest,
                periodIndex,
                periodIndex + 24
        ));
        cashLedgerService.deposit(
                company,
                "bond:" + bond.getId() + ":issue",
                CompanyCashFlowType.FINANCING,
                "회사채 발행",
                issueAmount
        );
        return "회사채 발행 완료 · " + issueAmount + "원";
    }

    @Transactional
    public String repayOrdinaryBond(long playerId, long bondId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        if (company.isOperationsSuspended()) {
            return "기업 운영중단 중에는 채권을 상환할 수 없습니다.";
        }
        CompanyBond bond = bondRepository.findById(bondId).orElseThrow();
        if (!bond.getCompany().getId().equals(company.getId())
                || bond.getStatus() != CompanyBondStatus.ACTIVE) {
            return "중도상환 가능한 회사채가 아닙니다.";
        }
        if (!cashLedgerService.withdraw(
                company,
                "bond:" + bond.getId() + ":prepay",
                CompanyCashFlowType.FINANCING,
                "회사채 중도상환",
                bond.getPrincipal()
        )) {
            return "법인 현금이 부족합니다.";
        }
        bond.repay();
        return "회사채 원금 전액 상환 완료";
    }

    @Transactional
    public BondObligation prepareMonthlyObligation(PlayerCompany company, int currentPeriodIndex) {
        long interest = 0;
        long principal = 0;
        var dueBonds = new java.util.ArrayList<Long>();
        for (CompanyBond bond : outstandingBonds(company)) {
            if (bond.getStatus() != CompanyBondStatus.ACTIVE
                    || currentPeriodIndex <= bond.getIssuedPeriodIndex()) {
                continue;
            }
            interest = Math.addExact(interest, bond.getMonthlyInterest());
            if (currentPeriodIndex >= bond.getMaturityPeriodIndex()) {
                principal = Math.addExact(principal, bond.getPrincipal());
                dueBonds.add(bond.getId());
            }
        }
        return new BondObligation(interest, principal, List.copyOf(dueBonds));
    }

    @Transactional
    public void completeMonthlyObligation(BondObligation obligation, boolean paid) {
        for (Long bondId : obligation.dueBondIds()) {
            CompanyBond bond = bondRepository.findById(bondId).orElseThrow();
            if (paid) {
                bond.repay();
            } else {
                bond.markDefaulted();
            }
        }
    }

    @Transactional
    public void resolveDefaultedBonds(PlayerCompany company) {
        outstandingBonds(company).stream()
                .filter(bond -> bond.getStatus() == CompanyBondStatus.DEFAULTED)
                .forEach(CompanyBond::repay);
    }

    @Transactional(readOnly = true)
    public FinanceView view(PlayerCompany company, long essentialMonthlyCost) {
        PlayerCompany managedCompany = companyRepository.findById(company.getId()).orElseThrow();
        var valuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(managedCompany);
        var reports = quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(managedCompany);
        int currentPeriodIndex = periodIndex(managedCompany.getPlayer());
        var bonds = bondRepository.findByCompanyOrderByIdDesc(managedCompany).stream()
                .map(bond -> new BondView(
                        bond.getId(),
                        bondStatusText(bond.getStatus()),
                        bond.getPrincipal(),
                        bond.getMonthlyInterest(),
                        Math.max(0, bond.getMaturityPeriodIndex() - currentPeriodIndex),
                        bond.getStatus() == CompanyBondStatus.ACTIVE
                )).toList();
        DividendPolicy dividendPolicy = dividendPolicy(managedCompany, reports, essentialMonthlyCost);
        long enterpriseValue = valuation.map(CompanyValuationSnapshot::getEnterpriseValue).orElse(0L);
        long remainingBondCapacity = Math.max(0, enterpriseValue * 10 / 100 - outstandingPrincipal(managedCompany));
        List<BondOption> bondOptions = BOND_ISSUE_OPTIONS.stream().sorted()
                .map(percent -> bondOption(managedCompany, enterpriseValue, remainingBondCapacity, percent))
                .toList();
        return new FinanceView(
                valuation.orElse(null),
                managedCompany.getPlayerOwnershipPercent(),
                governanceText(managedCompany.getPlayerOwnershipPercent()),
                governanceImpactText(managedCompany.getPlayerOwnershipPercent()),
                governanceWorkSlotPenalty(managedCompany),
                bonds,
                outstandingPrincipal(managedCompany),
                remainingBondCapacity,
                bondOptions,
                listingBenefitService.isListed(managedCompany),
                listingBenefitService.bondMonthlyInterestBasisPoints(managedCompany),
                dividendPolicy.decisionPending(),
                dividendPolicy.positiveDividendAvailable(),
                dividendPolicy.period(),
                dividendPolicy.status(),
                dividendPolicy.reason(),
                dividendPolicy.amountLabel(),
                dividendPolicy.maximumDividend(),
                dividendPolicy.options()
        );
    }

    private DividendPolicy dividendPolicy(
            PlayerCompany company,
            List<CompanyQuarterlyReport> reports,
            long essentialMonthlyCost
    ) {
        if (reports.isEmpty()) {
            return new DividendPolicy(false, false, "첫 분기", "결산 대기",
                    "확정된 분기 실적이 없어 배당을 결정할 수 없습니다.",
                    "최대 배당 가능액", 0, List.of());
        }
        CompanyQuarterlyReport report = reports.getFirst();
        String period = FiscalQuarter.periodText(report, company.getPlayer());
        if (report.isDividendDecided()) {
            String status = report.getDividendRate() == 0
                    ? "무배당 결정 완료"
                    : "배당성향 " + report.getDividendRate() + "% 결정 완료";
            return new DividendPolicy(false, false, period, status,
                    "최근 확정 분기의 배당 결정을 완료했습니다.",
                    "확정 총배당금", report.getDividendAmount(), List.of());
        }

        long reserve = Math.addExact(upcomingConstructionPayment(company),
                Math.multiplyExact(essentialMonthlyCost, 6));
        long distributableCash = Math.max(0, company.getCorporateCash() - reserve);
        long maximumDividend = Math.min(Math.max(0, report.getNetIncome() / 2), distributableCash);
        String unavailableReason = null;
        if (report.getNetIncome() <= 0) {
            unavailableReason = "최근 확정 분기가 적자이므로 무배당만 선택할 수 있습니다.";
        } else if (company.isOperationsSuspended()) {
            unavailableReason = "기업 운영이 중단되어 무배당만 선택할 수 있습니다.";
        } else if (company.getUnpaidSettlementAmount() > 0) {
            unavailableReason = "미지급 정산금이 있어 무배당만 선택할 수 있습니다.";
        } else if (hasMaturedUnpaidBond(company)) {
            unavailableReason = "만기 미상환 회사채가 있어 무배당만 선택할 수 있습니다.";
        } else if (maximumDividend <= 0) {
            unavailableReason = "예정 투자금과 필수 운영비 6개월분을 제외하면 배당 재원이 없습니다.";
        }
        boolean positiveAvailable = unavailableReason == null;
        List<DividendOption> options = DIVIDEND_RATES.stream().sorted()
                .map(rate -> new DividendOption(
                        rate,
                        rate == 0 ? 0 : Math.min(report.getNetIncome() * rate / 100, distributableCash),
                        rate == 0 || positiveAvailable
                ))
                .toList();
        return new DividendPolicy(true, positiveAvailable, period,
                positiveAvailable ? "배당 결정 필요" : "배당 제한",
                positiveAvailable
                        ? "순이익과 배당 가능 현금 범위에서 배당성향을 선택할 수 있습니다."
                        : unavailableReason,
                "최대 배당 가능액",
                maximumDividend,
                options);
    }

    /** 개인 추가 자금은 최신 주당 기업가치로 플레이어 신주를 발행하는 유상증자로 처리한다. */
    @Transactional
    public long applyPlayerContribution(PlayerCompany company, long amount) {
        long referenceValue = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company)
                .map(CompanyValuationSnapshot::getEnterpriseValue)
                .orElse(Math.max(company.getPaidInCapital(), company.getCorporateCash()));
        long pricePerShare = Math.max(1, referenceValue / Math.max(1, company.getIssuedShares()));
        long newShares = Math.max(1, amount / pricePerShare);
        company.contributeCapital(amount, newShares);
        listedCompanyRepository.findByPlayerAndStockKey(
                        company.getPlayer(), CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY)
                .ifPresent(listedCompany -> listedCompany.issueFounderShares(newShares));
        return newShares;
    }

    public int governanceWorkSlotPenalty(PlayerCompany company) {
        return governanceWorkSlotPenalty(company.getPlayerOwnershipPercent());
    }

    public static int governanceWorkSlotPenalty(double ownership) {
        if (ownership > 50) return 0;
        if (ownership >= 33.4) return 1;
        if (ownership >= 20) return 2;
        return 3;
    }

    private BondOption bondOption(
            PlayerCompany company,
            long enterpriseValue,
            long remainingCapacity,
            int percent
    ) {
        long principal = enterpriseValue * percent / 100;
        return new BondOption(percent, principal, listingBenefitService.monthlyBondInterest(company, principal),
                principal > 0 && principal <= remainingCapacity);
    }

    private String bondStatusText(CompanyBondStatus status) {
        return switch (status) {
            case ACTIVE -> "상환 중";
            case REPAID -> "상환 완료";
            case DEFAULTED -> "만기 미상환";
        };
    }

    private String governanceImpactText(double ownershipPercent) {
        if (ownershipPercent > 50) return "경영 제약 없음";
        if (ownershipPercent >= 33.4) return "이사회 승인 절차로 전사 주요 업무 슬롯 1개 감소";
        if (ownershipPercent >= 20) return "강화된 이사회 통제로 전사 주요 업무 슬롯 2개 감소";
        return "경영권 상실 위험으로 전사 주요 업무 슬롯 3개 감소";
    }

    private int revenueMultiple(PlayerCompany company, CompanyQuarterlyReport report) {
        var reports = quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        CompanyQuarterlyReport previous = reports.stream()
                .filter(candidate -> candidate.getQuarterSequence() < report.getQuarterSequence())
                .findFirst().orElse(null);
        if (report.getQuarterSequence() == 1 || previous == null
                || previous.getRecurringRevenueAtEnd() <= 0) {
            return 6;
        }
        double growth = (report.getRecurringRevenueAtEnd() - previous.getRecurringRevenueAtEnd())
                * 100.0 / previous.getRecurringRevenueAtEnd();
        if (growth < 0) return 2;
        int growthMultiple = growth < 10 ? 3
                : growth < 25 ? 4
                : growth < 50 ? 5
                : 6;
        return Math.max(growthMultiple, maturityRevenueMultiple(company, report, reports));
    }

    /** 저성장 성숙기업의 가치가 분기 성장률 하나만으로 급락하지 않도록 지속 가능한 품질을 반영한다. */
    private int maturityRevenueMultiple(
            PlayerCompany company,
            CompanyQuarterlyReport report,
            List<CompanyQuarterlyReport> reports
    ) {
        if (company.getGrowthStage().ordinal() < CompanyGrowthStage.GROWTH.ordinal()) {
            return 3;
        }
        int multiple = 4;
        if (company.getPrototypeBenchmark() >= 400) {
            multiple++;
        }
        if (report.getPaidUsersAtEnd() >= 1_000_000L) {
            multiple++;
        }
        List<CompanyQuarterlyReport> recentReports = reports.stream()
                .filter(candidate -> candidate.getQuarterSequence() <= report.getQuarterSequence())
                .limit(2)
                .toList();
        boolean consecutiveProfits = recentReports.size() == 2
                && recentReports.stream().allMatch(candidate -> candidate.getOperatingProfit() > 0);
        if (consecutiveProfits) {
            multiple++;
        }
        return multiple;
    }

    private int adjustmentBasisPoints(
            PlayerCompany company,
            CompanyQuarterlyReport report,
            long essentialMonthlyCost
    ) {
        int adjustment = 10_000;
        int highestCompetitorBenchmark = competitorRepository.findByCompanyOrderById(company).stream()
                .mapToInt(competitor -> competitor.getBenchmark())
                .max().orElse(Integer.MAX_VALUE);
        if (company.getPrototypeBenchmark() >= highestCompetitorBenchmark) {
            adjustment += 1_000;
        }
        var reports = quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        CompanyQuarterlyReport previous = reports.stream()
                .filter(candidate -> candidate.getQuarterSequence() < report.getQuarterSequence())
                .findFirst().orElse(null);
        if (previous != null && report.getPaidUsersAtEnd() > previous.getPaidUsersAtEnd()) {
            adjustment += 500;
        }
        long activeIncidentCount = incidentRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(incident -> incident.hasCustomerImpact())
                .limit(3)
                .count();
        adjustment -= (int) activeIncidentCount * 500;

        List<Long> activeContractFees = contractRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(contract -> contract.getStatus() == CompanyCustomerContractStatus.ACTIVE
                        || contract.getStatus() == CompanyCustomerContractStatus.RENEWAL_OFFERED)
                .map(contract -> contract.getMonthlyFee())
                .toList();
        long totalContractFees = activeContractFees.stream().mapToLong(Long::longValue).sum();
        long largestContractFee = activeContractFees.stream().mapToLong(Long::longValue).max().orElse(0);
        if (totalContractFees > 0) {
            double concentration = largestContractFee * 100.0 / totalContractFees;
            if (concentration > 50) adjustment -= 1_000;
            else if (concentration > 35) adjustment -= 500;
        }
        if (essentialMonthlyCost > 0) {
            long runwayMonths = company.getCorporateCash() / essentialMonthlyCost;
            if (runwayMonths < 3) adjustment -= 1_500;
            else if (runwayMonths < 6) adjustment -= 800;
        }
        return Math.max(7_000, Math.min(11_500, adjustment));
    }

    private long annualizedOperatingProfit(List<CompanyMonthlySettlement> recentSettlements) {
        List<CompanyMonthlySettlement> months = recentSettlements.stream().limit(3).toList();
        if (months.isEmpty()) {
            return 0;
        }
        long total = months.stream().mapToLong(CompanyMonthlySettlement::getOperatingProfit).sum();
        return total * 12 / months.size();
    }

    private long outstandingPrincipal(PlayerCompany company) {
        return outstandingBonds(company).stream().mapToLong(CompanyBond::getPrincipal).sum();
    }

    private List<CompanyBond> outstandingBonds(PlayerCompany company) {
        return bondRepository.findByCompanyAndStatusInOrderByIdAsc(
                company, List.of(CompanyBondStatus.ACTIVE, CompanyBondStatus.DEFAULTED));
    }

    private boolean hasMaturedUnpaidBond(PlayerCompany company) {
        return outstandingBonds(company).stream()
                .anyMatch(bond -> bond.getStatus() == CompanyBondStatus.DEFAULTED);
    }

    private long upcomingConstructionPayment(PlayerCompany company) {
        return constructionRepository.findByCompanyOrderByIdDesc(company).stream()
                .filter(construction -> construction.getStatus() == CompanyComputeConstructionStatus.PAYMENT_DUE
                        || (construction.getStatus() == CompanyComputeConstructionStatus.ACTIVE
                        && construction.getTotalMonths() - construction.getElapsedMonths() <= 1))
                .mapToLong(construction -> construction.getCompletionPayment())
                .sum();
    }

    private long multiplyBasisPoints(long amount, int basisPoints) {
        return multiplyRatio(amount, basisPoints, 10_000);
    }

    private long multiplyRatio(long amount, long numerator, long denominator) {
        if (amount == 0 || numerator == 0) {
            return 0;
        }
        return java.math.BigInteger.valueOf(amount)
                .multiply(java.math.BigInteger.valueOf(numerator))
                .divide(java.math.BigInteger.valueOf(denominator))
                .longValueExact();
    }

    private String governanceText(double ownershipPercent) {
        if (ownershipPercent > 50) return "단독 경영";
        if (ownershipPercent >= 33.4) return "주요 결정 이사회 승인";
        if (ownershipPercent >= 20) return "이사회 제한 강화";
        return "경영권 상실 위험";
    }

    private int periodIndex(Player player) {
        return (player.getYear() - 1) * 12 + player.getMonth() - 1;
    }

    public record BondObligation(long interest, long principal, List<Long> dueBondIds) {
        public long totalCashOut() {
            return Math.addExact(interest, principal);
        }
    }

    public record FinanceView(
            CompanyValuationSnapshot valuation,
            double playerOwnershipPercent,
            String governance,
            String governanceImpact,
            int governanceWorkSlotPenalty,
            List<BondView> bonds,
            long outstandingPrincipal,
            long remainingBondCapacity,
            List<BondOption> bondOptions,
            boolean listedBenefitsActive,
            int bondMonthlyInterestBasisPoints,
            boolean dividendPending,
            boolean positiveDividendAvailable,
            String dividendPeriod,
            String dividendStatus,
            String dividendReason,
            String dividendAmountLabel,
            long maximumDividend,
            List<DividendOption> dividendOptions
    ) {
    }

    private record DividendPolicy(
            boolean decisionPending,
            boolean positiveDividendAvailable,
            String period,
            String status,
            String reason,
            String amountLabel,
            long maximumDividend,
            List<DividendOption> options
    ) {
    }

    public record DividendOption(int rate, long expectedAmount, boolean available) {
    }

    public record BondView(
            long id,
            String status,
            long principal,
            long monthlyInterest,
            int remainingMonths,
            boolean repayable
    ) {
    }

    public record BondOption(
            int percent,
            long principal,
            long monthlyInterest,
            boolean available
    ) {
    }
}
