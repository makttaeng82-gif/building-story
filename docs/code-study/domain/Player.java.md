# Player.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/Player.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
@Table(name = "players")
public class Player {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    @Id
    // 해설: 엔티티의 기본키 필드다. DB에서 한 행을 식별한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 해설: DB가 기본키 값을 자동 증가 방식으로 생성한다.
    private Long id;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false, unique = true, length = 40)
    private String username;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(nullable = false)
    private String passwordHash;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    // 현금은 부동산, 선물, 명품, 기부처럼 기본 경제 활동에 쓰는 메인 재화다.
    private long cash;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 코인은 주식 거래 전용 재화다. 기존 저장 데이터에는 null일 수 있어 getter에서 0으로 보정한다.
    private Long coin = 0L;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 스토리를 완료해야 메인 게임이 열린다. 완료 시 초기 현금과 첫 건물이 지급된다.
    private boolean storySeen;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 첫 임차인 이벤트는 한 번만 발생해야 하므로 완료 여부를 플레이어 상태에 저장한다.
    private Boolean firstTenantEventDone = false;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 이벤트 모달, 일시정지 버튼, 특정 선택지 처리 중에는 자동 날짜 진행을 막는다.
    private Boolean paused = false;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 현재 선택된 도시는 매물 화면, 비서 배치 효과, 도시 이벤트의 기준이 된다.
    private String currentCity = "청주";
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 평판은 도시 해금, 칭호, 일부 기능 접근 조건에 쓰이는 성장 자원이다.
    private int reputation = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String title = "회사의 최하급노예";
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 퇴사 전에는 월급을 받지만, 일부 평판 조건에는 고용 상태가 반대로 작동한다.
    private Boolean employed = true;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Boolean firstSecretaryHired = false;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // elapsedDays는 월/일과 별개로 흐른 총 일수다. 쿨다운과 5일 주가 갱신처럼 절대 시간이 필요한 기능에 쓴다.
    private Integer elapsedDays = 1;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 다음 부동산 매물 갱신일이다. tick에서 현재 elapsedDays가 이 값 이상이면 새 매물을 만든다.
    private Integer nextOfferRefreshDay = 6;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    @Column(name = "game_month")
    private int month = 1;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @Column(name = "game_day")
    private int day = 1;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 아래 monthly* 값들은 이번 달 누적 기록이다. 월이 바뀌면 SettlementService가 월간 기록으로 남기고 초기화한다.
    private long monthlyRentIncome;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlySideIncome;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Long monthlySalaryIncome = 0L;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlyAdCost;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlySecretarySalary;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private long monthlyLoanPayment;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 도시 랜덤 이벤트는 매달 날짜를 미리 뽑아 저장한다. 저장하지 않으면 매 tick마다 결과가 바뀔 수 있다.
    private Integer eventScheduleMonth;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer eventScheduleCycle;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveInEventDayOne;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveInEventDayTwo;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveOutEventDayOne;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveOutEventDayTwo;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer repairEventDay;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer repairEventDayTwo;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveInChancePercent = 35;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer moveOutChancePercent = 25;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer repairRequestChancePercent = 35;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String dismissedSecretaryOfferKeys = "";
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 부동산 뉴스 이벤트 예약/활성 상태다. 예약은 "이번 달 며칠에 뉴스가 날지", active는 "현재 가격 효과가 남았는지"를 뜻한다.
    private Integer marketNewsScheduleMonth;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer marketNewsScheduleCycle;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer marketNewsEventDay;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String marketNewsEventCity;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String marketNewsEventTrend;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String activeMarketNewsCity;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String activeMarketNewsTrend;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer activeMarketNewsRefreshesLeft = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 서울 진출 후 바로 주식이 열리지 않고, 지정된 elapsedDays에 주식 개방 이벤트가 뜬다.
    private Integer stockUnlockAvailableDay;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Boolean stockContentUnlocked = false;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Boolean stockUnlockNoticeShown = false;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    // 주식 업종 뉴스 이벤트 예약/활성 상태다. activeStockNewsRefreshesLeft는 앞으로 몇 번의 주가 갱신에 효과가 남았는지다.
    private Integer stockNewsScheduleMonth;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer stockNewsScheduleCycle;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer stockNewsEventDay;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String stockNewsEventIndustry;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String stockNewsEventTrend;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String activeStockNewsIndustry;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String activeStockNewsTrend;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer activeStockNewsRefreshesLeft = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected Player() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public Player(String username, String passwordHash) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
    // 해설: `getId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return id;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getUsername() {
    // 해설: `getUsername` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return username;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getPasswordHash() {
    // 해설: `getPasswordHash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return passwordHash;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getCash() {
    // 해설: `getCash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return cash;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getCoin() {
    // 해설: `getCoin` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return coin == null ? 0L : coin;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void addCash(long amount) {
    // 해설: `addCash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.cash += amount;
    }

    public boolean spendCash(long amount) {
    // 해설: `spendCash` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // spend 계열 메서드는 성공 여부를 boolean으로 돌려준다. 서비스는 이 값으로 "현금 부족" 같은 메시지를 결정한다.
        if (cash < amount) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return false;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        cash -= amount;
        return true;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void addCoin(long amount) {
    // 해설: `addCoin` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.coin = getCoin() + amount;
    }

    public boolean spendCoin(long amount) {
    // 해설: `spendCoin` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (getCoin() < amount) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return false;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        coin = getCoin() - amount;
        return true;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isStorySeen() {
    // 해설: `isStorySeen` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return storySeen;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void completeStory() {
    // 해설: `completeStory` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 스토리 완료는 새 게임의 경제 시작점이다. 여기서 초기 자금과 칭호를 한 번에 확정한다.
        this.storySeen = true;
        this.cash = 2_000_000L;
        this.title = "회사의 최하급노예";
        this.reputation = 0;
    }

    public void advanceDay() {
    // 해설: `advanceDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 이 게임은 한 달을 실제 달력 길이처럼 처리하지 않고 daysInMonth 규칙으로 순환시킨다.
        // elapsedDays는 월이 12월에서 1월로 돌아가도 계속 증가하므로 쿨다운 계산에 안전하다.
        day++;
        elapsedDays = getElapsedDays() + 1;
        if (day > daysInMonth(month)) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            day = 1;
            month++;
            if (month > 12) {
            // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
                month = 1;
            }
        }
    }

    public boolean isFirstTenantEventDue() {
    // 해설: `isFirstTenantEventDue` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return storySeen && !Boolean.TRUE.equals(firstTenantEventDone) && month == 1 && day == 3;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void markFirstTenantEventDone() {
    // 해설: `markFirstTenantEventDone` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.firstTenantEventDone = true;
    }

    public boolean isPaused() {
    // 해설: `isPaused` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(paused);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void pause() {
    // 해설: `pause` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.paused = true;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
    }

    public void resume() {
    // 해설: `resume` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.paused = false;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
    }

    public void togglePause() {
    // 해설: `togglePause` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.paused = !isPaused();
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
    }

    public String getCurrentCity() {
    // 해설: `getCurrentCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return currentCity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void changeCity(String city) {
    // 해설: `changeCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.currentCity = city;
    }

    public int getReputation() {
    // 해설: `getReputation` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return reputation;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getTitle() {
    // 해설: `getTitle` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return title;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void updateTitle(String title) {
    // 해설: `updateTitle` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.title = title;
    }

    public boolean isEmployed() {
    // 해설: `isEmployed` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return !Boolean.FALSE.equals(employed);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void resign() {
    // 해설: `resign` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 퇴사는 한 번만 보상을 지급해야 하므로 현재 employed 상태를 먼저 확인한다.
        if (isEmployed()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            employed = false;
            cash += 30_000_000L;
        }
    }

    public void leaveJob() {
    // 해설: `leaveJob` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        employed = false;
    }

    public boolean isFirstSecretaryHired() {
    // 해설: `isFirstSecretaryHired` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(firstSecretaryHired);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void hireFirstSecretary() {
    // 해설: `hireFirstSecretary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.firstSecretaryHired = true;
    }

    public int getElapsedDays() {
    // 해설: `getElapsedDays` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return elapsedDays == null ? 1 : elapsedDays;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getNextOfferRefreshDay() {
    // 해설: `getNextOfferRefreshDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return nextOfferRefreshDay == null ? getElapsedDays() + 5 : nextOfferRefreshDay;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void scheduleNextOfferRefresh() {
    // 해설: `scheduleNextOfferRefresh` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.nextOfferRefreshDay = getElapsedDays() + 5;
    }

    public int offerRefreshDday() {
    // 해설: `offerRefreshDday` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Math.max(0, getNextOfferRefreshDay() - getElapsedDays());
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int offerRefreshProgressPercent() {
    // 해설: `offerRefreshProgressPercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int daysLeft = Math.min(5, offerRefreshDday());
        return Math.max(0, Math.min(100, (5 - daysLeft) * 100 / 5));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getMonth() {
    // 해설: `getMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return month;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getDay() {
    // 해설: `getDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getDaysInCurrentMonth() {
    // 해설: `getDaysInCurrentMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return daysInMonth(month);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMonthlyRentIncome() {
    // 해설: `getMonthlyRentIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyRentIncome;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void addMonthlyRentIncome(long amount) {
    // 해설: `addMonthlyRentIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        monthlyRentIncome += amount;
        cash += amount;
    }

    public long getMonthlySideIncome() {
    // 해설: `getMonthlySideIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlySideIncome;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void addSideIncome(long amount) {
    // 해설: `addSideIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        monthlySideIncome += amount;
        cash += amount;
    }

    public long getMonthlySalaryIncome() {
    // 해설: `getMonthlySalaryIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlySalaryIncome == null ? 0L : monthlySalaryIncome;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void addSalaryIncome(long amount) {
    // 해설: `addSalaryIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        monthlySalaryIncome = getMonthlySalaryIncome() + amount;
        cash += amount;
    }

    public void addReputation(int amount) {
    // 해설: `addReputation` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        reputation = Math.max(0, reputation + amount);
    }

    public void setReputationForTest(int reputation) {
    // 해설: `setReputationForTest` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.reputation = Math.max(0, reputation);
    }

    public void addSecretarySalaryCost(long amount) {
    // 해설: `addSecretarySalaryCost` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        monthlySecretarySalary += amount;
        cash -= amount;
    }

    public long getMonthlyAdCost() {
    // 해설: `getMonthlyAdCost` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyAdCost;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMonthlySecretarySalary() {
    // 해설: `getMonthlySecretarySalary` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlySecretarySalary;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long getMonthlyLoanPayment() {
    // 해설: `getMonthlyLoanPayment` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyLoanPayment;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public long monthlyNetIncome() {
    // 해설: `monthlyNetIncome` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return monthlyRentIncome + monthlySideIncome + getMonthlySalaryIncome() - monthlyAdCost - monthlySecretarySalary - monthlyLoanPayment;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getMoveInChancePercent() {
    // 해설: `getMoveInChancePercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (hasLegacyDefaultChances()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 35;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return moveInChancePercent == null ? 35 : moveInChancePercent;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getMoveOutChancePercent() {
    // 해설: `getMoveOutChancePercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (hasLegacyDefaultChances()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 25;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return moveOutChancePercent == null ? 25 : moveOutChancePercent;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getRepairRequestChancePercent() {
    // 해설: `getRepairRequestChancePercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (hasLegacyDefaultChances()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 35;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return repairRequestChancePercent == null ? 35 : repairRequestChancePercent;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void updateTestChances(int moveInChancePercent, int moveOutChancePercent, int repairRequestChancePercent) {
    // 해설: `updateTestChances` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.moveInChancePercent = clampPercent(moveInChancePercent);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.moveOutChancePercent = clampPercent(moveOutChancePercent);
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.repairRequestChancePercent = clampPercent(repairRequestChancePercent);
    }

    public boolean canResign() {
    // 해설: `canResign` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return getElapsedDays() > 30;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int daysUntilResignAvailable() {
    // 해설: `daysUntilResignAvailable` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Math.max(0, 31 - getElapsedDays());
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String dateTextAfterDays(int days) {
    // 해설: `dateTextAfterDays` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int targetMonth = month;
        int targetDay = day + Math.max(0, days);
        while (targetDay > daysInMonth(targetMonth)) {
            targetDay -= daysInMonth(targetMonth);
            targetMonth++;
            if (targetMonth > 12) {
            // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
                targetMonth = 1;
            }
        }
        return targetMonth + "월 " + targetDay + "일";
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String ddayText(int days) {
    // 해설: `ddayText` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int safeDays = Math.max(0, days);
        if (safeDays == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return "오늘";
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return dateTextAfterDays(safeDays) + " · D-" + safeDays;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isSecretaryOfferDismissed(String key) {
    // 해설: `isSecretaryOfferDismissed` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return dismissedSecretaryOfferKeys != null && List.of(dismissedSecretaryOfferKeys.split(",")).contains(key);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void dismissSecretaryOffer(String key) {
    // 해설: `dismissSecretaryOffer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (key == null || key.isBlank() || isSecretaryOfferDismissed(key)) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        dismissedSecretaryOfferKeys = dismissedSecretaryOfferKeys == null || dismissedSecretaryOfferKeys.isBlank()
                ? key
                : dismissedSecretaryOfferKeys + "," + key;
    }

    public boolean hasEventScheduleForCurrentMonth() {
    // 해설: `hasEventScheduleForCurrentMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // currentScheduleCycle까지 비교하는 이유: 12월 다음에 1월로 돌아왔을 때 작년 1월 예약을 재사용하지 않기 위해서다.
        return eventScheduleMonth != null && eventScheduleMonth == month
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && eventScheduleCycle != null && eventScheduleCycle == currentScheduleCycle()
                && moveInEventDayOne != null
                && moveInEventDayTwo != null
                && moveOutEventDayOne != null
                && moveOutEventDayTwo != null
                && repairEventDay != null
                && repairEventDayTwo != null;
    }

    public void scheduleMonthlyRandomEvents(int moveInEventDayOne, int moveInEventDayTwo, int moveOutEventDayOne, int moveOutEventDayTwo, int repairEventDay, int repairEventDayTwo) {
    // 해설: `scheduleMonthlyRandomEvents` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 랜덤 날짜를 한 번 뽑아 저장하면 같은 달 안에서는 새로고침이나 tick마다 이벤트 날짜가 흔들리지 않는다.
        this.eventScheduleMonth = month;
        this.eventScheduleCycle = currentScheduleCycle();
        this.moveInEventDayOne = moveInEventDayOne;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.moveInEventDayTwo = moveInEventDayTwo;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.moveOutEventDayOne = moveOutEventDayOne;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.moveOutEventDayTwo = moveOutEventDayTwo;
        // 해설: 도메인 객체의 상태 변경 메서드를 호출한다. 실제 필드 변경은 해당 객체 안에서 캡슐화된다.
        this.repairEventDay = repairEventDay;
        this.repairEventDayTwo = repairEventDayTwo;
    }

    public boolean isMoveInEventDay() {
    // 해설: `isMoveInEventDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day == valueOrImpossible(moveInEventDayOne) || day == valueOrImpossible(moveInEventDayTwo);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isMoveOutEventDay() {
    // 해설: `isMoveOutEventDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day == valueOrImpossible(moveOutEventDayOne) || day == valueOrImpossible(moveOutEventDayTwo);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isRepairEventDay() {
    // 해설: `isRepairEventDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day == valueOrImpossible(repairEventDay) || day == valueOrImpossible(repairEventDayTwo);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean hasMarketNewsScheduleForCurrentMonth() {
    // 해설: `hasMarketNewsScheduleForCurrentMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketNewsScheduleMonth != null && marketNewsScheduleMonth == month
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && marketNewsScheduleCycle != null && marketNewsScheduleCycle == currentScheduleCycle()
                && marketNewsEventDay != null;
    }

    public void scheduleNoMonthlyMarketNews() {
    // 해설: `scheduleNoMonthlyMarketNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // -1은 "이번 달에는 뉴스 없음"을 뜻한다. null로 두면 아직 스케줄을 안 만든 상태와 구분되지 않는다.
        this.marketNewsScheduleMonth = month;
        this.marketNewsScheduleCycle = currentScheduleCycle();
        this.marketNewsEventDay = -1;
        this.marketNewsEventCity = null;
        this.marketNewsEventTrend = null;
    }

    public void scheduleMonthlyMarketNews(int eventDay, String city, String trend) {
    // 해설: `scheduleMonthlyMarketNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.marketNewsScheduleMonth = month;
        this.marketNewsScheduleCycle = currentScheduleCycle();
        this.marketNewsEventDay = eventDay;
        this.marketNewsEventCity = city;
        this.marketNewsEventTrend = trend;
    }

    public boolean isMarketNewsEventDay() {
    // 해설: `isMarketNewsEventDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day == valueOrImpossible(marketNewsEventDay)
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && marketNewsEventCity != null
                && marketNewsEventTrend != null;
    }

    public String getMarketNewsEventCity() {
    // 해설: `getMarketNewsEventCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketNewsEventCity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getMarketNewsEventTrend() {
    // 해설: `getMarketNewsEventTrend` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return marketNewsEventTrend;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void activateMarketNews() {
    // 해설: `activateMarketNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 예약된 뉴스를 active 상태로 옮긴다. 이후 매물 갱신이 일어날 때마다 refreshesLeft가 1씩 줄어든다.
        this.activeMarketNewsCity = marketNewsEventCity;
        this.activeMarketNewsTrend = marketNewsEventTrend;
        this.activeMarketNewsRefreshesLeft = 2;
        this.marketNewsEventDay = -1;
    }

    public boolean hasActiveMarketNewsForCity(String city) {
    // 해설: `hasActiveMarketNewsForCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return city != null
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && city.equals(activeMarketNewsCity)
                && activeMarketNewsTrend != null
                && getActiveMarketNewsRefreshesLeft() > 0;
    }

    public String getActiveMarketNewsCity() {
    // 해설: `getActiveMarketNewsCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeMarketNewsCity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getActiveMarketNewsTrend() {
    // 해설: `getActiveMarketNewsTrend` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeMarketNewsTrend;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getActiveMarketNewsRefreshesLeft() {
    // 해설: `getActiveMarketNewsRefreshesLeft` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeMarketNewsRefreshesLeft == null ? 0 : activeMarketNewsRefreshesLeft;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void consumeMarketNewsRefresh(String city) {
    // 해설: `consumeMarketNewsRefresh` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 뉴스 효과는 해당 도시 매물 갱신에만 소비된다. 다른 도시 갱신으로 지속 시간이 줄면 안 된다.
        if (!hasActiveMarketNewsForCity(city)) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        activeMarketNewsRefreshesLeft = getActiveMarketNewsRefreshesLeft() - 1;
        if (activeMarketNewsRefreshesLeft <= 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            activeMarketNewsCity = null;
            activeMarketNewsTrend = null;
            activeMarketNewsRefreshesLeft = 0;
        }
    }

    public void scheduleStockUnlock(int availableDay) {
    // 해설: `scheduleStockUnlock` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 한 번 예약된 주식 개방일은 앞당기거나 덮어쓰지 않는다. 중복 예약 이벤트를 방지하기 위한 방어 코드다.
        if (stockUnlockAvailableDay == null) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            stockUnlockAvailableDay = Math.max(getElapsedDays(), availableDay);
        }
    }

    public boolean hasStockUnlockSchedule() {
    // 해설: `hasStockUnlockSchedule` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return stockUnlockAvailableDay != null;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isStockContentUnlocked() {
    // 해설: `isStockContentUnlocked` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(stockContentUnlocked);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isStockUnlockNoticeShown() {
    // 해설: `isStockUnlockNoticeShown` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Boolean.TRUE.equals(stockUnlockNoticeShown);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean isStockUnlockDue() {
    // 해설: `isStockUnlockDue` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return !isStockContentUnlocked()
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && stockUnlockAvailableDay != null
                && getElapsedDays() >= stockUnlockAvailableDay;
    }

    public void unlockStockContent() {
    // 해설: `unlockStockContent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        stockContentUnlocked = true;
    }

    public void markStockUnlockNoticeShown() {
    // 해설: `markStockUnlockNoticeShown` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        stockUnlockNoticeShown = true;
    }

    public boolean hasStockNewsScheduleForCurrentMonth() {
    // 해설: `hasStockNewsScheduleForCurrentMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return stockNewsScheduleMonth != null && stockNewsScheduleMonth == month
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && stockNewsScheduleCycle != null && stockNewsScheduleCycle == currentScheduleCycle()
                && stockNewsEventDay != null;
    }

    public void scheduleNoMonthlyStockNews() {
    // 해설: `scheduleNoMonthlyStockNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        // 부동산 뉴스와 동일하게 -1은 "이번 달 주식 뉴스 없음"을 의미한다.
        this.stockNewsScheduleMonth = month;
        this.stockNewsScheduleCycle = currentScheduleCycle();
        this.stockNewsEventDay = -1;
        this.stockNewsEventIndustry = null;
        this.stockNewsEventTrend = null;
    }

    public void scheduleMonthlyStockNews(int eventDay, String industry, String trend) {
    // 해설: `scheduleMonthlyStockNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.stockNewsScheduleMonth = month;
        this.stockNewsScheduleCycle = currentScheduleCycle();
        this.stockNewsEventDay = eventDay;
        this.stockNewsEventIndustry = industry;
        this.stockNewsEventTrend = trend;
    }

    public boolean isStockNewsEventDay() {
    // 해설: `isStockNewsEventDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return day == valueOrImpossible(stockNewsEventDay)
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && stockNewsEventIndustry != null
                && stockNewsEventTrend != null;
    }

    public String getStockNewsEventIndustry() {
    // 해설: `getStockNewsEventIndustry` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return stockNewsEventIndustry;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getStockNewsEventTrend() {
    // 해설: `getStockNewsEventTrend` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return stockNewsEventTrend;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void activateStockNews() {
    // 해설: `activateStockNews` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.activeStockNewsIndustry = stockNewsEventIndustry;
        this.activeStockNewsTrend = stockNewsEventTrend;
        this.activeStockNewsRefreshesLeft = 2;
        this.stockNewsEventDay = -1;
    }

    public boolean hasActiveStockNewsForIndustry(String industry) {
    // 해설: `hasActiveStockNewsForIndustry` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return industry != null
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && industry.equals(activeStockNewsIndustry)
                && activeStockNewsTrend != null
                && getActiveStockNewsRefreshesLeft() > 0;
    }

    public String getActiveStockNewsIndustry() {
    // 해설: `getActiveStockNewsIndustry` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeStockNewsIndustry;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getActiveStockNewsTrend() {
    // 해설: `getActiveStockNewsTrend` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeStockNewsTrend;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getActiveStockNewsRefreshesLeft() {
    // 해설: `getActiveStockNewsRefreshesLeft` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return activeStockNewsRefreshesLeft == null ? 0 : activeStockNewsRefreshesLeft;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void consumeStockNewsRefresh() {
    // 해설: `consumeStockNewsRefresh` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (getActiveStockNewsRefreshesLeft() <= 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        activeStockNewsRefreshesLeft = getActiveStockNewsRefreshesLeft() - 1;
        if (activeStockNewsRefreshesLeft <= 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            activeStockNewsIndustry = null;
            activeStockNewsTrend = null;
            activeStockNewsRefreshesLeft = 0;
        }
    }

    private int valueOrImpossible(Integer value) {
    // 해설: `valueOrImpossible` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return value == null ? -1 : value;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    private int currentScheduleCycle() {
    // 해설: `currentScheduleCycle` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int completedYears = Math.max(0, getElapsedDays() - getDay()) / 365;
        return completedYears * 12 + month;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    private int clampPercent(int value) {
    // 해설: `clampPercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Math.max(0, Math.min(100, value));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    private boolean hasLegacyDefaultChances() {
    // 해설: `hasLegacyDefaultChances` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Integer.valueOf(40).equals(moveInChancePercent)
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                && Integer.valueOf(20).equals(moveOutChancePercent)
                && Integer.valueOf(30).equals(repairRequestChancePercent);
    }

    private int daysInMonth(int month) {
    // 해설: `daysInMonth` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return switch (month) {
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
            case 2 -> 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
}
```