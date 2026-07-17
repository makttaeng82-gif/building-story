# EventFlowService 설명

파일: `src/main/java/com/game/buildingstory/service/EventFlowService.java`

## 역할

게임 이벤트 모달의 생성과 완료 처리를 담당한다.

이벤트 예:

- 첫 임차인 이벤트
- 퇴사 이벤트
- 자동 퇴사 이벤트
- 비서 임차인 이벤트 연결
- 주식/부동산 뉴스 이벤트 표시

## 주요 메서드

### `activeEvent(Player player)`

현재 표시 중인 ACTIVE 이벤트를 찾는다.

### `activateEvent(Player player, GameEventDefinition definition)`

고정 이벤트 정의를 실제 `GameEvent` 엔티티로 저장한다.

### `completeEvent(long playerId, long eventId)`

이벤트 확인 버튼을 눌렀을 때 실행된다.

흐름:

1. 플레이어 조회.
2. 이벤트 조회.
3. 이벤트 소유자 확인.
4. effect 적용.
5. 이벤트 완료 처리.
6. 플레이어 pause 해제.

### `applyEventEffect(Player player, GameEvent event)`

event의 effect 문자열을 보고 실제 효과를 적용한다.

문자열 prefix 방식:

```text
SECRETARY_TENANT_REQUEST:secretary-2
SECRETARY_TENANT_HIRE:secretary-6
```

prefix는 처리 종류, 뒤쪽 값은 대상 비서다.

### `resign(long playerId)`

플레이어가 직접 퇴사 이벤트를 시작한다.

### `processAutoResignation(Player player)`

특정 날짜가 지나면 자동 퇴사 이벤트를 만든다.

### `completeResignation(...)`

퇴사 보상금을 지급하고 직장 상태를 해제한다.

## 주의점

- 이벤트는 하나씩 처리해야 한다.
- effect 문자열과 처리 코드가 일치해야 한다.
- 이벤트 완료 후 pause 상태를 풀어야 자동 시간이 다시 흐른다.

