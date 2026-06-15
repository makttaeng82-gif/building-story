package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Loan {
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

    public long getTotalRepayment() {
        return totalRepayment;
    }

    public int getRemainingMonths() {
        return remainingMonths;
    }

    public long getMonthlyPayment() {
        return monthlyPayment;
    }
}
