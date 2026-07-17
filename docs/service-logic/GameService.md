# GameService 설명

파일: `src/main/java/com/game/buildingstory/service/GameService.java`

## 역할

`GameService`는 게임 전체 기능의 입구다.

컨트롤러가 부동산, 주식, 비서, 경매 서비스를 직접 호출하지 않고 대부분 `GameService`를 거치는 이유는 다음과 같다.

- 요청 흐름을 한 곳에서 관리한다.
- 하루 진행 순서를 고정한다.
- 컨트롤러가 비즈니스 규칙을 알 필요 없게 한다.
- 화면에서 필요한 값들을 여러 서비스에서 가져와 제공한다.

## 핵심 문법

```java
@Service
public class GameService {
```

- `@Service`: Spring이 이 클래스를 서비스 Bean으로 등록한다.
- `public class`: 다른 패키지에서 사용할 수 있는 클래스.

```java
private final StockService stockService;
```

- `private`: 클래스 내부에서만 사용.
- `final`: 생성자에서 한 번 받은 뒤 바꾸지 않음.
- 의존성 주입 대상이다.

```java
@Transactional
public String tick(long playerId, boolean deferCityEvents) {
```

- `@Transactional`: DB 변경을 하나의 작업으로 묶는다.
- `String`: 처리 결과 메시지나 이벤트 신호를 반환한다.
- `deferCityEvents`: 주식 화면에서 도시 이벤트 표시를 미룰지 결정한다.

## 주요 메서드

### `player(long playerId)`

```java
return playerRepository.findById(playerId).orElseThrow();
```

플레이어 ID로 `Player` 엔티티를 찾는다.

- `findById`: Repository 기본 조회 메서드.
- `orElseThrow`: 값이 없으면 예외 발생.

읽기 전용 조회라 `@Transactional(readOnly = true)`가 붙어 있다.

### `completeStory(long playerId)`

스토리 완료 처리다.

실행 흐름:

1. 플레이어 조회.
2. 이미 스토리를 봤는지 확인.
3. `player.completeStory()`로 초기 상태 설정.
4. 청주 첫 원룸을 지급.
5. 첫 매물을 생성.

핵심 원리:

```java
if (!player.isStorySeen()) {
```

한 번만 실행되게 막는다.

### `sideJob(long playerId)`

부업 버튼 처리다.

```java
if (player.isPaused()) {
    return pausedActionMessage();
}
```

게임이 일시정지면 행동을 막는다.

```java
player.addSideIncome(SIDE_JOB_REWARD);
```

현금 증가와 월간 부업 수입 누적을 `Player` 엔티티에 위임한다.

### `tick(long playerId, boolean deferCityEvents)`

가장 중요한 메서드다.

하루 진행 순서:

1. 플레이어 조회.
2. 일시정지면 종료.
3. 경매가 이미 있으면 경매 신호 반환.
4. `player.advanceDay()`로 날짜를 정확히 한 번 증가.
5. `DailyGameOrchestrator`에 날짜 이후 처리를 위임.
6. 정산, 경매 차단, 주식, 도시 이벤트 프로세서를 순서대로 실행.
7. 프로세서가 `EVENT:` 또는 `AUCTION:` 신호를 반환하면 이후 단계를 중단.

핵심 코드:

```java
player.advanceDay();
```

날짜 증가는 `GameService`만 담당한다. 각 콘텐츠 프로세서는 날짜를 직접 증가시키지 않는다.

```java
return dailyGameOrchestrator.process(player, deferCityEvents);
```

`DailyGameOrchestrator`는 `DailyGameProcessor.order()` 값으로 실행 순서를 고정한다.

| 순서 | 프로세서 | 역할 |
| --- | --- | --- |
| 100 | `DailySettlementProcessor` | 정산, 자동 퇴사, 매물과 칭호 갱신 |
| 200 | `DailyAuctionGateProcessor` | 이미 활성화된 경매 우선 처리 |
| 300 | `DailyStockProcessor` | 주식 해금, 주가 갱신, 업종 뉴스 |
| 400 | `DailyCityEventProcessor` | 도시·비서 이벤트와 신규 경매 |

새 콘텐츠가 같은 날짜 흐름을 사용하려면 `DailyGameProcessor` 구현을 추가한다. 이 방식은
`GameService`의 날짜 증가 코드를 복제하지 않으므로 하루가 두 번 지나는 문제를 예방한다.

주식 화면의 도시 이벤트 지연 분기는 `DailyCityEventProcessor`가 담당한다.

### 위임 메서드들

예:

```java
public String buyOffer(long playerId, long offerId, boolean loanPurchase) {
    return buildingTradeService.buyOffer(playerId, offerId, loanPurchase);
}
```

`GameService`는 외부 입구 역할만 하고, 실제 부동산 구매 규칙은 `BuildingTradeService`에 위임한다.

이런 메서드가 많은 이유:

- 컨트롤러는 `GameService`만 알면 된다.
- 내부 서비스 구조를 바꿔도 컨트롤러 변경이 줄어든다.

## 주의점

- `tick` 순서를 바꾸면 게임 밸런스가 바뀐다.
- `player.advanceDay()`를 다른 곳에서 또 호출하면 하루가 2번 지날 수 있다.
- 주식 화면에서 도시 이벤트를 지연시키는 `deferCityEvents` 규칙을 유지해야 한다.
