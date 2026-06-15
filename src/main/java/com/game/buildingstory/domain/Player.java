package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private long cash;
    private boolean storySeen;
    private Boolean firstTenantEventDone = false;
    private Boolean paused = false;
    private String currentCity = "청주";
    private int reputation = 0;
    private String title = "회사의 최하급노예";
    private Boolean employed = true;
    private Boolean firstSecretaryHired = false;
    private Integer elapsedDays = 1;
    private Integer nextOfferRefreshDay = 6;
    @Column(name = "game_month")
    private int month = 1;

    @Column(name = "game_day")
    private int day = 1;
    private long monthlyRentIncome;
    private long monthlySideIncome;
    private Long monthlySalaryIncome = 0L;
    private long monthlyAdCost;
    private long monthlySecretarySalary;
    private long monthlyLoanPayment;

    protected Player() {
    }

    public Player(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getCash() {
        return cash;
    }

    public void addCash(long amount) {
        this.cash += amount;
    }

    public boolean spendCash(long amount) {
        if (cash < amount) {
            return false;
        }
        cash -= amount;
        return true;
    }

    public boolean isStorySeen() {
        return storySeen;
    }

    public void completeStory() {
        this.storySeen = true;
        this.cash = 2_000_000L;
        this.title = "회사의 최하급노예";
        this.reputation = 0;
    }

    public void advanceDay() {
        day++;
        elapsedDays = getElapsedDays() + 1;
        if (day > 31) {
            day = 1;
            month++;
            if (month > 12) {
                month = 1;
            }
        }
    }

    public boolean isFirstTenantEventDue() {
        return storySeen && !Boolean.TRUE.equals(firstTenantEventDone) && month == 1 && day == 3;
    }

    public void markFirstTenantEventDone() {
        this.firstTenantEventDone = true;
    }

    public boolean isPaused() {
        return Boolean.TRUE.equals(paused);
    }

    public void pause() {
        this.paused = true;
    }

    public void resume() {
        this.paused = false;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void changeCity(String city) {
        this.currentCity = city;
    }

    public int getReputation() {
        return reputation;
    }

    public String getTitle() {
        return title;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public boolean isEmployed() {
        return !Boolean.FALSE.equals(employed);
    }

    public void resign() {
        if (isEmployed()) {
            employed = false;
            cash += 30_000_000L;
        }
    }

    public boolean isFirstSecretaryHired() {
        return Boolean.TRUE.equals(firstSecretaryHired);
    }

    public void hireFirstSecretary() {
        this.firstSecretaryHired = true;
    }

    public int getElapsedDays() {
        return elapsedDays == null ? 1 : elapsedDays;
    }

    public int getNextOfferRefreshDay() {
        return nextOfferRefreshDay == null ? getElapsedDays() + 5 : nextOfferRefreshDay;
    }

    public void scheduleNextOfferRefresh() {
        this.nextOfferRefreshDay = getElapsedDays() + 5;
    }

    public int offerRefreshDday() {
        return Math.max(0, nextOfferRefreshDay - elapsedDays);
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public long getMonthlyRentIncome() {
        return monthlyRentIncome;
    }

    public void addMonthlyRentIncome(long amount) {
        monthlyRentIncome += amount;
        cash += amount;
    }

    public long getMonthlySideIncome() {
        return monthlySideIncome;
    }

    public void addSideIncome(long amount) {
        monthlySideIncome += amount;
        cash += amount;
    }

    public long getMonthlySalaryIncome() {
        return monthlySalaryIncome == null ? 0L : monthlySalaryIncome;
    }

    public void addSalaryIncome(long amount) {
        monthlySalaryIncome = getMonthlySalaryIncome() + amount;
        cash += amount;
    }

    public void addReputation(int amount) {
        reputation += amount;
    }

    public void addSecretarySalaryCost(long amount) {
        monthlySecretarySalary += amount;
        cash -= amount;
    }

    public long getMonthlyAdCost() {
        return monthlyAdCost;
    }

    public long getMonthlySecretarySalary() {
        return monthlySecretarySalary;
    }

    public long getMonthlyLoanPayment() {
        return monthlyLoanPayment;
    }

    public long monthlyNetIncome() {
        return monthlyRentIncome + monthlySideIncome + getMonthlySalaryIncome() - monthlyAdCost - monthlySecretarySalary - monthlyLoanPayment;
    }
}
