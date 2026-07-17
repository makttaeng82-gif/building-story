package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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

    private long principal;
    private long totalRepayment;
    private int remainingMonths;
    private long monthlyPayment;

    protected Loan() {
    }

    public Loan(Player player, long principal) {
        this.player = player;
        this.principal = principal;
        this.totalRepayment = principal * 120 / 100;
        this.remainingMonths = 6;
        this.monthlyPayment = totalRepayment / 6;
    }

    public long getPrincipal() {
        return principal;
    }

    public Player getPlayer() {
        return player;
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
        return monthlyPayment * remainingMonths;
    }

    public void advanceMonth() {
        if (remainingMonths > 0) {
            remainingMonths--;
        }
    }

    public boolean isMatured() {
        return remainingMonths <= 0;
    }

    public void extendGracePeriod() {
        this.remainingMonths = 6;
    }
}
