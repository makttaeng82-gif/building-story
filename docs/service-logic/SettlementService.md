# SettlementService 설명

파일: `src/main/java/com/game/buildingstory/service/SettlementService.java`

## 역할

하루가 지날 때 자동으로 일어나는 정산을 담당한다.

담당 기능:

- 월초 월세 지급
- 직장 월급 지급
- 비서 월급 처리
- 대출 만기 처리
- 수리 방치 패널티
- 랜덤 입주/퇴거/수리 이벤트
- 도시 부동산 뉴스 이벤트
- 도시 해금 이벤트

## 주요 메서드

### `runDailySettlement(Player player)`

하루 진행 중 경제 정산을 처리한다.

흐름:

1. 월간 랜덤 이벤트 날짜 예약.
2. 매월 1일이면 월초 정산.
3. 임대료 지급.
4. 월급 지급.
5. 비서 월급 처리.
6. 대출 만기 처리.
7. 랜덤 건물 이벤트 처리.
8. 부동산 뉴스 이벤트 처리.
9. 도시 해금 이벤트 처리.

핵심 조건:

```java
if (player.getDay() == 1) {
```

월초에만 실행할 정산을 구분한다.

### `clearVacantRepairRequests(Player player)`

공실 건물의 수리 요청을 제거한다.

이유:

임차인이 없는 건물은 수리 요청을 유지할 필요가 없다.

### `totalMonthlyRent(Player player)`

현재 보유 건물의 월세 총합을 계산한다.

### `effectiveMonthlyRent(Player player, OwnedBuilding building)`

기본 월세에 비서 보너스를 적용한 실제 월세를 계산한다.

### `moveInChancePercent`, `moveOutChancePercent`, `repairRequestChancePercent`

도시별 입주/퇴거/수리 확률을 계산한다.

기본 확률에 비서 효과가 더해지거나 빠진다.

### `ensureMonthlyEventSchedule(Player player)`

이번 달 랜덤 이벤트 날짜를 예약한다.

왜 저장하는가:

- 매 tick마다 랜덤으로 뽑으면 날짜가 흔들린다.
- DB에 저장하면 새로고침 후에도 같은 달 일정이 유지된다.

### `runMonthlyRandomBuildingEvent(Player player)`

예약된 날짜에 입주/퇴거/수리 이벤트를 실행한다.

### `attemptMoveIn(Player player)`

공실 건물 중 하나에 임차인을 입주시킨다.

### `attemptMoveOut(Player player)`

입주 중인 건물 중 하나에서 임차인을 퇴거시킨다.

비서 방어 효과가 있으면 퇴거가 막힐 수 있다.

### `attemptRepairRequest(Player player)`

입주 중인 건물에 수리 요청을 발생시킨다.

### `processRepairNeglect(Player player)`

수리를 방치한 건물에 패널티를 적용한다.

### `activateMarketNews(Player player)`

도시 부동산 뉴스 이벤트를 활성화한다.

뉴스 효과:

- 상승 뉴스: 매물 가격 상승
- 하락 뉴스: 매물 가격 하락
- 다음 2번의 매물 갱신에 적용

## 주의점

- 월초 정산과 일일 이벤트를 섞어 처리하므로 실행 순서가 중요하다.
- 랜덤 이벤트는 반드시 월별 예약 값을 저장해야 한다.
- 뉴스 효과는 날짜가 아니라 매물 갱신 횟수 기준으로 줄어든다.

