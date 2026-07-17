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

    // 현금은 부동산, 선물, 명품, 기부처럼 기본 경제 활동에 쓰는 메인 재화다.
    private long cash;
    // 코인은 주식 거래 전용 재화다. 기존 저장 데이터에는 null일 수 있어 getter에서 0으로 보정한다.
    private Long coin = 0L;
    // 스토리를 완료해야 메인 게임이 열린다. 완료 시 초기 현금과 첫 건물이 지급된다.
    private boolean storySeen;
    // 첫 임차인 이벤트는 한 번만 발생해야 하므로 완료 여부를 플레이어 상태에 저장한다.
    private Boolean firstTenantEventDone = false;
    // 이벤트 모달, 일시정지 버튼, 특정 선택지 처리 중에는 자동 날짜 진행을 막는다.
    private Boolean paused = false;
    // 현재 선택된 도시는 매물 화면, 비서 배치 효과, 도시 이벤트의 기준이 된다.
    private String currentCity = "청주";
    // 평판은 도시 해금, 칭호, 일부 기능 접근 조건에 쓰이는 성장 자원이다.
    private int reputation = 0;
    private String title = "첫 건물주";
    private Long cumulativeDonation = 0L;
    private String rewardedBuildingMilestones = "";
    private Integer economyVersion = 2;
    // 퇴사 전에는 월급을 받지만, 일부 평판 조건에는 고용 상태가 반대로 작동한다.
    private Boolean employed = true;
    private Boolean firstSecretaryHired = false;
    // elapsedDays는 월/일과 별개로 흐른 총 일수다. 쿨다운과 5일 주가 갱신처럼 절대 시간이 필요한 기능에 쓴다.
    private Integer elapsedDays = 1;
    // 다음 부동산 매물 갱신일이다. tick에서 현재 elapsedDays가 이 값 이상이면 새 매물을 만든다.
    private Integer nextOfferRefreshDay = 6;
    @Column(name = "game_month")
    private int month = 1;

    @Column(name = "game_day")
    private int day = 1;
    // 아래 monthly* 값들은 이번 달 누적 기록이다. 월이 바뀌면 SettlementService가 월간 기록으로 남기고 초기화한다.
    private long monthlyRentIncome;
    private long monthlySideIncome;
    private Integer lastSideJobElapsedDay;
    private Long monthlySalaryIncome = 0L;
    private long monthlyAdCost;
    private long monthlySecretarySalary;
    private long monthlyLoanPayment;
    // 도시 랜덤 이벤트는 매달 날짜를 미리 뽑아 저장한다. 저장하지 않으면 매 tick마다 결과가 바뀔 수 있다.
    private Integer eventScheduleMonth;
    private Integer eventScheduleCycle;
    private Integer moveInEventDayOne;
    private Integer moveInEventDayTwo;
    private Integer moveOutEventDayOne;
    private Integer moveOutEventDayTwo;
    private Integer repairEventDay;
    private Integer repairEventDayTwo;
    private Integer moveInChancePercent = 35;
    private Integer moveOutChancePercent = 18;
    private Integer repairRequestChancePercent = 10;
    private String dismissedSecretaryOfferKeys = "";
    // 부동산 뉴스 이벤트 예약/활성 상태다. 예약은 "이번 달 며칠에 뉴스가 날지", active는 "현재 가격 효과가 남았는지"를 뜻한다.
    private Integer marketNewsScheduleMonth;
    private Integer marketNewsScheduleCycle;
    private Integer marketNewsEventDay;
    private String marketNewsEventCity;
    private String marketNewsEventTrend;
    private String activeMarketNewsCity;
    private String activeMarketNewsTrend;
    private Integer activeMarketNewsRefreshesLeft = 0;
    // 서울 진출 후 바로 주식이 열리지 않고, 지정된 elapsedDays에 주식 개방 이벤트가 뜬다.
    private Integer stockUnlockAvailableDay;
    private Boolean stockContentUnlocked = false;
    private Boolean stockUnlockNoticeShown = false;
    // 주식 업종 뉴스 이벤트 예약/활성 상태다. activeStockNewsRefreshesLeft는 앞으로 몇 번의 주가 갱신에 효과가 남았는지다.
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
        if (amount < 0) {
            throw new IllegalArgumentException("추가 현금은 음수일 수 없습니다");
        }
        this.cash = Math.addExact(this.cash, amount);
    }

    public boolean spendCash(long amount) {
        // spend 계열 메서드는 성공 여부를 boolean으로 돌려준다. 서비스는 이 값으로 "현금 부족" 같은 메시지를 결정한다.
        if (amount < 0 || cash < amount) {
            return false;
        }
        cash -= amount;
        return true;
    }

    public void addCoin(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("추가 코인은 음수일 수 없습니다");
        }
        this.coin = Math.addExact(getCoin(), amount);
    }

    public boolean spendCoin(long amount) {
        if (amount < 0 || getCoin() < amount) {
            return false;
        }
        coin = getCoin() - amount;
        return true;
    }

    public boolean isStorySeen() {
        return storySeen;
    }

    public void completeStory() {
        // 스토리 완료는 새 게임의 경제 시작점이다. 여기서 초기 자금과 칭호를 한 번에 확정한다.
        this.storySeen = true;
        this.cash = 2_000_000L;
        this.title = "첫 건물주";
        this.reputation = 0;
        this.cumulativeDonation = 0L;
        this.rewardedBuildingMilestones = "";
    }

    public void advanceDay() {
        // 이 게임은 한 달을 실제 달력 길이처럼 처리하지 않고 daysInMonth 규칙으로 순환시킨다.
        // elapsedDays는 월이 12월에서 1월로 돌아가도 계속 증가하므로 쿨다운 계산에 안전하다.
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
        // 퇴사는 한 번만 보상을 지급해야 하므로 현재 employed 상태를 먼저 확인한다.
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
        lastSideJobElapsedDay = getElapsedDays();
    }

    public boolean canDoSideJobToday() {
        return lastSideJobElapsedDay == null || lastSideJobElapsedDay != getElapsedDays();
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

    public long getCumulativeDonation() {
        return cumulativeDonation == null ? 0L : cumulativeDonation;
    }

    public int getEconomyVersion() {
        return economyVersion == null ? 1 : economyVersion;
    }

    public void addDonation(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("기부액은 음수일 수 없습니다");
        }
        cumulativeDonation = Math.addExact(getCumulativeDonation(), amount);
    }

    public boolean claimBuildingMilestone(String city, int slot) {
        String milestone = "|" + city + ":" + slot + "|";
        String claimed = rewardedBuildingMilestones == null ? "" : rewardedBuildingMilestones;
        if (claimed.contains(milestone)) {
            return false;
        }
        rewardedBuildingMilestones = claimed + milestone;
        return true;
    }

    public void setReputationForTest(int reputation) {
        this.reputation = Math.max(0, reputation);
    }

    public boolean paySecretarySalary(long amount) {
        if (!spendCash(amount)) {
            return false;
        }
        monthlySecretarySalary = Math.addExact(monthlySecretarySalary, amount);
        return true;
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

    public boolean payLoanInterest(long amount) {
        if (!spendCash(amount)) {
            return false;
        }
        monthlyLoanPayment = Math.addExact(monthlyLoanPayment, amount);
        return true;
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
            return 18;
        }
        return moveOutChancePercent == null ? 18 : moveOutChancePercent;
    }

    public int getRepairRequestChancePercent() {
        if (hasLegacyDefaultChances()) {
            return 10;
        }
        return repairRequestChancePercent == null ? 10 : repairRequestChancePercent;
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
        // currentScheduleCycle까지 비교하는 이유: 12월 다음에 1월로 돌아왔을 때 작년 1월 예약을 재사용하지 않기 위해서다.
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
        // 랜덤 날짜를 한 번 뽑아 저장하면 같은 달 안에서는 새로고침이나 tick마다 이벤트 날짜가 흔들리지 않는다.
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
        // -1은 "이번 달에는 뉴스 없음"을 뜻한다. null로 두면 아직 스케줄을 안 만든 상태와 구분되지 않는다.
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
        // 예약된 뉴스를 active 상태로 옮긴다. 이후 매물 갱신이 일어날 때마다 refreshesLeft가 1씩 줄어든다.
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
        // 뉴스 효과는 해당 도시 매물 갱신에만 소비된다. 다른 도시 갱신으로 지속 시간이 줄면 안 된다.
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
        // 한 번 예약된 주식 개방일은 앞당기거나 덮어쓰지 않는다. 중복 예약 이벤트를 방지하기 위한 방어 코드다.
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
        // 부동산 뉴스와 동일하게 -1은 "이번 달 주식 뉴스 없음"을 의미한다.
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
