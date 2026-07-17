package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Loan {
    /*
     * 건물 대출 한 건이다.
     *
     * principal은 남은 원금이고, monthlyPayment는 매월 상환되는 금액이다.
     * 월초 정산에서 상환 후 원금이 0이 되면 대출은 종료된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @OneToOne(fetch = FetchType.LAZY)
    private OwnedBuilding building;

    private long principal;
    private Long originalPrincipal;
    private long totalRepayment;
    private int remainingMonths;
    private long monthlyPayment;
    private Integer delinquentMonths = 0;

    protected Loan() {
    }

    public Loan(Player player, long principal) {
        this(player, null, principal);
    }

    public Loan(Player player, OwnedBuilding building, long principal) {
        this.player = player;
        this.building = building;
        this.principal = principal;
        this.originalPrincipal = principal;
        this.totalRepayment = principal;
        this.remainingMonths = 24;
        this.monthlyPayment = monthlyInterest(principal);
    }

    public long getPrincipal() {
        return principal;
    }

    public Player getPlayer() {
        return player;
    }

    public OwnedBuilding getBuilding() {
        return building;
    }

    public long getOriginalPrincipal() {
        return originalPrincipal == null ? principal : originalPrincipal;
    }

    public Long getId() {
        return id;
    }

    public long getTotalRepayment() {
        return totalRepayment;
    }

    public int getRemainingMonths() {
        return remainingMonths;
    }

    public long getMonthlyPayment() {
        return monthlyPayment;
    }

    public long remainingRepayment() {
        return principal;
    }

    public void advanceMonth() {
        if (remainingMonths > 0) {
            remainingMonths--;
        }
    }

    public boolean isMatured() {
        return remainingMonths <= 0;
    }

    public void refinance() {
        this.remainingMonths = 24;
    }

    public int getDelinquentMonths() {
        return delinquentMonths == null ? 0 : delinquentMonths;
    }

    public void recordPayment() {
        delinquentMonths = 0;
    }

    public void recordDelinquency() {
        delinquentMonths = getDelinquentMonths() + 1;
    }

    private long monthlyInterest(long amount) {
        long quotient = amount / 250;
        return amount % 250 == 0 ? quotient : quotient + 1;
    }
}
