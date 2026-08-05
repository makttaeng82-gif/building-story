package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyBond;
import com.game.buildingstory.domain.CompanyBondStatus;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyComputeConstructionStatus;
import com.game.buildingstory.domain.CompanyCustomerContractStatus;
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
            CompanyCashLedgerService cashLedgerService
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
        cashLedgerService.withdraw(
                company,
                "dividend:" + report.getQuarterSequence(),
                CompanyCashFlowType.FINANCING,
                report.getQuarterSequence() + "분기 주주배당",
                dividend
        );
        player.addCash(playerReceipt);
        report.decideDividend(rate, dividend);
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
        long monthlyInterest = issueAmount * 5 / 1000;
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
        var valuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company);
        var reports = quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        PlayerCompany managedCompany = companyRepository.findById(company.getId()).orElseThrow();
        int currentPeriodIndex = periodIndex(managedCompany.getPlayer());
        var bonds = bondRepository.findByCompanyOrderByIdDesc(company).stream()
                .map(bond -> new BondView(
                        bond.getId(),
                        bondStatusText(bond.getStatus()),
                        bond.getPrincipal(),
                        bond.getMonthlyInterest(),
                        Math.max(0, bond.getMaturityPeriodIndex() - currentPeriodIndex),
                        bond.getStatus() == CompanyBondStatus.ACTIVE
                )).toList();
        long dividendAvailable = 0;
        boolean dividendPending = false;
        if (!reports.isEmpty() && !reports.getFirst().isDividendDecided()) {
            CompanyQuarterlyReport report = reports.getFirst();
            dividendPending = report.getNetIncome() > 0 && !company.isOperationsSuspended();
            long reserve = upcomingConstructionPayment(company) + essentialMonthlyCost * 6;
            dividendAvailable = Math.min(
                    Math.max(0, report.getNetIncome() / 2),
                    Math.max(0, company.getCorporateCash() - reserve));
        }
        long enterpriseValue = valuation.map(CompanyValuationSnapshot::getEnterpriseValue).orElse(0L);
        long remainingBondCapacity = Math.max(0, enterpriseValue * 10 / 100 - outstandingPrincipal(company));
        List<BondOption> bondOptions = BOND_ISSUE_OPTIONS.stream().sorted()
                .map(percent -> bondOption(enterpriseValue, remainingBondCapacity, percent))
                .toList();
        return new FinanceView(
                valuation.orElse(null),
                company.getPlayerOwnershipPercent(),
                governanceText(company.getPlayerOwnershipPercent()),
                governanceImpactText(company.getPlayerOwnershipPercent()),
                governanceWorkSlotPenalty(company),
                bonds,
                outstandingPrincipal(company),
                remainingBondCapacity,
                bondOptions,
                dividendPending,
                dividendAvailable
        );
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

    private BondOption bondOption(long enterpriseValue, long remainingCapacity, int percent) {
        long principal = enterpriseValue * percent / 100;
        return new BondOption(percent, principal, principal * 5 / 1000,
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
        if (growth < 10) return 3;
        if (growth < 25) return 4;
        if (growth < 50) return 5;
        return 6;
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
        int gameYear = Math.max(0, player.getElapsedDays() - 1) / 365 + 1;
        return (gameYear - 1) * 12 + player.getMonth() - 1;
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
            boolean dividendPending,
            long maximumDividend
    ) {
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
