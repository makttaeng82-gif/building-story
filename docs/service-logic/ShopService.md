# ShopService 설명

파일: `src/main/java/com/game/buildingstory/service/ShopService.java`

## 역할

상점 관련 기능을 담당한다.

담당 기능:

- 명품 목록 조회
- 선물 목록 조회
- 기부
- 명품 구매
- 선물 구매
- 비서에게 선물 주기
- 보유 수량 계산

## 주요 메서드

### `luxuryItems()`

명품 카탈로그 목록을 반환한다.

### `giftItems()`

선물 카탈로그 목록을 반환한다.

### `donate(long playerId, int multiplier)`

기부를 처리한다.

현금을 평판으로 바꾼다.

### `buyLuxuryItem(long playerId, String itemKey)`

명품을 구매한다.

흐름:

1. 플레이어 조회.
2. 일시정지 확인.
3. 아이템 스펙 조회.
4. 이미 구매했는지 확인.
5. 현금 차감.
6. OwnedLuxuryItem 저장.
7. 평판 보상.
8. 기록 저장.

### `buyGiftItem(long playerId, String giftKey, int quantity)`

선물 아이템을 구매한다.

선물은 여러 개 보유 가능하므로 기존 `OwnedGiftItem`이 있으면 수량을 증가시킨다.

### `giveGiftToSecretary(...)`

보유 선물을 비서에게 준다.

검증:

- 선물 보유 수량 충분.
- 비서 소유자 일치.
- 선물의 호감도 구간에 맞음.
- 현재 구간에서 줄 수 있는 최대 수량 초과하지 않음.

성공 시:

- 선물 수량 감소.
- 비서 호감도 경험치 증가.
- 기록 저장.

## 주의점

- 명품은 보통 1회 구매.
- 선물은 수량형 아이템.
- 선물마다 적용 가능한 호감도 구간이 있다.

