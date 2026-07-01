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
    private Long coin = 0L;
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
    private Integer repairEventDayTwo;
    private Integer moveInChancePercent = 35;
    private Integer moveOutChancePercent = 25;
    private Integer repairRequestChancePercent = 35;
    private String dismissedSecretaryOfferKeys = "";
    private Integer marketNewsScheduleMonth;
    private Integer marketNewsScheduleCycle;
    private Integer marketNewsEventDay;
    private String marketNewsEventCity;
    private String marketNewsEventTrend;
    private String activeMarketNewsCity;
    private String activeMarketNewsTrend;
    private Integer activeMarketNewsRefreshesLeft = 0;
    private Integer stockUnlockAvailableDay;
    private Boolean stockContentUnlocked = false;
    private Boolean stockUnlockNoticeShown = false;
    private Integer stockNewsScheduleMonth;
    private Integer stockNewsScheduleCycle;
    private Integer stockNewsEventDay;
    private String stockNewsEventIndustry;
    private String stockNewsEventTrend;
    private String activeStockNewsIndustry;
    private String activeStockNewsTrend;
    private Integer activeStockNewsRefreshesLeft = 0;

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

    public long getCoin() {
        return coin == null ? 0L : coin;
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

    public void addCoin(long amount) {
        this.coin = getCoin() + amount;
    }

    public boolean spendCoin(long amount) {
        if (getCoin() < amount) {
            return false;
        }
        coin = getCoin() - amount;
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
        if (hasLegacyDefaultChances()) {
            return 35;
        }
        return moveInChancePercent == null ? 35 : moveInChancePercent;
    }

    public int getMoveOutChancePercent() {
        if (hasLegacyDefaultChances()) {
            return 25;
        }
        return moveOutChancePercent == null ? 25 : moveOutChancePercent;
    }

    public int getRepairRequestChancePercent() {
        if (hasLegacyDefaultChances()) {
            return 35;
        }
        return repairRequestChancePercent == null ? 35 : repairRequestChancePercent;
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
                && repairEventDay != null
                && repairEventDayTwo != null;
    }

    public void scheduleMonthlyRandomEvents(int moveInEventDayOne, int moveInEventDayTwo, int moveOutEventDayOne, int moveOutEventDayTwo, int repairEventDay, int repairEventDayTwo) {
        this.eventScheduleMonth = month;
        this.eventScheduleCycle = currentScheduleCycle();
        this.moveInEventDayOne = moveInEventDayOne;
        this.moveInEventDayTwo = moveInEventDayTwo;
        this.moveOutEventDayOne = moveOutEventDayOne;
        this.moveOutEventDayTwo = moveOutEventDayTwo;
        this.repairEventDay = repairEventDay;
        this.repairEventDayTwo = repairEventDayTwo;
    }

    public boolean isMoveInEventDay() {
        return day == valueOrImpossible(moveInEventDayOne) || day == valueOrImpossible(moveInEventDayTwo);
    }

    public boolean isMoveOutEventDay() {
        return day == valueOrImpossible(moveOutEventDayOne) || day == valueOrImpossible(moveOutEventDayTwo);
    }

    public boolean isRepairEventDay() {
        return day == valueOrImpossible(repairEventDay) || day == valueOrImpossible(repairEventDayTwo);
    }

    public boolean hasMarketNewsScheduleForCurrentMonth() {
        return marketNewsScheduleMonth != null && marketNewsScheduleMonth == month
                && marketNewsScheduleCycle != null && marketNewsScheduleCycle == currentScheduleCycle()
                && marketNewsEventDay != null;
    }

    public void scheduleNoMonthlyMarketNews() {
        this.marketNewsScheduleMonth = month;
        this.marketNewsScheduleCycle = currentScheduleCycle();
        this.marketNewsEventDay = -1;
        this.marketNewsEventCity = null;
        this.marketNewsEventTrend = null;
    }

    public void scheduleMonthlyMarketNews(int eventDay, String city, String trend) {
        this.marketNewsScheduleMonth = month;
        this.marketNewsScheduleCycle = currentScheduleCycle();
        this.marketNewsEventDay = eventDay;
        this.marketNewsEventCity = city;
        this.marketNewsEventTrend = trend;
    }

    public boolean isMarketNewsEventDay() {
        return day == valueOrImpossible(marketNewsEventDay)
                && marketNewsEventCity != null
                && marketNewsEventTrend != null;
    }

    public String getMarketNewsEventCity() {
        return marketNewsEventCity;
    }

    public String getMarketNewsEventTrend() {
        return marketNewsEventTrend;
    }

    public void activateMarketNews() {
        this.activeMarketNewsCity = marketNewsEventCity;
        this.activeMarketNewsTrend = marketNewsEventTrend;
        this.activeMarketNewsRefreshesLeft = 2;
        this.marketNewsEventDay = -1;
    }

    public boolean hasActiveMarketNewsForCity(String city) {
        return city != null
                && city.equals(activeMarketNewsCity)
                && activeMarketNewsTrend != null
                && getActiveMarketNewsRefreshesLeft() > 0;
    }

    public String getActiveMarketNewsCity() {
        return activeMarketNewsCity;
    }

    public String getActiveMarketNewsTrend() {
        return activeMarketNewsTrend;
    }

    public int getActiveMarketNewsRefreshesLeft() {
        return activeMarketNewsRefreshesLeft == null ? 0 : activeMarketNewsRefreshesLeft;
    }

    public void consumeMarketNewsRefresh(String city) {
        if (!hasActiveMarketNewsForCity(city)) {
            return;
        }
        activeMarketNewsRefreshesLeft = getActiveMarketNewsRefreshesLeft() - 1;
        if (activeMarketNewsRefreshesLeft <= 0) {
            activeMarketNewsCity = null;
            activeMarketNewsTrend = null;
            activeMarketNewsRefreshesLeft = 0;
        }
    }

    public void scheduleStockUnlock(int availableDay) {
        if (stockUnlockAvailableDay == null) {
            stockUnlockAvailableDay = Math.max(getElapsedDays(), availableDay);
        }
    }

    public boolean hasStockUnlockSchedule() {
        return stockUnlockAvailableDay != null;
    }

    public boolean isStockContentUnlocked() {
        return Boolean.TRUE.equals(stockContentUnlocked);
    }

    public boolean isStockUnlockNoticeShown() {
        return Boolean.TRUE.equals(stockUnlockNoticeShown);
    }

    public boolean isStockUnlockDue() {
        return !isStockContentUnlocked()
                && stockUnlockAvailableDay != null
                && getElapsedDays() >= stockUnlockAvailableDay;
    }

    public void unlockStockContent() {
        stockContentUnlocked = true;
    }

    public void markStockUnlockNoticeShown() {
        stockUnlockNoticeShown = true;
    }

    public boolean hasStockNewsScheduleForCurrentMonth() {
        return stockNewsScheduleMonth != null && stockNewsScheduleMonth == month
                && stockNewsScheduleCycle != null && stockNewsScheduleCycle == currentScheduleCycle()
                && stockNewsEventDay != null;
    }

    public void scheduleNoMonthlyStockNews() {
        this.stockNewsScheduleMonth = month;
        this.stockNewsScheduleCycle = currentScheduleCycle();
        this.stockNewsEventDay = -1;
        this.stockNewsEventIndustry = null;
        this.stockNewsEventTrend = null;
    }

    public void scheduleMonthlyStockNews(int eventDay, String industry, String trend) {
        this.stockNewsScheduleMonth = month;
        this.stockNewsScheduleCycle = currentScheduleCycle();
        this.stockNewsEventDay = eventDay;
        this.stockNewsEventIndustry = industry;
        this.stockNewsEventTrend = trend;
    }

    public boolean isStockNewsEventDay() {
        return day == valueOrImpossible(stockNewsEventDay)
                && stockNewsEventIndustry != null
                && stockNewsEventTrend != null;
    }

    public String getStockNewsEventIndustry() {
        return stockNewsEventIndustry;
    }

    public String getStockNewsEventTrend() {
        return stockNewsEventTrend;
    }

    public void activateStockNews() {
        this.activeStockNewsIndustry = stockNewsEventIndustry;
        this.activeStockNewsTrend = stockNewsEventTrend;
        this.activeStockNewsRefreshesLeft = 2;
        this.stockNewsEventDay = -1;
    }

    public boolean hasActiveStockNewsForIndustry(String industry) {
        return industry != null
                && industry.equals(activeStockNewsIndustry)
                && activeStockNewsTrend != null
                && getActiveStockNewsRefreshesLeft() > 0;
    }

    public String getActiveStockNewsIndustry() {
        return activeStockNewsIndustry;
    }

    public String getActiveStockNewsTrend() {
        return activeStockNewsTrend;
    }

    public int getActiveStockNewsRefreshesLeft() {
        return activeStockNewsRefreshesLeft == null ? 0 : activeStockNewsRefreshesLeft;
    }

    public void consumeStockNewsRefresh() {
        if (getActiveStockNewsRefreshesLeft() <= 0) {
            return;
        }
        activeStockNewsRefreshesLeft = getActiveStockNewsRefreshesLeft() - 1;
        if (activeStockNewsRefreshesLeft <= 0) {
            activeStockNewsIndustry = null;
            activeStockNewsTrend = null;
            activeStockNewsRefreshesLeft = 0;
        }
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

    private boolean hasLegacyDefaultChances() {
        return Integer.valueOf(40).equals(moveInChancePercent)
                && Integer.valueOf(20).equals(moveOutChancePercent)
                && Integer.valueOf(30).equals(repairRequestChancePercent);
    }

    private int daysInMonth(int month) {
        return switch (month) {
            case 2 -> 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
}
