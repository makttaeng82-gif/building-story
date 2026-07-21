# StockService 설명

파일: `src/main/java/com/game/buildingstory/service/StockService.java`

## 역할

`StockService`는 주식 컨텐츠 전체를 담당한다.

담당 기능:

- 주식 개방 예약
- 주식 개방 이벤트 생성
- 종목 초기 가격 생성
- 5일마다 가격 갱신
- 개인 현금/증권계좌 예수금 입출금
- 매수/매도
- 보유요약 계산
- 거래내역 조회
- 업종 호황/불황 뉴스 이벤트
- SVG 캔들 좌표 계산

## 주요 상수

```java
private static final int UPDATE_INTERVAL_DAYS = 5;
```

주가 갱신 주기다. `elapsedDays` 기준으로 5일마다 갱신된다.

```java
private static final long STOCK_UNLOCK_NET_WORTH = 3_000_000_000L;
private static final int STOCK_UNLOCK_REPUTATION = 8_250;
```

주식 기본 기능은 순자산 30억원과 평판 8,250을 함께 충족해야 개방 예약된다. 평판 8,250은 부산 4단계 해금값이다.

```java
private static final double TRADE_FEE_RATE = 0.0025;
```

거래 수수료 0.25%.

## 주요 메서드

### `ensureUnlockSchedule(Player player)`

부산 후반부의 평판과 순자산 조건을 만족하면 주식 개방일을 예약한다.

```java
if (player.getReputation() >= STOCK_UNLOCK_REPUTATION && netWorth(player) >= STOCK_UNLOCK_NET_WORTH) {
    player.scheduleStockUnlock(player.getElapsedDays() + 2);
}
```

원리:

- 두 조건을 충족해도 바로 주식이 열리지는 않는다.
- 현재 elapsedDays + 2일에 개방되도록 예약한다.

### `activateUnlockNoticeIfDue(Player player)`

예약된 날짜가 되었으면 주식 개방 이벤트를 만든다.

흐름:

1. 개방 예약 확인.
2. 이미 표시했는지 확인.
3. 다른 이벤트가 활성화되어 있으면 대기.
4. 주식 컨텐츠 unlock.
5. 최초 가격 이력 생성.
6. `GameEvent` 저장.
7. 플레이어 일시정지.

### `processPriceUpdates(Player player)`

5일마다 모든 종목 가격을 갱신한다.

핵심:

```java
int lastUpdateDay = stockPriceHistoryRepository
        .findFirstByPlayerOrderByElapsedDaysDescIdDesc(player)
        .map(StockPriceHistory::getElapsedDays)
        .orElse(player.getElapsedDays());
```

가장 최근 가격 이력의 elapsedDays를 찾는다.

```java
if (player.getElapsedDays() - lastUpdateDay < UPDATE_INTERVAL_DAYS) {
    return;
}
```

5일이 안 지났으면 갱신하지 않는다.

```java
stockCatalog.all().forEach(stock -> appendNextHistory(player, stock, marketEffectPercent));
```

모든 종목을 같은 시점에 갱신한다.

### `appendNextHistory(...)`

종목 하나의 새 캔들 데이터를 만든다.

```java
long open = latest.getClosePrice();
```

이번 캔들의 시작가는 직전 종가다.

```java
long close = Math.max(1L, Math.round(open * (100.0 + changePercent) / 100.0));
```

변동률을 적용해 종가를 만든다. 가격은 최소 1코인.

```java
long high = ...
long low = ...
```

고가/저가는 시각적으로 캔들처럼 보이도록 추가 변동을 준다.

### `stockChangePercent(...)`

주가 변동률을 계산한다.

구성 요소:

- 시장 전체 분위기
- 업종 뉴스 효과
- 최근 추세 효과
- 종목 위험도 노이즈
- 희귀 급등/급락 충격

```java
double rawPercent = marketEffectPercent
        + industryEffectPercent(player, stock)
        + trendEffectPercent(player, stock)
        + randomChangePercent(...)
        + shockEffectPercent;
```

여러 효과를 더해 최종 변동률을 만든다.

### `buyStock(...)`

현재가로 즉시 매수한다.

흐름:

1. 수량 검증.
2. 주식 unlock 검증.
3. 종목 조회.
4. 현재가 조회.
5. 총액 계산.
6. 수수료 계산.
7. 코인 차감.
8. 보유 주식 평균단가 갱신.
9. 거래내역 저장.

```java
long fee = tradeFee(grossAmount);
long totalCost = grossAmount + fee;
```

매수는 수수료까지 포함해 코인이 필요하다.

### `sellStock(...)`

현재가로 즉시 매도한다.

흐름:

1. 수량 검증.
2. 보유 수량 확인.
3. 현재가 조회.
4. 수수료 차감 후 지급액 계산.
5. 보유 주식 수량 감소.
6. 코인 증가.
7. 거래내역 저장.

```java
long payout = Math.max(0, grossAmount - fee);
```

매도 지급액은 매도금액 - 수수료다.

### `holdingSummary(Player player)`

보유 종목 전체 평가손익을 계산한다.

```java
long totalCost = ownedQuotes.stream()
        .mapToLong(quote -> quote.averagePrice() * quote.quantity())
        .sum();
```

평균단가 * 수량 = 매입원가.

```java
long totalValuation = ownedQuotes.stream()
        .mapToLong(quote -> quote.currentPrice() * quote.quantity())
        .sum();
```

현재가 * 수량 = 평가금액.

### `quote(...)`

종목 화면 표시용 `StockQuoteView`를 만든다.

중요 원리:

- Entity를 그대로 화면에 넘기지 않는다.
- 화면에 필요한 텍스트, 퍼센트, 캔들 좌표를 미리 계산한다.

### `candleViews(...)`

SVG 캔들 좌표를 계산한다.

```java
int openY = (int) Math.round(priceY(row.getOpenPrice(), scale));
```

가격을 SVG y좌표로 변환한다.

차트에서 높은 가격은 위에 있어야 하므로 `priceY`는 가격이 클수록 y가 작아지게 계산한다.

## 주의점

- 주식 가격 갱신은 모든 종목이 동시에 되어야 한다.
- `OwnedStock`은 보유 수량/평균단가만 저장한다.
- 현재가는 `StockPriceHistory`에서 가져온다.
- 거래내역은 `StockTradeHistory`에 별도로 저장한다.
