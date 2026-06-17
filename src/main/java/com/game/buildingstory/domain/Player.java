package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

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
    private Integer eventScheduleMonth;
    private Integer eventScheduleCycle;
    private Integer moveInEventDayOne;
    private Integer moveInEventDayTwo;
    private Integer moveOutEventDayOne;
    private Integer moveOutEventDayTwo;
    private Integer repairEventDay;
    private Integer moveInChancePercent = 40;
    private Integer moveOutChancePercent = 20;
    private Integer repairRequestChancePercent = 30;
    private String dismissedSecretaryOfferKeys = "";

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
        if (day > daysInMonth(month)) {
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

    public void togglePause() {
        this.paused = !isPaused();
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

    public void leaveJob() {
        employed = false;
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
        return Math.max(0, getNextOfferRefreshDay() - getElapsedDays());
    }

    public int offerRefreshProgressPercent() {
        int daysLeft = Math.min(5, offerRefreshDday());
        return Math.max(0, Math.min(100, (5 - daysLeft) * 100 / 5));
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getDaysInCurrentMonth() {
        return daysInMonth(month);
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
        reputation = Math.max(0, reputation + amount);
    }

    public void setReputationForTest(int reputation) {
        this.reputation = Math.max(0, reputation);
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

    public int getMoveInChancePercent() {
        return moveInChancePercent == null ? 40 : moveInChancePercent;
    }

    public int getMoveOutChancePercent() {
        return moveOutChancePercent == null ? 20 : moveOutChancePercent;
    }

    public int getRepairRequestChancePercent() {
        return repairRequestChancePercent == null ? 30 : repairRequestChancePercent;
    }

    public void updateTestChances(int moveInChancePercent, int moveOutChancePercent, int repairRequestChancePercent) {
        this.moveInChancePercent = clampPercent(moveInChancePercent);
        this.moveOutChancePercent = clampPercent(moveOutChancePercent);
        this.repairRequestChancePercent = clampPercent(repairRequestChancePercent);
    }

    public boolean canResign() {
        return getElapsedDays() > 30;
    }

    public int daysUntilResignAvailable() {
        return Math.max(0, 31 - getElapsedDays());
    }

    public String dateTextAfterDays(int days) {
        int targetMonth = month;
        int targetDay = day + Math.max(0, days);
        while (targetDay > daysInMonth(targetMonth)) {
            targetDay -= daysInMonth(targetMonth);
            targetMonth++;
            if (targetMonth > 12) {
                targetMonth = 1;
            }
        }
        return targetMonth + "월 " + targetDay + "일";
    }

    public String ddayText(int days) {
        int safeDays = Math.max(0, days);
        if (safeDays == 0) {
            return "오늘";
        }
        return dateTextAfterDays(safeDays) + " · D-" + safeDays;
    }

    public boolean isSecretaryOfferDismissed(String key) {
        return dismissedSecretaryOfferKeys != null && List.of(dismissedSecretaryOfferKeys.split(",")).contains(key);
    }

    public void dismissSecretaryOffer(String key) {
        if (key == null || key.isBlank() || isSecretaryOfferDismissed(key)) {
            return;
        }
        dismissedSecretaryOfferKeys = dismissedSecretaryOfferKeys == null || dismissedSecretaryOfferKeys.isBlank()
                ? key
                : dismissedSecretaryOfferKeys + "," + key;
    }

    public boolean hasEventScheduleForCurrentMonth() {
        return eventScheduleMonth != null && eventScheduleMonth == month
                && eventScheduleCycle != null && eventScheduleCycle == currentScheduleCycle()
                && moveInEventDayOne != null
                && moveInEventDayTwo != null
                && moveOutEventDayOne != null
                && moveOutEventDayTwo != null
                && repairEventDay != null;
    }

    public void scheduleMonthlyRandomEvents(int moveInEventDayOne, int moveInEventDayTwo, int moveOutEventDayOne, int moveOutEventDayTwo, int repairEventDay) {
        this.eventScheduleMonth = month;
        this.eventScheduleCycle = currentScheduleCycle();
        this.moveInEventDayOne = moveInEventDayOne;
        this.moveInEventDayTwo = moveInEventDayTwo;
        this.moveOutEventDayOne = moveOutEventDayOne;
        this.moveOutEventDayTwo = moveOutEventDayTwo;
        this.repairEventDay = repairEventDay;
    }

    public boolean isMoveInEventDay() {
        return day == valueOrImpossible(moveInEventDayOne) || day == valueOrImpossible(moveInEventDayTwo);
    }

    public boolean isMoveOutEventDay() {
        return day == valueOrImpossible(moveOutEventDayOne) || day == valueOrImpossible(moveOutEventDayTwo);
    }

    public boolean isRepairEventDay() {
        return day == valueOrImpossible(repairEventDay);
    }

    private int valueOrImpossible(Integer value) {
        return value == null ? -1 : value;
    }

    private int currentScheduleCycle() {
        int completedYears = Math.max(0, getElapsedDays() - getDay()) / 365;
        return completedYears * 12 + month;
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int daysInMonth(int month) {
        return switch (month) {
            case 2 -> 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
}
