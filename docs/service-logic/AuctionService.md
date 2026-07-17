# AuctionService 설명

파일: `src/main/java/com/game/buildingstory/service/AuctionService.java`

## 역할

경매 이벤트를 담당한다.

담당 기능:

- 활성 경매 조회
- 경매 랜덤 생성
- 입찰 처리
- 경매 취소
- 결과 완료 처리

## 주요 메서드

### `activeAuction(Player player)`

플레이어의 활성 경매를 찾는다.

특징:

- 조회 시 만료 여부도 확인한다.
- 만료된 ACTIVE 경매는 완료 처리한다.
- COMPLETED 상태는 반환하지 않는다.

### `tryActivate(Player player)`

하루 진행 중 낮은 확률로 경매를 생성한다.

조건:

- 확률 체크 성공.
- 현재 도시 보유 건물 수가 8채 미만.
- 현재 도시에 건물 스펙이 존재.

### `bid(long playerId, long auctionId, int rate)`

입찰 처리다.

`rate`는 기준가 대비 몇 %로 입찰할지 의미한다.

흐름:

1. 플레이어 조회.
2. 경매 조회.
3. 소유자 검증.
4. 입찰 금액 계산.
5. 현금 부족 확인.
6. 성공 확률 계산.
7. 성공 시 건물 지급.
8. 실패/성공 기록 저장.

### `successChance(int rate)`

입찰률에 따라 성공 확률을 계산한다.

높게 입찰할수록 성공 확률이 올라간다.

### `cancel(...)`

활성 경매를 취소한다.

### `completeResult(...)`

결과 확인이 끝난 경매를 완료 상태로 바꾼다.

## 주의점

- 경매는 제한 시간이 있다.
- 경매 결과 상태와 활성 상태를 구분해야 한다.
- 성공 시 일반 구매와 비슷하게 OwnedBuilding이 생성된다.

