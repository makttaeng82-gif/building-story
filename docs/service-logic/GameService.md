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
4. `player.advanceDay()`로 날짜 증가.
5. 수리 요청 정리.
6. 월세/월급/대출 등 정산.
7. 매물 갱신일이면 매물 갱신.
8. 칭호 갱신.
9. 주식 개방 이벤트 확인.
10. 주가 갱신.
11. 주식 업종 뉴스 확인.
12. 주식 화면이면 도시 이벤트 표시만 지연.
13. 도시 이벤트, 비서 이벤트, 경매 이벤트 확인.

핵심 코드:

```java
player.advanceDay();
```

서버에서 하루를 증가시키는 유일한 핵심 지점이다.

```java
stockService.processPriceUpdates(player);
```

주식 화면이 아니어도 주식 가격은 같은 시간축으로 갱신된다.

```java
if (deferCityEvents) {
```

주식 화면에서 도시 이벤트 모달을 바로 띄우지 않기 위한 분기다.

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

