package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 플레이어의 게임 세계에 존재하는 상장기업의 주주 구성을 저장한다.
 *
 * <p>같은 종목이라도 플레이어마다 별도의 게임 시간이 흐르므로 상장기업 상태도 플레이어별로 가진다.
 * 현재 플레이어 개인 보유량은 {@link OwnedStock}이 이미 원본으로 관리하므로 이 엔티티에 중복 저장하지 않는다.
 * 중복 저장하면 매매 도중 한쪽만 변경되어 지분 합계가 어긋날 수 있기 때문이다.</p>
 */
@Entity
@Table(name = "listed_company", uniqueConstraints =
        @UniqueConstraint(name = "uk_listed_company_player_key", columnNames = {"player_id", "stock_key"}))
public class ListedCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    private long issuedShares;
    private long founderShares;
    private long institutionalShares;
    private long marketParticipantShares;
    private long corporatePlayerShares;
    // 기존 DB에 컬럼을 nullable로 먼저 추가할 수 있도록 래퍼 타입을 사용한다.
    // 새 기업은 초기화 과정에서 모두 채우며 getter는 이전 행의 null을 안전한 기본값으로 바꾼다.
    private Boolean financialInitialized;
    private Long quarterlyRevenue;
    private Long expectedRevenue;
    private Long expectedNetIncome;
    private Long cash;
    private Long nonCashAssets;
    private Long debt;
    private Long otherLiabilities;
    private Long netAssets;
    private Integer annualGrowthBasisPoints;
    private Integer grossMarginBasisPoints;
    private Integer operatingExpenseBasisPoints;
    private Integer capitalExpenditureBasisPoints;
    private Integer annualDepreciationBasisPoints;
    private Integer dividendPayoutBasisPoints;
    private Integer latestSettledFiscalPeriod;
    private Integer nextEarningsElapsedDay;
    private Integer pendingEarningsImpactBasisPoints;
    private Integer pendingEarningsFiscalPeriod;
    private Integer pendingIndustryRevenueImpactBasisPoints;
    private Integer pendingCompanyRevenueImpactBasisPoints;
    private Integer pendingCompanyOperatingExpenseImpactBasisPoints;

    protected ListedCompany() {
    }

    public ListedCompany(
            Player player,
            String stockKey,
            long issuedShares,
            long founderShares,
            long institutionalShares,
            long marketParticipantShares,
            long corporatePlayerShares,
            long personalPlayerShares
    ) {
        this.player = player;
        this.stockKey = stockKey;
        this.issuedShares = issuedShares;
        this.founderShares = founderShares;
        this.institutionalShares = institutionalShares;
        this.marketParticipantShares = marketParticipantShares;
        this.corporatePlayerShares = corporatePlayerShares;
        validateShareComposition(personalPlayerShares);
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getStockKey() {
        return stockKey;
    }

    public long getIssuedShares() {
        return issuedShares;
    }

    public long getFounderShares() {
        return founderShares;
    }

    public long getInstitutionalShares() {
        return institutionalShares;
    }

    public long getMarketParticipantShares() {
        return marketParticipantShares;
    }

    public long getCorporatePlayerShares() {
        return corporatePlayerShares;
    }

    public boolean isFinancialInitialized() { return Boolean.TRUE.equals(financialInitialized); }
    public long getQuarterlyRevenue() { return quarterlyRevenue == null ? 0 : quarterlyRevenue; }
    public long getExpectedRevenue() { return expectedRevenue == null ? 0 : expectedRevenue; }
    public long getExpectedNetIncome() { return expectedNetIncome == null ? 0 : expectedNetIncome; }
    public long getCash() { return cash == null ? 0 : cash; }
    public long getNonCashAssets() { return nonCashAssets == null ? 0 : nonCashAssets; }
    public long getDebt() { return debt == null ? 0 : debt; }
    public long getOtherLiabilities() { return otherLiabilities == null ? 0 : otherLiabilities; }
    public long getNetAssets() { return netAssets == null ? 0 : netAssets; }
    public int getAnnualGrowthBasisPoints() { return annualGrowthBasisPoints == null ? 0 : annualGrowthBasisPoints; }
    public int getGrossMarginBasisPoints() { return grossMarginBasisPoints == null ? 0 : grossMarginBasisPoints; }
    public int getOperatingExpenseBasisPoints() { return operatingExpenseBasisPoints == null ? 0 : operatingExpenseBasisPoints; }
    public int getCapitalExpenditureBasisPoints() { return capitalExpenditureBasisPoints == null ? 0 : capitalExpenditureBasisPoints; }
    public int getAnnualDepreciationBasisPoints() { return annualDepreciationBasisPoints == null ? 0 : annualDepreciationBasisPoints; }
    public int getDividendPayoutBasisPoints() { return dividendPayoutBasisPoints == null ? 0 : dividendPayoutBasisPoints; }
    public int getLatestSettledFiscalPeriod() { return latestSettledFiscalPeriod == null ? -1 : latestSettledFiscalPeriod; }
    public int getNextEarningsElapsedDay() { return nextEarningsElapsedDay == null ? 0 : nextEarningsElapsedDay; }
    public int getPendingIndustryRevenueImpactBasisPoints() {
        return pendingIndustryRevenueImpactBasisPoints == null ? 0 : pendingIndustryRevenueImpactBasisPoints;
    }
    public int getPendingCompanyRevenueImpactBasisPoints() {
        return pendingCompanyRevenueImpactBasisPoints == null ? 0 : pendingCompanyRevenueImpactBasisPoints;
    }
    public int getPendingCompanyOperatingExpenseImpactBasisPoints() {
        return pendingCompanyOperatingExpenseImpactBasisPoints == null
                ? 0 : pendingCompanyOperatingExpenseImpactBasisPoints;
    }

    /** 확정 업종 사건의 매출 영향을 현재 분기에 누적하되 과도한 중첩은 ±20%로 제한한다. */
    public void registerIndustryRevenueImpact(int impactBasisPoints) {
        int accumulated = getPendingIndustryRevenueImpactBasisPoints() + impactBasisPoints;
        pendingIndustryRevenueImpactBasisPoints = Math.max(-2_000, Math.min(2_000, accumulated));
    }

    /** 다음 결산에서 실제 매출에 반영한 뒤 같은 사건이 다시 적용되지 않도록 비운다. */
    public int consumeIndustryRevenueImpactBasisPoints() {
        int impact = getPendingIndustryRevenueImpactBasisPoints();
        pendingIndustryRevenueImpactBasisPoints = 0;
        return impact;
    }

    /** 확정 기업 사건의 다음 분기 매출·영업비용 영향을 누적한다. */
    public void registerCompanyOperatingImpact(int revenueImpactBasisPoints, int operatingExpenseImpactBasisPoints) {
        pendingCompanyRevenueImpactBasisPoints = Math.max(-2_500, Math.min(
                2_500,
                getPendingCompanyRevenueImpactBasisPoints() + revenueImpactBasisPoints
        ));
        pendingCompanyOperatingExpenseImpactBasisPoints = Math.max(-2_000, Math.min(
                2_000,
                getPendingCompanyOperatingExpenseImpactBasisPoints() + operatingExpenseImpactBasisPoints
        ));
    }

    public int consumeCompanyRevenueImpactBasisPoints() {
        int impact = getPendingCompanyRevenueImpactBasisPoints();
        pendingCompanyRevenueImpactBasisPoints = 0;
        return impact;
    }

    public int consumeCompanyOperatingExpenseImpactBasisPoints() {
        int impact = getPendingCompanyOperatingExpenseImpactBasisPoints();
        pendingCompanyOperatingExpenseImpactBasisPoints = 0;
        return impact;
    }

    /** 차입은 현금과 부채를 같은 금액만큼 늘려 재무상태표 균형을 유지한다. */
    public void applyDebtFinancing(long amount) {
        if (amount <= 0) {
            return;
        }
        cash = Math.addExact(getCash(), amount);
        debt = Math.addExact(getDebt(), amount);
    }

    /**
     * 보조금·보상금은 현금과 순자산을 늘리고, 벌금·리콜비는 현금과 순자산을 줄인다.
     * 손실이 보유 현금을 넘으면 부족액을 차입해 결제한 것으로 처리한다.
     */
    public void applyImmediateProfitOrLoss(long signedAmount) {
        if (signedAmount >= 0) {
            cash = Math.addExact(getCash(), signedAmount);
            netAssets = Math.addExact(getNetAssets(), signedAmount);
            return;
        }
        long loss = Math.negateExact(signedAmount);
        long availableCash = getCash();
        if (availableCash >= loss) {
            cash = availableCash - loss;
        } else {
            cash = 0L;
            debt = Math.addExact(getDebt(), loss - availableCash);
        }
        netAssets = Math.subtractExact(getNetAssets(), loss);
    }

    /** 뉴스로 변경된 시장 기대치를 다음 실적 발표 전까지 보관한다. */
    public void reviseExpectedResults(long expectedRevenue, long expectedNetIncome) {
        if (!isFinancialInitialized() || expectedRevenue <= 0) {
            throw new IllegalStateException("초기화되지 않은 기업의 실적 전망은 수정할 수 없습니다.");
        }
        this.expectedRevenue = expectedRevenue;
        this.expectedNetIncome = expectedNetIncome;
    }

    public void registerEarningsImpact(int fiscalPeriodIndex, int surpriseBasisPoints) {
        if (pendingEarningsFiscalPeriod != null && pendingEarningsFiscalPeriod >= fiscalPeriodIndex) {
            return;
        }
        this.pendingEarningsFiscalPeriod = fiscalPeriodIndex;
        this.pendingEarningsImpactBasisPoints = surpriseBasisPoints;
    }

    /** 실적 발표 충격을 다음 가격 갱신 한 번에만 사용하고 즉시 제거한다. */
    public int consumePendingEarningsImpactBasisPoints() {
        int impact = pendingEarningsImpactBasisPoints == null ? 0 : pendingEarningsImpactBasisPoints;
        pendingEarningsImpactBasisPoints = null;
        return impact;
    }

    /**
     * 카탈로그의 초기 재무 기준값을 기업의 현재 상태로 옮긴다.
     * 자산과 부채·순자산이 일치하지 않으면 이후 모든 분기 계산이 틀어지므로 생성 시점에 거부한다.
     */
    public void initializeFinancialState(
            long quarterlyRevenue,
            long expectedRevenue,
            long expectedNetIncome,
            long cash,
            long nonCashAssets,
            long debt,
            long otherLiabilities,
            int annualGrowthBasisPoints,
            int grossMarginBasisPoints,
            int operatingExpenseBasisPoints,
            int capitalExpenditureBasisPoints,
            int annualDepreciationBasisPoints,
            int dividendPayoutBasisPoints,
            int latestSettledFiscalPeriod,
            int nextEarningsElapsedDay
    ) {
        if (isFinancialInitialized()) {
            return;
        }
        long assets = Math.addExact(cash, nonCashAssets);
        long liabilities = Math.addExact(debt, otherLiabilities);
        if (quarterlyRevenue <= 0 || assets < liabilities) {
            throw new IllegalArgumentException("상장기업의 초기 재무상태가 유효하지 않습니다.");
        }
        this.financialInitialized = true;
        this.quarterlyRevenue = quarterlyRevenue;
        this.expectedRevenue = expectedRevenue;
        this.expectedNetIncome = expectedNetIncome;
        this.cash = cash;
        this.nonCashAssets = nonCashAssets;
        this.debt = debt;
        this.otherLiabilities = otherLiabilities;
        this.netAssets = assets - liabilities;
        this.annualGrowthBasisPoints = annualGrowthBasisPoints;
        this.grossMarginBasisPoints = grossMarginBasisPoints;
        this.operatingExpenseBasisPoints = operatingExpenseBasisPoints;
        this.capitalExpenditureBasisPoints = capitalExpenditureBasisPoints;
        this.annualDepreciationBasisPoints = annualDepreciationBasisPoints;
        this.dividendPayoutBasisPoints = dividendPayoutBasisPoints;
        this.latestSettledFiscalPeriod = latestSettledFiscalPeriod;
        this.nextEarningsElapsedDay = nextEarningsElapsedDay;
    }

    /** 확정된 분기 결과를 다음 분기의 출발 상태로 반영한다. */
    public void applyQuarterlySettlement(
            int fiscalPeriodIndex,
            long revenue,
            long expectedRevenue,
            long expectedNetIncome,
            long cash,
            long nonCashAssets,
            long debt,
            long otherLiabilities,
            long netAssets,
            int nextEarningsElapsedDay
    ) {
        if (!isFinancialInitialized() || fiscalPeriodIndex <= getLatestSettledFiscalPeriod()) {
            throw new IllegalStateException("이미 처리했거나 초기화되지 않은 분기입니다.");
        }
        long assets = Math.addExact(cash, nonCashAssets);
        long liabilitiesAndEquity = Math.addExact(Math.addExact(debt, otherLiabilities), netAssets);
        if (assets != liabilitiesAndEquity) {
            throw new IllegalArgumentException("분기 결산 후 자산과 부채·순자산이 일치해야 합니다.");
        }
        this.latestSettledFiscalPeriod = fiscalPeriodIndex;
        this.quarterlyRevenue = revenue;
        this.expectedRevenue = expectedRevenue;
        this.expectedNetIncome = expectedNetIncome;
        this.cash = cash;
        this.nonCashAssets = nonCashAssets;
        this.debt = debt;
        this.otherLiabilities = otherLiabilities;
        this.netAssets = netAssets;
        this.nextEarningsElapsedDay = nextEarningsElapsedDay;
    }

    public boolean hasBalancedFinancialPosition() {
        return isFinancialInitialized()
                && Math.addExact(getCash(), getNonCashAssets())
                == Math.addExact(Math.addExact(getDebt(), getOtherLiabilities()), getNetAssets());
    }

    /**
     * 일반시장 참여자가 보유한 주식을 플레이어 개인에게 이전한다.
     *
     * <p>현 단계에서는 일반시장 매집 한도와 별도로 실제 남은 시장 지분도 검사한다.
     * 이후 유동성 시스템이 추가되면 이 수량 중 일부만 시점별 주문 가능 물량으로 제공한다.</p>
     */
    public boolean transferMarketSharesToPlayer(long quantity) {
        if (quantity <= 0 || marketParticipantShares < quantity) {
            return false;
        }
        marketParticipantShares -= quantity;
        return true;
    }

    /** 플레이어가 매도한 주식을 일반시장 참여자 보유분으로 되돌린다. */
    public void receivePlayerShares(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("반환 주식 수는 1주 이상이어야 합니다.");
        }
        marketParticipantShares = Math.addExact(marketParticipantShares, quantity);
    }

    /** 상장 후 플레이어 유상증자로 발행된 신주를 창업자 보유분과 총발행주식에 함께 반영한다. */
    public void issueFounderShares(long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("신규 창업자 주식 수는 1주 이상이어야 합니다.");
        }
        issuedShares = Math.addExact(issuedShares, quantity);
        founderShares = Math.addExact(founderShares, quantity);
    }

    public boolean hasConservedShares(long personalPlayerShares) {
        try {
            return accountedShares(personalPlayerShares) == issuedShares;
        } catch (ArithmeticException exception) {
            return false;
        }
    }

    private long accountedShares(long personalPlayerShares) {
        long total = Math.addExact(founderShares, institutionalShares);
        total = Math.addExact(total, marketParticipantShares);
        total = Math.addExact(total, corporatePlayerShares);
        return Math.addExact(total, personalPlayerShares);
    }

    private void validateShareComposition(long personalPlayerShares) {
        if (issuedShares <= 0
                || founderShares < 0
                || institutionalShares < 0
                || marketParticipantShares < 0
                || corporatePlayerShares < 0
                || personalPlayerShares < 0
                || !hasConservedShares(personalPlayerShares)) {
            throw new IllegalArgumentException("상장기업의 주주별 주식 합계가 발행주식 수와 일치해야 합니다.");
        }
    }
}
