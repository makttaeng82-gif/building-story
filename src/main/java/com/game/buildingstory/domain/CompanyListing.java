package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 플레이어 기업의 IPO 신청부터 상장 완료까지 변하지 않아야 하는 사실을 보존한다.
 *
 * <p>기업 운영 상태는 {@link PlayerCompany}, 주식시장 상태는 {@link ListedCompany}가 담당한다.
 * 이 엔티티는 두 영역 사이의 연결과 IPO 진행상태만 책임져 같은 값을 여러 곳에서 갱신하는 문제를
 * 방지한다.</p>
 */
@Entity
@Table(name = "company_listing", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_listing_company", columnNames = "company_id"))
public class CompanyListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Column(nullable = false, length = 40)
    private String stockKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyListingStatus status;

    private int selectedOfferPercent;
    private int applicationSequence;
    private int applicationElapsedDay;
    private int preparationMonthsCompleted;
    private long preparationCost;
    private Long valuationAtListing;
    private Long offerPrice;
    private Long newShares;
    private Long proceeds;
    private Integer listedElapsedDay;
    private Integer lastPricedQuarterSequence;
    private Integer quarterSequenceAtListing;
    private Integer lastDividendQuarterSequenceAtListing;

    protected CompanyListing() {
    }

    public CompanyListing(
            PlayerCompany company,
            String stockKey,
            int selectedOfferPercent,
            int applicationElapsedDay,
            long preparationCost
    ) {
        if (company == null) {
            throw new IllegalArgumentException("상장 대상 기업이 필요합니다.");
        }
        if (stockKey == null || stockKey.isBlank()) {
            throw new IllegalArgumentException("상장 종목 키가 필요합니다.");
        }
        validateApplication(selectedOfferPercent, preparationCost);
        this.company = company;
        this.stockKey = stockKey;
        this.selectedOfferPercent = selectedOfferPercent;
        this.applicationSequence = 1;
        this.applicationElapsedDay = applicationElapsedDay;
        this.preparationCost = preparationCost;
        this.preparationMonthsCompleted = 0;
        this.status = CompanyListingStatus.PREPARING;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getStockKey() { return stockKey; }
    public CompanyListingStatus getStatus() { return status; }
    public int getSelectedOfferPercent() { return selectedOfferPercent; }
    public int getApplicationSequence() { return applicationSequence; }
    public int getApplicationElapsedDay() { return applicationElapsedDay; }
    public int getPreparationMonthsCompleted() { return preparationMonthsCompleted; }
    public long getPreparationCost() { return preparationCost; }
    public long getValuationAtListing() { return valuationAtListing == null ? 0 : valuationAtListing; }
    public long getOfferPrice() { return offerPrice == null ? 0 : offerPrice; }
    public long getNewShares() { return newShares == null ? 0 : newShares; }
    public long getProceeds() { return proceeds == null ? 0 : proceeds; }
    public Integer getListedElapsedDay() { return listedElapsedDay; }
    public int getLastPricedQuarterSequence() {
        return lastPricedQuarterSequence == null ? -1 : lastPricedQuarterSequence;
    }
    public int getQuarterSequenceAtListing() {
        return quarterSequenceAtListing == null ? -1 : quarterSequenceAtListing;
    }
    public int getLastDividendQuarterSequenceAtListing() {
        return lastDividendQuarterSequenceAtListing == null ? -1 : lastDividendQuarterSequenceAtListing;
    }

    /**
     * 취소된 신청 기록을 새 신청으로 초기화한다.
     *
     * <p>기업당 상장 레코드를 하나만 유지하므로 재신청 때 행을 추가하지 않는다. 신청 차수는
     * 법인 거래원장의 준비비 eventKey를 고유하게 만들어 같은 날 재신청도 정상 처리되게 한다.</p>
     */
    public void restartApplication(int offerPercent, int elapsedDay, long cost) {
        validateApplication(offerPercent, cost);
        if (status != CompanyListingStatus.CANCELLED) {
            throw new IllegalStateException("취소된 IPO만 다시 신청할 수 있습니다.");
        }
        selectedOfferPercent = offerPercent;
        applicationSequence = Math.addExact(applicationSequence, 1);
        applicationElapsedDay = elapsedDay;
        preparationMonthsCompleted = 0;
        preparationCost = cost;
        valuationAtListing = null;
        offerPrice = null;
        newShares = null;
        proceeds = null;
        listedElapsedDay = null;
        lastPricedQuarterSequence = null;
        quarterSequenceAtListing = null;
        lastDividendQuarterSequenceAtListing = null;
        status = CompanyListingStatus.PREPARING;
    }

    /** 월 정산이 정상 완료된 경우에만 준비 개월을 한 번 진행한다. */
    public boolean advancePreparationMonth(int requiredMonths) {
        if (status != CompanyListingStatus.PREPARING) {
            return false;
        }
        if (requiredMonths <= 0) {
            throw new IllegalArgumentException("IPO 준비기간은 1개월 이상이어야 합니다.");
        }
        preparationMonthsCompleted = Math.min(
                requiredMonths,
                Math.addExact(preparationMonthsCompleted, 1)
        );
        if (preparationMonthsCompleted == requiredMonths) {
            status = CompanyListingStatus.READY;
            return true;
        }
        return false;
    }

    public void cancel() {
        if (status != CompanyListingStatus.PREPARING && status != CompanyListingStatus.READY) {
            throw new IllegalStateException("진행 중인 IPO만 취소할 수 있습니다.");
        }
        status = CompanyListingStatus.CANCELLED;
    }

    /** 상장 확정 결과를 한 번만 기록한다. 실제 자금·주식 변경은 상위 트랜잭션 서비스가 담당한다. */
    public void completeListing(
            long valuation,
            long confirmedOfferPrice,
            long confirmedNewShares,
            long confirmedProceeds,
            int elapsedDay,
            int latestQuarterSequence,
            int latestDividendQuarterSequence
    ) {
        if (status != CompanyListingStatus.READY) {
            throw new IllegalStateException("준비 완료된 IPO만 상장을 확정할 수 있습니다.");
        }
        if (valuation <= 0 || confirmedOfferPrice <= 0
                || confirmedNewShares <= 0 || confirmedProceeds <= 0) {
            throw new IllegalArgumentException("상장 확정값은 모두 양수여야 합니다.");
        }
        valuationAtListing = valuation;
        offerPrice = confirmedOfferPrice;
        newShares = confirmedNewShares;
        proceeds = confirmedProceeds;
        listedElapsedDay = elapsedDay;
        lastPricedQuarterSequence = latestQuarterSequence;
        quarterSequenceAtListing = latestQuarterSequence;
        lastDividendQuarterSequenceAtListing = latestDividendQuarterSequence;
        status = CompanyListingStatus.LISTED;
    }

    /** 새 분기 실적 충격을 가격에 한 번 반영한 뒤 처리 지점을 전진시킨다. */
    public boolean markQuarterPriced(int quarterSequence) {
        if (status != CompanyListingStatus.LISTED
                || quarterSequence <= getLastPricedQuarterSequence()) {
            return false;
        }
        lastPricedQuarterSequence = quarterSequence;
        return true;
    }

    private static void validateApplication(int offerPercent, long cost) {
        if (offerPercent <= 0 || offerPercent >= 100) {
            throw new IllegalArgumentException("공모비율은 1% 이상 100% 미만이어야 합니다.");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("IPO 준비비는 음수일 수 없습니다.");
        }
    }
}
