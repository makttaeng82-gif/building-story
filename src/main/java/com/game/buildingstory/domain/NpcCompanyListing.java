package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 플레이어별 NPC 신규상장 일정과 확정 결과를 보존한다. */
@Entity
@Table(name = "npc_company_listing", uniqueConstraints =
        @UniqueConstraint(name = "uk_npc_listing_player_key", columnNames = {"player_id", "stock_key"}))
public class NpcCompanyListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    @Enumerated(EnumType.STRING)
    private NpcCompanyListingStage stage;
    private int targetElapsedDay;
    private int postponementCount;
    private Long offerPrice;
    private Integer demandBasisPoints;
    private Integer listedElapsedDay;
    private Long subscriptionRequestedQuantity;
    private Long subscriptionReservedAmount;
    private Long subscriptionFee;
    private Long subscriptionAllocatedQuantity;
    private Long subscriptionRefundAmount;
    private Boolean subscriptionSettled;
    private Boolean subscriptionResultAcknowledged;

    protected NpcCompanyListing() {
    }

    public NpcCompanyListing(Player player, String stockKey, int targetElapsedDay) {
        this.player = player;
        this.stockKey = stockKey;
        this.targetElapsedDay = targetElapsedDay;
        this.stage = NpcCompanyListingStage.SCHEDULED;
    }

    public void advanceTo(NpcCompanyListingStage next) {
        if (next.ordinal() != stage.ordinal() + 1) {
            throw new IllegalStateException("신규상장 단계는 순서대로 진행해야 합니다.");
        }
        stage = next;
    }

    public void postpone(int days) {
        if (days <= 0 || stage != NpcCompanyListingStage.INFORMATION_PUBLISHED) {
            throw new IllegalStateException("심사 전 단계에서만 상장을 연기할 수 있습니다.");
        }
        targetElapsedDay = Math.addExact(targetElapsedDay, days);
        postponementCount++;
    }

    public void shiftTarget(int days) {
        if (days <= 0 || stage == NpcCompanyListingStage.LISTED) {
            return;
        }
        targetElapsedDay = Math.addExact(targetElapsedDay, days);
    }

    public void confirmOffer(long offerPrice, int demandBasisPoints) {
        if (offerPrice <= 0 || stage != NpcCompanyListingStage.REVIEWED) {
            throw new IllegalStateException("심사 승인 후에만 공모가를 확정할 수 있습니다.");
        }
        this.offerPrice = offerPrice;
        this.demandBasisPoints = demandBasisPoints;
        advanceTo(NpcCompanyListingStage.OFFER_CONFIRMED);
    }

    public void markListed(int elapsedDay) {
        if (stage != NpcCompanyListingStage.OFFER_CONFIRMED || elapsedDay < 0) {
            throw new IllegalStateException("공모가 확정 후에만 상장을 완료할 수 있습니다.");
        }
        listedElapsedDay = elapsedDay;
        advanceTo(NpcCompanyListingStage.LISTED);
    }

    public void subscribe(long requestedQuantity, long reservedAmount, long fee) {
        if (stage != NpcCompanyListingStage.OFFER_CONFIRMED) {
            throw new IllegalStateException("공모가 확정된 청약 기간에만 신청할 수 있습니다.");
        }
        if (hasSubscription() || requestedQuantity <= 0 || reservedAmount <= 0 || fee < 0) {
            throw new IllegalStateException("유효하지 않은 공모주 청약입니다.");
        }
        subscriptionRequestedQuantity = requestedQuantity;
        subscriptionReservedAmount = reservedAmount;
        subscriptionFee = fee;
        subscriptionSettled = false;
        subscriptionResultAcknowledged = false;
    }

    public void settleSubscription(long allocatedQuantity, long refundAmount) {
        if (!hasSubscription() || isSubscriptionSettled() || allocatedQuantity < 0 || refundAmount < 0) {
            throw new IllegalStateException("정산할 수 없는 공모주 청약입니다.");
        }
        subscriptionAllocatedQuantity = allocatedQuantity;
        subscriptionRefundAmount = refundAmount;
        subscriptionSettled = true;
    }

    public void acknowledgeSubscriptionResult() {
        if (!isSubscriptionSettled()) {
            throw new IllegalStateException("아직 확정되지 않은 청약 결과입니다.");
        }
        subscriptionResultAcknowledged = true;
    }

    public Player getPlayer() { return player; }
    public String getStockKey() { return stockKey; }
    public NpcCompanyListingStage getStage() { return stage; }
    public int getTargetElapsedDay() { return targetElapsedDay; }
    public int getPostponementCount() { return postponementCount; }
    public long getOfferPrice() { return offerPrice == null ? 0 : offerPrice; }
    public int getDemandBasisPoints() { return demandBasisPoints == null ? 0 : demandBasisPoints; }
    /** 이전 저장 데이터의 상장일이 비어 있으면 확정된 목표 상장일을 사용한다. */
    public int getListedElapsedDay() {
        return listedElapsedDay == null && isListed() ? targetElapsedDay
                : listedElapsedDay == null ? 0 : listedElapsedDay;
    }
    public long getSubscriptionRequestedQuantity() {
        return subscriptionRequestedQuantity == null ? 0 : subscriptionRequestedQuantity;
    }
    public long getSubscriptionReservedAmount() {
        return subscriptionReservedAmount == null ? 0 : subscriptionReservedAmount;
    }
    public long getSubscriptionFee() { return subscriptionFee == null ? 0 : subscriptionFee; }
    public long getSubscriptionAllocatedQuantity() {
        return subscriptionAllocatedQuantity == null ? 0 : subscriptionAllocatedQuantity;
    }
    public long getSubscriptionRefundAmount() {
        return subscriptionRefundAmount == null ? 0 : subscriptionRefundAmount;
    }
    public boolean hasSubscription() {
        return subscriptionRequestedQuantity != null && subscriptionRequestedQuantity > 0;
    }
    public boolean isSubscriptionSettled() { return Boolean.TRUE.equals(subscriptionSettled); }
    public boolean isSubscriptionResultAcknowledged() {
        return Boolean.TRUE.equals(subscriptionResultAcknowledged);
    }
    public boolean isListed() { return stage == NpcCompanyListingStage.LISTED; }
}
