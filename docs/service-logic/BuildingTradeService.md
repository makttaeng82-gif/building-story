# BuildingTradeService 설명

파일: `src/main/java/com/game/buildingstory/service/BuildingTradeService.java`

## 역할

부동산 거래 규칙을 담당한다.

담당 기능:

- 현재 도시 매물 조회
- 건물 구매
- 건물 판매
- 건물 수리
- 매물 자동 생성/갱신
- 구매 쿨다운 계산
- 매각 가능일 계산
- 건물 이미지 경로 계산
- 비서의 건물 대기시간 감소 효과 적용

## 핵심 개념

### BuildingOffer

아직 구매하지 않은 시장 매물이다.

### OwnedBuilding

플레이어가 구매해서 보유 중인 건물이다.

### PurchaseCooldown

같은 건물을 너무 빨리 다시 구매하지 못하게 막는 기록이다.

## 주요 메서드

### `offers(Player player)`

현재 도시의 매물 목록을 가져온다.

```java
return offerRepository.findByPlayerAndCityOrderById(player, player.getCurrentCity());
```

플레이어와 현재 도시를 조건으로 조회한다.

### `ownedBuildings(Player player)`

현재 도시의 보유 건물 목록을 가져온다.

### `buyOffer(long playerId, long offerId, boolean loanPurchase)`

매물을 구매한다.

흐름:

1. 플레이어 조회.
2. 일시정지 확인.
3. 매물 조회.
4. 매물 소유자 검증.
5. 도시 보유 한도 확인.
6. 해금 여부 확인.
7. 구매 쿨다운 확인.
8. 현금구매/대출구매 비용 계산.
9. 대출 한도 확인.
10. 현금 차감.
11. `OwnedBuilding` 생성.
12. 대출구매면 `Loan` 생성.
13. 기록 저장.
14. 구매 쿨다운 시작.
15. 비서 임차인 이벤트 확인.

문법 예:

```java
if (!offer.getPlayer().getId().equals(player.getId())) {
    throw new IllegalArgumentException("잘못된 매물");
}
```

매물이 현재 플레이어의 것인지 확인한다.

### `sellBuilding(...)`

보유 건물을 판매한다.

핵심:

```java
ValuationStatus valuationStatus = randomValuation();
long sellPrice = building.getMarketPrice() * valuationStatus.rate() / 100;
```

매각 가격은 기준가에 평가 상태 비율을 곱한다.

### `repairBuilding(...)`

수리 요청 상태인 건물을 수리한다.

- 수리 비용 차감.
- 수리 상태 해제.
- 평판 보상 지급.
- 기록 저장.

### `ensureOffers(Player player)`

현재 도시 매물이 없으면 새로 만든다.

메인 화면 렌더링 전에 호출되어 빈 매물 화면을 방지한다.

### `refreshOffers(Player player)`

현재 도시 매물을 새로 생성한다.

후보 제외 조건:

- 이미 보유 중인 슬롯
- 구매 쿨다운 중인 슬롯
- 아직 평판으로 해금되지 않은 슬롯

### `isOfferUnlocked(...)`

해당 매물이 현재 평판에서 구매 가능한지 확인한다.

### `purchaseCooldownDaysLeft(...)`

구매 쿨다운이 며칠 남았는지 계산한다.

### `daysUntilSellable(...)`

매각 가능일까지 며칠 남았는지 계산한다.

비서 효과로 대기시간이 줄 수 있다.

### `buildingImagePath(...)`

도시와 슬롯으로 이미지 경로를 만든다.

예:

```text
/assets/buildings/seoul-4.jpg
```

## 주의점

- 구매는 현금 부족, 대출 한도, 보유 한도, 쿨다운을 모두 확인해야 한다.
- 판매는 보호 임차인/비서 거주 상태면 막아야 한다.
- 매물 갱신은 현재 도시 기준이다.

